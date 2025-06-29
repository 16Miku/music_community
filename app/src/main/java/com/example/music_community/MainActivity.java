package com.example.music_community;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.music_community.adapter.HomePageAdapter;
import com.example.music_community.adapter.MusicItemAdapter;
import com.example.music_community.api.MusicApiService;
import com.example.music_community.model.HomePageInfo;
import com.example.music_community.model.HomePageResponse;
import com.example.music_community.model.MusicInfo;
import com.example.music_community.network.RetrofitClient;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements MusicItemAdapter.OnMusicItemPlayListener {

    private static final String TAG = "MainActivity";

    private SmartRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private HomePageAdapter homePageAdapter;
    private List<HomePageInfo> homePageDataList = new ArrayList<>();

    // 悬浮播放器控件
    private ConstraintLayout floatingPlayerContainer;
    private ImageView ivFloatingCover;
    private TextView tvFloatingMusicName;
    private TextView tvFloatingAuthor;
    private ImageView ivFloatingPlayPause;
    private ImageView ivFloatingPlaylist;
    private ProgressBar progressBarFloating;

    // MusicPlayerService 相关
    private MusicPlayerService musicPlayerService;
    private boolean isServiceBound = false;
    private MusicPlayerService.OnMusicPlayerEventListener musicPlayerListener;

    private int currentPage = 1;
    private final int pageSize = 5;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isServiceBound && musicPlayerService.isPlaying()) {
                updateFloatingPlayerProgress();
            }
            progressHandler.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicPlayerBinder binder = (MusicPlayerService.MusicPlayerBinder) service;
            musicPlayerService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "MusicPlayerService connected");

            musicPlayerService.addOnMusicPlayerEventListener(musicPlayerListener);

            // 连接成功后，立即同步UI状态
            updateFloatingPlayerUI(musicPlayerService.getCurrentMusic(), musicPlayerService.isPlaying());
            if (musicPlayerService.isPlaying()) {
                startUpdatingProgress();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            Log.d(TAG, "MusicPlayerService disconnected");
            if (musicPlayerService != null) {
                musicPlayerService.removeOnMusicPlayerEventListener(musicPlayerListener);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initHomePage();
        initMusicPlayerListener();
        setupFloatingPlayerListeners();

        Intent serviceIntent = new Intent(this, MusicPlayerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 当Activity返回前台时，再次同步UI状态
        if (isServiceBound) {
            updateFloatingPlayerUI(musicPlayerService.getCurrentMusic(), musicPlayerService.isPlaying());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            musicPlayerService.removeOnMusicPlayerEventListener(musicPlayerListener);
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopUpdatingProgress();
    }

    private void initViews() {
        refreshLayout = findViewById(R.id.refreshLayout);
        recyclerView = findViewById(R.id.recyclerView);

        floatingPlayerContainer = findViewById(R.id.floating_player);

        if (floatingPlayerContainer != null) {
            ivFloatingCover = floatingPlayerContainer.findViewById(R.id.iv_floating_cover);
            tvFloatingMusicName = floatingPlayerContainer.findViewById(R.id.tv_floating_music_name);
            tvFloatingAuthor = floatingPlayerContainer.findViewById(R.id.tv_floating_author);
            ivFloatingPlayPause = floatingPlayerContainer.findViewById(R.id.iv_floating_play_pause);
            ivFloatingPlaylist = floatingPlayerContainer.findViewById(R.id.iv_floating_playlist);
            progressBarFloating = floatingPlayerContainer.findViewById(R.id.progress_bar_floating);
        } else {
            Log.e(TAG, "CRITICAL: Floating player container not found in layout!");
        }
    }

    private void initHomePage() {
        homePageAdapter = new HomePageAdapter(homePageDataList, this); // 将 this 作为播放监听器传入
        recyclerView.setAdapter(homePageAdapter);

        refreshLayout.setOnRefreshListener(refreshlayout -> {
            currentPage = 1;
            fetchHomePageData(true);
        });

        refreshLayout.setOnLoadMoreListener(refreshlayout -> {
            currentPage++;
            fetchHomePageData(false);
        });

        refreshLayout.autoRefresh();
    }

    private void initMusicPlayerListener() {
        musicPlayerListener = new MusicPlayerService.OnMusicPlayerEventListener() {
            @Override
            public void onMusicPrepared(MusicInfo musicInfo) {
                updateFloatingPlayerUI(musicInfo, true);
                startUpdatingProgress();
            }

            @Override
            public void onMusicPlayStatusChanged(boolean isPlaying, MusicInfo musicInfo) {
                updateFloatingPlayerUI(musicInfo, isPlaying);
                if (isPlaying) {
                    startUpdatingProgress();
                } else {
                    stopUpdatingProgress();
                }
            }

            @Override
            public void onMusicCompleted(MusicInfo nextMusicInfo) {
                updateFloatingPlayerUI(nextMusicInfo, true);
            }

            @Override
            public void onPlaylistChanged(List<MusicInfo> newPlaylist) {
                // 当前用不到，但为以后功能预留
            }

            @Override
            public void onMusicError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                updateFloatingPlayerUI(null, false);
            }

            @Override
            public void onLoopModeChanged(MusicPlayerService.LoopMode newMode) {
                // 悬浮窗不显示循环模式，暂不处理
            }
        };
    }

    private void setupFloatingPlayerListeners() {
        if (floatingPlayerContainer == null) {
            Log.e(TAG, "Cannot setup listeners, floating player container is null.");
            return;
        }

        floatingPlayerContainer.setOnClickListener(v -> {
            // 点击整个悬浮窗，打开全屏播放页
            Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
            startActivity(intent);
            // 使用自定义动画 (从底部向上滑入)
            overridePendingTransition(R.anim.slide_in_up, 0); // 0 表示当前Activity的退出动画为无
        });

        ivFloatingPlayPause.setOnClickListener(v -> {
            if (isServiceBound) {
                musicPlayerService.togglePlayPause();
            }
        });

        ivFloatingPlaylist.setOnClickListener(v -> {
            Toast.makeText(this, "播放列表功能待开发", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFloatingPlayerUI(MusicInfo musicInfo, boolean isPlaying) {
        if (floatingPlayerContainer == null) return;

        if (musicInfo != null) {
            floatingPlayerContainer.setVisibility(View.VISIBLE);
            tvFloatingMusicName.setText(musicInfo.getMusicName());
            tvFloatingAuthor.setText(musicInfo.getAuthor());
            Glide.with(this).load(musicInfo.getCoverUrl()).into(ivFloatingCover);
            ivFloatingPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_big : R.drawable.ic_play_big);
            updateFloatingPlayerProgress();
        } else {
            floatingPlayerContainer.setVisibility(View.GONE);
        }
    }

    private void updateFloatingPlayerProgress() {
        if (isServiceBound && musicPlayerService.getDuration() > 0) {
            progressBarFloating.setMax(musicPlayerService.getDuration());
            progressBarFloating.setProgress(musicPlayerService.getCurrentPosition());
        }
    }

    private void startUpdatingProgress() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    private void stopUpdatingProgress() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    public void onPlayMusic(List<MusicInfo> musicList, int position) {
        if (isServiceBound) {
            // 1. 通知服务播放歌曲
            musicPlayerService.setPlayListAndIndex(musicList, position);
            Log.d(TAG, "onPlayMusic: Sent new playlist to service. Song: " + musicList.get(position).getMusicName());

            // 2. 【核心修复】启动 MusicPlayerActivity
            Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
            startActivity(intent);
            // 使用自定义动画 (从底部向上滑入)
            overridePendingTransition(R.anim.slide_in_up, 0); // 0 表示当前Activity的退出动画为无
        } else {
            Toast.makeText(this, "播放服务尚未准备好", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchHomePageData(final boolean isRefresh) {
        MusicApiService apiService = RetrofitClient.getApiService(MusicApiService.class);
        apiService.getHomePageData(currentPage, pageSize).enqueue(new Callback<HomePageResponse>() {
            @Override
            public void onResponse(@NonNull Call<HomePageResponse> call, @NonNull Response<HomePageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    HomePageResponse homePageResponse = response.body();
                    if (homePageResponse.getCode() == 200) {
                        List<HomePageInfo> newRecords = homePageResponse.getData().getRecords();
                        if (isRefresh) {
                            homePageDataList.clear();
                        }
                        if (newRecords != null && !newRecords.isEmpty()) {
                            homePageDataList.addAll(newRecords);
                        } else if (!isRefresh) {
                            Toast.makeText(MainActivity.this, "没有更多数据了", Toast.LENGTH_SHORT).show();
                            refreshLayout.finishLoadMoreWithNoMoreData();
                        }
                        homePageAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(MainActivity.this, "请求失败: " + homePageResponse.getMsg(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "网络请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                }

                if (isRefresh) {
                    refreshLayout.finishRefresh();
                } else {
                    refreshLayout.finishLoadMore();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HomePageResponse> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "网络异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (isRefresh) {
                    refreshLayout.finishRefresh(false);
                } else {
                    refreshLayout.finishLoadMore(false);
                }
            }
        });
    }
}
