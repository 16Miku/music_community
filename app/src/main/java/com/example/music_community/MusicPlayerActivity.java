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
import jp.wasabeef.glide.transformations.BlurTransformation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// 【修改】实现 MusicPlaybackFragmentListener 接口
public class MusicPlayerActivity extends AppCompatActivity implements MusicPlaybackFragmentListener {

    private static final String TAG = "MusicPlayerActivity";
    public static final String EXTRA_MUSIC_LIST = "extra_music_list";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_position";

    private List<MusicInfo> musicList;
    private int currentPlayPosition;

    // UI 控件声明
    private ImageView ivClosePlayer;
    private ViewPager2 viewPagerPlayer;
    private ImageView ivBackgroundBlur;

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

    // 【新增】持有 MusicPlaybackFragment 的引用
    private MusicPlaybackFragment musicPlaybackFragmentInstance;

    // 【新增】用于存储待处理的音乐信息和播放状态
    private MusicInfo pendingMusicInfo;
    private Boolean pendingIsPlaying; // 使用 Boolean 类型允许 null (表示没有待处理状态)


    // 用于绑定 Service 的 ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "MusicPlayerService 已连接");
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;

            // 【关键】将音乐列表和当前播放索引传递给 Service
            musicPlayerService.setPlayListAndIndex(musicList, currentPlayPosition); // 这会触发音乐播放，并最终导致 onMusicPrepared 被调用

            // 注册 Service 的回调监听器
            musicPlayerService.setOnMusicPlayerEventListener(new MusicPlayerService.OnMusicPlayerEventListener() {
                @Override
                public void onMusicPrepared(MusicInfo musicInfo) {
                    Log.d(TAG, "onMusicPrepared: Music prepared: " + musicInfo.getMusicName());
                    updateMusicInfoUI(musicInfo);
                    updatePlayPauseButton();

                    // 【修改】更新待处理状态并尝试同步
                    pendingMusicInfo = musicInfo;
                    pendingIsPlaying = true; // 音乐准备好，表示将开始播放
                    syncFragmentState(); // 尝试立即同步状态
                }

                @Override
                public void onMusicPlayStatusChanged(boolean isPlaying) {
                    Log.d(TAG, "onMusicPlayStatusChanged: isPlaying = " + isPlaying);
                    updatePlayPauseButton();

                    // 【修改】更新待处理状态并尝试同步
                    pendingMusicInfo = musicPlayerService.getCurrentMusic(); // 获取当前音乐
                    pendingIsPlaying = isPlaying; // 更新播放状态
                    syncFragmentState(); // 尝试立即同步状态
                }

                @Override
                public void onMusicCompleted(MusicInfo nextMusicInfo) {
                    Log.d(TAG, "onMusicCompleted: Music completed, next: " + (nextMusicInfo != null ? nextMusicInfo.getMusicName() : "null"));
                    updateMusicInfoUI(nextMusicInfo);
                    updatePlayPauseButton();

                    // 【修改】更新待处理状态并尝试同步
                    pendingMusicInfo = nextMusicInfo;
                    pendingIsPlaying = true; // 下一首歌曲默认开始播放
                    syncFragmentState(); // 尝试立即同步状态
                }

                @Override
                public void onMusicError(String errorMessage) {
                    Toast.makeText(MusicPlayerActivity.this, "播放出错: " + errorMessage, Toast.LENGTH_SHORT).show();

                    // 【修改】更新待处理状态并尝试同步
                    pendingMusicInfo = musicPlayerService.getCurrentMusic();
                    pendingIsPlaying = false; // 播放出错，动画应停止
                    syncFragmentState(); // 尝试立即同步状态
                }

                @Override
                public void onLoopModeChanged(MusicPlayerService.LoopMode newMode) {
                    updateLoopModeIcon(newMode);
                    showLoopModeToast(newMode);
                }
            });

            // 初始启动时设置待处理状态
            // 此时 MusicPlayerService.isPlaying() 可能仍为 false，因为 prepareAsync 尚未完成
            // 我们将依赖 onMusicPrepared 来触发实际的播放状态更新
            if (musicPlayerService.getCurrentMusic() != null) {
                updateMusicInfoUI(musicPlayerService.getCurrentMusic());
                updatePlayPauseButton();
                updateLoopModeIcon(musicPlayerService.getLoopMode());
                // 设置初始待处理状态，等待 Fragment 报告准备就绪时同步
                pendingMusicInfo = musicPlayerService.getCurrentMusic();
                pendingIsPlaying = musicPlayerService.isPlaying(); // 使用当前服务实际的播放状态
            } else {
                Log.d(TAG, "Service not playing and no current music, UI will be updated when music starts.");
                pendingMusicInfo = null;
                pendingIsPlaying = null;
            }
            syncFragmentState(); // 尝试立即同步一次，如果 Fragment 已经准备好
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "MusicPlayerService 连接断开");
            isServiceBound = false;
            musicPlayerService = null;
            stopUpdatingSeekBar();

            // 【修改】服务断开时，清空待处理状态并停止 Fragment 动画
            pendingMusicInfo = null;
            pendingIsPlaying = null;
            if (musicPlaybackFragmentInstance != null) {
                musicPlaybackFragmentInstance.stopCoverAnimation(); // 直接停止动画
                Log.d(TAG, "onServiceDisconnected: MusicPlaybackFragment animation stopped via instance.");
            }
            musicPlaybackFragmentInstance = null; // 清空 Fragment 引用
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
        getIntentData();
        setupListeners();
        initViewPager();

        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerActivity onDestroy");
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopUpdatingSeekBar();

        // 【修改】Activity 销毁时，停止 Fragment 动画并清空引用
        if (musicPlaybackFragmentInstance != null) {
            musicPlaybackFragmentInstance.stopCoverAnimation();
            Log.d(TAG, "onDestroy: MusicPlaybackFragment animation stopped via instance.");
        }
        musicPlaybackFragmentInstance = null; // 清空 Fragment 引用

        if (viewPagerPlayer != null && pageChangeCallback != null) {
            viewPagerPlayer.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    /**
     * 初始化所有 UI 控件
     */
    private void initViews() {
        ivClosePlayer = findViewById(R.id.iv_close_player);
        viewPagerPlayer = findViewById(R.id.view_pager_player);
        ivBackgroundBlur = findViewById(R.id.iv_background_blur);

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

    /**
     * 获取从上一个 Activity 传递过来的数据
     */
    private void getIntentData() {
        if (getIntent() != null) {
            musicList = (List<MusicInfo>) getIntent().getSerializableExtra(EXTRA_MUSIC_LIST);
            currentPlayPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);

            if (musicList == null) {
                musicList = new ArrayList<>();
                Log.e(TAG, "传递的音乐列表为空！");
            }
            Log.d(TAG, "接收到音乐列表大小: " + musicList.size() + ", 初始播放位置: " + currentPlayPosition);
        }
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

                // 【修改】页面切换时，更新待处理状态并尝试同步
                if (position == 0) { // 封面页
                    if (musicPlayerService != null) {
                        pendingMusicInfo = musicPlayerService.getCurrentMusic();
                        pendingIsPlaying = musicPlayerService.isPlaying();
                        syncFragmentState(); // 尝试同步
                    }
                } else { // 歌词页或其他页面
                    if (musicPlaybackFragmentInstance != null) {
                        musicPlaybackFragmentInstance.stopCoverAnimation(); // 直接停止动画
                        Log.d(TAG, "onPageSelected: MusicPlaybackFragment animation stopped via instance on non-cover page.");
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
            // TODO: 实现关闭动画 (后续步骤实现)
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
            Toast.makeText(this, "显示歌曲列表 ", Toast.LENGTH_SHORT).show();
            showSongListDialog();
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

    /**
     * 显示歌曲列表对话框
     */
    private void showSongListDialog() {
        if (musicList == null || musicList.isEmpty()) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder songListBuilder = new StringBuilder();
        for (int i = 0; i < musicList.size(); i++) {
            MusicInfo song = musicList.get(i);
            songListBuilder.append(i + 1)
                    .append(". ")
                    .append(song.getMusicName())
                    .append(" - ")
                    .append(song.getAuthor());
            if (musicPlayerService != null && musicPlayerService.getCurrentMusic() != null &&
                    musicPlayerService.getCurrentMusic().equals(song)) {
                songListBuilder.append(" (当前播放)");
            }
            songListBuilder.append("\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("当前播放列表")
                .setMessage(songListBuilder.toString())
                .setPositiveButton("确定", null)
                .show();
    }

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

            playerPagerAdapter.setCurrentMusicInfo(musicInfo); // 更新 ViewPager2 适配器数据
        } else {
            tvPlayerMusicName.setText("未知歌曲");
            tvPlayerAuthor.setText("未知歌手");
            ivBackgroundBlur.setImageDrawable(null);
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
     * 加载背景模糊图片，并提取主题色设置背景色
     * @param imageUrl 封面图片URL
     */
    private void loadBackgroundAndCover(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Log.e(TAG, "背景图加载失败", e);
                        musicPlayerRootLayout.setBackgroundColor(Color.parseColor("#333333"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        ivBackgroundBlur.setImageBitmap(resource);

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

                                findViewById(R.id.music_player_root_layout).setBackgroundColor(bgColor);
                            }
                        });
                        return false;
                    }
                })
                .into(ivBackgroundBlur);
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

        // 【核心】Fragment 准备好后，尝试同步所有待处理的状态
        syncFragmentState();
    }

    @Override
    public void onMusicPlaybackFragmentDetached() {
        // 当 Fragment 被销毁或分离时，清空引用
        this.musicPlaybackFragmentInstance = null;
        Log.d(TAG, "onMusicPlaybackFragmentDetached: MusicPlaybackFragment instance cleared.");
    }

    /**
     * 【新增】同步 Fragment 的音乐信息和播放状态
     * 只有当 Fragment 实例可用且有待处理状态时才执行
     */
    private void syncFragmentState() {
        // 如果 Fragment 实例存在，并且有待处理的音乐信息和播放状态
        if (musicPlaybackFragmentInstance != null && pendingMusicInfo != null && pendingIsPlaying != null) {
            musicPlaybackFragmentInstance.updateMusicInfoAndPlayState(pendingMusicInfo, pendingIsPlaying);
            Log.d(TAG, "syncFragmentState: Synced pending state to fragment: " + pendingMusicInfo.getMusicName() + ", isPlaying: " + pendingIsPlaying);
            // 同步完成后，清除待处理状态
            pendingMusicInfo = null;
            pendingIsPlaying = null;
        } else if (musicPlaybackFragmentInstance != null && musicPlayerService != null && pendingMusicInfo == null) {
            // 另一种情况：Fragment 刚刚准备好，但没有待处理的状态 (pendingMusicInfo 为 null)
            // 此时，直接从 Service 获取当前状态进行同步
            musicPlaybackFragmentInstance.updateMusicInfoAndPlayState(musicPlayerService.getCurrentMusic(), musicPlayerService.isPlaying());
            Log.d(TAG, "syncFragmentState: Synced current service state to newly ready fragment.");
        } else {
            // Fragment 未准备好，或者没有待处理的状态，或者 Service 未连接
            Log.d(TAG, "syncFragmentState: Fragment not ready or no valid state to sync. Fragment Available: " + (musicPlaybackFragmentInstance != null) + ", PendingInfo: " + (pendingMusicInfo != null) + ", PendingPlay: " + (pendingIsPlaying != null) + ", Service Connected: " + (musicPlayerService != null));
        }
    }
}
