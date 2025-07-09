package com.example.music_community;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.music_community.adapter.PlayerPagerAdapter;
import com.example.music_community.model.MusicInfo;
import com.google.android.material.snackbar.Snackbar;
import android.content.SharedPreferences;

import java.util.List;
import java.util.Locale;

// 实现 MusicPlaybackFragmentListener 和 LyricFragment.OnLyricFragmentInteractionListener 接口
public class MusicPlayerActivity extends AppCompatActivity implements MusicPlaybackFragmentListener, LyricFragment.OnLyricFragmentInteractionListener {

    private static final String TAG = "MusicPlayerActivity";

    // UI 控件
    private ImageView ivClosePlayer;
    private ViewPager2 viewPagerPlayer;
    private ConstraintLayout musicPlayerRootLayout;

    private ImageView ivFavorite;
    private boolean isFavorite = false; // 默认未收藏
    private MusicInfo currentMusic; // 当前播放的音乐
    private static final String PREFS_NAME = "MusicPrefs"; // SharedPreferences 文件名
    private static final String KEY_FAVORITE = "favorite_"; // 收藏状态的键

    // 底部控制面板的UI
    private TextView tvPlayerMusicName;
    private TextView tvPlayerAuthor;
    private TextView tvCurrentTime;
    private SeekBar seekBarMusic;
    private TextView tvTotalTime;
    private ImageView ivLoopMode;
    private ImageView ivPreviousSong;
    private ImageView ivPlayPause;
    private ImageView ivNextSong;
    private ImageView ivSongList;

    // MusicPlayerService 相关
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;

    // ViewPager2 适配器
    private PlayerPagerAdapter playerPagerAdapter;

    // ViewPager2 页面切换监听器
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    // 持有 MusicPlaybackFragment 和 LyricFragment 的引用
    private MusicPlaybackFragment musicPlaybackFragmentInstance;
    private LyricFragment lyricFragmentInstance; // 【新增】

    // 用于存储待处理的音乐信息和播放状态
    private MusicInfo pendingMusicInfo;
    private Boolean pendingIsPlaying;
    private Integer pendingCurrentPosition; // 【新增】用于传递给歌词Fragment的进度

    // Service 的监听器引用
    private MusicPlayerService.OnMusicPlayerEventListener musicPlayerListener;

    // 用于绑定 Service 的 ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "MusicPlayerService 已连接");
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;

            musicPlayerService.addOnMusicPlayerEventListener(musicPlayerListener);

            // 连接后立即用 Service 的当前状态更新 UI
            MusicInfo currentMusic = musicPlayerService.getCurrentMusic();
            if (currentMusic != null) {
                // 立即切换到封面页，防止页面状态不一致
                if (viewPagerPlayer.getCurrentItem() != 0) {
                    viewPagerPlayer.setCurrentItem(0, false);
                }
                updateMusicInfoUI(currentMusic);
                updatePlayPauseButton();
                updateLoopModeIcon(musicPlayerService.getLoopMode());
                startUpdatingSeekBar();

                // 同步 Fragment 状态
                pendingMusicInfo = currentMusic;
                pendingIsPlaying = musicPlayerService.isPlaying();
                pendingCurrentPosition = musicPlayerService.getCurrentPosition(); // 【新增】
                syncFragmentState();

                MusicPlayerActivity.this.currentMusic = currentMusic;
                isFavorite = getFavoriteState(MusicPlayerActivity.this.currentMusic.getId());
                updateFavoriteIcon();

            } else {
                Toast.makeText(MusicPlayerActivity.this, "当前无播放歌曲", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "MusicPlayerService 连接断开");
            isServiceBound = false;
            musicPlayerService = null;
            stopUpdatingSeekBar();

            pendingMusicInfo = null;
            pendingIsPlaying = null;
            pendingCurrentPosition = null; // 【新增】

            if (musicPlaybackFragmentInstance != null) {
                musicPlaybackFragmentInstance.stopCoverAnimation();
            }
            musicPlaybackFragmentInstance = null;

            if (lyricFragmentInstance != null) { // 【新增】清除歌词Fragment状态
                lyricFragmentInstance.updateLyricDisplay(null, 0);
            }
            lyricFragmentInstance = null;
        }
    };

    // Handler 用于定时更新进度条
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (isServiceBound && musicPlayerService != null) {
                updateSeekBarProgress();
                // 【新增】实时更新歌词Fragment的播放进度
                if (lyricFragmentInstance != null) {
                    lyricFragmentInstance.updateLyricDisplay(musicPlayerService.getCurrentMusic(), musicPlayerService.getCurrentPosition());
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        Log.d(TAG, "onCreate: Activity created.");

        initViews();
        initMusicPlayerListener();
        setupListeners();
        initViewPager();

        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 绑定服务后，读取收藏状态
        if (isServiceBound && musicPlayerService != null) {
            currentMusic = musicPlayerService.getCurrentMusic();
            if (currentMusic != null) {
                isFavorite = getFavoriteState(currentMusic.getId());
                updateFavoriteIcon();
            }
        }
    }

    /**
     * 从本地存储读取收藏状态
     * @param musicId 音乐ID
     * @return 收藏状态
     */
    private boolean getFavoriteState(Long musicId) {
        if (musicId == null) return false; // 避免 null ID
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_FAVORITE + musicId, false);
    }

    /**
     * 将收藏状态保存到本地存储
     * @param musicId 音乐ID
     * @param favorite 收藏状态
     */
    private void saveFavoriteState(Long musicId, boolean favorite) {
        if (musicId == null) return; // 避免 null ID
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_FAVORITE + musicId, favorite);
        editor.apply();
    }

    /**
     * 更新收藏图标
     */
    private void updateFavoriteIcon() {
        if (ivFavorite == null) return;
        if (isFavorite) {
            ivFavorite.setImageResource(R.drawable.ic_favorite); // 已收藏图标
        } else {
            ivFavorite.setImageResource(R.drawable.ic_favorite_border); // 未收藏图标
        }
    }

    // 初始化服务监听器
    private void initMusicPlayerListener() {
        musicPlayerListener = new MusicPlayerService.OnMusicPlayerEventListener() {
            @Override
            public void onMusicPrepared(MusicInfo musicInfo) {
                if (viewPagerPlayer.getCurrentItem() != 0) {
                    viewPagerPlayer.setCurrentItem(0, false);
                }
                updateMusicInfoUI(musicInfo);
                updatePlayPauseButton();
                startUpdatingSeekBar();
                pendingMusicInfo = musicInfo;
                pendingIsPlaying = true;
                pendingCurrentPosition = 0; // 歌曲刚准备好，进度从0开始
                syncFragmentState();

                MusicPlayerActivity.this.currentMusic = musicInfo;
                isFavorite = getFavoriteState(MusicPlayerActivity.this.currentMusic.getId());
                updateFavoriteIcon();
            }

            @Override
            public void onMusicPlayStatusChanged(boolean isPlaying, MusicInfo musicInfo) {
                updatePlayPauseButton();
                pendingMusicInfo = musicInfo;
                pendingIsPlaying = isPlaying;
                if (isServiceBound && musicPlayerService != null) {
                    pendingCurrentPosition = musicPlayerService.getCurrentPosition(); // 获取当前真实进度
                }
                syncFragmentState();
            }

            @Override
            public void onMusicCompleted(MusicInfo nextMusicInfo) {
                if (viewPagerPlayer.getCurrentItem() != 0) {
                    viewPagerPlayer.setCurrentItem(0, false);
                }
                updateMusicInfoUI(nextMusicInfo);
                updatePlayPauseButton();
                pendingMusicInfo = nextMusicInfo;
                pendingIsPlaying = true;
                pendingCurrentPosition = 0; // 新歌曲开始，进度从0开始
                syncFragmentState();
            }

            @Override
            public void onMusicError(String errorMessage) {
//                Toast.makeText(MusicPlayerActivity.this, "播放出错: " + errorMessage, Toast.LENGTH_SHORT).show();

                // 更新待处理状态并尝试同步
                pendingMusicInfo = musicPlayerService.getCurrentMusic();
                pendingIsPlaying = false; // 播放出错，动画应停止
                pendingCurrentPosition = 0; // 进度重置
                syncFragmentState(); // 尝试立即同步状态
            }

            @Override
            public void onLoopModeChanged(MusicPlayerService.LoopMode newMode) {
                updateLoopModeIcon(newMode);
                showLoopModeToast(newMode);
            }

            @Override
            public void onPlaylistChanged(List<MusicInfo> newPlaylist) {
                // 播放列表改变时，如果当前正在播放的歌曲还在列表中，则UI不变
                // 如果当前播放的歌曲被删除，则onMusicCompleted或onMusicPlayStatusChanged会处理
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerActivity onDestroy");
        if (isServiceBound) {
            musicPlayerService.removeOnMusicPlayerEventListener(musicPlayerListener);
            unbindService(serviceConnection);
            isServiceBound = false;
        }

        stopUpdatingSeekBar();

        if (musicPlaybackFragmentInstance != null) {
            musicPlaybackFragmentInstance.stopCoverAnimation();
        }
        musicPlaybackFragmentInstance = null;
        lyricFragmentInstance = null; // 【新增】清除引用

        if (viewPagerPlayer != null && pageChangeCallback != null) {
            viewPagerPlayer.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    // 重写 finish 方法以应用退出动画
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);
    }

    /**
     * 初始化所有 UI 控件
     */
    private void initViews() {
        ivClosePlayer = findViewById(R.id.iv_close_player);
        viewPagerPlayer = findViewById(R.id.view_pager_player);

        tvPlayerMusicName = findViewById(R.id.tv_player_music_name);
        tvPlayerAuthor = findViewById(R.id.tv_player_author);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        seekBarMusic = findViewById(R.id.seek_bar_music);
        tvTotalTime = findViewById(R.id.tv_total_time);
        ivLoopMode = findViewById(R.id.iv_loop_mode);
        ivPreviousSong = findViewById(R.id.iv_previous_song);
        ivPlayPause = findViewById(R.id.iv_play_pause);
        ivNextSong = findViewById(R.id.iv_next_song);
        ivSongList = findViewById(R.id.iv_song_list);

        ivFavorite = findViewById(R.id.iv_favorite);

        musicPlayerRootLayout = findViewById(R.id.music_player_root_layout);
    }

    /**
     * 初始化 ViewPager2
     */
    private void initViewPager() {
        playerPagerAdapter = new PlayerPagerAdapter(this);
        viewPagerPlayer.setAdapter(playerPagerAdapter);
        viewPagerPlayer.setUserInputEnabled(true);

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d(TAG, "ViewPager2: onPageSelected, position: " + position);

                if (position == 0) { // 切换到封面页
                    if (musicPlayerService != null) {
                        pendingMusicInfo = musicPlayerService.getCurrentMusic();
                        pendingIsPlaying = musicPlayerService.isPlaying();
                        pendingCurrentPosition = musicPlayerService.getCurrentPosition(); // 【新增】
                        syncFragmentState();
                    }
                } else { // 切换到歌词页
                    if (musicPlaybackFragmentInstance != null) {
                        musicPlaybackFragmentInstance.stopCoverAnimation();
                    }
                    if (musicPlayerService != null && lyricFragmentInstance != null) { // 【新增】确保歌词页显示时更新歌词
                        lyricFragmentInstance.updateLyricDisplay(musicPlayerService.getCurrentMusic(), musicPlayerService.getCurrentPosition());
                    }
                }
            }
        };
        viewPagerPlayer.registerOnPageChangeCallback(pageChangeCallback);
    }

    /**
     * 设置各个 UI 控件的点击监听器
     */
    private void setupListeners() {
        ivClosePlayer.setOnClickListener(v -> {
            finish();
        });

        ivLoopMode.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                musicPlayerService.switchLoopMode();
            }
        });

        ivPreviousSong.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                musicPlayerService.playPrevious();
            }
        });

        ivPlayPause.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                musicPlayerService.togglePlayPause();
            }
        });

        ivNextSong.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                musicPlayerService.playNext();
            }
        });

        ivSongList.setOnClickListener(v -> {
            if (isServiceBound && musicPlayerService != null) {
                PlaylistDialogFragment playlistDialog = new PlaylistDialogFragment();
                playlistDialog.show(getSupportFragmentManager(), "playlist_dialog");
            } else {
                Toast.makeText(this, "播放服务未连接", Toast.LENGTH_SHORT).show();
            }
        });

        ivFavorite.setOnClickListener(v -> {
            if (musicPlayerService != null && musicPlayerService.getCurrentMusic() != null) {
                currentMusic = musicPlayerService.getCurrentMusic(); // 确保获取最新的 currentMusic
                isFavorite = !isFavorite;
                Log.d(TAG, "Favorite status toggled to: " + isFavorite);
                saveFavoriteState(currentMusic.getId(), isFavorite);
                Log.d(TAG, "Favorite state saved for musicId: " + currentMusic.getId());
                updateFavoriteIcon();
                Log.d(TAG, "Favorite icon updated");
                startFavoriteAnimation();
            } else {
                Log.w(TAG, "No current music to favorite.");
            }
        });

        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopUpdatingSeekBar();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (musicPlayerService != null) {
                    musicPlayerService.seekTo(seekBar.getProgress());
                    // 【新增】拖动进度条后，立即更新歌词Fragment的进度
                    if (lyricFragmentInstance != null) {
                        lyricFragmentInstance.updateLyricDisplay(musicPlayerService.getCurrentMusic(), musicPlayerService.getCurrentPosition());
                    }
                }
                startUpdatingSeekBar();
            }
        });
    }

    private void startFavoriteAnimation() {
        float startScale = isFavorite ? 1.0f : 1.2f;
        float endScale = isFavorite ? 1.2f : 1.0f;

        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(ivFavorite, "scaleX", startScale, endScale);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(ivFavorite, "scaleY", startScale, endScale);
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(ivFavorite, "rotationY", 0f, 360f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, rotationAnimator);
        animatorSet.start();
    }

    /**
     * 更新播放/暂停按钮的图标
     */
    private void updatePlayPauseButton() {
        if (ivPlayPause == null) return;
        if (isServiceBound && musicPlayerService != null && musicPlayerService.isPlaying()) {
            ivPlayPause.setImageResource(R.drawable.ic_pause_big);
        } else {
            ivPlayPause.setImageResource(R.drawable.ic_play_big);
        }
    }

    /**
     * 更新歌曲名称、歌手信息、背景主题色和 ViewPager2 中的 Fragment 数据
     * @param musicInfo 当前播放的音乐信息
     */
    private void updateMusicInfoUI(MusicInfo musicInfo) {
        if (musicInfo != null) {
            tvPlayerMusicName.setText(musicInfo.getMusicName());
            tvPlayerAuthor.setText(musicInfo.getAuthor());
            loadBackgroundAndCover(musicInfo.getCoverUrl());

            playerPagerAdapter.setCurrentMusicInfo(musicInfo);
        } else {
            tvPlayerMusicName.setText("未知歌曲");
            tvPlayerAuthor.setText("未知歌手");
            musicPlayerRootLayout.setBackgroundColor(Color.BLACK); // 没有歌曲时设置默认背景
            playerPagerAdapter.setCurrentMusicInfo(null);
        }
    }

    /**
     * 更新进度条和时间显示
     */
    private void updateSeekBarProgress() {
        if (musicPlayerService != null) {
            int currentPosition = musicPlayerService.getCurrentPosition();
            int duration = musicPlayerService.getDuration();

            tvCurrentTime.setText(formatTime(currentPosition));
            tvTotalTime.setText(formatTime(duration));
            seekBarMusic.setMax(duration);
            seekBarMusic.setProgress(currentPosition);
        }
    }

    /**
     * 启动进度条定时更新
     */
    private void startUpdatingSeekBar() {
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.post(updateSeekBarRunnable);
    }

    /**
     * 停止进度条定时更新
     */
    private void stopUpdatingSeekBar() {
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    /**
     * 将毫秒时间格式化为“MM:SS”字符串
     * @param milliseconds 毫秒数
     * @return 格式化后的时间字符串
     */
    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 加载背景主题色
     * @param imageUrl 封面图片URL
     */
    private void loadBackgroundAndCover(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Log.e(TAG, "背景主题色加载失败", e);
                        musicPlayerRootLayout.setBackgroundColor(Color.parseColor("#333333"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                int defaultColor = Color.parseColor("#333333");
                                int dominantColor = palette.getDominantColor(defaultColor);
                                int vibrantColor = palette.getVibrantColor(defaultColor);
                                int darkVibrantColor = palette.getDarkVibrantColor(defaultColor);
                                int mutedColor = palette.getMutedColor(defaultColor);
                                int darkMutedColor = palette.getDarkMutedColor(defaultColor);

                                int bgColor = darkVibrantColor;
                                if (bgColor == defaultColor) {
                                    bgColor = dominantColor;
                                }
                                if (bgColor == defaultColor) {
                                    bgColor = darkMutedColor;
                                }
                                if (bgColor == defaultColor) {
                                    bgColor = mutedColor;
                                }

                                musicPlayerRootLayout.setBackgroundColor(bgColor);
                                Log.d(TAG, "loadBackgroundAndCover: Background color set to: " + String.format("#%06X", (0xFFFFFF & bgColor)));
                            }
                        });
                        return false;
                    }
                })
                .preload(100, 100); // 预加载图片到内存，触发listener回调，而无需实际显示到ImageView
    }

    /**
     * 更新循环模式图标
     * @param mode 当前循环模式
     */
    private void updateLoopModeIcon(MusicPlayerService.LoopMode mode) {
        if (ivLoopMode == null) return;
        switch (mode) {
            case SEQUENCE:
                ivLoopMode.setImageResource(R.drawable.ic_loop_sequence);
                break;
            case SINGLE:
                ivLoopMode.setImageResource(R.drawable.ic_loop_one);
                break;
            case SHUFFLE:
                ivLoopMode.setImageResource(R.drawable.ic_loop_shuffle);
                break;
        }
    }

    /**
     * 显示循环模式切换的 Toast 提示
     * @param mode 当前循环模式
     */
    private void showLoopModeToast(MusicPlayerService.LoopMode mode) {
        String message = "";
        switch (mode) {
            case SEQUENCE:
                message = "顺序播放";
                break;
            case SINGLE:
                message = "单曲循环";
                break;
            case SHUFFLE:
                message = "随机播放";
                break;
        }
        Snackbar.make(ivLoopMode, message, Snackbar.LENGTH_SHORT).show();
    }

    // 实现 MusicPlaybackFragmentListener 接口的方法
    @Override
    public void onMusicPlaybackFragmentReady(MusicPlaybackFragment fragment) {
        this.musicPlaybackFragmentInstance = fragment;
        Log.d(TAG, "onMusicPlaybackFragmentReady: MusicPlaybackFragment instance received.");
        syncFragmentState();
    }

    @Override
    public void onMusicPlaybackFragmentDetached() {
        this.musicPlaybackFragmentInstance = null;
        Log.d(TAG, "onMusicPlaybackFragmentDetached: MusicPlaybackFragment instance cleared.");
    }

    // 【新增】实现 LyricFragment.OnLyricFragmentInteractionListener 接口的方法
    @Override
    public void onLyricFragmentReady(LyricFragment fragment) {
        this.lyricFragmentInstance = fragment;
        Log.d(TAG, "onLyricFragmentReady: LyricFragment instance received.");
        syncFragmentState(); // 当歌词Fragment准备好时，也同步状态
    }

    /**
     * 同步 Fragment 的音乐信息和播放状态
     * 只有当 Fragment 实例可用且有待处理状态时才执行
     */
    private void syncFragmentState() {
        // 同步 MusicPlaybackFragment
        if (musicPlaybackFragmentInstance != null && pendingMusicInfo != null && pendingIsPlaying != null) {
            musicPlaybackFragmentInstance.updateMusicInfoAndPlayState(pendingMusicInfo, pendingIsPlaying);
            Log.d(TAG, "syncFragmentState: Synced pending state to MusicPlaybackFragment: " + pendingMusicInfo.getMusicName() + ", isPlaying: " + pendingIsPlaying);
            // 这里不清除 pendingMusicInfo 和 pendingIsPlaying，因为 LyricFragment 可能也需要它们
        } else if (musicPlaybackFragmentInstance != null && musicPlayerService != null && pendingMusicInfo == null) {
            musicPlaybackFragmentInstance.updateMusicInfoAndPlayState(musicPlayerService.getCurrentMusic(), musicPlayerService.isPlaying());
            Log.d(TAG, "syncFragmentState: Synced current service state to newly ready MusicPlaybackFragment.");
        }

        // 同步 LyricFragment
        // 只有当 LyricFragment 处于可见状态（即 viewPagerPlayer.getCurrentItem() == 1）时才同步歌词
        // 或者当 LyricFragment 刚准备好，且有 pending 状态时，强制同步一次
        if (lyricFragmentInstance != null && pendingMusicInfo != null && pendingCurrentPosition != null) {
            if (viewPagerPlayer.getCurrentItem() == 1 || (pendingMusicInfo != null && pendingCurrentPosition != null)) { // 确保歌词页可见或有待处理数据
                lyricFragmentInstance.updateLyricDisplay(pendingMusicInfo, pendingCurrentPosition);
                Log.d(TAG, "syncFragmentState: Synced pending state to LyricFragment: " + pendingMusicInfo.getMusicName() + ", currentPosition: " + pendingCurrentPosition);
            }
            // 清除 pending 状态，防止重复处理
            pendingMusicInfo = null;
            pendingIsPlaying = null;
            pendingCurrentPosition = null;
        } else if (lyricFragmentInstance != null && musicPlayerService != null && viewPagerPlayer.getCurrentItem() == 1 && pendingMusicInfo == null) {
            // 如果 LyricFragment 刚准备好，且当前就是歌词页，但没有 pending 状态，则使用服务当前状态同步
            lyricFragmentInstance.updateLyricDisplay(musicPlayerService.getCurrentMusic(), musicPlayerService.getCurrentPosition());
            Log.d(TAG, "syncFragmentState: Synced current service state to newly ready LyricFragment (current page).");
        } else {
            Log.d(TAG, "syncFragmentState: Fragment not ready or no valid state to sync. MusicPlaybackFragment Available: " + (musicPlaybackFragmentInstance != null) + ", LyricFragment Available: " + (lyricFragmentInstance != null) + ", PendingInfo: " + (pendingMusicInfo != null) + ", PendingPlay: " + (pendingIsPlaying != null) + ", PendingPos: " + (pendingCurrentPosition != null) + ", Service Connected: " + (musicPlayerService != null) + ", Current Page: " + viewPagerPlayer.getCurrentItem());
        }
    }
}
