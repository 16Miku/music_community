package com.example.music_community.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast; // 导入 Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.music_community.R;
import com.example.music_community.model.MusicInfo;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<MusicInfo> bannerList;

    public BannerAdapter(List<MusicInfo> bannerList) {
        this.bannerList = bannerList;
    }

    // 更新 Banner 数据的方法
    public void setData(List<MusicInfo> newBannerList) {
        this.bannerList = newBannerList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建 ViewHolder，加载 Banner 图片布局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_info_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        // 由于 Banner 可能需要循环滚动，这里使用真实位置取模来获取数据
        int realPosition = position % bannerList.size();
        MusicInfo musicInfo = bannerList.get(realPosition);

        // 使用 Glide 加载图片
        Glide.with(holder.itemView.getContext())
                .load(musicInfo.getCoverUrl())
                .placeholder(R.drawable.ic_launcher_foreground) // 占位图
                .error(R.drawable.ic_launcher_foreground) // 错误图
                .into(holder.bannerImageView);

        // 设置点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "点击了 Banner: " + musicInfo.getMusicName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        // 如果只有一个 Item，返回 1，不启用循环
        // 如果有多个 Item，返回一个很大的值来模拟无限循环
        return bannerList.size() > 1 ? Integer.MAX_VALUE : bannerList.size();
    }

    // ViewHolder 类
    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImageView;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImageView = itemView.findViewById(R.id.iv_banner_image);
        }
    }
}


