package com.example.music_community.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.bumptech.glide.Glide;
import com.example.music_community.R;
import com.example.music_community.model.MusicInfo;

import java.util.List;

public class MusicItemAdapter extends BaseQuickAdapter<MusicInfo, BaseViewHolder> {

    private int currentLayoutResId;

    public MusicItemAdapter(int layoutResId, List<MusicInfo> data) {
        super(layoutResId, data);
        this.currentLayoutResId = layoutResId;

        // 设置 Item 点击监听器
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                if (clickedItem != null) {
                    Toast.makeText(view.getContext(), clickedItem.getMusicName(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 根据传入的布局ID判断是否需要注册加号按钮或播放按钮的点击事件
        if (currentLayoutResId == R.layout.item_music_info_small) {
            addChildClickViewIds(R.id.iv_add_music); // 注册加号按钮的点击事件
            setOnItemChildClickListener((adapter, view, position) -> {
                if (view.getId() == R.id.iv_add_music) {
                    MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                    if (clickedItem != null) {
                        Toast.makeText(view.getContext(), "将音乐 " + clickedItem.getMusicName() + " 添加到音乐列表", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (currentLayoutResId == R.layout.item_music_info_large) {
            addChildClickViewIds(R.id.iv_play_button_large); // 注册播放按钮的点击事件
            setOnItemChildClickListener((adapter, view, position) -> {
                if (view.getId() == R.id.iv_play_button_large) {
                    MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                    if (clickedItem != null) {
                        Toast.makeText(view.getContext(), "播放音乐: " + clickedItem.getMusicName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, MusicInfo item) {
        ImageView coverImageView;
        TextView musicNameTextView;
        TextView authorTextView;
        ImageView playButton; // 新增播放按钮引用

        if (currentLayoutResId == R.layout.item_music_info_large) {
            coverImageView = holder.getView(R.id.iv_music_cover_large);
            musicNameTextView = holder.getView(R.id.tv_music_name_large);
            authorTextView = holder.getView(R.id.tv_music_author_large);
            playButton = holder.getView(R.id.iv_play_button_large); // 查找播放按钮

            // 设置播放按钮可见
            playButton.setVisibility(View.VISIBLE);


        } else { // currentLayoutResId == R.layout.item_music_info_small
            coverImageView = holder.getView(R.id.iv_music_cover_small);
            musicNameTextView = holder.getView(R.id.tv_music_name_small);
            authorTextView = holder.getView(R.id.tv_music_author_small);
            // 对于小卡片，可能没有播放按钮，或者需要隐藏
            // ImageView addMusicButton = holder.getView(R.id.iv_add_music);
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
