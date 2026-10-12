package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Post;
import com.example.item.demo35.entity.PostComment;
import com.example.item.demo35.entity.PostDTO;
import com.example.item.demo35.entity.PostLike;
import com.example.item.demo35.service.PostService;
import com.example.item.demo35.util.AliOssUtils;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.awt.image.BufferedImage;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private com.example.item.demo35.service.UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * 单独上传文件接口
     */
    @PostMapping("/upload/file")
    public Result uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Result result = new Result();

        try {
            // 验证用户登录
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            // 验证文件是否为空
            if (file == null || file.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("文件不能为空");
                return result;
            }

            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 判断文件类型（图片或视频）
            String fileType = "unknown";
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    fileType = "images";
                } else if (contentType.startsWith("video/")) {
                    fileType = "videos";
                }
            }

            // 生成唯一的文件名
            String objectName = "posts/" + System.currentTimeMillis() + "_" + userId + "_" + fileType + "_" + System.nanoTime() + extension;

            // 上传文件到阿里云OSS
            String fileUrl = AliOssUtils.uploadFile(objectName, file.getInputStream());

            if (fileUrl == null || fileUrl.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("文件上传失败");
                return result;
            }

            // 返回上传成功的文件URL
            result.setSuccess(true);
            result.setMessage("上传成功");

            Map<String, Object> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("type", fileType);
            result.setData(data);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("上传失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 创建帖子 - 支持多张图片和视频（同时支持单个和多个视频）
     */
    @PostMapping("/create")
    public Result createPost(@RequestBody PostDTO postDto, @RequestHeader("Authorization") String token) {

        Result result = new Result();
        System.out.println("========== 开始处理帖子发布 ==========");

        // 从 DTO 中获取数据
        String content = postDto.getContent();
        List<String> imageUrls = postDto.getImages();
        List<String> videoUrls = postDto.getVideos();

        System.out.println("帖子内容: " + content);
        System.out.println("图片URL数量: " + (imageUrls != null ? imageUrls.size() : 0));
        System.out.println("视频URL数量: " + (videoUrls != null ? videoUrls.size() : 0));

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            // 注意：这里移除了文件上传到 OSS 的代码，
            // 因为根据日志分析，前端已经完成了文件上传并传回了 URL。

            // 确定封面：优先使用第一张图片，没有图片则使用视频第一帧
            String coverUrl = null;
            if (imageUrls != null && !imageUrls.isEmpty()) {
                coverUrl = imageUrls.get(0);
                System.out.println("使用第一张图片图片URL作为封面：" + coverUrl);
            } else if (videoUrls != null && !videoUrls.isEmpty()) {
                // 从视频提取第一帧作为封面
                System.out.println("没有图片，尝试从视频提取第一帧作为封面");
                // 注意：extractVideoFrame 方法需要确保在当前类或 Service 中可用
                coverUrl = extractVideoFrame(videoUrls.get(0), userId);
                if (coverUrl == null) {
                    System.err.println("警告：提取视频封面失败，但没有终止发布流程");
                } else {
                    System.out.println("视频封面提取成功：" + coverUrl);
                }
            } else {
                System.out.println("纯文本帖子，无封面");
            }

            boolean success = postService.createPost(userId, content, imageUrls, videoUrls, coverUrl);

            if (success) {
                result.setSuccess(true);
                result.setMessage("发布成功");
                System.out.println("帖子发布成功，用户ID：" + userId);
            } else {
                result.setSuccess(false);
                result.setMessage("发布失败");
                System.err.println("帖子发布失败，用户ID：" + userId);
            }
        } catch (Exception e) {
            System.err.println("发布过程发生异常: " + e.getMessage());
            e.printStackTrace();
            result.setSuccess(false);
            result.setMessage("发布失败: " + e.getMessage());
        }

        System.out.println("========== 帖子发布处理完成 ==========");
        return result;
    }

    /**
     * 创建帖子 - 支持单张图片和单个视频，兼容UniApp uploadFile
     */
    @PostMapping("/create/single")
    public Result createPostSingle(
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            // 上传图片到阿里云OSS
            List<String> imageUrls = new ArrayList<>();
            if (image != null && !image.isEmpty()) {
                String originalFilename = image.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String objectName = "posts/" + System.currentTimeMillis() + "_" + userId + "_img_0" + extension;

                String imageUrl = AliOssUtils.uploadFile(objectName, image.getInputStream());
                imageUrls.add(imageUrl);
                System.out.println("图片上传成功：" + imageUrl);
            }

            // 处理视频上传
            List<String> videoUrls = new ArrayList<>();
            if (video == null || video.isEmpty()) {
                // 视频为null或空，在控制台输出提示
                System.out.println("【提示】无视频传入，继续处理帖子发布");
            } else {
                // 有视频传入，进行上传处理
                System.out.println("【开始上传】检测到视频，开始上传到阿里云OSS");

                try {
                    String originalFilename = video.getOriginalFilename();
                    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String objectName = "posts/" + System.currentTimeMillis() + "_" + userId + "_video_0" + extension;

                    System.out.println("正在上传视频到阿里云OSS，文件名：" + originalFilename);

                    String videoUrl = AliOssUtils.uploadFile(objectName, video.getInputStream());

                    // 验证上传结果
                    if (videoUrl == null || videoUrl.isEmpty()) {
                        throw new RuntimeException("视频上传失败，返回的URL为空");
                    }

                    videoUrls.add(videoUrl);
                    System.out.println("视频上传成功，URL：" + videoUrl);

                } catch (Exception e) {
                    // 视频上传失败，直接报错返回
                    System.err.println("视频上传失败: " + e.getMessage());
                    result.setSuccess(false);
                    result.setMessage("视频上传失败: " + e.getMessage());
                    return result;
                }
            }

            // 确定封面：优先使用第一张图片，没有图片则使用视频第一帧
            String coverUrl = null;
            if (!imageUrls.isEmpty()) {
                coverUrl = imageUrls.get(0);
                System.out.println("使用图片作为封面：" + coverUrl);
            } else if (!videoUrls.isEmpty()) {
                // 从视频提取第一帧作为封面
                System.out.println("没有图片，尝试从视频提取第一帧作为封面");
                coverUrl = extractVideoFrame(videoUrls.get(0), userId);
                if (coverUrl == null) {
                    System.err.println("警告：提取视频封面失败，但没有终止发布流程");
                } else {
                    System.out.println("视频封面提取成功：" + coverUrl);
                }
            } else {
                System.out.println("纯文本帖子，无封面");
            }

            boolean success = postService.createPost(userId, content, imageUrls, videoUrls, coverUrl);

            if (success) {
                result.setSuccess(true);
                result.setMessage("发布成功");
                System.out.println("帖子发布成功，用户ID：" + userId);
            } else {
                result.setSuccess(false);
                result.setMessage("发布失败");
                System.err.println("帖子发布失败，用户ID：" + userId);
            }
        } catch (Exception e) {
            System.err.println("发布过程发生异常: " + e.getMessage());
            e.printStackTrace();
            result.setSuccess(false);
            result.setMessage("发布失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新帖子
     */
    @PutMapping("/{id}")
    public Result updatePost(
            @PathVariable("id") Integer postId,
            @RequestParam("content") String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "videos", required = false) List<MultipartFile> videos,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            // 上传新图片
            List<String> imageUrls = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    String originalFilename = image.getOriginalFilename();
                    String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    String objectName = "posts/" + System.currentTimeMillis() + "_" + userId + "_img_" + imageUrls.size() + extension;

                    String imageUrl = AliOssUtils.uploadFile(objectName, image.getInputStream());
                    imageUrls.add(imageUrl);
                }
            }

            // 处理视频上传
            List<String> videoUrls = new ArrayList<>();
            if (videos == null || videos.isEmpty()) {
                System.out.println("【更新-提示】无新视频传入");
            } else {
                System.out.println("【更新-开始上传】检测到" + videos.size() + "个新视频");

                for (int i = 0; i < videos.size(); i++) {
                    MultipartFile video = videos.get(i);
                    try {
                        String originalFilename = video.getOriginalFilename();
                        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                        String objectName = "posts/" + System.currentTimeMillis() + "_" + userId + "_video_" + i + extension;

                        String videoUrl = AliOssUtils.uploadFile(objectName, video.getInputStream());

                        if (videoUrl == null || videoUrl.isEmpty()) {
                            throw new RuntimeException("视频上传失败，返回的URL为空");
                        }

                        videoUrls.add(videoUrl);

                    } catch (Exception e) {
                        System.err.println("更新-视频上传失败: " + e.getMessage());
                        result.setSuccess(false);
                        result.setMessage("视频上传失败: " + e.getMessage());
                        return result;
                    }
                }
            }

            // 确定封面：优先使用第一张图片，没有图片则使用视频第一帧
            String coverUrl = null;
            if (!imageUrls.isEmpty()) {
                coverUrl = imageUrls.get(0);
            } else if (!videoUrls.isEmpty()) {
                coverUrl = extractVideoFrame(videoUrls.get(0), userId);
                if (coverUrl == null) {
                    System.err.println("更新-警告：提取视频封面失败");
                }
            }

            boolean success = postService.updatePost(postId, userId, content, imageUrls, videoUrls, coverUrl);

            if (success) {
                result.setSuccess(true);
                result.setMessage("更新成功");
            } else {
                result.setSuccess(false);
                result.setMessage("更新失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    public Result deletePost(
            @PathVariable("id") Integer postId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.deletePost(postId, userId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("删除成功");
            } else {
                result.setSuccess(false);
                result.setMessage("删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/{id}")
    public Result getPostById(
            @PathVariable("id") Integer postId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Result result = new Result();

        try {
            Integer currentUserId = getUserIdFromToken(token);
            Post post = postService.getPostById(postId, currentUserId);

            if (post != null) {
                result.setSuccess(true);
                result.setMessage("获取成功");
                result.setData(post);
            } else {
                result.setSuccess(false);
                result.setMessage("帖子不存在");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取所有帖子（分页）
     */
    @GetMapping("/list")
    public Result getAllPosts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Result result = new Result();

        try {
            Integer currentUserId = getUserIdFromToken(token);
            List<Post> posts = postService.getPostsByPage(page, size, currentUserId);

            result.setSuccess(true);
            result.setMessage("获取成功");

            Map<String, Object> data = new HashMap<>();
            data.put("posts", posts);
            data.put("page", page);
            data.put("size", size);
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取指定用户的帖子
     */
    @GetMapping("/user/{userId}")
    public Result getUserPosts(
            @PathVariable("userId") Integer userId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Result result = new Result();

        try {
            Integer currentUserId = getUserIdFromToken(token);
            List<Post> posts = postService.getUserPosts(userId, currentUserId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(posts);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 点赞帖子
     */
    @PostMapping("/{id}/like")
    public Result likePost(
            @PathVariable("id") Integer postId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.likePost(postId, userId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("点赞成功");
            } else {
                result.setSuccess(false);
                result.setMessage("已经点赞过或操作失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("点赞失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{id}/like")
    public Result unlikePost(
            @PathVariable("id") Integer postId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.unlikePost(postId, userId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("取消点赞成功");
            } else {
                result.setSuccess(false);
                result.setMessage("未点赞或操作失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消点赞失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取帖子的点赞列表
     */
    @GetMapping("/{id}/likes")
    public Result getPostLikes(@PathVariable("id") Integer postId) {
        Result result = new Result();

        try {
            List<PostLike> likes = postService.getPostLikes(postId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(likes);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 发表评论
     */
    @PostMapping("/{id}/comment")
    public Result createComment(
            @PathVariable("id") Integer postId,
            @RequestParam("content") String content,
            @RequestParam(value = "parentId", required = false) Integer parentId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.createComment(postId, userId, content, parentId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("评论成功");
            } else {
                result.setSuccess(false);
                result.setMessage("评论失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("评论失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新评论
     */
    @PutMapping("/comment/{id}")
    public Result updateComment(
            @PathVariable("id") Integer commentId,
            @RequestParam("content") String content,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.updateComment(commentId, userId, content);

            if (success) {
                result.setSuccess(true);
                result.setMessage("更新成功");
            } else {
                result.setSuccess(false);
                result.setMessage("更新失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{id}")
    public Result deleteComment(
            @PathVariable("id") Integer commentId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.deleteComment(commentId, userId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("删除成功");
            } else {
                result.setSuccess(false);
                result.setMessage("删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取帖子的所有评论
     */
    @GetMapping("/{id}/comments")
    public Result getPostComments(@PathVariable("id") Integer postId) {
        Result result = new Result();

        try {
            List<PostComment> comments = postService.getPostComments(postId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(comments);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取帖子的根评论（不包括回复）
     */
    @GetMapping("/{id}/comments/root")
    public Result getRootComments(@PathVariable("id") Integer postId) {
        Result result = new Result();

        try {
            List<PostComment> comments = postService.getRootComments(postId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(comments);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取某个评论的回复
     */
    @GetMapping("/comment/{parentId}/replies")
    public Result getReplies(@PathVariable("parentId") Integer parentId) {
        Result result = new Result();

        try {
            List<PostComment> replies = postService.getReplies(parentId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(replies);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户的评论
     */
    @GetMapping("/user/{userId}/comments")
    public Result getUserComments(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Result result = new Result();

        try {
            List<PostComment> comments = postService.getUserCommentsByPage(userId, page, size);

            result.setSuccess(true);
            result.setMessage("获取成功");

            Map<String, Object> data = new HashMap<>();
            data.put("comments", comments);
            data.put("page", page);
            data.put("size", size);
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 点赞评论
     */
    @PostMapping("/comment/{id}/like")
    public Result likeComment(
            @PathVariable("id") Integer commentId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.likeComment(commentId, userId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("点赞成功");
            } else {
                result.setSuccess(false);
                result.setMessage("已经点赞过或操作失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("点赞失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/comment/{id}/like")
    public Result unlikeComment(
            @PathVariable("id") Integer commentId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = postService.unlikeComment(commentId, userId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("取消点赞成功");
            } else {
                result.setSuccess(false);
                result.setMessage("未点赞或操作失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消点赞失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public Result getStatistics() {
        Result result = new Result();

        try {
            Map<String, Object> stats = postService.getPostStatistics();

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(stats);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取当前用户的帖子信息和点赞统计
     */
    @GetMapping("/my-posts")
    public Result getMyPosts(@RequestHeader("Authorization") String token) {
        Result result = new Result();
        System.out.println("盖亚！！！！！！！！！！！！！！！--------------------------------------------------" + token);
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            Map<String, Object> data = postService.getUserPostsWithStats(userId, userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }
        System.out.println("over over over===================================================" + token);
        return result;
    }

    /**
     * 获取当前用户的推荐帖子
     */
    @GetMapping("/recommend")
    public Result getRecommendedPosts(@RequestHeader("Authorization") String token) {
        Result result = new Result();
        System.out.println("--------------------------------------------------" + token);
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            // 获取推荐帖子列表
            List<Post> recommendedPosts = postService.getRecommendedPosts(userId, userId);

            if (recommendedPosts == null || recommendedPosts.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("获取推荐帖子失败: ");
                System.out.println("获取推荐帖子失败: ");
            } else{
                System.out.println(recommendedPosts);
                result.setSuccess(true);
                result.setMessage("获取推荐帖子成功");
                System.out.println("获取推荐帖子成功："+recommendedPosts);
                result.setData(recommendedPosts);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取推荐帖子失败: " + e.getMessage());
            System.out.println("获取推荐帖子失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("===================================================" + token);
        return result;
    }

    /**
     * 从token中获取用户ID
     */
    private Integer getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtUtils.validateToken(actualToken)) {
            return null;
        }

        String username = jwtUtils.getUsernameFromToken(actualToken);
        return userService.selectIdByName(username);
    }

    /**
     * 从视频提取第一帧作为封面
     */
    private String extractVideoFrame(String videoUrl, Integer userId) {
        FFmpegFrameGrabber grabber = null;
        try {
            // 创建临时文件
            java.io.File tempFrame = java.io.File.createTempFile("frame_", ".jpg");

            // 使用JavaCV提取第一帧
            grabber = new FFmpegFrameGrabber(String.valueOf(new java.net.URL(videoUrl)));
            grabber.start();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.convert(grabber.grabImage());
            grabber.stop();

            // 保存为图片
            javax.imageio.ImageIO.write(image, "jpg", tempFrame);

            // 上传封面图片到OSS
            java.io.InputStream frameStream = new java.io.FileInputStream(tempFrame);
            String objectName = "posts/" + System.currentTimeMillis() + "_" + userId + "_cover.jpg";
            String coverUrl = AliOssUtils.uploadFile(objectName, frameStream);
            frameStream.close();

            // 清理临时文件
            tempFrame.delete();

            return coverUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 临时方法：更新数据库表结构，添加 videos 和 cover 字段
     */
    @PostMapping("/update-table-structure")
    public Result updateTableStructure() {
        Result result = new Result();

        try {
            // 检查 videos 字段是否存在
            try {
                jdbcTemplate.queryForObject("SELECT videos FROM post LIMIT 1", String.class);
                System.out.println("videos 字段已存在");
            } catch (Exception e) {
                System.out.println("videos 字段不存在，正在添加...");
                jdbcTemplate.execute("ALTER TABLE `post` ADD COLUMN `videos` json DEFAULT NULL COMMENT '视频列表（JSON数组）' AFTER `images`");
                System.out.println("videos 字段添加成功");
            }

            // 检查 cover 字段是否存在
            try {
                jdbcTemplate.queryForObject("SELECT cover FROM post LIMIT 1", String.class);
                System.out.println("cover 字段已存在");
            } catch (Exception e) {
                System.out.println("cover 字段不存在，正在添加...");
                jdbcTemplate.execute("ALTER TABLE `post` ADD COLUMN `cover` varchar(500) DEFAULT NULL COMMENT '封面URL（优先使用第一张图片，没有图片则使用视频第一帧）' AFTER `videos`");
                System.out.println("cover 字段添加成功");
            }

            result.setSuccess(true);
            result.setMessage("数据库表结构更新成功");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("数据库表结构更新失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}
