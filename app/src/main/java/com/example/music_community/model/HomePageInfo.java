package com.example.music_community.model;

import com.google.gson.annotations.SerializedName;
import com.chad.library.adapter.base.entity.MultiItemEntity; // **导入 MultiItemEntity 接口**

import java.util.List;

// 实现 MultiItemEntity 接口
public class HomePageInfo implements MultiItemEntity {
    @SerializedName("moduleConfigId")
    private int moduleConfigId; // 模块配置ID
    @SerializedName("moduleName")
    private String moduleName; // 模块名称 (文档说明本地写死，但接口有返回)
    @SerializedName("style")
    private int style; // 样式 (1: banner, 2: 横滑大卡, 3: 一行一列, 4: 一行两列)
    @SerializedName("musicInfoList")
    private List<MusicInfo> musicInfoList; // 模块下的音乐列表信息

    // 无参构造函数
    public HomePageInfo() {
    }

    // 构造函数
    public HomePageInfo(int moduleConfigId, String moduleName, int style, List<MusicInfo> musicInfoList) {
        this.moduleConfigId = moduleConfigId;
        this.moduleName = moduleName;
        this.style = style;
        this.musicInfoList = musicInfoList;
    }

    // Getter 和 Setter 方法
    public int getModuleConfigId() {
        return moduleConfigId;
    }

    public void setModuleConfigId(int moduleConfigId) {
        this.moduleConfigId = moduleConfigId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public List<MusicInfo> getMusicInfoList() {
        return musicInfoList;
    }

    public void setMusicInfoList(List<MusicInfo> musicInfoList) {
        this.musicInfoList = musicInfoList;
    }

    @Override
    public String toString() {
        return "HomePageInfo{" +
                "moduleConfigId=" + moduleConfigId +
                ", moduleName='" + moduleName + '\'' +
                ", style=" + style +
                ", musicInfoList=" + musicInfoList +
                '}';
    }

    //实现 MultiItemEntity 接口的 getItemType() 方法
    @Override
    public int getItemType() {
        // 返回 style 字段作为当前 item 的视图类型
        // 这个值会与 HomePageAdapter 中 addItemType 注册的 viewType 进行匹配
        return style;
    }
}
