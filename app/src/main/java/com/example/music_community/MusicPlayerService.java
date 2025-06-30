// B:\Android_Project\music-community\music_community\app\src\main\java\com\example\music_community\MusicPlayerService.java
package com.example.music_community;

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
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.example.music_community.model.MusicInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicPlayerService";
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "music_player_channel";
    private static final String CHANNEL_NAME = "音乐播放";

    MediaPlayer mediaPlayer;
    private List<MusicInfo> musicList;
    private int currentMusicIndex = -1; // 当前播放音乐在 musicList 中的索引


    private MusicInfo currentMusicInfo; // 【新增】用于直接存储当前播放的音乐信息



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

    // 线程安全的监听器列表
    final List<OnMusicPlayerEventListener> listeners = new CopyOnWriteArrayList<>();

    // Service 事件监听器接口
    public interface OnMusicPlayerEventListener {
        void onMusicPrepared(MusicInfo musicInfo); // 音乐准备好播放
        void onMusicPlayStatusChanged(boolean isPlaying, MusicInfo musicInfo); // 播放状态改变
        void onMusicCompleted(MusicInfo nextMusicInfo); // 音乐播放完成
        void onMusicError(String errorMessage); // 播放出错
        void onLoopModeChanged(LoopMode newMode); // 播放模式改变
        void onPlaylistChanged(List<MusicInfo> newPlaylist); // 播放列表变化的回调
    }

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
        listeners.clear(); // 清空所有监听器
    }

    /**
     * 添加音乐播放事件监听器
     * @param listener 监听器实例
     */
    public void addOnMusicPlayerEventListener(OnMusicPlayerEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Listener added: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * 移除音乐播放事件监听器
     * @param listener 监听器实例
     */
    public void removeOnMusicPlayerEventListener(OnMusicPlayerEventListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "Listener removed: " + listener.getClass().getSimpleName());
    }

    /**
     * 【新增】更新播放列表，但不改变当前播放状态或重新开始播放。
     * 用于在播放列表内容发生变化时同步服务内部的列表。
     *
     * @param newList 新的播放列表。
     */
    public void updatePlaylist(List<MusicInfo> newList) {
        if (newList == null) {
            this.musicList = new CopyOnWriteArrayList<>(); // 确保不为null
        } else {
            this.musicList = new ArrayList<>(newList); // 更新列表
        }

        // 重新构建原始索引列表
        originalIndexes = new ArrayList<>();
        for (int i = 0; i < this.musicList.size(); i++) {
            originalIndexes.add(i);
        }

        // 如果处于随机模式，重新打乱列表
        if (currentLoopMode == LoopMode.SHUFFLE) {
            shufflePlaylist();
        }

        // 调整 currentMusicIndex：如果当前播放的歌曲仍然在新列表中，找到其新索引
        // 否则，如果当前播放的歌曲被删除，currentMusicIndex 应该被设置为 -1，
        // 或者让后续的播放逻辑（如 playNext）来决定。
        MusicInfo previouslyPlaying = getCurrentMusic(); // 获取修改列表前正在播放的歌曲对象
        if (previouslyPlaying != null) {
            int newIndex = this.musicList.indexOf(previouslyPlaying);
            if (newIndex != -1) {
                currentMusicIndex = newIndex; // 歌曲仍在列表中，更新索引
            } else {
                currentMusicIndex = -1; // 之前播放的歌曲已被删除
            }
        } else {
            currentMusicIndex = -1; // 之前没有歌曲在播放
        }

        // 通知所有监听器播放列表已改变
        for (OnMusicPlayerEventListener listener : listeners) {
            listener.onPlaylistChanged(this.musicList);
        }

        // 如果播放列表变为空，停止播放并清除状态
        if (this.musicList.isEmpty()) {
            clearCurrentMusicAndStop(); // 这会进一步通知监听器播放停止和列表清空
        }
    }


    /**
     * 【修改】设置播放列表并播放指定索引的音乐。
     * @param list 音乐列表
     * @param index 要播放的音乐在列表中的索引
     */
    public void setPlayListAndIndex(List<MusicInfo> list, int index) {
        if (list == null || list.isEmpty()) {
            clearCurrentMusicAndStop(); // 如果传入空列表，则清空并停止
            return;
        }

        // 先更新播放列表
        updatePlaylist(list); // 调用新方法更新列表，并通知 onPlaylistChanged

        // 然后根据指定索引播放音乐
        // 确保索引有效
        if (index >= 0 && index < this.musicList.size()) {
            this.currentMusicIndex = index;
            playMusic(this.currentMusicIndex); // 播放音乐
        } else {
            // 如果索引无效，但列表不为空，尝试从头播放或报错
            if (!this.musicList.isEmpty()) {
                this.currentMusicIndex = 0;
                playMusic(this.currentMusicIndex);
            } else {
                // 此时列表理论上应该为空，或者出现逻辑错误
                for (OnMusicPlayerEventListener listener : listeners) {
                    listener.onMusicError("无法开始播放：索引无效且播放列表为空");
                }
                clearCurrentMusicAndStop();
            }
        }
    }

    /**
     * 播放指定索引的音乐
     * @param index 音乐在列表中的索引 (原始列表索引)
     */
    private void playMusic(int index) {
        if (musicList == null || musicList.isEmpty() || index < 0 || index >= musicList.size()) {
            Log.e(TAG, "playMusic: 播放列表为空或索引无效，无法播放。");
            clearCurrentMusicAndStop(); // 尝试清空并停止
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
                    for (OnMusicPlayerEventListener listener : listeners) {
                        listener.onMusicError("随机播放列表异常");
                    }
                    clearCurrentMusicAndStop();
                    return;
                }
            }
        }

        MusicInfo musicToPlay = musicList.get(currentMusicIndex);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicToPlay.getMusicUrl());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                updateNotification(musicToPlay, true);
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                // 遍历所有监听器进行通知
                for (OnMusicPlayerEventListener listener : listeners) {
                    listener.onMusicPrepared(musicToPlay);
                }

                // 确保在这里设置 currentMusicInfo
                this.currentMusicInfo = musicToPlay;



            });
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "playMusic: 播放失败: " + e.getMessage(), e);
            // 遍历所有监听器进行通知
            for (OnMusicPlayerEventListener listener : listeners) {
                listener.onMusicError("播放失败: " + e.getMessage());
            }
            playNext(); // 尝试播放下一首
        }
    }

    /**
     * 播放/暂停音乐
     */
    public void togglePlayPause() {
        if (mediaPlayer != null && getCurrentMusic() != null) {
            MusicInfo currentMusic = getCurrentMusic();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                updateNotification(currentMusic, false);
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                // 遍历所有监听器进行通知
                for (OnMusicPlayerEventListener listener : listeners) {
                    listener.onMusicPlayStatusChanged(false, currentMusic);
                }
            } else {
                mediaPlayer.start();
                updateNotification(currentMusic, true);
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                // 遍历所有监听器进行通知
                for (OnMusicPlayerEventListener listener : listeners) {
                    listener.onMusicPlayStatusChanged(true, currentMusic);
                }
            }
        } else {
            Log.w(TAG, "togglePlayPause: MediaPlayer is null or no current music. Cannot toggle play/pause.");
            // 如果没有音乐，尝试从头播放列表的第一首
            if (musicList != null && !musicList.isEmpty()) {
                playMusic(0);
            } else {
                Toast.makeText(this, "当前无播放歌曲", Toast.LENGTH_SHORT).show();
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
            clearCurrentMusicAndStop(); // 清空并停止，通知UI
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

        // 遍历所有监听器进行通知
        for (OnMusicPlayerEventListener listener : listeners) {
            listener.onLoopModeChanged(currentLoopMode);
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
            originalIndexes = new ArrayList<>(); // 确保 originalIndexes 已初始化
            for (int i = 0; i < musicList.size(); i++) {
                originalIndexes.add(i);
            }
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


        return currentMusicInfo; // 直接返回存储的当前音乐信息
    }

    /**
     * 【新增】获取当前播放音乐在列表中的原始索引。
     * @return 当前播放音乐的索引，如果未播放则返回 -1。
     */
    public int getCurrentMusicIndex() {
        return currentMusicIndex;
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

        // 通知 Activity 播放完成，并传递下一首音乐信息
        for (OnMusicPlayerEventListener listener : listeners) {
            listener.onMusicCompleted(getCurrentMusic());
        }
    }

    // --- MediaPlayer.OnErrorListener 回调 ---
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
//        Log.e(TAG, "MediaPlayer 播放错误: what=" + what + ", extra=" + extra);
        // 释放资源并尝试播放下一首或提示用户
        mp.reset();
//        String errorMessage = "播放出错，错误码: " + what;
//        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();

        // 遍历所有监听器进行通知
        for (OnMusicPlayerEventListener listener : listeners) {
//            listener.onMusicError(errorMessage);
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
                .addAction(R.drawable.ic_next, "下一首", nextPendingIntent);

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
                    public void onStart() {}
                    @Override
                    public void onStop() {}
                    @Override
                    public void onDestroy() {}
                    @Override
                    public void getSize(@NonNull SizeReadyCallback cb) {
                        cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
                    }
                    @Override
                    public void removeCallback(@NonNull SizeReadyCallback cb) {}
                    @Override
                    public void setRequest(@Nullable Request request) {}
                    @Nullable
                    @Override
                    public Request getRequest() {return null;}
                    @Override
                    public void onLoadStarted(@Nullable Drawable placeholder) {}
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        builder.setLargeIcon((Bitmap) null);
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        builder.setLargeIcon((Bitmap) null);
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        builder.setLargeIcon(resource); // 设置大图标
                        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.notify(NOTIFICATION_ID, builder.build());
                        }
                    }
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
    }

    /**
     * 获取播放列表，提供给外部使用
     * @return 当前播放列表的副本
     */
    public List<MusicInfo> getPlaylist() {
        return new ArrayList<>(musicList != null ? musicList : new ArrayList<>()); // 返回一个副本，防止外部修改影响服务
    }

    /**
     * 清空当前播放的音乐信息，停止播放，并通知所有监听器。
     * 当播放列表为空时调用此方法，以确保UI同步。
     */
    public void clearCurrentMusicAndStop() {
        Log.d(TAG, "clearCurrentMusicAndStop: Clearing current music and stopping playback.");
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop(); // 停止当前播放
            }
            mediaPlayer.reset(); // 重置MediaPlayer到未初始化状态
        }
        currentMusicIndex = -1; // 将当前播放索引设置为无效值
        if (musicList != null) {
            musicList.clear(); // 清空播放列表
        } else {
            musicList = new CopyOnWriteArrayList<>(); // 如果为空，则初始化一个空列表
        }
        originalIndexes = new ArrayList<>(); // 清空原始索引
        shuffledIndexes = new ArrayList<>(); // 清空随机索引
        currentShuffledIndex = -1; // 重置随机索引

        // 停止通知栏显示
        updateNotification(null, false);
        // 更新MediaSession的播放状态为停止
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);

        // 遍历所有监听器，通知它们音乐已停止且没有当前播放歌曲
        for (OnMusicPlayerEventListener listener : listeners) {
            // 通知播放状态改变：不再播放，且当前歌曲为null
            listener.onMusicPlayStatusChanged(false, null);
            // 通知播放列表已改变为空列表
            listener.onPlaylistChanged(new CopyOnWriteArrayList<>());
            // 通知音乐播放完成，且没有下一首歌曲
            listener.onMusicCompleted(null);
        }
    }
}
