package com.example.music_community;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.music_community.model.MusicInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// 【修改】导入顶层接口
import com.example.music_community.MusicPlaybackFragmentListener;


public class MusicPlaybackFragment extends Fragment {

    private static final String TAG = "MusicPlaybackFragment";
    private static final String ARG_MUSIC_INFO = "arg_music_info";

    private ImageView ivMusicCoverPlayer;
    private ObjectAnimator coverAnimator;

    private MusicInfo musicInfo;
    private boolean isPlaying = false;


    // 【修改】Fragment 回调监听器，使用顶层接口类型
    private MusicPlaybackFragmentListener listener;


    /**
     * 创建 MusicPlaybackFragment 实例的工厂方法
     * @param musicInfo 要显示的音乐信息
     * @return MusicPlaybackFragment 实例
     */
    public static MusicPlaybackFragment newInstance(MusicInfo musicInfo) {
        MusicPlaybackFragment fragment = new MusicPlaybackFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC_INFO, musicInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 【修改】在 Fragment 附加到 Activity 时，尝试获取监听器
        // 现在 MusicPlayerActivity 直接实现了 MusicPlaybackFragmentListener 接口
        if (context instanceof MusicPlaybackFragmentListener) {
            listener = (MusicPlaybackFragmentListener) context;
            Log.d(TAG, "onAttach: Listener attached.");
        } else {
            // 如果宿主 Activity 没有实现该接口，则抛出运行时异常或记录错误
            Log.e(TAG, "onAttach: The hosting Activity must implement MusicPlaybackFragmentListener");
            // throw new RuntimeException(context.toString() + " must implement MusicPlaybackFragmentListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            musicInfo = (MusicInfo) getArguments().getSerializable(ARG_MUSIC_INFO);
        }
        Log.d(TAG, "onCreate: Fragment created. MusicInfo: " + (musicInfo != null ? musicInfo.getMusicName() : "null"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: View inflated.");
        View view = inflater.inflate(R.layout.fragment_music_playback, container, false);
        ivMusicCoverPlayer = view.findViewById(R.id.iv_music_cover_player);
        Log.d(TAG, "onCreateView: ivMusicCoverPlayer initialized.");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (musicInfo != null) {
            loadMusicCover(musicInfo.getCoverUrl());
        }
        setupCoverAnimator();
        Log.d(TAG, "onViewCreated: Cover animator setup.");

        // 【新增】在 Fragment 的视图创建完成后，通知 Activity 自身已准备好
        if (listener != null) {
            listener.onMusicPlaybackFragmentReady(this);
        }
        // 此处不再调用 updateMusicInfoAndPlayState，它将由 Activity 在收到 onMusicPlaybackFragmentReady 后统一同步
    }

    /**
     * 加载音乐封面图片
     * @param coverUrl 封面图片 URL
     */
    private void loadMusicCover(String coverUrl) {
        Log.d(TAG, "loadMusicCover: Loading cover from URL: " + coverUrl);
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CenterCrop(), new CircleCrop());

        Glide.with(this)
                .load(coverUrl)
                .apply(requestOptions)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(ivMusicCoverPlayer);
    }

    /**
     * 设置封面图片旋转动画
     */
    private void setupCoverAnimator() {
        coverAnimator = ObjectAnimator.ofFloat(ivMusicCoverPlayer, "rotation", 0f, 360f);
        coverAnimator.setDuration(20000);
        coverAnimator.setInterpolator(new LinearInterpolator());
        coverAnimator.setRepeatCount(ValueAnimator.INFINITE);
        coverAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    /**
     * 启动封面旋转动画
     */
    public void startCoverAnimation() {
        if (coverAnimator != null) {
            if (!coverAnimator.isRunning()) {
                coverAnimator.start();
                Log.d(TAG, "startCoverAnimation: Animator started.");
            } else if (coverAnimator.isPaused()) {
                coverAnimator.resume();
                Log.d(TAG, "startCoverAnimation: Animator resumed.");
            } else {
                Log.d(TAG, "startCoverAnimation: Animator already running.");
            }
        } else {
            Log.w(TAG, "startCoverAnimation: coverAnimator is null.");
        }
    }

    /**
     * 暂停封面旋转动画
     */
    public void pauseCoverAnimation() {
        if (coverAnimator != null) {
            if (coverAnimator.isRunning()) {
                coverAnimator.pause();
                Log.d(TAG, "pauseCoverAnimation: Animator paused.");
            } else if (coverAnimator.isStarted() && !coverAnimator.isPaused()) {
                coverAnimator.pause();
                Log.d(TAG, "pauseCoverAnimation: Animator paused (from started but not running).");
            } else {
                Log.d(TAG, "pauseCoverAnimation: Animator not running or already paused. isRunning: " + coverAnimator.isRunning() + ", isPaused: " + coverAnimator.isPaused());
            }
        } else {
            Log.w(TAG, "pauseCoverAnimation: coverAnimator is null.");
        }
    }


    /**
     * 停止封面旋转动画 (取消并回到初始状态)
     */
    public void stopCoverAnimation() {
        if (coverAnimator != null) {
            if (coverAnimator.isRunning() || coverAnimator.isPaused() || coverAnimator.isStarted()) {
                coverAnimator.cancel();
                Log.d(TAG, "stopCoverAnimation: Animator cancelled.");
            } else {
                Log.d(TAG, "stopCoverAnimation: Animator not active. isRunning: " + coverAnimator.isRunning() + ", isPaused: " + coverAnimator.isPaused());
            }
        } else {
            Log.w(TAG, "stopCoverAnimation: coverAnimator is null.");
        }
    }

    /**
     * 【优化】更新 Fragment 的音乐信息和播放状态，并根据播放状态控制动画
     * 此方法现在是动画控制的唯一入口，确保动画状态与播放状态同步
     * @param newMusicInfo 新的音乐信息
     * @param newIsPlaying 新的播放状态
     */
    public void updateMusicInfoAndPlayState(MusicInfo newMusicInfo, boolean newIsPlaying) {
        Log.d(TAG, "updateMusicInfoAndPlayState: newMusicInfo=" + (newMusicInfo != null ? newMusicInfo.getMusicName() : "null") + ", newIsPlaying=" + newIsPlaying + ", currentIsPlaying=" + this.isPlaying);

        // 1. 更新音乐信息并重新加载封面（如果音乐发生变化）
        if (newMusicInfo != null && !newMusicInfo.equals(this.musicInfo)) {
            this.musicInfo = newMusicInfo;
            if (ivMusicCoverPlayer != null) {
                loadMusicCover(this.musicInfo.getCoverUrl());
                Log.d(TAG, "updateMusicInfoAndPlayState: MusicInfo updated, cover reloaded.");
            } else {
                Log.w(TAG, "updateMusicInfoAndPlayState: ivMusicCoverPlayer is null, cannot load cover.");
            }
        } else if (newMusicInfo == null && this.musicInfo != null) {
            this.musicInfo = null;
            if (ivMusicCoverPlayer != null) {
                ivMusicCoverPlayer.setImageDrawable(null); // 清空图片
            }
            Log.d(TAG, "updateMusicInfoAndPlayState: MusicInfo set to null, cover cleared.");
        }

        // 2. 根据新的播放状态控制动画
        if (this.isPlaying != newIsPlaying) {
            this.isPlaying = newIsPlaying; // 更新 Fragment 内部的播放状态

            if (coverAnimator != null) {
                if (this.isPlaying) {
                    startCoverAnimation(); // 启动或恢复动画
                } else {
                    pauseCoverAnimation(); // 暂停动画
                }
            } else {
                Log.w(TAG, "updateMusicInfoAndPlayState: coverAnimator is null, cannot control animation.");
            }
        } else {
            Log.d(TAG, "updateMusicInfoAndPlayState: isPlaying state unchanged (" + newIsPlaying + "). No animation control needed.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCoverAnimation();
        Log.d(TAG, "onDestroyView: Animation stopped.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Fragment destroyed.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // 【修改】在 Fragment 从 Activity 分离时，通知 Activity 清空引用
        if (listener != null) {
            listener.onMusicPlaybackFragmentDetached();
        }
        listener = null; // 清空监听器引用，防止内存泄漏
        Log.d(TAG, "onDetach: Listener detached.");
    }
}
