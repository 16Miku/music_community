package com.example.music_community.model; // 建议创建一个 model 包来存放数据模型

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

// 实现 Serializable 接口
public class MusicInfo implements Serializable {
    @SerializedName("id")
    private Long id; // 音乐ID
    @SerializedName("musicName")
    private String musicName; // 音乐名称
    @SerializedName("author")
    private String author; // 作者/歌手
    @SerializedName("coverUrl")
    private String coverUrl; // 封面图URL
    @SerializedName("musicUrl")
    private String musicUrl; // 音乐地址URL
    @SerializedName("lyricUrl")
    private String lyricUrl; // 音乐歌词文件地址URL

    // 无参构造函数是 Gson 反序列化所必需的
    public MusicInfo() {
    }

    // 构造函数，方便创建对象
    public MusicInfo(Long id, String musicName, String author, String coverUrl, String musicUrl, String lyricUrl) {
        this.id = id;
        this.musicName = musicName;
        this.author = author;
        this.coverUrl = coverUrl;
        this.musicUrl = musicUrl;
        this.lyricUrl = lyricUrl;
    }


    // ... 其他代码 ...

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicInfo musicInfo = (MusicInfo) o;
        // 通常，音乐的唯一标识是 ID，如果 ID 相同，则认为是同一首歌
        // 使用 Objects.equals 处理可能为 null 的情况
        return Objects.equals(id, musicInfo.id);
    }

    @Override
    public int hashCode() {
        // 使用 Objects.hash 处理可能为 null 的情况
        return Objects.hash(id);
    }
// ... 其他代码 ...




    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        this.musicUrl = musicUrl;
    }

    public String getLyricUrl() {
        return lyricUrl;
    }

    public void setLyricUrl(String lyricUrl) {
        this.lyricUrl = lyricUrl;
    }

    @Override
    public String toString() {
        return "MusicInfo{" +
                "id=" + id +
                ", musicName='" + musicName + '\'' +
                ", author='" + author + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", musicUrl='" + musicUrl + '\'' +
                ", lyricUrl='" + lyricUrl + '\'' +
                '}';
    }
}


