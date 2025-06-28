package com.example.music_community.model;

import com.google.gson.annotations.SerializedName;

public class HomePageResponse {
    @SerializedName("code")
    private int code; // 业务返回码
    @SerializedName("msg")
    private String msg; // 返回消息
    @SerializedName("data")
    private Page<HomePageInfo> data; // 首页信息，这里 Page 的泛型是 HomePageInfo

    // 无参构造函数
    public HomePageResponse() {
    }

    // 构造函数
    public HomePageResponse(int code, String msg, Page<HomePageInfo> data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // Getter 和 Setter 方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Page<HomePageInfo> getData() {
        return data;
    }

    public void setData(Page<HomePageInfo> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "HomePageResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
