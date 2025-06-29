package com.example.music_community;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import jp.wasabeef.glide.transformations.BlurTransformation; // 导入 BlurTransformation

import com.example.music_community.model.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MusicPlayerActivity";
    public static final String EXTRA_MUSIC_LIST = "extra_music_list"; // 传递音乐列表的键
    public static final String EXTRA_CURRENT_POSITION = "extra_current_position"; // 传递当前播放位置的键

    private List<MusicInfo> musicList; // 音乐播放列表
    private int currentPlayPosition; // 当前播放音乐在列表中的索引

    // UI 控件
    private ImageView ivClosePlayer;
    private ViewPager2 viewPagerPlayer;
    private ImageView ivBackgroundBlur; // 背景模糊图片

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




    // 【新增】MusicPlayerService 相关
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;

    // 【新增】用于绑定 Service 的 ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Service 连接成功
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "MusicPlayerService 已连接");

            // 【关键】将音乐列表和当前播放索引传递给 Service
            musicPlayerService.setPlayListAndIndex(musicList, currentPlayPosition);

            // 【新增】启动进度条更新
            startUpdatingSeekBar();
            // 【新增】更新 UI 状态 (播放/暂停按钮图标，歌曲信息)
            updatePlayPauseButton();
            updateMusicInfoUI(musicPlayerService.getCurrentMusic());
            updateSeekBarProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Service 连接断开 (通常是 Service 异常终止)
            isServiceBound = false;
            musicPlayerService = null;
            Log.w(TAG, "MusicPlayerService 连接断开");
            // 【新增】停止进度条更新
            stopUpdatingSeekBar();
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

        // 初始化 UI 控件
        initViews();

        // 获取传递过来的数据
        getIntentData();

        // 设置点击监听器
        setupListeners();

//        // 初始显示音乐信息 (仅用于测试，后续会由Service更新)
//        if (musicList != null && !musicList.isEmpty() && currentPlayPosition >= 0 && currentPlayPosition < musicList.size()) {
//            MusicInfo currentMusic = musicList.get(currentPlayPosition);
//            tvPlayerMusicName.setText(currentMusic.getMusicName());
//            tvPlayerAuthor.setText(currentMusic.getAuthor());
//            // TODO: 加载背景模糊图片和封面图片 (后续步骤实现)
//        } else {
//            Toast.makeText(this, "未获取到音乐信息", Toast.LENGTH_SHORT).show();
//            Log.e(TAG, "未获取到有效的音乐列表或播放位置");
//            // 可以在这里禁用播放控制，或者直接关闭Activity
//            finish();
//        }


        // 【修改】启动并绑定 MusicPlayerService
        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        // 对于 Android O (API 26) 及更高版本，如果应用在后台，则必须先调用 startForegroundService()
        // 否则，系统会抛出 IllegalStateException。
        // 即使应用在前台，也建议使用 startService/startForegroundService 来确保服务生命周期独立于 Activity。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(serviceIntent);

        } else {

            startService(serviceIntent);

        }

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);





        // TODO: 初始化 ViewPager2 和 Fragment (后续步骤实现)
        // TODO: 初始化和绑定 MusicPlayerService (后续步骤实现)
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerActivity onDestroy");
        // 【新增】解绑 Service
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        // 【新增】停止进度条更新
        stopUpdatingSeekBar();
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
    }

    /**
     * 获取从上一个 Activity 传递过来的数据
     */
    private void getIntentData() {
        if (getIntent() != null) {
            // 获取音乐列表，使用 ArrayList<MusicInfo> 确保类型安全
            musicList = (List<MusicInfo>) getIntent().getSerializableExtra(EXTRA_MUSIC_LIST);
            // 获取当前播放位置
            currentPlayPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);

            if (musicList == null) {
                musicList = new ArrayList<>(); // 避免空指针
                Log.e(TAG, "传递的音乐列表为空！");
            }
            Log.d(TAG, "接收到音乐列表大小: " + musicList.size() + ", 初始播放位置: " + currentPlayPosition);
        }
    }

    /**
     * 设置各个 UI 控件的点击监听器
     */
    private void setupListeners() {
        // 关闭按钮
        ivClosePlayer.setOnClickListener(v -> {
            // TODO: 实现关闭动画 (后续步骤实现)
            finish(); // 暂时直接关闭
        });

        // 循环模式按钮
        ivLoopMode.setOnClickListener(v -> {
            Toast.makeText(this, "切换循环模式 (待实现)", Toast.LENGTH_SHORT).show();
            // TODO: 切换播放模式逻辑 (后续步骤实现)
        });

        // 上一首按钮
        ivPreviousSong.setOnClickListener(v -> {
//            Toast.makeText(this, "播放上一首 (待实现)", Toast.LENGTH_SHORT).show();


            if( musicPlayerService != null ) {

                // 播放前一首歌曲
                musicPlayerService.playPrevious();

                // 更新歌曲信息
                updateMusicInfoUI( musicPlayerService.getCurrentMusic() );

                // 更新播放按钮
                updatePlayPauseButton();

            }

        });

        // 播放/暂停按钮
        ivPlayPause.setOnClickListener(v -> {
//            Toast.makeText(this, "播放/暂停 (待实现)", Toast.LENGTH_SHORT).show();


            if (musicPlayerService != null) {

                // 切换播放状态
                musicPlayerService.togglePlayPause();

                // 更新播放按钮状态
                updatePlayPauseButton();
            }

        });

        // 下一首按钮
        ivNextSong.setOnClickListener(v -> {
//            Toast.makeText(this, "播放下一首 (待实现)", Toast.LENGTH_SHORT).show();


            if( musicPlayerService != null ) {

                // 播放下一首歌曲
                musicPlayerService.playNext();

                // 更新歌曲信息
                updateMusicInfoUI( musicPlayerService.getCurrentMusic() );

                // 更新播放按钮
                updatePlayPauseButton();

            }


        });

        // 歌曲列表按钮
        ivSongList.setOnClickListener(v -> {
//            Toast.makeText(this, "显示歌曲列表 (待实现)", Toast.LENGTH_SHORT).show();


            if (musicPlayerService != null) {

                // 播放下一首音乐
                musicPlayerService.playNext();

                // 更新歌曲信息
                updateMusicInfoUI(musicPlayerService.getCurrentMusic());

                // 更新播放按钮状态
                updatePlayPauseButton();

            }



        });



        ivSongList.setOnClickListener(v -> {
            Toast.makeText(this, "显示歌曲列表 (待实现)", Toast.LENGTH_SHORT).show();
        });





        // 进度条拖动监听器
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 当进度改变时，如果是由用户拖动引起的，更新显示时间
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用户开始拖动进度条


                // 用户开始拖动时，停止 Handler 的自动更新，避免冲突
                stopUpdatingSeekBar();


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用户停止拖动进度条
//                Toast.makeText(MusicPlayerActivity.this, "拖动到: " + formatTime(seekBar.getProgress()) + " (待实现)", Toast.LENGTH_SHORT).show();


                if (musicPlayerService != null) {

                    musicPlayerService.seekTo(seekBar.getProgress());
                }

                // 用户停止拖动后，重新开始 Handler 的自动更新
                startUpdatingSeekBar();


            }
        });
    }


    /**
     * 更新播放/暂停按钮的图标
     */
    private void updatePlayPauseButton() {

        if (musicPlayerService != null && musicPlayerService.isPlaying()) {

            // 将按钮改为暂停图标
            ivPlayPause.setImageResource(R.drawable.ic_pause_big);


        } else {

            // 将按钮改为播放图标
            ivPlayPause.setImageResource(R.drawable.ic_play_big);
        }
    }


    /**
     * 更新歌曲名称和歌手信息
     * @param musicInfo 当前播放的音乐信息
     */
    private void updateMusicInfoUI(MusicInfo musicInfo) {

        if (musicInfo != null) {

            // 设置歌名
            tvPlayerMusicName.setText(musicInfo.getMusicName());

            // 设置歌手名
            tvPlayerAuthor.setText(musicInfo.getAuthor());



            // 【新增】加载背景模糊图片
            Glide.with(this)
                    .load(musicInfo.getCoverUrl())
                    // 应用 CenterCrop 确保图片填充 ImageView，然后应用模糊转换
                    .transform(new CenterCrop(), new BlurTransformation(25, 3)) // 模糊半径25，采样率3
                    .placeholder(R.drawable.ic_launcher_background) // 占位图
                    .error(R.drawable.ic_launcher_background) // 错误图
                    .into(ivBackgroundBlur);



        } else {

            tvPlayerMusicName.setText("未知歌曲");

            tvPlayerAuthor.setText("未知歌手");

            ivBackgroundBlur.setImageResource(R.drawable.ic_launcher_background); // 清除或设置默认背景


        }
    }

    /**
     * 更新进度条和时间显示
     */
    private void updateSeekBarProgress() {

        if (musicPlayerService != null) {

            int currentPosition = musicPlayerService.getCurrentPosition();

            int duration = musicPlayerService.getDuration();

            // 设置当前播放进度
            tvCurrentTime.setText(formatTime(currentPosition));

            // 设置总时长
            tvTotalTime.setText(formatTime(duration));


            seekBarMusic.setMax(duration);

            seekBarMusic.setProgress(currentPosition);


        }

    }

    /**
     * 启动进度条定时更新
     */
    private void startUpdatingSeekBar() {


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
        int minutes = milliseconds / 1000 / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // TODO: 后续会添加 Service 连接、UI 更新、动画、ViewPager2 Adapter 等方法
}
