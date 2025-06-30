package com.example.music_community;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // 仍然保留，以防万一

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // 【新增】
import androidx.recyclerview.widget.RecyclerView; // 【新增】

import com.example.music_community.adapter.LyricAdapter; // 【新增】

import com.example.music_community.model.LyricLine; // 【新增】
import com.example.music_community.model.MusicInfo;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList; // 【新增】
import java.util.Collections; // 【新增】
import java.util.List;
import java.util.regex.Matcher; // 【新增】
import java.util.regex.Pattern; // 【新增】
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler; // 【新增】
import android.os.Looper; // 【新增】


public class LyricFragment extends Fragment {

    private static final String TAG = "LyricFragment";
    private static final String ARG_MUSIC_INFO = "arg_music_info";

    // private TextView tvLyrics; // 【移除】不再需要单个 TextView
    private RecyclerView recyclerViewLyrics; // 【新增】
    private LyricAdapter lyricAdapter;     // 【新增】
    private LinearLayoutManager layoutManager; // 【新增】

    private MusicInfo musicInfo;
    private List<LyricLine> currentLyricList = new ArrayList<>(); // 【新增】存储当前歌词列表

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper()); // 【新增】用于在主线程更新UI

    // 【新增】提供一个接口，让 Activity 可以更新歌词进度
    public interface OnLyricFragmentInteractionListener {
        void onLyricFragmentReady(LyricFragment fragment);
    }
    private OnLyricFragmentInteractionListener listener;

    public static LyricFragment newInstance(MusicInfo musicInfo) {
        LyricFragment fragment = new LyricFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MUSIC_INFO, musicInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnLyricFragmentInteractionListener) {
            listener = (OnLyricFragmentInteractionListener) context;
        } else {
            Log.e(TAG, "Activity must implement OnLyricFragmentInteractionListener");
            // throw new RuntimeException(context.toString() + " must implement OnLyricFragmentInteractionListener");
        }
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
        recyclerViewLyrics = view.findViewById(R.id.recyclerView_lyrics); // 【修改】
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 【新增】初始化 RecyclerView
        layoutManager = new LinearLayoutManager(getContext());
        recyclerViewLyrics.setLayoutManager(layoutManager);
        lyricAdapter = new LyricAdapter(currentLyricList); // 初始传入空列表
        recyclerViewLyrics.setAdapter(lyricAdapter);

        // 【新增】设置 RecyclerView 的间距，使其顶部和底部有足够的空间让歌词居中
        // 计算屏幕高度的一半，作为 RecyclerView 的上下 padding
        // 这样，中间的歌词行才能真正滚动到屏幕中央
        recyclerViewLyrics.post(() -> {
            int recyclerViewHeight = recyclerViewLyrics.getHeight();
            int paddingTop = recyclerViewHeight / 2;
            int paddingBottom = recyclerViewHeight / 2;
            recyclerViewLyrics.setPadding(recyclerViewLyrics.getPaddingLeft(), paddingTop, recyclerViewLyrics.getPaddingRight(), paddingBottom);
            Log.d(TAG, "RecyclerView padding set: Top=" + paddingTop + ", Bottom=" + paddingBottom);

            // 当 RecyclerView 布局完成后，加载歌词
            if (musicInfo != null && musicInfo.getLyricUrl() != null && !musicInfo.getLyricUrl().isEmpty()) {
                loadLyrics(musicInfo.getLyricUrl());
            } else {
                displayNoLyricsMessage();
            }

            // 通知 Activity Fragment 已准备就绪
            if (listener != null) {
                listener.onLyricFragmentReady(this);
            }
        });
    }

    /**
     * 【修改】加载歌词文件
     * @param lyricUrl 歌词文件 URL
     */
    private void loadLyrics(String lyricUrl) {
        Log.d(TAG, "loadLyrics: Loading lyrics from URL: " + lyricUrl);
        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            StringBuilder lyricContentBuilder = new StringBuilder();
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
                        lyricContentBuilder.append(line).append("\n");
                    }
                    String rawLyrics = lyricContentBuilder.toString();
                    Log.d(TAG, "loadLyrics: Raw lyrics loaded. Parsing...");
                    // 【新增】解析歌词
                    currentLyricList = parseLrc(rawLyrics);

                    mainHandler.post(() -> {
                        if (!currentLyricList.isEmpty()) {
                            lyricAdapter.setLyricList(currentLyricList);
                            // 成功加载歌词后，隐藏所有提示TextView
                            TextView noLyricsHint = getView().findViewById(R.id.tv_no_lyrics_hint);
                            TextView errorHint = getView().findViewById(R.id.tv_error_hint);
                            if (noLyricsHint != null) noLyricsHint.setVisibility(View.GONE);
                            if (errorHint != null) errorHint.setVisibility(View.GONE);
                            if (recyclerViewLyrics != null) recyclerViewLyrics.setVisibility(View.VISIBLE); // 显示 RecyclerView
                            Log.d(TAG, "loadLyrics: Lyrics parsed and set to adapter successfully.");
                        } else {
                            displayNoLyricsMessage();
                            Log.w(TAG, "loadLyrics: Parsed lyric list is empty.");
                        }
                    });

                } else {
                    String errorMessage = "歌词下载失败: HTTP " + urlConnection.getResponseCode();
                    Log.e(TAG, errorMessage);
                    mainHandler.post(() -> displayErrorMessage(errorMessage));
                }
            } catch (IOException e) {
                Log.e(TAG, "loadLyrics: Failed to load lyrics", e);
                mainHandler.post(() -> displayErrorMessage("加载歌词异常: " + e.getMessage() + "\n请检查网络连接。"));
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

    /**
     * 【新增】解析 LRC 歌词文件内容
     * 格式示例: [00:10.50]这是一行歌词
     * @param lrcContent LRC 文件的字符串内容
     * @return 解析后的 LyricLine 列表
     */
    private List<LyricLine> parseLrc(String lrcContent) {
        List<LyricLine> parsedLyrics = new ArrayList<>();
        if (lrcContent == null || lrcContent.isEmpty()) {
            return parsedLyrics;
        }

        // 正则表达式匹配 [mm:ss.SS] 格式的时间戳
        Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");
        // 匹配多个时间戳的情况，如 [00:01.00][00:02.00]歌词
        Pattern multipleTimestampsPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]");


        String[] lines = lrcContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher multipleTimestampsMatcher = multipleTimestampsPattern.matcher(line);
            List<Long> timestamps = new ArrayList<>();
            int lastMatchEnd = 0;

            // 提取所有时间戳
            while (multipleTimestampsMatcher.find()) {
                long minutes = Long.parseLong(multipleTimestampsMatcher.group(1));
                long seconds = Long.parseLong(multipleTimestampsMatcher.group(2));
                long milliseconds = Long.parseLong(multipleTimestampsMatcher.group(3));

                // 如果毫秒是两位数，则乘以10，使其变为三位数，统一为毫秒
                if (multipleTimestampsMatcher.group(3).length() == 2) {
                    milliseconds *= 10;
                }
                long timestamp = minutes * 60 * 1000 + seconds * 1000 + milliseconds;
                timestamps.add(timestamp);
                lastMatchEnd = multipleTimestampsMatcher.end();
            }

            // 提取歌词文本
            String lyricText = line.substring(lastMatchEnd).trim();

            // 为每个时间戳创建 LyricLine 对象
            for (Long timestamp : timestamps) {
                parsedLyrics.add(new LyricLine(timestamp, lyricText));
            }
        }

        // 歌词按时间戳排序，确保顺序正确
        Collections.sort(parsedLyrics, (l1, l2) -> Long.compare(l1.getTimestamp(), l2.getTimestamp()));

        return parsedLyrics;
    }


    /**
     * 【新增】更新歌词显示和滚动
     * @param musicInfo 当前播放的音乐信息
     * @param currentPosition 当前播放进度（毫秒）
     */
    public void updateLyricDisplay(MusicInfo musicInfo, int currentPosition) {
        // 如果当前音乐信息与Fragment持有的不同，重新加载歌词
        if (this.musicInfo == null || !this.musicInfo.equals(musicInfo)) {
            this.musicInfo = musicInfo;
            if (musicInfo != null && musicInfo.getLyricUrl() != null && !musicInfo.getLyricUrl().isEmpty()) {
                loadLyrics(musicInfo.getLyricUrl());
            } else {
                displayNoLyricsMessage();
            }
        }

        // 更新适配器的播放时间，这将触发高亮刷新
        lyricAdapter.updateCurrentPlayTime(currentPosition);

        // 滚动 RecyclerView 使当前高亮行居中
        int highlightedPosition = lyricAdapter.getCurrentHighlightedPosition();
        if (highlightedPosition != -1) {
            // scrollToPositionWithOffset 可以将指定位置的 item 滚动到 RecyclerView 的某个偏移量
            // 这里我们希望它居中，所以偏移量是 RecyclerView 高度的一半减去 item 高度的一半
            // 由于 item 的高度是动态的，我们需要先测量它
            mainHandler.post(() -> {
                if (recyclerViewLyrics.getLayoutManager() != null && recyclerViewLyrics.getLayoutManager().findViewByPosition(highlightedPosition) != null) {
                    View itemView = layoutManager.findViewByPosition(highlightedPosition);
                    if (itemView != null) {
                        int itemHeight = itemView.getHeight();
                        int recyclerViewHeight = recyclerViewLyrics.getHeight();
                        int offset = (recyclerViewHeight / 2) - (itemHeight / 2);
                        layoutManager.scrollToPositionWithOffset(highlightedPosition, offset);
                        // Log.d(TAG, "滚动到位置: " + highlightedPosition + ", 偏移: " + offset);
                    }
                } else {
                    // 如果 item 尚未完全加载或可见，可能需要直接滚动到位置，等待下一次更新再居中
                    layoutManager.scrollToPosition(highlightedPosition);
                    // Log.d(TAG, "直接滚动到位置: " + highlightedPosition + " (ItemView 尚未完全加载)");
                }
            });
        }
    }


    private void displayNoLyricsMessage() {
        lyricAdapter.setLyricList(new ArrayList<>()); // 清空歌词列表
        if (recyclerViewLyrics != null) {
            recyclerViewLyrics.setVisibility(View.GONE); // 隐藏 RecyclerView
        }
        TextView noLyricsHint = getView().findViewById(R.id.tv_no_lyrics_hint);
        TextView errorHint = getView().findViewById(R.id.tv_error_hint);
        if (noLyricsHint != null) {
            noLyricsHint.setVisibility(View.VISIBLE);
            noLyricsHint.setText("暂无歌词");
        }
        if (errorHint != null) {
            errorHint.setVisibility(View.GONE); // 确保错误提示隐藏
        }
        Log.w(TAG, "displayNoLyricsMessage: No lyrics available or failed to load.");
    }

    private void displayErrorMessage(String message) {
        lyricAdapter.setLyricList(new ArrayList<>()); // 清空歌词列表
        if (recyclerViewLyrics != null) {
            recyclerViewLyrics.setVisibility(View.GONE); // 隐藏 RecyclerView
        }
        TextView noLyricsHint = getView().findViewById(R.id.tv_no_lyrics_hint);
        TextView errorHint = getView().findViewById(R.id.tv_error_hint);
        if (noLyricsHint != null) {
            noLyricsHint.setVisibility(View.GONE); // 确保无歌词提示隐藏
        }
        if (errorHint != null) {
            errorHint.setVisibility(View.VISIBLE);
            errorHint.setText(message);
        }
        Log.e(TAG, "displayErrorMessage: " + message);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清除对 RecyclerView 及其适配器的引用，防止内存泄漏
        recyclerViewLyrics.setAdapter(null);
        lyricAdapter = null;
        layoutManager = null;
        Log.d(TAG, "onDestroyView: LyricFragment view destroyed.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
            Log.d(TAG, "onDestroy: LyricFragment thread pool shut down.");
        }
        mainHandler.removeCallbacksAndMessages(null); // 清除所有主线程回调
        Log.d(TAG, "onDestroy: LyricFragment destroyed.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // 解除监听器引用
        Log.d(TAG, "onDetach: LyricFragment detached.");
    }

    // 【移除】不再需要此方法，更新逻辑已整合到 updateLyricDisplay
    // public void updateLyrics(String lyrics) {
    //     if (tvLyrics != null) {
    //         tvLyrics.setText(lyrics);
    //     }
    // }

    // 【移除】不再需要此方法，更新逻辑已整合到 updateLyricDisplay
    // public void updateMusicInfo(MusicInfo musicInfo) {
    //     Log.d(TAG, "updateMusicInfo: Not implemented for LyricFragment yet.");
    // }
}
