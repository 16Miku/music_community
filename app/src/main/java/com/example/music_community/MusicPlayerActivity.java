package com.example.music_community;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap; // 【新增】
import android.graphics.Color; // 【新增】
import android.graphics.drawable.BitmapDrawable; // 【新增】
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
import androidx.palette.graphics.Palette; // 【新增】
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

public class MusicPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MusicPlayerActivity";
    public static final String EXTRA_MUSIC_LIST = "extra_music_list";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_position";

    private List<MusicInfo> musicList;
    private int currentPlayPosition;

    // UI 控件
    private ImageView ivClosePlayer;
    private ViewPager2 viewPagerPlayer;
    private ImageView ivBackgroundBlur; // 背景模糊图片

    private ConstraintLayout musicPlayerRootLayout; // 【修改】声明根布局变量



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
    private PlayerPagerAdapter playerPagerAdapter; // 【新增】

    // 用于绑定 Service 的 ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;

            musicPlayerService = binder.getService();

            isServiceBound = true;

            Log.d(TAG, "MusicPlayerService 已连接");

            // 【关键】将音乐列表和当前播放索引传递给 Service
            musicPlayerService.setPlayListAndIndex(musicList, currentPlayPosition);

            // 【新增】注册 Service 的回调监听器，以便 Service 状态变化时通知 Activity
            musicPlayerService.setOnMusicPlayerEventListener(new MusicPlayerService.OnMusicPlayerEventListener() {
                @Override
                public void onMusicPrepared(MusicInfo musicInfo) {
                    // 音乐准备好播放时回调
                    updateMusicInfoUI(musicInfo); // 更新歌曲信息
                    updatePlayPauseButton(); // 更新播放按钮状态
                    startCoverAnimation(); // 启动封面动画
                    startUpdatingSeekBar(); // 启动进度条更新
                }

                @Override
                public void onMusicPlayStatusChanged(boolean isPlaying) {

                    // 播放状态改变时回调 (播放/暂停)
                    updatePlayPauseButton();

                    if (isPlaying) {

                        startCoverAnimation(); // 播放时启动动画


                    } else {

                        pauseCoverAnimation(); // 暂停时暂停动画

                    }
                }

                @Override
                public void onMusicCompleted(MusicInfo nextMusicInfo) {
                    // 音乐播放完成时回调
                    updateMusicInfoUI(nextMusicInfo); // 更新下一首歌曲信息
                    updatePlayPauseButton(); // 更新播放按钮状态
                    startCoverAnimation(); // 重新启动动画
                    // 进度条会自动从头开始更新
                }

                @Override
                public void onMusicError(String errorMessage) {
                    Toast.makeText(MusicPlayerActivity.this, "播放出错: " + errorMessage, Toast.LENGTH_SHORT).show();
                    stopCoverAnimation(); // 停止动画
                }

                @Override
                public void onLoopModeChanged(MusicPlayerService.LoopMode newMode) {
                    // 播放模式改变时回调
                    updateLoopModeIcon(newMode);
                    showLoopModeToast(newMode);
                }
            });

            // 初始启动时更新 UI 状态
            // 如果 Service 已经在播放，则恢复其状态
            if (musicPlayerService.isPlaying()) {

                updateMusicInfoUI(musicPlayerService.getCurrentMusic());

                updatePlayPauseButton();

                startCoverAnimation();

                startUpdatingSeekBar();

                updateLoopModeIcon(musicPlayerService.getLoopMode()); // 更新循环模式图标

            } else {
                // 如果 Service 未播放，但有当前音乐信息，也更新 UI
                if (musicPlayerService.getCurrentMusic() != null) {
                    updateMusicInfoUI(musicPlayerService.getCurrentMusic());
                    updatePlayPauseButton(); // 确保显示播放状态
                    updateLoopModeIcon(musicPlayerService.getLoopMode()); // 更新循环模式图标
                    // 此时不启动动画，因为未播放
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            musicPlayerService = null;
            Log.w(TAG, "MusicPlayerService 连接断开");
            stopUpdatingSeekBar();
            stopCoverAnimation(); // 服务断开时停止动画
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
            // 每秒更新一次
            handler.postDelayed(this, 1000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initViews();
        getIntentData();
        setupListeners();
        initViewPager(); // 【新增】初始化 ViewPager2

        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        // 对于 Android O (API 26) 及更高版本，如果服务在后台运行，需要使用 startForegroundService
        // 这里我们先 startService 再 bindService，确保 Service 即使 Activity 销毁也能继续运行
        startService(serviceIntent); // 【修改】先启动服务
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerActivity onDestroy");
        if (isServiceBound) {
            // 解绑 Service，但 Service 不会被停止，因为之前调用了 startService
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopUpdatingSeekBar();
        stopCoverAnimation(); // 确保动画停止
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

        // 禁用用户滑动，因为 PDF 要求左右滑动切换页面
        viewPagerPlayer.setUserInputEnabled(false); // 【注意】这里暂时禁用，后续会通过手势监听实现左右滑动切换
    }

    /**
     * 设置各个 UI 控件的点击监听器
     */
    private void setupListeners() {
        ivClosePlayer.setOnClickListener(v -> {
            // TODO: 实现关闭动画 (后续步骤实现)
            finish();
            // 如果Service不再需要，可以在这里选择停止Service
            // stopService(new Intent(this, MusicPlayerService.class));
        });

        ivLoopMode.setOnClickListener(v -> {
            if (musicPlayerService != null) {
                musicPlayerService.switchLoopMode(); // 调用 Service 切换模式
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
            Toast.makeText(this, "显示歌曲列表 (待实现)", Toast.LENGTH_SHORT).show();
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
                stopUpdatingSeekBar(); // 用户开始拖动时，停止 Handler 的自动更新，避免冲突
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (musicPlayerService != null) {
                    musicPlayerService.seekTo(seekBar.getProgress());
                }
                startUpdatingSeekBar(); // 用户停止拖动后，重新开始 Handler 的自动更新
            }
        });
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
            loadBackgroundAndCover(musicInfo.getCoverUrl()); // 加载背景模糊图和封面
            playerPagerAdapter.setCurrentMusicInfo(musicInfo); // 更新 ViewPager2 中的 Fragment 数据
        } else {
            tvPlayerMusicName.setText("未知歌曲");
            tvPlayerAuthor.setText("未知歌手");
            ivBackgroundBlur.setImageDrawable(null); // 清空背景
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
        handler.removeCallbacks(updateSeekBarRunnable); // 确保之前没有运行
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
                .asBitmap() // 请求位图
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3))) // 应用模糊转换，半径25，采样率3
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        Log.e(TAG, "背景图加载失败", e);
                        // 加载失败时设置默认背景色
                        musicPlayerRootLayout.setBackgroundColor(Color.parseColor("#333333"));
                        return false; // 返回 false 让 Glide 继续处理错误占位符
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        // 图片加载成功，设置模糊背景
                        ivBackgroundBlur.setImageBitmap(resource);

                        // 从图片中提取主题色
                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                // 尝试获取各种颜色，优先使用 Vibrant 或 DarkVibrant
                                int defaultColor = Color.parseColor("#333333"); // 默认深灰色
                                int dominantColor = palette.getDominantColor(defaultColor); // 主导色
                                int vibrantColor = palette.getVibrantColor(defaultColor); // 充满活力的颜色
                                int darkVibrantColor = palette.getDarkVibrantColor(defaultColor); // 暗色充满活力的颜色
                                int mutedColor = palette.getMutedColor(defaultColor); // 柔和色
                                int darkMutedColor = palette.getDarkMutedColor(defaultColor); // 暗色柔和色

                                // 选择一个合适的颜色作为背景，优先使用暗色充满活力的颜色
                                int bgColor = darkVibrantColor;
                                if (bgColor == defaultColor) { // 如果没有找到，尝试其他
                                    bgColor = dominantColor;
                                }
                                if (bgColor == defaultColor) {
                                    bgColor = darkMutedColor;
                                }
                                if (bgColor == defaultColor) {
                                    bgColor = mutedColor;
                                }

                                // 设置根布局的背景色
                                findViewById(R.id.music_player_root_layout).setBackgroundColor(bgColor);
                            }
                        });
                        return false; // 返回 false 让 Glide 继续设置图片
                    }
                })
                .into(ivBackgroundBlur);
    }

    /**
     * 启动封面动画
     */
    private void startCoverAnimation() {
        // 获取当前显示的 MusicPlaybackFragment 实例
        if (playerPagerAdapter != null && playerPagerAdapter.getItemCount() > 0) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPagerPlayer.getCurrentItem());
            if (fragment instanceof MusicPlaybackFragment) {
                ((MusicPlaybackFragment) fragment).startCoverAnimation();
            }
        }
    }

    /**
     * 暂停封面动画
     */
    private void pauseCoverAnimation() {
        if (playerPagerAdapter != null && playerPagerAdapter.getItemCount() > 0) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPagerPlayer.getCurrentItem());
            if (fragment instanceof MusicPlaybackFragment) {
                ((MusicPlaybackFragment) fragment).pauseCoverAnimation();
            }
        }
    }

    /**
     * 停止封面动画 (用于歌曲切换或页面销毁)
     */
    private void stopCoverAnimation() {
        if (playerPagerAdapter != null && playerPagerAdapter.getItemCount() > 0) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + viewPagerPlayer.getCurrentItem());
            if (fragment instanceof MusicPlaybackFragment) {
                ((MusicPlaybackFragment) fragment).stopCoverAnimation();
            }
        }
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

    // TODO: 后续会添加手势识别器来处理左右滑动切换页面
}
