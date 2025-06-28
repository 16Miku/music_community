package com.example.music_community.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Page<T> { // 使用泛型 T 来表示 records 列表中的具体数据类型
    @SerializedName("records")
    private List<T> records; // 音乐列表，这里是 HomePageInfo 的列表
    @SerializedName("total")
    private int total; // 总数
    @SerializedName("size")
    private int size; // 当前页大小
    @SerializedName("current")
    private int current; // 当前页
    @SerializedName("pages")
    private int pages; // 总页数

    // 无参构造函数
    public Page() {
    }

    // 构造函数
    public Page(List<T> records, int total, int size, int current, int pages) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = pages;
    }

    // Getter 和 Setter 方法
    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    @Override
    public String toString() {
        return "Page{" +
                "records=" + records +
                ", total=" + total +
                ", size=" + size +
                ", current=" + current +
                ", pages=" + pages +
                '}';
    }
}

