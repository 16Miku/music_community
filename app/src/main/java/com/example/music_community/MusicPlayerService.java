package com.example.music_community;

import android.app.Notification; // 导入 Notification
import android.app.NotificationChannel; // 导入 NotificationChannel
import android.app.NotificationManager; // 导入 NotificationManager
import android.app.PendingIntent; // 导入 PendingIntent
import android.app.Service;
import android.content.Context; // 导入 Context
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable; // 导入 Drawable 类，用于 Glide Target 接口
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build; // 导入 Build
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat; // 导入 MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat; // 导入 PlaybackStateCompat
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat; // 导入 NotificationCompat

import com.bumptech.glide.Glide; // 导入 Glide
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback; // 【新增】导入 SizeReadyCallback
import com.bumptech.glide.request.target.Target; // 导入 Glide Target 接口
import com.bumptech.glide.request.transition.Transition; // 导入 Glide Transition 类
import com.example.music_community.model.MusicInfo;

import java.io.IOException;
import java.util.ArrayList; // 导入 ArrayList
import java.util.Collections; // 导入 Collections
import java.util.List;
import java.util.Random; // 导入 Random

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicPlayerService";
    private static final int NOTIFICATION_ID = 101; // 通知ID
    private static final String CHANNEL_ID = "music_player_channel"; // 通知渠道ID
    private static final String CHANNEL_NAME = "音乐播放"; // 通知渠道名称

    private MediaPlayer mediaPlayer;
    private List<MusicInfo> musicList;
    private int currentMusicIndex = -1;

    // 播放模式枚举
    public enum LoopMode {
        SEQUENCE, // 顺序播放
        SINGLE,   // 单曲循环
        SHUFFLE   // 随机播放
    }
    private LoopMode currentLoopMode = LoopMode.SEQUENCE; // 默认顺序播放

    // 用于随机播放的原始索引列表和随机索引列表
    private List<Integer> originalIndexes;
    private List<Integer> shuffledIndexes;
    private int currentShuffledIndex = -1; // 在随机模式下，当前播放的在 shuffledIndexes 中的索引

    // Service 事件监听器接口
    public interface OnMusicPlayerEventListener {
        void onMusicPrepared(MusicInfo musicInfo); // 音乐准备好播放
        void onMusicPlayStatusChanged(boolean isPlaying); // 播放状态改变
        void onMusicCompleted(MusicInfo nextMusicInfo); // 音乐播放完成
        void onMusicError(String errorMessage); // 播放出错
        void onLoopModeChanged(LoopMode newMode); // 播放模式改变

    }

    private OnMusicPlayerEventListener eventListener; // 事件监听器实例

    // MediaSessionCompat 用于通知栏媒体控制
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder playbackStateBuilder;

    private final IBinder binder = new MusicPlayerBinder();

    public class MusicPlayerBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();
        Log.d(TAG, "MusicPlayerService onCreate");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);

        // 初始化 MediaSession
        mediaSession = new MediaSessionCompat(this, "MusicPlayerService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCallback());
        mediaSession.setActive(true);

        // 初始化 PlaybackStateCompat.Builder
        playbackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SEEK_TO
                );

        createNotificationChannel(); // 创建通知渠道
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
        // 当所有客户端都解绑时，如果服务不是由 startService 启动的，则会销毁。
        // 但我们现在是 startService + bindService，所以解绑不会销毁服务。
        // 可以在这里移除事件监听器，避免内存泄漏
        eventListener = null;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MusicPlayerService onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                // 处理来自通知栏的控制事件
                switch (action) {
                    case "ACTION_PLAY_PAUSE":
                        togglePlayPause();
                        break;
                    case "ACTION_PREVIOUS":
                        playPrevious();
                        break;
                    case "ACTION_NEXT":
                        playNext();
                        break;
                    case "ACTION_STOP":
                        stopSelf(); // 停止服务
                        break;
                }
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerService onDestroy");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        stopForeground(true); // 停止前台服务并移除通知
    }

    /**
     * 设置 Service 事件监听器
     * @param listener 监听器实例
     */
    public void setOnMusicPlayerEventListener(OnMusicPlayerEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * 设置播放列表和当前播放的音乐索引
     * @param list 音乐列表
     * @param index 当前播放的音乐索引
     */
    public void setPlayListAndIndex(List<MusicInfo> list, int index) {
        if (list == null || list.isEmpty()) {
            Log.e(TAG, "设置播放列表失败：列表为空");
            return;
        }
        this.musicList = list;
        this.currentMusicIndex = index;
        // 初始化原始索引列表
        originalIndexes = new ArrayList<>();
        for (int i = 0; i < musicList.size(); i++) {
            originalIndexes.add(i);
        }
        // 如果是随机模式，需要初始化随机索引列表
        if (currentLoopMode == LoopMode.SHUFFLE) {
            shufflePlaylist();
        }

        Log.d(TAG, "设置播放列表，大小: " + musicList.size() + ", 初始索引: " + index);
        playMusic(currentMusicIndex); // 立即播放当前选中的音乐
    }

    /**
     * 播放指定索引的音乐
     * @param index 音乐在列表中的索引 (原始列表索引)
     */
    private void playMusic(int index) {
        if (musicList == null || musicList.isEmpty() || index < 0 || index >= musicList.size()) {
            Log.e(TAG, "播放列表为空或索引无效");
            if (eventListener != null) {
                eventListener.onMusicError("播放列表为空或索引无效");
            }
            return;
        }

        currentMusicIndex = index; // 更新当前播放的原始索引

        // 如果当前是随机模式，需要更新 currentShuffledIndex
        if (currentLoopMode == LoopMode.SHUFFLE && shuffledIndexes != null) {
            // 找到当前音乐在随机列表中的位置
            currentShuffledIndex = shuffledIndexes.indexOf(currentMusicIndex);
            if (currentShuffledIndex == -1) { // 如果没找到，重新生成随机列表并设置
                shufflePlaylist();
                currentShuffledIndex = shuffledIndexes.indexOf(currentMusicIndex);
                if (currentShuffledIndex == -1) { // 仍然没找到，说明逻辑有问题或者列表为空
                    Log.e(TAG, "随机列表未包含当前音乐索引，无法播放");
                    if (eventListener != null) eventListener.onMusicError("随机播放列表异常");
                    return;
                }
            }
        }


        MusicInfo musicToPlay = musicList.get(currentMusicIndex);
        Log.d(TAG, "准备播放音乐: " + musicToPlay.getMusicName() + " - " + musicToPlay.getAuthor() + ", URL: " + musicToPlay.getMusicUrl());

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
                updateNotification(musicToPlay, true); // 更新通知栏为播放状态
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING); // 更新 MediaSession 状态

                if (eventListener != null) {
                    eventListener.onMusicPrepared(musicToPlay); // 通知 Activity 音乐已准备好
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "设置数据源或准备播放器失败: " + e.getMessage());
            Toast.makeText(this, "播放失败: " + musicToPlay.getMusicName(), Toast.LENGTH_SHORT).show();
            if (eventListener != null) {
                eventListener.onMusicError("播放失败: " + e.getMessage());
            }
            // 尝试播放下一首
            playNext(); // 避免卡死
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer 状态错误: " + e.getMessage());
            if (eventListener != null) {
                eventListener.onMusicError("播放器状态异常: " + e.getMessage());
            }
            playNext(); // 尝试播放下一首
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
                updateNotification(getCurrentMusic(), false); // 更新通知栏为暂停状态
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED); // 更新 MediaSession 状态
                if (eventListener != null) {
                    eventListener.onMusicPlayStatusChanged(false); // 通知 Activity 播放状态改变
                }
            } else {
                mediaPlayer.start();
                Log.d(TAG, "音乐继续播放");
                updateNotification(getCurrentMusic(), true); // 更新通知栏为播放状态
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING); // 更新 MediaSession 状态
                if (eventListener != null) {
                    eventListener.onMusicPlayStatusChanged(true); // 通知 Activity 播放状态改变
                }
            }
        } else {
            // 如果 MediaPlayer 未初始化或未播放，尝试播放当前音乐
            if (musicList != null && !musicList.isEmpty()) {
                playMusic(currentMusicIndex);
            } else {
                Log.w(TAG, "无法播放/暂停，MediaPlayer为空且无播放列表");
                Toast.makeText(this, "无音乐可播放", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 播放上一首音乐
     */
    public void playPrevious() {
        if (musicList == null || musicList.isEmpty()) {
            Log.w(TAG, "播放列表为空，无法播放上一首");
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        int newIndex = currentMusicIndex; // 默认不变

        switch (currentLoopMode) {
            case SEQUENCE:
            case SINGLE: // 单曲循环模式下，上一首还是上一首
                newIndex = (currentMusicIndex - 1 + musicList.size()) % musicList.size();
                break;
            case SHUFFLE:
                if (shuffledIndexes == null || shuffledIndexes.isEmpty()) {
                    shufflePlaylist();
                }
                currentShuffledIndex = (currentShuffledIndex - 1 + shuffledIndexes.size()) % shuffledIndexes.size();
                newIndex = shuffledIndexes.get(currentShuffledIndex);
                break;
        }
        playMusic(newIndex);
    }

    /**
     * 播放下一首音乐
     */
    public void playNext() {
        if (musicList == null || musicList.isEmpty()) {
            Log.w(TAG, "播放列表为空，无法播放下一首");
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show();
            return;
        }

        int newIndex = currentMusicIndex; // 默认不变

        switch (currentLoopMode) {
            case SEQUENCE:
            case SINGLE: // 单曲循环模式下，下一首还是下一首
                newIndex = (currentMusicIndex + 1) % musicList.size();
                break;
            case SHUFFLE:
                if (shuffledIndexes == null || shuffledIndexes.isEmpty()) {
                    shufflePlaylist();
                }
                currentShuffledIndex = (currentShuffledIndex + 1) % shuffledIndexes.size();
                newIndex = shuffledIndexes.get(currentShuffledIndex);
                break;
        }
        playMusic(newIndex);
    }

    /**
     * 切换播放模式 (顺序 -> 单曲 -> 随机 -> 顺序)
     */
    public void switchLoopMode() {
        switch (currentLoopMode) {
            case SEQUENCE:
                currentLoopMode = LoopMode.SINGLE;
                break;
            case SINGLE:
                currentLoopMode = LoopMode.SHUFFLE;
                shufflePlaylist(); // 切换到随机模式时，重新打乱列表
                // 设置随机播放的初始索引为当前歌曲在随机列表中的位置
                if (musicList != null && currentMusicIndex != -1) {
                    currentShuffledIndex = shuffledIndexes.indexOf(currentMusicIndex);
                    if (currentShuffledIndex == -1) { // 如果当前歌曲不在随机列表中，则从0开始
                        currentShuffledIndex = 0;
                    }
                } else {
                    currentShuffledIndex = 0;
                }
                break;
            case SHUFFLE:
                currentLoopMode = LoopMode.SEQUENCE;
                break;
        }
        Log.d(TAG, "切换到播放模式: " + currentLoopMode);
        if (eventListener != null) {
            eventListener.onLoopModeChanged(currentLoopMode); // 通知 Activity 模式改变
        }
    }

    /**
     * 获取当前播放模式
     * @return 当前 LoopMode
     */
    public LoopMode getLoopMode() {
        return currentLoopMode;
    }

    /**
     * 打乱播放列表索引 (用于随机播放)
     */
    private void shufflePlaylist() {
        if (musicList != null && !musicList.isEmpty()) {
            shuffledIndexes = new ArrayList<>(originalIndexes); // 从原始索引复制一份
            Collections.shuffle(shuffledIndexes, new Random(System.nanoTime())); // 打乱顺序
            Log.d(TAG, "播放列表已打乱: " + shuffledIndexes);
        } else {
            shuffledIndexes = new ArrayList<>();
            Log.w(TAG, "播放列表为空，无法打乱");
        }
    }

    /**
     * 获取当前播放进度 (毫秒)
     * @return 当前播放进度，如果未播放则返回0
     */
    public int getCurrentPosition() {
        if (mediaPlayer != null && (mediaPlayer.isPlaying() || currentMusicIndex != -1)) { // 即使暂停也要返回进度
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                Log.e(TAG, "getCurrentPosition IllegalStateException: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    /**
     * 获取当前音乐总时长 (毫秒)
     * @return 音乐总时长，如果未加载则返回0
     */
    public int getDuration() {
        if (mediaPlayer != null && currentMusicIndex != -1) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                Log.e(TAG, "getDuration IllegalStateException: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    /**
     * 判断是否正在播放
     * @return true if playing, false otherwise
     */
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.isPlaying();
            } catch (IllegalStateException e) {
                Log.e(TAG, "isPlaying IllegalStateException: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * 跳转到指定播放位置
     * @param positionMs 目标位置 (毫秒)
     */
    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(positionMs);
                Log.d(TAG, "跳转到: " + positionMs + "ms");
                updatePlaybackState(mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
            } catch (IllegalStateException e) {
                Log.e(TAG, "seekTo IllegalStateException: " + e.getMessage());
            }
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
        // 根据播放模式自动播放下一首
        switch (currentLoopMode) {
            case SEQUENCE:
                playNext();
                break;
            case SINGLE:
                // 单曲循环，重新播放当前歌曲
                playMusic(currentMusicIndex);
                break;
            case SHUFFLE:
                playNext(); // 随机模式下，也调用 playNext
                break;
        }
        if (eventListener != null) {
            eventListener.onMusicCompleted(getCurrentMusic()); // 通知 Activity 播放完成，并传递下一首音乐信息
        }
    }

    // --- MediaPlayer.OnErrorListener 回调 ---
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer 播放错误: what=" + what + ", extra=" + extra);
        // 释放资源并尝试播放下一首或提示用户
        mp.reset();
        String errorMessage = "播放出错，错误码: " + what;
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        if (eventListener != null) {
            eventListener.onMusicError(errorMessage);
        }
        playNext(); // 尝试播放下一首
        return true; // 表示已处理错误
    }

    /**
     * 创建通知渠道 (Android 8.0 Oreo 及以上需要)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("用于音乐播放控制");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 更新通知栏
     * @param musicInfo 当前播放的音乐信息
     * @param isPlaying 是否正在播放
     */
    private void updateNotification(MusicInfo musicInfo, boolean isPlaying) {
        if (musicInfo == null) {
            stopForeground(true); // 没有音乐时停止前台服务
            return;
        }

        // 创建点击通知时跳转的 Intent
        Intent notificationIntent = new Intent(this, MusicPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        // 创建播放/暂停按钮的 PendingIntent
        Intent playPauseIntent = new Intent(this, MusicPlayerService.class);
        playPauseIntent.setAction("ACTION_PLAY_PAUSE");
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 创建上一首按钮的 PendingIntent
        Intent previousIntent = new Intent(this, MusicPlayerService.class);
        previousIntent.setAction("ACTION_PREVIOUS");
        PendingIntent previousPendingIntent = PendingIntent.getService(this, 0, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 创建下一首按钮的 PendingIntent
        Intent nextIntent = new Intent(this, MusicPlayerService.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 创建停止按钮的 PendingIntent
        Intent stopIntent = new Intent(this, MusicPlayerService.class);
        stopIntent.setAction("ACTION_STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_big) // 小图标
                .setContentTitle(musicInfo.getMusicName()) // 标题
                .setContentText(musicInfo.getAuthor()) // 内容
                .setContentIntent(pendingIntent) // 点击通知跳转
                .setPriority(NotificationCompat.PRIORITY_LOW) // 优先级
                .setOnlyAlertOnce(true) // 只响一次
                .setWhen(0) // 隐藏时间戳
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
                .setOngoing(isPlaying) // 播放时持续显示，不可滑动清除
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle() // 媒体样式通知
                        .setShowActionsInCompactView(0, 1, 2) // 在紧凑视图中显示上一首、播放/暂停、下一首
                        .setMediaSession(mediaSession.getSessionToken())) // 关联 MediaSession

                // 添加控制按钮
                .addAction(R.drawable.ic_previous, "上一首", previousPendingIntent)
                .addAction(isPlaying ? R.drawable.ic_pause_big : R.drawable.ic_play_big, isPlaying ? "暂停" : "播放", playPausePendingIntent)
                .addAction(R.drawable.ic_next, "下一首", nextPendingIntent)
                // 可以选择添加停止按钮
                // .addAction(R.drawable.ic_close, "停止", stopPendingIntent)
                ;


        Notification notification = builder.build();

        // 将服务设置为前台服务，并显示通知
        startForeground(NOTIFICATION_ID, notification);


        // 使用 Glide 加载封面图作为通知的大图标
        Glide.with(getApplicationContext())
                .asBitmap()
                .load(musicInfo.getCoverUrl())
                // 使用匿名 Target 接口来处理图片加载结果
                .into(new Target<Bitmap>() {



                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onStop() {
                        // 加载停止时回调，可选操作
                    }

                    @Override
                    public void onDestroy() {

                    }

                    // 【修复】实现 Target 接口中缺失的 getSize 方法
                    @Override
                    public void getSize(@NonNull SizeReadyCallback cb) {
                        // 告知 Glide 目标尺寸。对于通知栏图标，通常不需要特定尺寸，
                        // 可以使用 Target.SIZE_ORIGINAL 来加载原始尺寸，
                        // 或者根据通知图标的最佳实践设置一个固定大小，例如 512x512。
                        // 这里我们使用原始尺寸，让 Glide 自行处理缩放。
                        cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                    }

                    @Override
                    public void removeCallback(@NonNull SizeReadyCallback cb) {

                    }

                    @Override
                    public void setRequest(@Nullable Request request) {

                    }

                    @Nullable
                    @Override
                    public Request getRequest() {
                        return null;
                    }

                    @Override
                    public void onLoadStarted(@Nullable Drawable placeholder) {
                        // 加载开始时回调，可选操作
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 当资源不再需要时（例如，请求被取消或另一个请求开始）回调。
                        // 在这里可以清除之前设置的图标，以避免显示旧图片。
                        builder.setLargeIcon((Bitmap) null); // 显式转换为 Bitmap，解决歧义
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // 图片加载失败时回调
                        builder.setLargeIcon((Bitmap) null); // 显式转换为 Bitmap，解决歧义
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // 图片加载成功时回调
                        builder.setLargeIcon(resource); // 设置大图标
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }
                    // 【移除】void_onDestroy() 方法，它不是 Target 接口的成员，且命名不正确
                });
    }

    /**
     * 更新 MediaSession 的播放状态
     * @param state 播放状态 (e.g., PlaybackStateCompat.STATE_PLAYING, STATE_PAUSED)
     */
    private void updatePlaybackState(int state) {
        if (mediaSession != null) {
            playbackStateBuilder.setState(state, getCurrentPosition(), 1.0f); // 1.0f 是播放速度
            mediaSession.setPlaybackState(playbackStateBuilder.build());
        }
    }

    // MediaSession 回调，处理来自通知栏或蓝牙设备的媒体控制事件
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            super.onPlay();
            togglePlayPause();
        }

        @Override
        public void onPause() {
            super.onPause();
            togglePlayPause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            playNext();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            playPrevious();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            seekTo((int) pos);
        }
        // 还可以覆盖其他方法，如 onStop, onPlayFromMediaId 等
    }
}
