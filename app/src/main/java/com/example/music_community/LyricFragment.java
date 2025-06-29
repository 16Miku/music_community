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

public class LyricFragment extends Fragment {

    private static final String ARG_MUSIC_INFO = "arg_music_info";

    private TextView tvLyrics;
    private MusicInfo musicInfo; // 当前 Fragment 显示的音乐信息

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
        if (musicInfo != null) {
            // TODO: 这里将来会加载和解析歌词文件
            tvLyrics.setText("当前歌曲: " + musicInfo.getMusicName() + "\n歌词地址: " + musicInfo.getLyricUrl() + "\n\n(歌词解析和滚动功能待实现)");
        }
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
}
