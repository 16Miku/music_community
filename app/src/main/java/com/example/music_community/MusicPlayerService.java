package com.example.music_community;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.music_community.model.MusicInfo;

import java.io.IOException;
import java.util.List;

public class MusicPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicPlayerService";

    private MediaPlayer mediaPlayer; // 媒体播放器实例
    private List<MusicInfo> musicList; // 播放列表
    private int currentMusicIndex = -1; // 当前播放音乐的索引

    private final IBinder binder = new MusicPlayerBinder(); // Binder 用于 Activity 与 Service 通信

    public class MusicPlayerBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this; // 返回 Service 实例
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicPlayerService onCreate");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this); // 设置播放完成监听器
        mediaPlayer.setOnErrorListener(this); // 设置播放错误监听器
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
        // 通常，播放命令会通过绑定 Service 后调用方法来执行，
        // 但这里也可以处理一些独立的启动命令，例如来自通知栏的播放/暂停。
        // 目前暂时不处理 Intent，后续会添加。
        return START_NOT_STICKY; // 服务被杀死后不重启
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicPlayerService onDestroy");
        if (mediaPlayer != null) {
            mediaPlayer.release(); // 释放 MediaPlayer 资源
            mediaPlayer = null;
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
                // TODO: 通知 Activity 更新 UI (播放状态、总时长等)
            });
        } catch (IOException e) {
            Log.e(TAG, "设置数据源或准备播放器失败: " + e.getMessage());
            Toast.makeText(this, "播放失败: " + musicToPlay.getMusicName(), Toast.LENGTH_SHORT).show();
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
                // TODO: 通知 Activity 更新 UI (暂停图标)
            } else {
                mediaPlayer.start();
                Log.d(TAG, "音乐继续播放");
                // TODO: 通知 Activity 更新 UI (播放图标)
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
}
