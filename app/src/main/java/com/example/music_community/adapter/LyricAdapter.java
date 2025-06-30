package com.example.music_community.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music_community.R;
import com.example.music_community.model.LyricLine;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌词适配器，用于在 RecyclerView 中显示歌词。
 * 负责高亮当前播放的歌词行，并提供更新当前播放时间的方法。
 */
public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.LyricViewHolder> {

    private static final String TAG = "LyricAdapter";
    private List<LyricLine> lyricList;
    private int currentHighlightedPosition = -1; // 当前高亮歌词的索引
    private long currentPlayTime = 0; // 当前播放时间，用于判断高亮哪一行

    // 构造函数
    public LyricAdapter(List<LyricLine> lyricList) {
        this.lyricList = lyricList != null ? lyricList : new ArrayList<>();
    }

    /**
     * 更新歌词数据
     * @param newLyricList 新的歌词列表
     */
    public void setLyricList(List<LyricLine> newLyricList) {
        this.lyricList = newLyricList != null ? newLyricList : new ArrayList<>();
        this.currentHighlightedPosition = -1; // 歌词列表更新，重置高亮位置
        this.currentPlayTime = 0; // 重置播放时间
        notifyDataSetChanged(); // 通知数据已改变
        Log.d(TAG, "歌词列表已更新，共 " + this.lyricList.size() + " 行。");
    }

    /**
     * 更新当前播放时间，并刷新高亮行。
     * @param playTime 当前音乐播放时间（毫秒）
     */
    public void updateCurrentPlayTime(long playTime) {
        this.currentPlayTime = playTime;
        // 查找当前时间对应的歌词行索引
        int newHighlightedPosition = findCurrentLyricIndex(playTime);

        if (newHighlightedPosition != currentHighlightedPosition) {
            // 如果高亮行发生变化，则只更新旧高亮行和新高亮行，以提高效率
            if (currentHighlightedPosition != -1) {
                notifyItemChanged(currentHighlightedPosition); // 取消旧高亮
            }
            currentHighlightedPosition = newHighlightedPosition;
            if (currentHighlightedPosition != -1) {
                notifyItemChanged(currentHighlightedPosition); // 设置新高亮
            }
            Log.d(TAG, "高亮歌词更新到索引: " + currentHighlightedPosition + ", 时间: " + playTime);
        }
    }

    /**
     * 根据当前播放时间查找对应的歌词行索引。
     * @param playTime 当前播放时间（毫秒）
     * @return 对应的歌词行索引，如果未找到则返回 -1。
     */
    private int findCurrentLyricIndex(long playTime) {
        if (lyricList.isEmpty()) {
            return -1;
        }

        // 遍历歌词列表，找到当前时间点应该高亮的歌词行
        // 找到第一个 timestamp > playTime 的歌词，那么前一行就是当前要高亮的
        for (int i = 0; i < lyricList.size(); i++) {
            if (lyricList.get(i).getTimestamp() > playTime) {
                // 如果当前时间小于此行歌词的开始时间，则前一行是当前播放的歌词
                // (如果 i == 0，则说明还没有到第一行歌词，此时返回 -1 或 0 都可以，这里返回 0 比较合理)
                return Math.max(0, i - 1);
            }
        }
        // 如果播放时间超过所有歌词的时间戳，则高亮最后一首歌词
        return lyricList.size() - 1;
    }

    /**
     * 获取当前高亮歌词的索引。
     * @return 当前高亮歌词的索引。
     */
    public int getCurrentHighlightedPosition() {
        return currentHighlightedPosition;
    }

    @NonNull
    @Override
    public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建 ViewHolder，加载歌词行布局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lyric_line, parent, false);
        return new LyricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
        LyricLine lyricLine = lyricList.get(position);
        holder.tvLyricLine.setText(lyricLine.getText());

        // 根据是否为当前高亮行设置不同的样式
        if (position == currentHighlightedPosition) {
            holder.tvLyricLine.setTextColor(Color.parseColor("#FFFFFF")); // 高亮颜色为白色
            holder.tvLyricLine.setTextSize(20f); // 高亮字体大小
            // 【修改】使用 setTypeface 方法设置字体样式为粗体
            holder.tvLyricLine.setTypeface(holder.tvLyricLine.getTypeface(), Typeface.BOLD);
            Log.d(TAG, "高亮显示歌词: " + lyricLine.getText() + " (索引: " + position + ")");
        } else {
            holder.tvLyricLine.setTextColor(Color.parseColor("#CCCCCC")); // 普通颜色为浅灰色
            holder.tvLyricLine.setTextSize(18f); // 普通字体大小
            // 【修改】使用 setTypeface 方法设置字体样式为正常
            holder.tvLyricLine.setTypeface(holder.tvLyricLine.getTypeface(), Typeface.NORMAL);
        }
    }

    @Override
    public int getItemCount() {
        return lyricList.size();
    }

    // ViewHolder 类
    static class LyricViewHolder extends RecyclerView.ViewHolder {
        TextView tvLyricLine;

        public LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLyricLine = itemView.findViewById(R.id.tv_lyric_line);
        }
    }
}
