package com.example.music_community;

// 导入 MusicPlaybackFragment 类，因为接口方法中会用到它
import com.example.music_community.MusicPlaybackFragment;

/**
 * MusicPlaybackFragment 的回调监听器接口。
 * 用于 MusicPlaybackFragment 在其生命周期中通知其宿主 Activity 自身状态。
 */
public interface MusicPlaybackFragmentListener {
    /**
     * 当 MusicPlaybackFragment 的视图创建完成并准备好接收数据时回调。
     * @param fragment 准备就绪的 MusicPlaybackFragment 实例。
     */
    void onMusicPlaybackFragmentReady(MusicPlaybackFragment fragment);

    /**
     * 当 MusicPlaybackFragment 从其宿主 Activity 分离或视图销毁时回调。
     * 用于通知 Activity 清除对 Fragment 的引用。
     */
    void onMusicPlaybackFragmentDetached();
}
