package com.example.music_community.adapter;

import android.os.Handler; // 导入 Handler
import android.os.Looper; // 导入 Looper
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.music_community.R;
import com.example.music_community.model.HomePageInfo;
import com.example.music_community.model.MusicInfo;

import java.util.HashMap; // 导入 HashMap
import java.util.List;
import java.util.Map; // 导入 Map

public class HomePageAdapter extends BaseMultiItemQuickAdapter<HomePageInfo, BaseViewHolder> {

    // Handler 用于实现 Banner 自动轮播
    private Handler bannerAutoScrollHandler = new Handler(Looper.getMainLooper());
    // Map 用于存储每个 Banner ViewHolder 对应的自动轮播 Runnable，以便在 ViewHolder 回收时停止轮播
    private Map<RecyclerView.ViewHolder, Runnable> bannerAutoScrollRunnables = new HashMap<>();
    private static final long BANNER_AUTO_SCROLL_DELAY = 3000; // 自动轮播间隔 3 秒 (3000毫秒)

    public HomePageAdapter(List<HomePageInfo> data) {
        super(data);

        // 注册多类型布局及其对应的布局文件
        addItemType(1, R.layout.item_home_page_banner); // Style 1: banner
        addItemType(2, R.layout.item_home_page_horizontal_card); // Style 2: 横滑大卡
        addItemType(3, R.layout.item_home_page_one_column); // Style 3: 一行一列
        addItemType(4, R.layout.item_home_page_two_columns); // Style 4: 一行两列
    }


    protected int getItemType(@NonNull HomePageInfo item) {
        // 返回 HomePageInfo 中的 style 字段作为 itemType
        return item.getStyle();
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, HomePageInfo item) {
        // 根据 item 的 style 字段来判断当前模块类型，并进行相应的视图绑定
        switch (holder.getItemViewType()) {
            case 1: // Style 1: banner
                ViewPager2 viewPagerBanner = holder.getView(R.id.viewPagerBanner);
                LinearLayout bannerIndicatorLayout = holder.getView(R.id.layout_banner_indicator);

                List<MusicInfo> bannerMusicList = item.getMusicInfoList();
                if (bannerMusicList != null && !bannerMusicList.isEmpty()) {
                    // 初始化 Banner 适配器
                    BannerAdapter bannerAdapter = new BannerAdapter(bannerMusicList);
                    viewPagerBanner.setAdapter(bannerAdapter);

                    // 如果只有一个 Item，禁用滑动和自动轮播，隐藏指示器
                    if (bannerMusicList.size() <= 1) {
                        viewPagerBanner.setUserInputEnabled(false); // 禁用用户滑动
                        bannerIndicatorLayout.setVisibility(View.GONE); // 隐藏指示器
                        // 确保停止可能存在的自动轮播（如果数据从多图变为单图）
                        if (bannerAutoScrollRunnables.containsKey(holder)) {
                            bannerAutoScrollHandler.removeCallbacks(bannerAutoScrollRunnables.get(holder));
                            bannerAutoScrollRunnables.remove(holder);
                        }
                    } else {
                        viewPagerBanner.setUserInputEnabled(true); // 启用用户滑动
                        bannerIndicatorLayout.setVisibility(View.VISIBLE);

                        // 初始设置指示器
                        // 确保 ViewPager2 的当前项是实际数据列表的有效索引，
                        // 在设置 ViewPager2 的 adapter 之后获取 currentItem
                        int initialRealPosition = viewPagerBanner.getCurrentItem() % bannerMusicList.size();
                        setupBannerIndicator(bannerIndicatorLayout, bannerMusicList.size(), initialRealPosition);

                        // 监听 ViewPager2 页面变化，更新指示器
                        // 每次 convert 都可能重新注册，但 ViewPager2 会处理重复注册
                        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                int realPosition = position % bannerMusicList.size(); // 获取真实位置
                                setupBannerIndicator(bannerIndicatorLayout, bannerMusicList.size(), realPosition);
                            }
                        });

                        // 设置初始位置，使其看起来从中间开始，方便循环
                        // 只有在 ViewPager2 第一次绑定数据时设置，避免重复设置导致跳动
                        // 使用 setTag 来标记是否已初始化，防止重复设置
                        if (viewPagerBanner.getTag() == null) {
                            // 设置一个较大的初始值，使其能够向前或向后滑动多圈
                            viewPagerBanner.setCurrentItem(bannerMusicList.size() * 100, false); // false 表示不平滑滚动
                            viewPagerBanner.setTag("initialized"); // 设置 Tag 标记已初始化
                        }

                        // **自动轮播逻辑移到 onViewAttachedToWindow 和 onViewDetachedFromWindow 管理**
                        // 这里不再直接启动 Runnable，而是确保 ViewHolder 被附加到窗口时才启动
                    }
                } else {
                    // 如果没有 Banner 数据，可以隐藏 ViewPager 或显示占位图
                    viewPagerBanner.setVisibility(View.GONE);
                    bannerIndicatorLayout.setVisibility(View.GONE);
                }
                break;

            case 2: // Style 2: 横滑大卡
                TextView tvHorizontalCardTitle = holder.getView(R.id.tv_module_title_horizontal_card);
                RecyclerView recyclerViewHorizontalCard = holder.getView(R.id.recyclerView_horizontal_card);

                tvHorizontalCardTitle.setText(R.string.module_title_horizontal_card);

                recyclerViewHorizontalCard.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

                // 调用 MusicItemAdapter 新的构造函数，传入布局ID
                MusicItemAdapter horizontalCardAdapter = new MusicItemAdapter(R.layout.item_music_info_large, item.getMusicInfoList());
                recyclerViewHorizontalCard.setAdapter(horizontalCardAdapter);
                break;

            case 3: // Style 3: 一行一列
                TextView tvOneColumnTitle = holder.getView(R.id.tv_module_title_one_column);
                RecyclerView recyclerViewOneColumn = holder.getView(R.id.recyclerView_one_column);

                // **修改：引用字符串资源**
                tvOneColumnTitle.setText(R.string.module_title_one_column);

                // **修改：每日推荐改为横向滑动，并使用新的矮长布局**
                recyclerViewOneColumn.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                MusicItemAdapter oneColumnAdapter = new MusicItemAdapter(R.layout.item_music_info_tall_narrow, item.getMusicInfoList()); // **使用新的布局**
                recyclerViewOneColumn.setAdapter(oneColumnAdapter);
                break;

            case 4: // Style 4: 一行两列
                TextView tvTwoColumnsTitle = holder.getView(R.id.tv_module_title_two_columns);
                RecyclerView recyclerViewTwoColumns = holder.getView(R.id.recyclerView_two_columns);

                // **修改：引用字符串资源**
                tvTwoColumnsTitle.setText(R.string.module_title_two_columns);

                // **修改：热门金曲使用新的正方形布局**
                recyclerViewTwoColumns.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 两列网格布局
                MusicItemAdapter twoColumnsAdapter = new MusicItemAdapter(R.layout.item_music_info_square, item.getMusicInfoList()); // **使用新的布局**
                recyclerViewTwoColumns.setAdapter(twoColumnsAdapter);
                break;
        }
    }

    /**
     * 设置 Banner 指示器
     * @param indicatorLayout 放置指示器点的布局
     * @param count Banner 总数
     * @param currentPosition 当前选中位置 (真实数据位置)
     */
    private void setupBannerIndicator(LinearLayout indicatorLayout, int count, int currentPosition) {
        indicatorLayout.removeAllViews(); // 清除所有旧的点

        if (count <= 1) {
            indicatorLayout.setVisibility(View.GONE); // 如果只有一张图，隐藏指示器
            return;
        }
        indicatorLayout.setVisibility(View.VISIBLE); // 显示指示器

        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            // 设置点的大小
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(12, 12);
            params.setMargins(6, 0, 6, 0); // 设置点之间的间距
            dot.setLayoutParams(params);
            // 设置点的背景 (选中和未选中状态)
            dot.setBackgroundResource(i == currentPosition ? R.drawable.indicator_dot_selected : R.drawable.indicator_dot_unselected);
            indicatorLayout.addView(dot);
        }
    }

    /**
     * 当 ViewHolder 被附加到窗口时调用 (即变得可见)
     * 在这里启动 Banner 的自动轮播
     * @param holder 当前 ViewHolder
     */
    @Override
    public void onViewAttachedToWindow(@NonNull BaseViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // 如果是 Banner 类型的 ViewHolder (viewType == 1)，启动自动轮播
        if (holder.getItemViewType() == 1) {
            ViewPager2 viewPagerBanner = holder.getView(R.id.viewPagerBanner);
            // 获取对应位置的 Banner 数据，确保数据有效
            HomePageInfo homePageInfo = getData().get(holder.getAdapterPosition());
            List<MusicInfo> bannerMusicList = homePageInfo.getMusicInfoList();

            if (bannerMusicList != null && bannerMusicList.size() > 1) {
                Runnable autoScrollRunnable = new Runnable() {
                    @Override
                    public void run() {
                        int currentItem = viewPagerBanner.getCurrentItem();
                        // 滚动到下一页，并设置为平滑滚动
                        viewPagerBanner.setCurrentItem(currentItem + 1, true);
                        // 再次发送延迟消息，实现循环轮播
                        bannerAutoScrollHandler.postDelayed(this, BANNER_AUTO_SCROLL_DELAY);
                    }
                };
                // 存储 Runnable，以便在 ViewHolder 回收时停止轮播
                bannerAutoScrollRunnables.put(holder, autoScrollRunnable);
                // 首次延迟启动自动轮播
                bannerAutoScrollHandler.postDelayed(autoScrollRunnable, BANNER_AUTO_SCROLL_DELAY);
            }
        }
    }

    /**
     * 当 ViewHolder 被从窗口分离时调用 (即变得不可见或被回收)
     * 在这里停止 Banner 的自动轮播，防止内存泄漏和不必要的 CPU 占用
     * @param holder 当前 ViewHolder
     */
    @Override
    public void onViewDetachedFromWindow(@NonNull BaseViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        // 如果是 Banner 类型的 ViewHolder (viewType == 1)，停止自动轮播
        if (holder.getItemViewType() == 1) {
            if (bannerAutoScrollRunnables.containsKey(holder)) {
                Runnable autoScrollRunnable = bannerAutoScrollRunnables.get(holder);
                // 移除所有与此 Runnable 相关的回调
                bannerAutoScrollHandler.removeCallbacks(autoScrollRunnable);
                // 从 Map 中移除，释放引用
                bannerAutoScrollRunnables.remove(holder);
            }
        }
    }

    /**
     * 当适配器从 RecyclerView 中分离时调用
     * 清理所有未处理的 Handler 回调，防止内存泄漏
     * @param recyclerView 适配器所关联的 RecyclerView
     */
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // 移除所有 Handler 回调和消息，确保所有轮播都停止
        bannerAutoScrollHandler.removeCallbacksAndMessages(null);
        // 清空 Map，释放所有 Runnable 引用
        bannerAutoScrollRunnables.clear();
    }
}