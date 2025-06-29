package com.example.music_community.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.music_community.LyricFragment;
import com.example.music_community.MusicPlaybackFragment;
import com.example.music_community.model.MusicInfo;

import android.util.Log;

import java.util.Objects;

public class PlayerPagerAdapter extends FragmentStateAdapter {

    private static final String TAG = "PlayerPagerAdapter";
    private MusicInfo currentMusicInfo;

    public PlayerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * 更新 Adapter 当前持有的音乐信息，并通知 ViewPager2 刷新。
     * @param musicInfo 最新的音乐信息。
     */
    public void setCurrentMusicInfo(MusicInfo musicInfo) {
        this.currentMusicInfo = musicInfo;
        Log.d(TAG, "setCurrentMusicInfo: Updated to " + (musicInfo != null ? musicInfo.getMusicName() : "null") + ". Calling notifyDataSetChanged().");
        // 关键：调用 notifyDataSetChanged() 来通知 ViewPager2 数据已更新，它会触发后续的 ID 检查和 Fragment 重建。
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 根据位置创建对应的 Fragment 实例，并传递当前的音乐信息。
        Log.d(TAG, "createFragment called for position: " + position + ", with music: " + (currentMusicInfo != null ? currentMusicInfo.getMusicName() : "null"));
        if (position == 0) {
            // 位置 0 是音乐封面页。
            return MusicPlaybackFragment.newInstance(currentMusicInfo);
        } else {
            // 位置 1 是歌词页。
            return LyricFragment.newInstance(currentMusicInfo);
        }
    }

    @Override
    public int getItemCount() {
        // 如果有音乐信息，则有两个页面（封面和歌词），否则没有页面。
        int count = currentMusicInfo != null ? 2 : 0;
        Log.d(TAG, "getItemCount: Returning " + count);
        return count;
    }

    /**
     * 【核心修复】返回一个基于当前歌曲内容（我们选择 musicUrl）的唯一标识符。
     * FragmentStateAdapter 使用此 ID 来确定 Fragment 是否已更改。
     * 当歌曲切换时，musicUrl 会改变，其 hashCode 也会改变，从而生成一个新的 ID。
     * 这会强制 ViewPager2 销毁旧的 Fragment 并创建新的 Fragment。
     * @param position 页面位置
     * @return 该位置上 Fragment 的唯一 ID
     */
    @Override
    public long getItemId(int position) {
        if (currentMusicInfo != null && currentMusicInfo.getMusicUrl() != null) {
            // 使用音乐URL的哈希码结合 position 生成一个唯一的、稳定的 ID。
            // Objects.hash() 是一个安全的方式来获取哈希码，能正确处理 null。
            return Objects.hash(currentMusicInfo.getMusicUrl(), position);
        }
        // 如果没有音乐信息，则返回一个默认的、不稳定的 ID。
        return super.getItemId(position);
    }

    /**
     * 【核心修复】检查给定的 itemId 是否仍然存在于当前数据集中。
     * 当 notifyDataSetChanged() 被调用时，ViewPager2 会用此方法检查现有的 Fragment
     * 是否还应该显示。如果返回 false，旧的 Fragment 将被移除。
     * @param itemId 要检查的 Fragment 的 ID
     * @return 如果该 ID 对应当前歌曲的 Fragment，则返回 true
     */
    @Override
    public boolean containsItem(long itemId) {
        if (currentMusicInfo != null && currentMusicInfo.getMusicUrl() != null) {
            // 检查传入的 itemId 是否等于当前歌曲的封面页或歌词页的 ID。
            long expectedId0 = Objects.hash(currentMusicInfo.getMusicUrl(), 0);
            long expectedId1 = Objects.hash(currentMusicInfo.getMusicUrl(), 1);
            return itemId == expectedId0 || itemId == expectedId1;
        }
        // 如果没有音乐信息，那么任何 ID 都不应该被包含。
        return false;
    }
}
