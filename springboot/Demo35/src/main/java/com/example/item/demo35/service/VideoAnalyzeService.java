package com.example.item.demo35.service;

import com.example.item.demo35.entity.VideoAnalyzeRequest;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 视频分析服务
 */
@Service
public class VideoAnalyzeService {

    // Python虚拟环境路径
    private static final String PYTHON_PATH = "D:\\VueProject\\rumbing\\running\\.venv\\Scripts\\python.exe";

    // Python分析脚本路径
    private static final String PYTHON_SCRIPT_PATH = "D:\\VueProject\\rumbing\\running\\video_analyzer.py";

    // 临时视频存储目录
    private static final String TEMP_VIDEO_DIR = "D:\\VueProject\\rumbing\\running\\temp_videos";

    public String processVideo(VideoAnalyzeRequest request) throws Exception {
        System.out.println("视频分析请求详情:");
        System.out.println("  OSS URL: " + request.getOssUrl());
        System.out.println("  用户名: " + request.getUsername());
        System.out.println("  运动类型: " + request.getExerciseType());
        System.out.println("  视频名称: " + request.getVideoName());

        // 1. 从OSS下载视频到本地
        String localVideoPath = downloadVideoFromOss(request.getOssUrl(), request.getVideoName());
        System.out.println("视频已下载到本地: " + localVideoPath);

        // 2. 调用Python分析脚本
        String analysisResult = callPythonAnalyzer(localVideoPath, request.getUsername(), request.getExerciseType());
        System.out.println("分析结果: " + analysisResult);

        // 3. 清理临时文件
        cleanupTempFile(localVideoPath);

        return analysisResult;
    }

    private String downloadVideoFromOss(String ossUrl, String videoName) throws Exception {
        Path tempDir = Paths.get(TEMP_VIDEO_DIR);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String safeFileName = videoName.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
        Path localPath = tempDir.resolve(safeFileName);

        URL url = new URL(ossUrl);
        try (InputStream is = url.openStream();
             OutputStream os = Files.newOutputStream(localPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        return localPath.toString();
    }

    private String callPythonAnalyzer(String videoPath, String username, String exerciseType) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            PYTHON_PATH, PYTHON_SCRIPT_PATH,
            "--video", videoPath,
            "--username", username,
            "--exercise-type", exerciseType
        );

        pb.directory(new File("D:\\VueProject\\rumbing\\running"));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("Python输出: " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python脚本执行失败，退出码: " + exitCode + "\n输出: " + output);
        }

        return output.toString().trim();
    }

    private void cleanupTempFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            System.out.println("临时文件已清理: " + filePath);
        } catch (IOException e) {
            System.out.println("清理临时文件失败: " + e.getMessage());
        }
    }
}
