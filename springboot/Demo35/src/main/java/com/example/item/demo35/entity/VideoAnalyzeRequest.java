package com.example.item.demo35.entity;

/**
 * 视频分析请求实体类
 */
public class VideoAnalyzeRequest {
    private String ossUrl;
    private String username;
    private String exerciseType;
    private String videoName;

    public VideoAnalyzeRequest() {}

    public VideoAnalyzeRequest(String ossUrl, String username, String exerciseType, String videoName) {
        this.ossUrl = ossUrl;
        this.username = username;
        this.exerciseType = exerciseType;
        this.videoName = videoName;
    }

    public String getOssUrl() {
        return ossUrl;
    }

    public void setOssUrl(String ossUrl) {
        this.ossUrl = ossUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }
}