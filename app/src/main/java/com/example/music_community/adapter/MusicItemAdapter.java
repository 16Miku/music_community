package com.example.music_community.adapter;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.music_community.R;
import com.example.music_community.model.MusicInfo;

import java.util.List;

public class MusicItemAdapter extends BaseQuickAdapter<MusicInfo, BaseViewHolder> {

    private final int currentLayoutResId;
    private final OnMusicItemPlayListener playListener; // 【新增】

    // 【新增】监听器接口
    public interface OnMusicItemPlayListener {
        void onPlayMusic(List<MusicInfo> musicList, int position);
    }

    /**
     * 【修改】构造函数，接收播放监听器
     * @param layoutResId 布局ID
     * @param data 数据
     * @param listener 播放监听器
     */
    public MusicItemAdapter(int layoutResId, List<MusicInfo> data, OnMusicItemPlayListener listener) {
        super(layoutResId, data);
        this.currentLayoutResId = layoutResId;
        this.playListener = listener;

        // 【核心修改】整个 item 的点击事件
        setOnItemClickListener((adapter, view, position) -> {
            if (playListener != null) {
                // 通知 MainActivity 播放音乐
                playListener.onPlayMusic(getData(), position);
            }
        });

        // 子 view 的点击事件也统一为播放音乐
        if (currentLayoutResId == R.layout.item_music_info_large) {
            addChildClickViewIds(R.id.iv_play_button_large);
        } else if (currentLayoutResId == R.layout.item_music_info_square) {
            addChildClickViewIds(R.id.iv_play_button_square);
        } else if (currentLayoutResId == R.layout.item_music_info_small) {
            addChildClickViewIds(R.id.iv_add_music);
        }

        setOnItemChildClickListener((adapter, view, position) -> {
            if (view.getId() == R.id.iv_add_music) {
                // TODO: 实现添加到播放列表的逻辑
                Toast.makeText(getContext(), "添加功能待开发", Toast.LENGTH_SHORT).show();
            } else {
                // 其他按钮（如播放按钮）也触发播放
                if (playListener != null) {
                    playListener.onPlayMusic(getData(), position);
                }
            }
        });
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, MusicInfo item) {
        ImageView coverImageView;
        TextView musicNameTextView;
        TextView authorTextView;

        // 根据当前适配器实例使用的布局ID来查找视图
        if (currentLayoutResId == R.layout.item_music_info_large) {
            coverImageView = holder.getView(R.id.iv_music_cover_large);
            musicNameTextView = holder.getView(R.id.tv_music_name_large);
            authorTextView = holder.getView(R.id.tv_music_author_large);
            // 播放按钮的可见性在布局中控制，这里无需额外设置
        } else if (currentLayoutResId == R.layout.item_music_info_small) {
            coverImageView = holder.getView(R.id.iv_music_cover_small);
            musicNameTextView = holder.getView(R.id.tv_music_name_small);
            authorTextView = holder.getView(R.id.tv_music_author_small);
        } else if (currentLayoutResId == R.layout.item_music_info_tall_narrow) {
            coverImageView = holder.getView(R.id.iv_music_cover_tall_narrow);
            musicNameTextView = holder.getView(R.id.tv_music_name_tall_narrow);
            authorTextView = holder.getView(R.id.tv_music_author_tall_narrow);
        } else if (currentLayoutResId == R.layout.item_music_info_square) {
            coverImageView = holder.getView(R.id.iv_music_cover_square);
            musicNameTextView = holder.getView(R.id.tv_music_name_square);
            authorTextView = holder.getView(R.id.tv_music_author_square);
        } else {
            // 默认情况或错误处理
            return;
        }

        // 使用 Glide 加载图片
        Glide.with(holder.itemView.getContext())
                .load(item.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_foreground) // 占位图
                .error(R.drawable.ic_launcher_foreground) // 错误图
                .into(coverImageView);

        musicNameTextView.setText(item.getMusicName());
        authorTextView.setText(item.getAuthor());
    }
}
