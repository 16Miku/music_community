package com.example.music_community.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    // 定义接口文档中提供的请求域名
    private static final String BASE_URL = "https://hotfix-service-prod.g.mi.com/";

    private static Retrofit retrofit = null;

    /**
     * 获取 Retrofit 实例
     * 使用单例模式确保 Retrofit 客户端只被创建一次
     * @return Retrofit 实例
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            // 创建 HttpLoggingInterceptor 用于打印网络请求日志
            // LEVEL.BODY 会打印请求和响应的所有内容，包括头部和体
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 创建 OkHttpClient，并添加拦截器和设置超时
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging) // 添加日志拦截器
                    .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间
                    .readTimeout(10, TimeUnit.SECONDS)    // 读取超时时间
                    .writeTimeout(10, TimeUnit.SECONDS)   // 写入超时时间
                    .build();

            // 构建 Retrofit 实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // 设置基础 URL
                    .client(okHttpClient) // 设置自定义的 OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create()) // 添加 Gson 转换器，用于 JSON 解析
                    .build();
        }
        return retrofit;
    }

    /**
     * 获取指定 API 服务的实例
     * @param serviceClass API 接口的 Class 对象，例如 MusicApiService.class
     * @param <T> API 接口类型
     * @return API 服务的实例
     */
    public static <T> T getApiService(Class<T> serviceClass) {
        return getClient().create(serviceClass);
    }
}


