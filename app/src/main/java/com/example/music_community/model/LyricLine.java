package com.example.music_community.model;

/**
 * 歌词行数据模型
 * 包含每行歌词的时间戳（毫秒）和歌词文本
 */
public class LyricLine {
    private long timestamp; // 歌词开始播放的时间戳，单位毫秒
    private String text;    // 歌词文本内容

    public LyricLine(long timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "LyricLine{" +
                "timestamp=" + timestamp +
                ", text='" + text + '\'' +
                '}';
    }
}
