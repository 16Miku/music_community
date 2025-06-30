package com.example.music_community;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_community.adapter.PlaylistAdapter;
import com.example.music_community.model.MusicInfo;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaylistDialogFragment extends BottomSheetDialogFragment implements PlaylistAdapter.OnPlaylistItemActionListener {

    private static final String TAG = "PlaylistDialogFragment";

    private RecyclerView recyclerViewPlaylist;
    private TextView tvSongCount;
    private ImageView ivClosePlaylist;
    private ImageView ivLoopModePlaylist;
    private TextView tvLoopModeText;
    private ImageView ivCollectAll;
    private ImageView ivClearPlaylist;

    private PlaylistAdapter playlistAdapter;
    private List<MusicInfo> currentPlaylist = new ArrayList<>();

    // MusicPlayerService 相关
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;
    private MusicPlayerService.OnMusicPlayerEventListener serviceEventListener;

    // 服务连接对象
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "MusicPlayerService connected to PlaylistDialogFragment");

            // 添加服务监听器
            musicPlayerService.addOnMusicPlayerEventListener(serviceEventListener);

            // 首次连接时更新UI
            updatePlaylistUI(musicPlayerService.getPlaylist(), musicPlayerService.getCurrentMusic(), musicPlayerService.getLoopMode());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            Log.d(TAG, "MusicPlayerService disconnected from PlaylistDialogFragment");
            if (musicPlayerService != null) {
                musicPlayerService.removeOnMusicPlayerEventListener(serviceEventListener);
            }
        }
    };

    public PlaylistDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 在这里绑定服务，确保服务已启动
        Intent serviceIntent = new Intent(context, MusicPlayerService.class);
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置样式，例如透明背景，否则默认会有白色背景
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_playlist, container, false);

        tvSongCount = view.findViewById(R.id.tv_song_count);
        ivClosePlaylist = view.findViewById(R.id.iv_close_playlist);
        ivLoopModePlaylist = view.findViewById(R.id.iv_loop_mode_playlist);
        tvLoopModeText = view.findViewById(R.id.tv_loop_mode_text);
        ivCollectAll = view.findViewById(R.id.iv_collect_all);
        ivClearPlaylist = view.findViewById(R.id.iv_clear_playlist);
        recyclerViewPlaylist = view.findViewById(R.id.recyclerView_playlist);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playlistAdapter = new PlaylistAdapter(currentPlaylist, this); // this 实现 OnPlaylistItemActionListener
        recyclerViewPlaylist.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPlaylist.setAdapter(playlistAdapter);

        setupListeners();
        initServiceEventListener(); // 初始化服务事件监听器
    }

    private void setupListeners() {
        ivClosePlaylist.setOnClickListener(v -> dismiss());

        ivLoopModePlaylist.setOnClickListener(v -> {
            if (isServiceBound && musicPlayerService != null) {
                musicPlayerService.switchLoopMode();
                MusicPlayerService.LoopMode currentLoopMode = musicPlayerService.getLoopMode();
                updateLoopModeIcon(currentLoopMode);
                updateLoopModeText(currentLoopMode);
            }
        });

        ivCollectAll.setOnClickListener(v -> {
            Toast.makeText(getContext(), "收藏全部功能待开发", Toast.LENGTH_SHORT).show();
        });

        ivClearPlaylist.setOnClickListener(v -> {
            // TODO: 实现清空播放列表逻辑
            Toast.makeText(getContext(), "清空播放列表功能待开发", Toast.LENGTH_SHORT).show();
        });
    }

    // 初始化服务事件监听器
    private void initServiceEventListener() {
        serviceEventListener = new MusicPlayerService.OnMusicPlayerEventListener() {
            @Override
            public void onMusicPrepared(MusicInfo musicInfo) {
                // 歌曲准备好播放，更新当前播放状态
                updatePlaylistUI(musicPlayerService.getPlaylist(), musicPlayerService.getCurrentMusic(), musicPlayerService.getLoopMode());
            }

            @Override
            public void onMusicPlayStatusChanged(boolean isPlaying, MusicInfo musicInfo) {
                // 播放状态改变，更新当前播放状态
                updatePlaylistUI(musicPlayerService.getPlaylist(), musicPlayerService.getCurrentMusic(), musicPlayerService.getLoopMode());
            }

            @Override
            public void onMusicCompleted(MusicInfo nextMusicInfo) {
                // 歌曲播放完成，更新当前播放状态
                updatePlaylistUI(musicPlayerService.getPlaylist(), musicPlayerService.getCurrentMusic(), musicPlayerService.getLoopMode());
            }

            @Override
            public void onMusicError(String errorMessage) {
                Toast.makeText(getContext(), "播放出错: " + errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoopModeChanged(MusicPlayerService.LoopMode newMode) {
                updateLoopModeIcon(newMode);
                updateLoopModeText(newMode);
            }

            @Override
            public void onPlaylistChanged(List<MusicInfo> newPlaylist) {
                // 播放列表改变，更新列表UI
                updatePlaylistUI(newPlaylist, musicPlayerService.getCurrentMusic(), musicPlayerService.getLoopMode());
            }
        };
    }

    // 更新播放列表UI的通用方法
    private void updatePlaylistUI(List<MusicInfo> playlist, MusicInfo currentPlayingMusic, MusicPlayerService.LoopMode loopMode) {
        if (playlist == null) {
            playlist = new ArrayList<>(); // 避免空指针
        }
        currentPlaylist.clear();
        currentPlaylist.addAll(playlist);
        playlistAdapter.setCurrentPlayingMusic(currentPlayingMusic); // 通知适配器当前播放的歌曲
        playlistAdapter.notifyDataSetChanged();
        tvSongCount.setText(String.format(Locale.getDefault(), "(%d首)", currentPlaylist.size()));

        updateLoopModeIcon(loopMode);
        updateLoopModeText(loopMode);
    }

    private void updateLoopModeIcon(MusicPlayerService.LoopMode mode) {
        if (ivLoopModePlaylist == null) return;
        switch (mode) {
            case SEQUENCE:
                ivLoopModePlaylist.setImageResource(R.drawable.ic_loop_sequence);
                break;
            case SINGLE:
                ivLoopModePlaylist.setImageResource(R.drawable.ic_loop_one);
                break;
            case SHUFFLE:
                ivLoopModePlaylist.setImageResource(R.drawable.ic_loop_shuffle);
                break;
        }
    }

    private void updateLoopModeText(MusicPlayerService.LoopMode mode) {
        if (tvLoopModeText == null) return;
        switch (mode) {
            case SEQUENCE:
                tvLoopModeText.setText("顺序播放");
                break;
            case SINGLE:
                tvLoopModeText.setText("单曲循环");
                break;
            case SHUFFLE:
                tvLoopModeText.setText("随机播放");
                break;
        }
    }

    // 实现 PlaylistAdapter.OnPlaylistItemActionListener 接口方法
    @Override
    public void onPlaylistItemClick(MusicInfo musicInfo, int position) {
        if (isServiceBound && musicPlayerService != null) {
            musicPlayerService.setPlayListAndIndex(musicPlayerService.getPlaylist(), position); // 播放点击的歌曲
            // 播放后通常会跳转到播放页面，这里不主动关闭弹窗，由用户决定
        }
    }

    @Override
    public void onPlaylistItemDelete(MusicInfo musicInfo, int position) {
        if (isServiceBound && musicPlayerService != null) {
            List<MusicInfo> currentList = new ArrayList<>(musicPlayerService.getPlaylist());  // 获取播放列表的拷贝

            if (currentList != null && position >= 0 && position < currentList.size()) {
                MusicInfo currentPlayingInService = musicPlayerService.getCurrentMusic(); // 获取当前服务中正在播放的歌曲

                // 1. 从播放列表中移除歌曲
                currentList.remove(position);

                // 2. 更新服务中的播放列表
                // updatePlaylist 方法会处理 currentMusicIndex 的调整，并通知 onPlaylistChanged
                musicPlayerService.updatePlaylist(currentList);

                // 3. 处理播放逻辑
                if (musicInfo.equals(currentPlayingInService)) { // 如果删除的是当前播放歌曲


                    if (currentList.isEmpty()) {
                        // 播放列表为空，服务已通过 updatePlaylist -> clearCurrentMusicAndStop 停止
                        dismiss(); // 关闭播放列表弹窗

                        // 如果是从 MusicPlayerActivity 删除的，则关闭该 Activity
                        if (getActivity() instanceof MusicPlayerActivity) {
                            MusicPlayerActivity activity = (MusicPlayerActivity) getActivity();
                            activity.finish();
                        }
                        // 服务停止已在 clearCurrentMusicAndStop 中处理，MainActivity 会收到通知隐藏悬浮窗
                    }
                    // 2.2 播放列表非空
                    else {
                        // 2.2.1 顺序模式和单曲循环
                        if (musicPlayerService.getLoopMode() == MusicPlayerService.LoopMode.SEQUENCE ||
                                musicPlayerService.getLoopMode() == MusicPlayerService.LoopMode.SINGLE) {
                            musicPlayerService.playNext();
                        }
                        // 2.2.2 随机模式
                        else {
                            musicPlayerService.playNext();
                        }
                    }
                }
                // 如果删除的不是当前播放歌曲，服务会自行调整 currentMusicIndex (通过 updatePlaylist 中的逻辑)
                // 并且不会停止当前播放。



                //  3. 更新播放列表
                musicPlayerService.setPlayListAndIndex(currentList,0);
                // 4. 通知所有监听器播放列表已更改
                for (MusicPlayerService.OnMusicPlayerEventListener listener : musicPlayerService.listeners) {
                    listener.onPlaylistChanged(currentList);
                }




                // 4. 更新UI (updatePlaylist 已经通知了 onPlaylistChanged，这里再刷新一下列表适配器)
                updatePlaylistUI(musicPlayerService.getPlaylist(), musicPlayerService.getCurrentMusic(), musicPlayerService.getLoopMode());
                playlistAdapter.notifyDataSetChanged();
            }
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // 解绑服务
        if (isServiceBound) {
            musicPlayerService.removeOnMusicPlayerEventListener(serviceEventListener);
            if (getContext() != null) {
                getContext().unbindService(serviceConnection);
            }
            isServiceBound = false;
        }
        Log.d(TAG, "PlaylistDialogFragment onDetach");
    }
}
