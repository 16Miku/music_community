package com.example.music_community.adapter;

import androidx.annotation.NonNull; // 导入 NonNull 注解
import androidx.fragment.app.Fragment; // 导入 Fragment 类
import androidx.fragment.app.FragmentActivity; // 导入 FragmentActivity 类
import androidx.viewpager2.adapter.FragmentStateAdapter; // 导入 FragmentStateAdapter 类

import com.example.music_community.LyricFragment; // 导入 LyricFragment 类
import com.example.music_community.MusicPlaybackFragment; // 导入 MusicPlaybackFragment 类
import com.example.music_community.model.MusicInfo; // 导入 MusicInfo 模型类

import java.util.List; // 导入 List 接口

public class PlayerPagerAdapter extends FragmentStateAdapter {

    private MusicInfo currentMusicInfo; // 当前播放的音乐信息
    // 【移除】不再需要 isPlaying 字段，因为播放状态将直接推送到 MusicPlaybackFragment

    public PlayerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * 设置当前播放的音乐信息，并通知适配器数据变更
     * @param musicInfo 当前播放的音乐信息
     */
    public void setCurrentMusicInfo(MusicInfo musicInfo) { // 【还原】不再接收 isPlaying 参数
        this.currentMusicInfo = musicInfo;
        notifyDataSetChanged(); // 通知 ViewPager2 重新创建或更新 Fragment
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 根据位置返回不同的 Fragment
        if (position == 0) {
            // 位置0是音乐播放页
            // 【还原】创建 MusicPlaybackFragment 时不再传入 isPlaying
            return MusicPlaybackFragment.newInstance(currentMusicInfo);
        } else {
            // 位置1是歌词页
            return LyricFragment.newInstance(currentMusicInfo);
        }
    }

    @Override
    public int getItemCount() {
        // 只有音乐信息存在时才显示两个页面 (播放页和歌词页)
        return currentMusicInfo != null ? 2 : 0;
    }

    // 重写 getItemId 和 containsItem，帮助 ViewPager2 更好地管理 Fragment 状态
    @Override
    public long getItemId(int position) {
        // 使用音乐ID + position 作为唯一ID，确保当音乐切换时，Fragment 会被重新创建
        if (currentMusicInfo != null) {
            return (currentMusicInfo.getId() != null ? currentMusicInfo.getId().hashCode() : 0) + position;
        }
        return super.getItemId(position);
    }

    @Override
    public boolean containsItem(long itemId) {
        // 判断 Item 是否仍然存在
        if (currentMusicInfo != null) {
            long expectedItemId0 = (currentMusicInfo.getId() != null ? currentMusicInfo.getId().hashCode() : 0) + 0;
            long expectedItemId1 = (currentMusicInfo.getId() != null ? currentMusicInfo.getId().hashCode() : 0) + 1;
            return itemId == expectedItemId0 || itemId == expectedItemId1;
        }
        return false;
    }
}
