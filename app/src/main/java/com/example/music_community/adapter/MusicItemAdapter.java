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

    /**
     * 构造函数
     * @param layoutResId 当前 MusicItemAdapter 实例要使用的布局资源ID
     * @param data 音乐信息列表
     */
    public MusicItemAdapter(int layoutResId, List<MusicInfo> data) {
        super(layoutResId, data);
        this.currentLayoutResId = layoutResId;

        // 设置 Item 点击监听器
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                if (clickedItem != null) {
                    Toast.makeText(view.getContext(), "点击了音乐: " + clickedItem.getMusicName(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 根据传入的布局ID判断是否需要注册子视图的点击事件
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
            addChildClickViewIds(R.id.iv_play_button_large); // 注册大卡片播放按钮的点击事件
            setOnItemChildClickListener((adapter, view, position) -> {
                if (view.getId() == R.id.iv_play_button_large) {
                    MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                    if (clickedItem != null) {
                        Toast.makeText(view.getContext(), "播放音乐: " + clickedItem.getMusicName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (currentLayoutResId == R.layout.item_music_info_square) {
            addChildClickViewIds(R.id.iv_play_button_square); // 注册正方形卡片播放按钮的点击事件
            setOnItemChildClickListener((adapter, view, position) -> {
                if (view.getId() == R.id.iv_play_button_square) {
                    MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                    if (clickedItem != null) {
                        Toast.makeText(view.getContext(), "播放音乐: " + clickedItem.getMusicName(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        // item_music_info_tall_narrow 暂时没有特殊子视图点击事件，如果有需要可以添加
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
