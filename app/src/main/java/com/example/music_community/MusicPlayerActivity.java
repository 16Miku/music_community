package com.example.music_community;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.music_community.adapter.PlayerPagerAdapter;
import com.example.music_community.model.MusicInfo;
import com.google.android.material.snackbar.Snackbar;
// import jp.wasabeef.glide.transformations.BlurTransformation; // 不再需要模糊转换

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 实现 MusicPlaybackFragmentListener 接口
public class MusicPlayerActivity extends AppCompatActivity implements MusicPlaybackFragmentListener {

    private static final String TAG = "MusicPlayerActivity";

    // 【移除】不再需要 Intent Extras
    // public static final String EXTRA_MUSIC_LIST = "extra_music_list";
    // public static final String EXTRA_CURRENT_POSITION = "extra_current_position";

    // 【移除】不再需要从 Intent 获取数据
    // private List<MusicInfo> musicList;
    // private int currentPlayPosition;

    // UI 控件
    private ImageView ivClosePlayer;
    private ViewPager2 viewPagerPlayer;
    private ConstraintLayout musicPlayerRootLayout;

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

    // 持有 MusicPlaybackFragment 的引用
    private MusicPlaybackFragment musicPlaybackFragmentInstance;

    // 用于存储待处理的音乐信息和播放状态
    private MusicInfo pendingMusicInfo;
    private Boolean pendingIsPlaying;


    // 【新增】Service 的监听器引用
    private MusicPlayerService.OnMusicPlayerEventListener musicPlayerListener;


    // 用于绑定 Service 的 ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "MusicPlayerService 已连接");
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;

            // 【修改】不再设置播放列表，而是添加监听器
            musicPlayerService.addOnMusicPlayerEventListener(musicPlayerListener);

            // 【修改】连接后立即用 Service 的当前状态更新 UI
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
                syncFragmentState();
            } else {
                // 如果服务没有音乐，可以关闭页面或显示提示
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
            if (musicPlaybackFragmentInstance != null) {
                musicPlaybackFragmentInstance.stopCoverAnimation();
            }
            musicPlaybackFragmentInstance = null;
        }
    };

    // Handler 用于定时更新进度条
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (musicPlayerService != null && musicPlayerService.isPlaying()) {
                updateSeekBarProgress();
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
        // 【移除】getIntentData();
        initMusicPlayerListener(); // 【新增】
        setupListeners();
        initViewPager();

        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }



    // 【新增】初始化服务监听器
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
                syncFragmentState();
            }

            @Override
            public void onMusicPlayStatusChanged(boolean isPlaying, MusicInfo musicInfo) {
                updatePlayPauseButton();
                pendingMusicInfo = musicInfo;
                pendingIsPlaying = isPlaying;
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
                syncFragmentState();
            }







            @Override
            public void onMusicError(String errorMessage) {
                Toast.makeText(MusicPlayerActivity.this, "播放出错: " + errorMessage, Toast.LENGTH_SHORT).show();

                // 更新待处理状态并尝试同步
                pendingMusicInfo = musicPlayerService.getCurrentMusic();
                pendingIsPlaying = false; // 播放出错，动画应停止
                syncFragmentState(); // 尝试立即同步状态
            }

            @Override
            public void onLoopModeChanged(MusicPlayerService.LoopMode newMode) {
                updateLoopModeIcon(newMode);
                showLoopModeToast(newMode);
            }

            @Override
            public void onPlaylistChanged(List<MusicInfo> newPlaylist) {

            }


        };
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerActivity onDestroy");
        if (isServiceBound) {
            musicPlayerService.removeOnMusicPlayerEventListener(musicPlayerListener); // 【修改】移除监听器
            unbindService(serviceConnection);
            isServiceBound = false;
        }

        stopUpdatingSeekBar();

        if (musicPlaybackFragmentInstance != null) {
            musicPlaybackFragmentInstance.stopCoverAnimation();
        }
        musicPlaybackFragmentInstance = null;

        if (viewPagerPlayer != null && pageChangeCallback != null) {
            viewPagerPlayer.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }



    // 【修改】重写 finish 方法以应用退出动画
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

        musicPlayerRootLayout = findViewById(R.id.music_player_root_layout);
    }

//    /**
//     * 获取从上一个 Activity 传递过来的数据
//     */
//    private void getIntentData() {
//        if (getIntent() != null) {
//            musicList = (List<MusicInfo>) getIntent().getSerializableExtra(EXTRA_MUSIC_LIST);
//            currentPlayPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);
//
//            if (musicList == null) {
//                musicList = new ArrayList<>();
//                Log.e(TAG, "传递的音乐列表为空！");
//            }
//            Log.d(TAG, "接收到音乐列表大小: " + musicList.size() + ", 初始播放位置: " + currentPlayPosition);
//        }
//    }

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

                if (position == 0) {
                    if (musicPlayerService != null) {
                        pendingMusicInfo = musicPlayerService.getCurrentMusic();
                        pendingIsPlaying = musicPlayerService.isPlaying();
                        syncFragmentState();
                    }
                } else {
                    if (musicPlaybackFragmentInstance != null) {
                        musicPlaybackFragmentInstance.stopCoverAnimation();
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


            // 点击歌曲列表按钮时，显示 PlaylistDialogFragment
            if (isServiceBound && musicPlayerService != null) {

                PlaylistDialogFragment playlistDialog = new PlaylistDialogFragment();

                playlistDialog.show(getSupportFragmentManager(), "playlist_dialog");

            } else {

                Toast.makeText(this, "播放服务未连接", Toast.LENGTH_SHORT).show();
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
                }
                startUpdatingSeekBar();
            }
        });
    }

//    /**
//     * 显示歌曲列表对话框
//     */
//    private void showSongListDialog() {
//        if (musicList == null || musicList.isEmpty()) {
//            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        StringBuilder songListBuilder = new StringBuilder();
//        for (int i = 0; i < musicList.size(); i++) {
//            MusicInfo song = musicList.get(i);
//            songListBuilder.append(i + 1)
//                    .append(". ")
//                    .append(song.getMusicName())
//                    .append(" - ")
//                    .append(song.getAuthor());
//            if (musicPlayerService != null && musicPlayerService.getCurrentMusic() != null &&
//                    musicPlayerService.getCurrentMusic().equals(song)) {
//                songListBuilder.append(" (当前播放)");
//            }
//            songListBuilder.append("\n");
//        }
//
//        new androidx.appcompat.app.AlertDialog.Builder(this)
//                .setTitle("当前播放列表")
//                .setMessage(songListBuilder.toString())
//                .setPositiveButton("确定", null)
//                .show();
//    }

    /**
     * 更新播放/暂停按钮的图标
     */
    private void updatePlayPauseButton() {
        if (musicPlayerService != null && musicPlayerService.isPlaying()) {
            ivPlayPause.setImageResource(R.drawable.ic_pause_big);
        } else {
            ivPlayPause.setImageResource(R.drawable.ic_play_big);
        }
    }

    /**
     * 更新歌曲名称、歌手信息、背景模糊图和 ViewPager2 中的 Fragment 数据
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
     * 【修改】加载背景主题色
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

    // 【新增】实现 MusicPlaybackFragmentListener 接口的方法

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

    /**
     * 同步 Fragment 的音乐信息和播放状态
     * 只有当 Fragment 实例可用且有待处理状态时才执行
     */
    private void syncFragmentState() {
        if (musicPlaybackFragmentInstance != null && pendingMusicInfo != null && pendingIsPlaying != null) {
            musicPlaybackFragmentInstance.updateMusicInfoAndPlayState(pendingMusicInfo, pendingIsPlaying);
            Log.d(TAG, "syncFragmentState: Synced pending state to fragment: " + pendingMusicInfo.getMusicName() + ", isPlaying: " + pendingIsPlaying);
            pendingMusicInfo = null;
            pendingIsPlaying = null;
        } else if (musicPlaybackFragmentInstance != null && musicPlayerService != null && pendingMusicInfo == null) {
            musicPlaybackFragmentInstance.updateMusicInfoAndPlayState(musicPlayerService.getCurrentMusic(), musicPlayerService.isPlaying());
            Log.d(TAG, "syncFragmentState: Synced current service state to newly ready fragment.");
        } else {
            Log.d(TAG, "syncFragmentState: Fragment not ready or no valid state to sync. Fragment Available: " + (musicPlaybackFragmentInstance != null) + ", PendingInfo: " + (pendingMusicInfo != null) + ", PendingPlay: " + (pendingIsPlaying != null) + ", Service Connected: " + (musicPlayerService != null));
        }
    }
}
