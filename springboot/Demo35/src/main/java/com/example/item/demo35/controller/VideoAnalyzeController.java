package com.example.item.demo35.controller;

import com.example.item.demo35.entity.VideoAnalyzeRequest;
import com.example.item.demo35.service.VideoAnalyzeService;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 视频分析控制器
 * 接收来自树莓派的视频分析请求
 */
@RestController
@RequestMapping("/api/video")
public class VideoAnalyzeController {

    @Autowired
    private VideoAnalyzeService videoAnalyzeService;

    /**
     * 接收视频分析请求
     * 树莓派上传视频到OSS后，调用此接口触发分析
     *
     * @param request 包含OSS URL、用户名和运动类型
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public Result analyzeVideo(@RequestBody VideoAnalyzeRequest request) {
        try {
            System.out.println("收到视频分析请求:");
            System.out.println("  OSS URL: " + request.getOssUrl());
            System.out.println("  用户名: " + request.getUsername());
            System.out.println("  运动类型: " + request.getExerciseType());

            // 调用分析服务
            String result = videoAnalyzeService.processVideo(request);

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("视频分析失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result healthCheck() {
        return Result.success("视频分析服务运行正常");
    }
}