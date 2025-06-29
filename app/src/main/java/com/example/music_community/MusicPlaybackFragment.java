package com.example.music_community;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable; // 导入 Drawable
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource; // 导入 DataSource
import com.bumptech.glide.load.engine.GlideException; // 导入 GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener; // 导入 RequestListener
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target; // 导入 Target
import com.example.music_community.model.MusicInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.music_community.MusicPlaybackFragmentListener;


public class MusicPlaybackFragment extends Fragment {

    private static final String TAG = "MusicPlaybackFragment";
    private static final String ARG_MUSIC_INFO = "arg_music_info";

    private ImageView ivMusicCoverPlayer;
    private ObjectAnimator coverAnimator;

    private MusicInfo musicInfo;
    private boolean isPlaying = false;

    private MusicPlaybackFragmentListener listener;


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
        if (context instanceof MusicPlaybackFragmentListener) {
            listener = (MusicPlaybackFragmentListener) context;
            Log.d(TAG, "onAttach: Listener attached.");
        } else {
            Log.e(TAG, "onAttach: The hosting Activity must implement MusicPlaybackFragmentListener");
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
        } else {
            Log.w(TAG, "onViewCreated: musicInfo is null, cannot load cover.");
        }
        setupCoverAnimator();
        Log.d(TAG, "onViewCreated: Cover animator setup.");

        // 【新增】检查 ivMusicCoverPlayer 的尺寸，确保它不是 0x0
        if (ivMusicCoverPlayer != null) {
            ivMusicCoverPlayer.post(() -> {
                Log.d(TAG, "ivMusicCoverPlayer dimensions: " + ivMusicCoverPlayer.getWidth() + "x" + ivMusicCoverPlayer.getHeight());
            });
        }

        if (listener != null) {
            listener.onMusicPlaybackFragmentReady(this);
        }
    }

    private void loadMusicCover(String coverUrl) {
        Log.d(TAG, "loadMusicCover: Loading cover from URL: " + coverUrl);
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CenterCrop(), new CircleCrop());

        Glide.with(this)
                .load(coverUrl)
                .apply(requestOptions)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .listener(new RequestListener<Drawable>() { // 使用 Drawable 作为类型
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Glide: Cover image load FAILED!", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Glide: Cover image loaded successfully.");
                        return false;
                    }
                })
                .into(ivMusicCoverPlayer);
    }

    private void setupCoverAnimator() {
        coverAnimator = ObjectAnimator.ofFloat(ivMusicCoverPlayer, "rotation", 0f, 360f);
        coverAnimator.setDuration(20000);
        coverAnimator.setInterpolator(new LinearInterpolator());
        coverAnimator.setRepeatCount(ValueAnimator.INFINITE);
        coverAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

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

    public void updateMusicInfoAndPlayState(MusicInfo newMusicInfo, boolean newIsPlaying) {
        Log.d(TAG, "updateMusicInfoAndPlayState: newMusicInfo=" + (newMusicInfo != null ? newMusicInfo.getMusicName() : "null") + ", newIsPlaying=" + newIsPlaying + ", currentIsPlaying=" + this.isPlaying);

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
                ivMusicCoverPlayer.setImageDrawable(null);
            }
            Log.d(TAG, "updateMusicInfoAndPlayState: MusicInfo set to null, cover cleared.");
        }

        if (this.isPlaying != newIsPlaying) {
            this.isPlaying = newIsPlaying;
            if (coverAnimator != null) {
                if (this.isPlaying) {
                    startCoverAnimation();
                } else {
                    pauseCoverAnimation();
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
        if (listener != null) {
            listener.onMusicPlaybackFragmentDetached();
        }
        listener = null;
        Log.d(TAG, "onDetach: Listener detached.");
    }
}
