package com.example.music_community.api;

import com.example.music_community.model.HomePageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MusicApiService {

    // 定义获取音乐首页数据的 GET 请求
    // Path: /music/homePage
    // Query Parameters: current (当前页), size (每页大小)
    @GET("music/homePage")
    Call<HomePageResponse> getHomePageData(
            @Query("current") int current, // 当前页码
            @Query("size") int size // 每页大小
    );
}

