package com.example.music_community.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.music_community.LyricFragment;
import com.example.music_community.MusicPlaybackFragment;
import com.example.music_community.model.MusicInfo;

import java.util.List;

public class PlayerPagerAdapter extends FragmentStateAdapter {

    private MusicInfo currentMusicInfo; // 当前播放的音乐信息

    public PlayerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * 设置当前播放的音乐信息，并通知适配器数据变更
     * @param musicInfo 当前播放的音乐信息
     */
    public void setCurrentMusicInfo(MusicInfo musicInfo) {
        this.currentMusicInfo = musicInfo;
        notifyDataSetChanged(); // 通知 ViewPager2 重新创建 Fragment
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 根据位置返回不同的 Fragment
        if (position == 0) {
            // 位置0是音乐播放页
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

    // 【新增】重写 getItemId 和 containsItem，帮助 ViewPager2 更好地管理 Fragment 状态
    @Override
    public long getItemId(int position) {
        // 使用音乐ID + position 作为唯一ID，确保当音乐切换时，Fragment 会被重新创建
        // 或者简单地使用 position，如果 Fragment 内容完全依赖于 currentMusicInfo
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
