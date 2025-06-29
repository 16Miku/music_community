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

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LyricFragment extends Fragment {

    private static final String TAG = "LyricFragment"; // 添加 TAG
    private static final String ARG_MUSIC_INFO = "arg_music_info";

    private TextView tvLyrics;
    private MusicInfo musicInfo;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();


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
        Log.d(TAG, "onCreate: LyricFragment created. MusicInfo: " + (musicInfo != null ? musicInfo.getMusicName() : "null"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: LyricFragment view inflated.");
        View view = inflater.inflate(R.layout.fragment_lyric, container, false);
        tvLyrics = view.findViewById(R.id.tv_lyrics);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (musicInfo != null && musicInfo.getLyricUrl() != null && !musicInfo.getLyricUrl().isEmpty()) {
            loadLyrics(musicInfo.getLyricUrl());
        } else {
            String text = "歌词加载失败或暂无歌词。\n当前歌曲: " + (musicInfo != null ? musicInfo.getMusicName() : "未知歌曲");
            tvLyrics.setText(text);
            Log.w(TAG, "onViewCreated: Lyric URL is empty or invalid. Displaying: " + text);
        }
    }

    private void loadLyrics(String lyricUrl) {
        Log.d(TAG, "loadLyrics: Loading lyrics from URL: " + lyricUrl);
        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            StringBuilder lyricsBuilder = new StringBuilder();
            try {
                URL url = new URL(lyricUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = urlConnection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lyricsBuilder.append(line).append("\n");
                    }
                    String finalLyrics = lyricsBuilder.toString();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvLyrics.setText(finalLyrics);
                            Log.d(TAG, "loadLyrics: Lyrics loaded successfully for: " + (musicInfo != null ? musicInfo.getMusicName() : "Unknown song"));
                        });
                    }
                } else {
                    String errorMessage = "歌词下载失败: HTTP " + urlConnection.getResponseCode();
                    Log.e(TAG, errorMessage);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tvLyrics.setText(errorMessage + "\n请检查歌词URL或网络连接。");
                        });
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "loadLyrics: Failed to load lyrics", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvLyrics.setText("加载歌词异常: " + e.getMessage() + "\n请检查网络连接。");
                    });
                }
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "loadLyrics: Error closing reader", e);
                    }
                }
            }
        });
    }

    public void updateLyrics(String lyrics) {
        if (tvLyrics != null) {
            tvLyrics.setText(lyrics);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
            Log.d(TAG, "onDestroy: LyricFragment thread pool shut down.");
        }
    }

    public void updateMusicInfo(MusicInfo musicInfo) {
        // This method is not used in current implementation, but can be kept for future use.
        Log.d(TAG, "updateMusicInfo: Not implemented for LyricFragment yet.");
    }
}
