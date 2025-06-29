package com.example.music_community;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.music_community.model.MusicInfo;

import java.io.IOException;
import java.util.List;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {



    // 通知相关常量
    public static final String CHANNEL_ID = "music_player_channel"; // 通知渠道ID，唯一标识
    public static final int NOTIFICATION_ID = 1; // 通知ID，唯一标识
    public static final String ACTION_PLAY_PAUSE = com.example.music_community" | Set-Content -Path replacements.txt.ACTION_PLAY_PAUSE"; // 播放/暂停动作
    public static final String ACTION_PREVIOUS = com.example.music_community" | Set-Content -Path replacements.txt.ACTION_PREVIOUS"; // 上一首动作
    public static final String ACTION_NEXT = com.example.music_community" | Set-Content -Path replacements.txt.ACTION_NEXT"; // 下一首动作
    public static final String ACTION_CLOSE = com.example.music_community" | Set-Content -Path replacements.txt.ACTION_CLOSE"; // 关闭动作



    private static final String TAG = "MusicPlayerService";

    private MediaPlayer mediaPlayer; // 媒体播放器实例
    private List<MusicInfo> musicList; // 播放列表
    private int currentMusicIndex = -1; // 当前播放音乐的索引

    private final IBinder binder = new MusicPlayerBinder(); // Binder 用于 Activity 与 Service 通信


    // 通知管理器
    private NotificationManager notificationManager;


    // MediaSessionCompat 用于通知栏和锁屏控制
    private MediaSessionCompat mediaSession;



    public class MusicPlayerBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this; // 返回 Service 实例
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicPlayerService onCreate");


        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE );

        // 创建通知渠道
        createNotificationChannel();






        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this); // 设置播放完成监听器
        mediaPlayer.setOnErrorListener(this); // 设置播放错误监听器



        // 初始化 MediaSession
        mediaSession = new MediaSessionCompat(this, TAG);

        // 设置 MediaSession 的 flags，使其能够处理媒体按钮和传输控制
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);


        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                togglePlayPause(); // 媒体会话收到播放指令，调用播放/暂停
            }

            @Override
            public void onPause() {
                super.onPause();
                togglePlayPause(); // 媒体会话收到暂停指令，调用播放/暂停
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                playNext(); // 媒体会话收到下一首指令
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                playPrevious(); // 媒体会话收到上一首指令
            }

            // 可以根据需要添加其他回调，如 onSeekTo 等
        });
        mediaSession.setActive(true); // 激活媒体会话，使其能够接收指令并显示在通知栏/锁屏





    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "MusicPlayerService onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "MusicPlayerService onUnbind");


        // 当所有客户端都解绑时，如果服务不是前台服务，它可能会被系统终止。
        // 这里我们希望它保持运行，所以后面会使用 startForegroundService。
        mediaSession.setActive(false); // 解绑时取消激活媒体会话，避免在没有 Activity 绑定时仍然占用资源


        return super.onUnbind(intent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        Log.d(TAG, "MusicPlayerService onStartCommand, Action: " + (intent != null ? intent.getAction() : "null"));


        if (intent != null) {

            String action = intent.getAction();

            if (action != null) {

                switch (action) {
                    case ACTION_PLAY_PAUSE:
                        togglePlayPause(); // 处理播放/暂停通知栏点击
                        break;
                    case ACTION_PREVIOUS:
                        playPrevious(); // 处理上一首通知栏点击
                        break;
                    case ACTION_NEXT:
                        playNext(); // 处理下一首通知栏点击
                        break;
                    case ACTION_CLOSE:
                        stopSelf(); // 停止服务，将触发 onDestroy
                        break;


                }

            }

        }
        // START_NOT_STICKY 表示服务被系统杀死后不会尝试重启
        // START_STICKY 表示如果服务被系统杀死，系统会尝试重启，但不会保留 Intent
        // START_REDELIVER_INTENT 表示如果服务被系统杀死，系统会尝试重启并重新传递最后一个 Intent
        return START_NOT_STICKY;


    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        Log.d(TAG, "MusicPlayerService onDestroy");

        if (mediaPlayer != null) {
            mediaPlayer.release(); // 释放 MediaPlayer 资源，非常重要，防止内存泄漏
            mediaPlayer = null;
        }

        // 释放 MediaSession 资源
        if (mediaSession != null) {
            mediaSession.release();
        }

        // 移除通知
        notificationManager.cancel(NOTIFICATION_ID); // 移除通知栏中显示的服务通知
    }



    /**
     * 【新增】创建通知渠道 (Android 8.0 Oreo 及以上版本需要)
     * 通知渠道用于对通知进行分类和管理，用户可以单独控制每个渠道的通知设置。
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name); // 通知渠道名称
            String description = getString(R.string.channel_description); // 通知渠道描述
            int importance = NotificationManager.IMPORTANCE_LOW; // 重要程度，LOW 表示不弹出，但会显示在状态栏
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setSound(null, null); // 关闭通知声音，避免播放音乐时通知声音干扰
            channel.enableVibration(false); // 关闭震动
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification Channel Created");
        }
    }





    /**
     * 【新增】构建并显示通知
     * 此方法负责创建和更新通知栏中的媒体播放通知。
     * @param musicInfo 当前播放的音乐信息
     * @param isPlaying 当前是否正在播放
     */
    @SuppressLint("ForegroundServiceType")
    private void buildAndShowNotification(MusicInfo musicInfo, boolean isPlaying) {
        if (musicInfo == null) {
            Log.e(TAG, "无法构建通知：音乐信息为空");
            stopForeground(true); // 如果音乐信息为空，停止前台服务并移除通知
            return;
        }

        // 点击通知跳转回 MusicPlayerActivity
        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP 和 FLAG_ACTIVITY_SINGLE_TOP 确保点击通知时，如果 Activity 已经在运行，则将其带到前台，而不是创建新的实例
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // PendingIntent.FLAG_IMMUTABLE (Android 6.0+) 确保 PendingIntent 不可变，提高安全性
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // 创建播放/暂停按钮的 PendingIntent
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // 创建上一首按钮的 PendingIntent
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_PREVIOUS),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // 创建下一首按钮的 PendingIntent
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // 创建关闭按钮的 PendingIntent
        PendingIntent closePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, MusicPlayerService.class).setAction(ACTION_CLOSE),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // 小图标，必须设置，显示在状态栏和通知抽屉中
                .setContentTitle(musicInfo.getMusicName()) // 歌曲名称
                .setContentText(musicInfo.getAuthor()) // 歌手名称
                .setContentIntent(pendingIntent) // 点击通知主体时的意图
                .setOngoing(isPlaying) // 正在播放时，通知不可滑动清除 (除非服务停止)
                .setOnlyAlertOnce(true) // 首次显示才响铃，后续更新不响
                .setPriority(NotificationCompat.PRIORITY_LOW) // 优先级与渠道一致
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见

                // 添加媒体控制按钮
                .addAction(R.drawable.ic_previous, "上一首", previousPendingIntent) // 上一首按钮
                .addAction(isPlaying ? R.drawable.ic_pause_big : R.drawable.ic_play_big, isPlaying ? "暂停" : "播放", playPausePendingIntent) // 播放/暂停按钮
                .addAction(R.drawable.ic_next, "下一首", nextPendingIntent) // 下一首按钮
                .addAction(R.drawable.ic_close, "关闭", closePendingIntent) // 关闭按钮

                // 使用 MediaStyle 样式，使通知看起来更像媒体播放器
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken()) // 关联媒体会话，实现锁屏控制
                        // 在折叠状态下显示哪些按钮，索引对应 addAction 的顺序
                        .setShowActionsInCompactView(0, 1, 2) // 显示上一首、播放/暂停、下一首
                        .setShowCancelButton(true) // 显示关闭按钮
                        .setCancelButtonIntent(closePendingIntent)); // 关闭按钮的 Intent


        // 异步加载封面图片并设置到通知
        // 使用 Glide 加载图片到 Bitmap，然后设置给通知的 setLargeIcon
        Glide.with(this)
                .asBitmap() // 请求 Glide 返回 Bitmap
                .load(musicInfo.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_foreground) // 占位图
                .error(R.drawable.ic_launcher_foreground) // 错误图
                .into(new CustomTarget<Bitmap>() { // 自定义 Target 来处理 Bitmap 结果


                    @SuppressLint("ForegroundServiceType")
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        builder.setLargeIcon(resource); // 设置大图标 (封面图)


                        Notification notification = builder.build();

                        if (isPlaying) {


                            startForeground(NOTIFICATION_ID, notification); // 启动前台服务
                        } else {
                            notificationManager.notify(NOTIFICATION_ID, notification); // 更新通知 (如果已是前台服务)
                        }
                        Log.d(TAG, "通知更新：封面图已加载");
                    }

                    @SuppressLint("ForegroundServiceType")
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 当图片加载取消或清除时
                        builder.setLargeIcon((Bitmap) null); // 清除大图标
                        Notification notification = builder.build();
                        if (isPlaying) {
                            startForeground(NOTIFICATION_ID, notification);
                        } else {
                            notificationManager.notify(NOTIFICATION_ID, notification);
                        }
                        Log.d(TAG, "通知更新：封面图已清除");
                    }

                    @SuppressLint("ForegroundServiceType")
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        // 加载失败时也清除大图标，避免显示旧图或不合适的图
                        builder.setLargeIcon((Bitmap) null);
                        Notification notification = builder.build();
                        if (isPlaying) {
                            startForeground(NOTIFICATION_ID, notification);
                        } else {
                            notificationManager.notify(NOTIFICATION_ID, notification);
                        }
                        Log.e(TAG, "通知更新：封面图加载失败");
                    }
                });

        // 【重要】在 Glide 异步加载完成之前，先显示一个不带大图标的通知，避免 ANR
        // 如果不这样做，并且图片加载很慢，可能会导致 ANR。
        if (isPlaying) {
            startForeground(NOTIFICATION_ID, builder.build());
        } else {
            // 如果已经处于暂停状态，并且服务之前是前台服务，则将其降级为普通通知
            // 如果服务不是前台服务，则只是更新通知
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }









    /**
     * 设置播放列表和当前播放的音乐索引
     * @param list 音乐列表
     * @param index 当前播放的音乐索引
     */
    public void setPlayListAndIndex(List<MusicInfo> list, int index) {
        this.musicList = list;
        this.currentMusicIndex = index;
        Log.d(TAG, "设置播放列表，大小: " + (list != null ? list.size() : 0) + ", 初始索引: " + index);
        // 立即播放当前选中的音乐
        playMusic(currentMusicIndex);
    }

    /**
     * 播放指定索引的音乐
     * @param index 音乐在列表中的索引
     */
    private void playMusic(int index) {
        if (musicList == null || musicList.isEmpty() || index < 0 || index >= musicList.size()) {
            Log.e(TAG, "播放列表为空或索引无效");
            return;
        }

        currentMusicIndex = index;
        MusicInfo musicToPlay = musicList.get(currentMusicIndex);
        Log.d(TAG, "准备播放音乐: " + musicToPlay.getMusicName() + " - " + musicToPlay.getAuthor());

        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
            } else {
                mediaPlayer.reset(); // 重置播放器，准备新的播放
            }

            mediaPlayer.setDataSource(musicToPlay.getMusicUrl()); // 设置音乐源
            mediaPlayer.prepareAsync(); // 异步准备
            mediaPlayer.setOnPreparedListener(mp -> {


                mp.start(); // 准备完成后开始播放


                Log.d(TAG, "音乐开始播放: " + musicToPlay.getMusicName());

                // 播放开始时更新通知，并将服务提升为前台服务
                buildAndShowNotification(musicToPlay, true);


                // TODO: 通知 Activity 更新 UI (播放状态、总时长等)
                // 可以通过 LocalBroadcastManager 或回调接口通知 Activity


            });
        } catch (IOException e) {

            Log.e(TAG, "设置数据源或准备播放器失败: " + e.getMessage());

            Toast.makeText(this, "播放失败: " + musicToPlay.getMusicName(), Toast.LENGTH_SHORT).show();

            // 播放失败时也更新通知（显示暂停状态），并停止前台服务
            buildAndShowNotification(musicToPlay, false);

            stopForeground(false); // 停止前台服务，但保留通知

        }
    }

    /**
     * 播放/暂停音乐
     */
    public void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                Log.d(TAG, "音乐暂停");

                // 暂停时更新通知
                buildAndShowNotification(getCurrentMusic(), false);

                // 从前台服务降级为普通服务，但通知仍然保留在通知栏
                stopForeground(false); // false 表示保留通知，但服务不再是前台



            } else {
                mediaPlayer.start();
                Log.d(TAG, "音乐继续播放");

                // 播放时更新通知，并重新提升为前台服务
                buildAndShowNotification(getCurrentMusic(), true);
                // 重新提升为前台服务，使用当前通知对象。注意：这里需要确保通知对象已存在或被重新构建。
                // 实际操作中，buildAndShowNotification 内部会处理 startForeground


            }
        } else {
            // 如果 MediaPlayer 未初始化或未播放，尝试播放当前音乐
            if (musicList != null && !musicList.isEmpty()) {
                playMusic(currentMusicIndex);
            } else {
                Log.w(TAG, "无法播放/暂停，MediaPlayer为空且无播放列表");
            }
        }
    }

    /**
     * 播放上一首音乐 (基础逻辑，不含循环模式)
     */
    public void playPrevious() {
        if (musicList == null || musicList.isEmpty()) {
            Log.w(TAG, "播放列表为空，无法播放上一首");
            return;
        }
        int newIndex = currentMusicIndex - 1;
        if (newIndex < 0) {
            newIndex = musicList.size() - 1; // 简单循环到列表末尾
        }
        playMusic(newIndex);
    }

    /**
     * 播放下一首音乐 (基础逻辑，不含循环模式)
     */
    public void playNext() {
        if (musicList == null || musicList.isEmpty()) {
            Log.w(TAG, "播放列表为空，无法播放下一首");
            return;
        }
        int newIndex = currentMusicIndex + 1;
        if (newIndex >= musicList.size()) {
            newIndex = 0; // 简单循环到列表开头
        }
        playMusic(newIndex);
    }

    /**
     * 获取当前播放进度 (毫秒)
     * @return 当前播放进度，如果未播放则返回0
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * 获取当前音乐总时长 (毫秒)
     * @return 音乐总时长，如果未加载则返回0
     */
    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    /**
     * 判断是否正在播放
     * @return true if playing, false otherwise
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * 跳转到指定播放位置
     * @param positionMs 目标位置 (毫秒)
     */
    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(positionMs);
            Log.d(TAG, "跳转到: " + positionMs + "ms");
        }
    }

    /**
     * 获取当前播放的音乐信息
     * @return MusicInfo 对象，如果未播放则返回 null
     */
    public MusicInfo getCurrentMusic() {
        if (musicList != null && currentMusicIndex >= 0 && currentMusicIndex < musicList.size()) {
            return musicList.get(currentMusicIndex);
        }
        return null;
    }

    // --- MediaPlayer.OnCompletionListener 回调 ---
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "音乐播放完成");
        // TODO: 根据播放模式自动播放下一首 (后续步骤实现)
        playNext(); // 暂时简单播放下一首
    }

    // --- MediaPlayer.OnErrorListener 回调 ---
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer 播放错误: what=" + what + ", extra=" + extra);
        // 释放资源并尝试播放下一首或提示用户
        mp.reset();
        Toast.makeText(this, "播放出错，尝试播放下一首", Toast.LENGTH_SHORT).show();
        playNext(); // 尝试播放下一首
        return true; // 表示已处理错误
    }


    /**
     * 获取当前播放状态
     * @return -1: 未初始化/未播放; 0: 暂停; 1: 播放中
     */
    public int getMusicState() {
        if (mediaPlayer == null) {
            return -1; // 未初始化
        }
        // 确保播放器处于有效状态，否则 isPlaying() 会抛出 IllegalStateException
        try {
            if (mediaPlayer.isPlaying()) {
                return 1; // 播放中
            } else {
                return 0; // 暂停中
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "获取播放状态异常: " + e.getMessage());
            return -1;
        }
    }





}
