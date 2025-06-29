package com.example.music_community;

import android.animation.ObjectAnimator; // 【新增】
import android.animation.ValueAnimator; // 【新增】
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator; // 【新增】
import android.widget.ImageView;
import com.example.music_community.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide; // 【新增】
import com.bumptech.glide.load.resource.bitmap.CenterCrop; // 【新增】
import com.bumptech.glide.load.resource.bitmap.RoundedCorners; // 【新增】
import com.bumptech.glide.request.RequestOptions; // 【新增】
import com.example.music_community.model.MusicInfo;

public class MusicPlaybackFragment extends Fragment {

    private static final String ARG_MUSIC_INFO = "arg_music_info"; // Fragment 参数键

    private ImageView ivMusicCoverPlayer;
    private ObjectAnimator coverAnimator; // 封面旋转动画

    private MusicInfo musicInfo; // 当前 Fragment 显示的音乐信息

    /**
     * 创建 MusicPlaybackFragment 实例的工厂方法
     * @param musicInfo 要显示的音乐信息
     * @return MusicPlaybackFragment 实例
     */
    public static MusicPlaybackFragment newInstance(MusicInfo musicInfo) {

        MusicPlaybackFragment fragment = new MusicPlaybackFragment();

        Bundle args = new Bundle();

        args.putSerializable(ARG_MUSIC_INFO, musicInfo); // 传递 MusicInfo 对象

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

            musicInfo = (MusicInfo) getArguments().getSerializable(ARG_MUSIC_INFO);

        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_music_playback, container, false);

        ivMusicCoverPlayer = view.findViewById(R.id.iv_music_cover_player);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (musicInfo != null) {
            loadMusicCover(musicInfo.getCoverUrl());
        }
        setupCoverAnimator(); // 设置旋转动画
    }

    /**
     * 加载音乐封面图片
     * @param coverUrl 封面图片 URL
     */
    private void loadMusicCover(String coverUrl) {

        // 使用 Glide 加载图片，并应用圆角转换
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CenterCrop(), new RoundedCorners(24)); // 圆角半径24dp

        Glide.with(this)
                .load(coverUrl)
                .apply(requestOptions)
                .placeholder( R.drawable.ic_launcher_foreground) // 占位图
                .error( R.drawable.ic_launcher_foreground) // 错误图
                .into(ivMusicCoverPlayer);
    }


    /**
     * 设置封面图片旋转动画
     */
    private void setupCoverAnimator() {

        coverAnimator = ObjectAnimator.ofFloat(ivMusicCoverPlayer, "rotation", 0f, 360f);

        coverAnimator.setDuration(20000); // 20秒转一圈

        coverAnimator.setInterpolator(new LinearInterpolator()); // 匀速旋转

        coverAnimator.setRepeatCount(ValueAnimator.INFINITE); // 无限循环

        coverAnimator.setRepeatMode(ValueAnimator.RESTART); // 重新开始

    }

    /**
     * 启动封面旋转动画
     */
    public void startCoverAnimation() {

        if (coverAnimator != null && !coverAnimator.isRunning()) {

            coverAnimator.start();

        }
    }

    /**
     * 暂停封面旋转动画
     */
    public void pauseCoverAnimation() {

        if (coverAnimator != null && coverAnimator.isRunning()) {

            coverAnimator.pause();

        }
    }

    /**
     * 恢复封面旋转动画
     */
    public void resumeCoverAnimation() {

        if (coverAnimator != null && coverAnimator.isPaused()) {

            coverAnimator.resume();

        }

    }

    /**
     * 停止封面旋转动画
     */
    public void stopCoverAnimation() {

        if (coverAnimator != null) {

            coverAnimator.cancel(); // 取消动画，并回到初始状态

        }
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        stopCoverAnimation(); // 视图销毁时停止动画

    }
}
