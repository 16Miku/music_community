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

    // 移除了 itemType 字段，因为布局ID将直接从构造函数传入，适配器本身不再需要区分类型
    private int currentLayoutResId; // **新增：用于存储当前适配器实例使用的布局ID**

    /**
     * 构造函数
     * @param layoutResId 当前 MusicItemAdapter 实例要使用的布局资源ID (例如 item_music_info_large 或 item_music_info_small)
     * @param data 音乐信息列表
     */
    public MusicItemAdapter(int layoutResId, List<MusicInfo> data) {
        super(layoutResId, data); // **核心修改：这里传入布局ID和数据列表**
        this.currentLayoutResId = layoutResId; // **保存传入的布局ID**

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

        // 根据传入的布局ID判断是否需要注册加号按钮的点击事件
        // 只有 item_music_info_small 布局包含 iv_add_music
        if (currentLayoutResId == R.layout.item_music_info_small) { // **使用保存的布局ID进行判断**
            addChildClickViewIds(R.id.iv_add_music); // 注册加号按钮的点击事件
            setOnItemChildClickListener((adapter, view, position) -> {
                if (view.getId() == R.id.iv_add_music) {
                    MusicInfo clickedItem = (MusicInfo) adapter.getItem(position);
                    if (clickedItem != null) {
                        Toast.makeText(view.getContext(), "将音乐 " + clickedItem.getMusicName() + " 添加到音乐列表", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // 移除了 setChildLayout 方法，因为布局ID直接在构造函数中设置

    @Override
    protected void convert(@NonNull BaseViewHolder holder, MusicInfo item) {
        ImageView coverImageView;
        TextView musicNameTextView;
        TextView authorTextView;

        // **核心修改：使用保存的布局ID来判断当前 Item 的布局类型，而不是 holder.getLayoutId()**
        if (currentLayoutResId == R.layout.item_music_info_large) {
            coverImageView = holder.getView(R.id.iv_music_cover_large);
            musicNameTextView = holder.getView(R.id.tv_music_name_large);
            authorTextView = holder.getView(R.id.tv_music_author_large);
        } else { // currentLayoutResId == R.layout.item_music_info_small
            coverImageView = holder.getView(R.id.iv_music_cover_small);
            musicNameTextView = holder.getView(R.id.tv_music_name_small);
            authorTextView = holder.getView(R.id.tv_music_author_small);
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