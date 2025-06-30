package com.example.music_community.adapter;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.music_community.R;
import com.example.music_community.model.MusicInfo;

import java.util.List;

public class PlaylistAdapter extends BaseQuickAdapter<MusicInfo, BaseViewHolder> {

    // 播放列表项操作监听器
    public interface OnPlaylistItemActionListener {
        void onPlaylistItemClick(MusicInfo musicInfo, int position);
        void onPlaylistItemDelete(MusicInfo musicInfo, int position);
    }

    private OnPlaylistItemActionListener listener;
    private MusicInfo currentPlayingMusic; // 用于标记当前播放的歌曲

    public PlaylistAdapter(List<MusicInfo> data, OnPlaylistItemActionListener listener) {
        super(R.layout.item_music_info_small, data); // 复用 item_music_info_small 布局
        this.listener = listener;

        // 设置整个 Item 的点击事件
        setOnItemClickListener((adapter, view, position) -> {
            if (listener != null) {
                listener.onPlaylistItemClick(getItem(position), position);
            }
        });

        // 注册子视图的点击事件 (删除按钮)
        addChildClickViewIds(R.id.iv_add_music); // 注意：这里复用了 iv_add_music 作为删除按钮

        setOnItemChildClickListener((adapter, view, position) -> {
            if (view.getId() == R.id.iv_add_music) { // 如果是删除按钮
                if (listener != null) {
                    listener.onPlaylistItemDelete(getItem(position), position);
                }
            }
        });
    }

    /**
     * 设置当前正在播放的歌曲，用于高亮显示
     * @param musicInfo 当前播放的歌曲信息
     */
    public void setCurrentPlayingMusic(MusicInfo musicInfo) {
        this.currentPlayingMusic = musicInfo;
        notifyDataSetChanged(); // 数据改变，刷新列表
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, MusicInfo item) {
        ImageView coverImageView = holder.getView(R.id.iv_music_cover_small);
        TextView musicNameTextView = holder.getView(R.id.tv_music_name_small);
        TextView authorTextView = holder.getView(R.id.tv_music_author_small);
        ImageView deleteButton = holder.getView(R.id.iv_add_music); // 这里是删除按钮

        // 加载歌曲封面（如果需要，可以隐藏封面，显示一个播放图标）
        // 为了简洁，这里仍然加载封面，但实际播放列表通常只显示文本
        // 或者可以替换成一个播放状态指示器
        // Glide.with(holder.itemView.getContext())
        //         .load(item.getCoverUrl())
        //         .placeholder(R.drawable.ic_launcher_foreground)
        //         .error(R.drawable.ic_launcher_foreground)
        //         .into(coverImageView);

        // 临时处理：在播放列表中，iv_music_cover_small 处显示一个默认图标或当前播放指示
        // 这里只是一个简单的占位，实际应该根据设计图来处理
        coverImageView.setImageResource(R.drawable.ic_play_big); // 默认显示播放图标
        coverImageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.button_blue)); // 设置颜色

        musicNameTextView.setText(item.getMusicName());
        authorTextView.setText(item.getAuthor());

        // 设置删除图标
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setColorFilter(ContextCompat.getColor(getContext(), R.color.black)); // 确保图标是黑色的

        // 高亮显示当前播放的歌曲
        if (currentPlayingMusic != null && currentPlayingMusic.equals(item)) {
            musicNameTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.button_blue)); // 蓝色高亮
            authorTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.button_blue));
        } else {
            musicNameTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black)); // 默认黑色
            authorTextView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray)); // 默认灰色
        }
    }
}
