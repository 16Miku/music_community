package com.example.music_community;

import android.os.Bundle;
import android.util.Log; // 导入 Log 用于调试输出
import android.widget.Toast; // 导入 Toast

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_community.adapter.HomePageAdapter;
import com.example.music_community.api.MusicApiService; // 导入 MusicApiService
import com.example.music_community.model.HomePageInfo; // 导入 HomePageInfo
import com.example.music_community.model.HomePageResponse; // 导入 HomePageResponse
import com.example.music_community.network.RetrofitClient; // 导入 RetrofitClient
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.util.ArrayList; // 导入 ArrayList
import java.util.List; // 导入 List

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // 用于日志输出的 TAG

    private SmartRefreshLayout refreshLayout; // 下拉刷新和上拉加载布局
    private RecyclerView recyclerView; // 音乐首页内容列表
    private HomePageAdapter homePageAdapter; // RecyclerView 的适配器
    private List<HomePageInfo> homePageDataList = new ArrayList<>(); // 存储首页数据列表

    private int currentPage = 1; // 当前页码
    private final int pageSize = 5; // 每页大小，根据文档使用 size=5 模拟上拉加载更多

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // 如果需要全屏显示且内容延伸到系统栏，可以保留
        setContentView(R.layout.activity_main);

        // 初始化视图
        refreshLayout = findViewById(R.id.refreshLayout);
        recyclerView = findViewById(R.id.recyclerView);


        // 初始化适配器并设置给 RecyclerView
        homePageAdapter = new HomePageAdapter(homePageDataList);
        recyclerView.setAdapter(homePageAdapter);

        // 设置下拉刷新监听器
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshlayout) {
                // 执行刷新操作，通常是从第一页开始重新加载数据
                currentPage = 1; // 重置页码
                fetchHomePageData(true); // 传入 true 表示是刷新操作
            }
        });

        // 设置上拉加载更多监听器
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshlayout) {
                // 执行加载更多操作，页码递增
                currentPage++; // 页码递增
                fetchHomePageData(false); // 传入 false 表示是加载更多操作
            }
        });

        // 首次进入页面，自动触发一次下拉刷新，加载第一页数据
        refreshLayout.autoRefresh();
    }

    /**
     * 从网络获取首页数据
     * @param isRefresh 是否是下拉刷新操作 (true: 刷新, false: 加载更多)
     */
    private void fetchHomePageData(final boolean isRefresh) {
        // 获取 MusicApiService 实例
        MusicApiService apiService = RetrofitClient.getApiService(MusicApiService.class);

        // 发起网络请求
        apiService.getHomePageData(currentPage, pageSize).enqueue(new Callback<HomePageResponse>() {
            @Override
            public void onResponse(@NonNull Call<HomePageResponse> call, @NonNull Response<HomePageResponse> response) {
                // 请求成功回调
                if (response.isSuccessful() && response.body() != null) {
                    HomePageResponse homePageResponse = response.body();
                    if (homePageResponse.getCode() == 200) { // 检查业务返回码
                        List<HomePageInfo> newRecords = homePageResponse.getData().getRecords();

                        if (isRefresh) {
                            // 如果是刷新操作，清空旧数据并添加新数据
                            homePageDataList.clear();
                            homePageDataList.addAll(newRecords);
                            homePageAdapter.notifyDataSetChanged(); // 通知适配器数据已改变
                            refreshLayout.finishRefresh(true); // 结束刷新状态
                            refreshLayout.setEnableLoadMore(true); // 刷新成功后重新启用加载更多
                            Log.d(TAG, "首页数据刷新成功，当前数据量：" + homePageDataList.size());
                        } else {
                            // 如果是加载更多操作，添加新数据
                            if (newRecords != null && !newRecords.isEmpty()) {
                                homePageDataList.addAll(newRecords);
                                homePageAdapter.notifyDataSetChanged(); // 通知适配器数据已改变
                                refreshLayout.finishLoadMore(true); // 结束加载更多状态
                                Log.d(TAG, "首页数据加载更多成功，新增：" + newRecords.size() + " 条，当前总数据量：" + homePageDataList.size());
                            } else {
                                // 没有更多数据了
                                refreshLayout.finishLoadMoreWithNoMoreData(); // 结束加载更多，显示没有更多数据
                                Toast.makeText(MainActivity.this, "没有更多数据了", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "没有更多数据了");
                            }
                        }
                    } else {
                        // 业务返回码不为 200，显示错误信息
                        Toast.makeText(MainActivity.this, "请求失败: " + homePageResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "API 业务错误: " + homePageResponse.getMsg());
                        if (isRefresh) {
                            refreshLayout.finishRefresh(false); // 结束刷新，表示失败
                        } else {
                            refreshLayout.finishLoadMore(false); // 结束加载，表示失败
                        }
                    }
                } else {
                    // HTTP 响应不成功 (例如 404, 500) 或响应体为空
                    Toast.makeText(MainActivity.this, "网络请求失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "HTTP 错误: " + response.code() + ", Message: " + response.message());
                    if (isRefresh) {
                        refreshLayout.finishRefresh(false);
                    } else {
                        refreshLayout.finishLoadMore(false);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<HomePageResponse> call, @NonNull Throwable t) {
                // 请求失败（例如网络连接问题）
                Toast.makeText(MainActivity.this, "网络异常: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "网络请求异常", t);
                if (isRefresh) {
                    refreshLayout.finishRefresh(false); // 结束刷新，表示失败
                } else {
                    refreshLayout.finishLoadMore(false); // 结束加载，表示失败
                }
            }
        });
    }
}
