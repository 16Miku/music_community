package com.example.music_community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.music_community.model.MusicInfo;

import android.util.Log; // 导入 Log 用于调试输出
import java.io.BufferedReader; // 导入 BufferedReader 用于读取文本流
import java.io.IOException;
import java.io.InputStream; // 导入 InputStream 用于获取网络流
import java.io.InputStreamReader; // 导入 InputStreamReader 用于将字节流转换为字符流
import java.net.HttpURLConnection; // 导入 HttpURLConnection 用于发起HTTP请求
import java.net.URL; // 导入 URL 用于处理URL地址
import java.util.concurrent.ExecutorService; // 导入 ExecutorService 用于管理线程池
import java.util.concurrent.Executors; // 导入 Executors 用于创建线程池




public class LyricFragment extends Fragment {

    private static final String ARG_MUSIC_INFO = "arg_music_info";

    private TextView tvLyrics;
    private MusicInfo musicInfo; // 当前 Fragment 显示的音乐信息

    private static final String TAG = "LyricFragment"; // 用于日志输出
    // 【新增】线程池，用于执行网络请求，避免在主线程进行耗时操作
    private ExecutorService executorService = Executors.newSingleThreadExecutor();




    /**
     * 创建 LyricFragment 实例的工厂方法
     * @param musicInfo 要显示的音乐信息 (包含歌词URL)
     * @return LyricFragment 实例
     */
    public static LyricFragment newInstance(MusicInfo musicInfo) {
        LyricFragment fragment = new LyricFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC_INFO, musicInfo);
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
        View view = inflater.inflate(R.layout.fragment_lyric, container, false);
        tvLyrics = view.findViewById(R.id.tv_lyrics);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 检查 musicInfo 是否存在，以及歌词 URL 是否有效
        if (musicInfo != null && musicInfo.getLyricUrl() != null && !musicInfo.getLyricUrl().isEmpty()) {
            // 【修改】调用新的方法来加载歌词
            loadLyrics(musicInfo.getLyricUrl());
        } else {
            // 如果没有歌词信息，显示相应的提示文本
            tvLyrics.setText("歌词加载失败或暂无歌词。\n当前歌曲: " + (musicInfo != null ? musicInfo.getMusicName() : "未知歌曲"));
            Log.w(TAG, "歌词URL为空或无效：" + (musicInfo != null ? musicInfo.getLyricUrl() : "null musicInfo"));
        }
    }


    /**
     * 【新增方法】从URL加载歌词内容
     * @param lyricUrl 歌词文件URL
     */
    private void loadLyrics(String lyricUrl) {
        // 在子线程中执行网络请求，避免阻塞主线程
        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            StringBuilder lyricsBuilder = new StringBuilder(); // 用于拼接歌词内容
            try {
                URL url = new URL(lyricUrl); // 根据歌词URL创建URL对象
                urlConnection = (HttpURLConnection) url.openConnection(); // 打开HTTP连接
                urlConnection.setRequestMethod("GET"); // 设置请求方法为GET
                urlConnection.connect(); // 连接到URL

                // 检查HTTP响应码是否为200 (OK)
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = urlConnection.getInputStream(); // 获取输入流
                    reader = new BufferedReader(new InputStreamReader(inputStream)); // 使用BufferedReader高效读取
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lyricsBuilder.append(line).append("\n"); // 读取每一行歌词并添加换行符
                    }
                    // 网络请求完成后，需要在主线程更新UI
                    String finalLyrics = lyricsBuilder.toString();
                    if (getActivity() != null) { // 确保Fragment仍然附加到Activity
                        getActivity().runOnUiThread(() -> {
                            tvLyrics.setText(finalLyrics); // 设置歌词文本到TextView
                            Log.d(TAG, "歌词加载成功: " + musicInfo.getMusicName());
                        });
                    }
                } else {
                    // HTTP请求失败的情况
                    String errorMessage = "歌词下载失败: HTTP " + urlConnection.getResponseCode();
                    Log.e(TAG, errorMessage);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvLyrics.setText(errorMessage + "\n请检查歌词URL或网络连接。");
                        });
                    }
                }
            } catch (IOException e) {
                // 处理网络连接或I/O异常
                Log.e(TAG, "加载歌词异常", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvLyrics.setText("加载歌词异常: " + e.getMessage() + "\n请检查网络连接。");
                    });
                }
            } finally {
                // 关闭连接和读取器，释放资源
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "关闭歌词文件读取器异常", e);
                    }
                }
            }
        });
    }



    /**
     * 更新歌词内容 (如果歌词解析逻辑在Service或Activity中)
     * @param lyrics 歌词字符串
     */
    public void updateLyrics(String lyrics) {
        if (tvLyrics != null) {
            tvLyrics.setText(lyrics);
        }
    }








    @Override
    public void onDestroy() {
        super.onDestroy();
        // 【新增】关闭线程池，防止内存泄漏
        if (executorService != null) {
            executorService.shutdownNow(); // 立即关闭线程池，中断正在执行的任务
            Log.d(TAG, "LyricFragment 线程池已关闭");
        }
    }


    public void updateMusicInfo(MusicInfo musicInfo) {
    }
}
