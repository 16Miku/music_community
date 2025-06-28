package com.example.music_community.adapter;

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

import java.util.List;

public class HomePageAdapter extends BaseMultiItemQuickAdapter<HomePageInfo, BaseViewHolder> {

    public HomePageAdapter(List<HomePageInfo> data) {
        super(data);

        addItemType(1, R.layout.item_home_page_banner);
        addItemType(2, R.layout.item_home_page_horizontal_card);
        addItemType(3, R.layout.item_home_page_one_column);
        addItemType(4, R.layout.item_home_page_two_columns);
    }


    protected int getItemType(@NonNull HomePageInfo item) {
        return item.getStyle();
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, HomePageInfo item) {
        switch (holder.getItemViewType()) {
            case 1: // Style 1: banner
                ViewPager2 viewPagerBanner = holder.getView(R.id.viewPagerBanner);
                LinearLayout bannerIndicatorLayout = holder.getView(R.id.layout_banner_indicator);

                List<MusicInfo> bannerMusicList = item.getMusicInfoList();
                if (bannerMusicList != null && !bannerMusicList.isEmpty()) {
                    BannerAdapter bannerAdapter = new BannerAdapter(bannerMusicList);
                    viewPagerBanner.setAdapter(bannerAdapter);

                    if (bannerMusicList.size() <= 1) {
                        viewPagerBanner.setUserInputEnabled(false);
                        bannerIndicatorLayout.setVisibility(View.GONE);
                    } else {
                        viewPagerBanner.setUserInputEnabled(true);
                        bannerIndicatorLayout.setVisibility(View.VISIBLE);
                        setupBannerIndicator(bannerIndicatorLayout, bannerMusicList.size(), 0);

                        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                int realPosition = position % bannerMusicList.size();
                                setupBannerIndicator(bannerIndicatorLayout, bannerMusicList.size(), realPosition);
                            }
                        });
                        viewPagerBanner.setCurrentItem(bannerMusicList.size() * 100);
                    }
                } else {
                    viewPagerBanner.setVisibility(View.GONE);
                    bannerIndicatorLayout.setVisibility(View.GONE);
                }
                break;

            case 2: // Style 2: 横滑大卡
                TextView tvHorizontalCardTitle = holder.getView(R.id.tv_module_title_horizontal_card);
                RecyclerView recyclerViewHorizontalCard = holder.getView(R.id.recyclerView_horizontal_card);

                tvHorizontalCardTitle.setText(item.getModuleName() != null ? item.getModuleName() : "横滑大卡");

                recyclerViewHorizontalCard.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

                // **核心修改：调用 MusicItemAdapter 新的构造函数，传入布局ID**
                MusicItemAdapter horizontalCardAdapter = new MusicItemAdapter(R.layout.item_music_info_large, item.getMusicInfoList());
                recyclerViewHorizontalCard.setAdapter(horizontalCardAdapter);
                break;

            case 3: // Style 3: 一行一列
                TextView tvOneColumnTitle = holder.getView(R.id.tv_module_title_one_column);
                RecyclerView recyclerViewOneColumn = holder.getView(R.id.recyclerView_one_column);

                tvOneColumnTitle.setText(item.getModuleName() != null ? item.getModuleName() : "一行一列");

                recyclerViewOneColumn.setLayoutManager(new LinearLayoutManager(getContext()));

                // **核心修改：调用 MusicItemAdapter 新的构造函数，传入布局ID**
                MusicItemAdapter oneColumnAdapter = new MusicItemAdapter(R.layout.item_music_info_small, item.getMusicInfoList());
                recyclerViewOneColumn.setAdapter(oneColumnAdapter);
                break;

            case 4: // Style 4: 一行两列
                TextView tvTwoColumnsTitle = holder.getView(R.id.tv_module_title_two_columns);
                RecyclerView recyclerViewTwoColumns = holder.getView(R.id.recyclerView_two_columns);

                tvTwoColumnsTitle.setText(item.getModuleName() != null ? item.getModuleName() : "一行两列");

                recyclerViewTwoColumns.setLayoutManager(new GridLayoutManager(getContext(), 2));

                // 调用 MusicItemAdapter 新的构造函数，传入布局ID**
                MusicItemAdapter twoColumnsAdapter = new MusicItemAdapter(R.layout.item_music_info_small, item.getMusicInfoList());
                recyclerViewTwoColumns.setAdapter(twoColumnsAdapter);
                break;
        }
    }

    private void setupBannerIndicator(LinearLayout indicatorLayout, int count, int currentPosition) {
        indicatorLayout.removeAllViews();

        if (count <= 1) {
            indicatorLayout.setVisibility(View.GONE);
            return;
        }
        indicatorLayout.setVisibility(View.VISIBLE);

        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(12, 12);
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == currentPosition ? R.drawable.indicator_dot_selected : R.drawable.indicator_dot_unselected);
            indicatorLayout.addView(dot);
        }
    }
}
