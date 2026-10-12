package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.PostDao;
import com.example.item.demo35.Dao.PostCommentDao;
import com.example.item.demo35.Dao.PostLikeDao;
import com.example.item.demo35.Dao.UserDao;
import com.example.item.demo35.Dao.CommentLikeDao;
import com.example.item.demo35.config.BigDataConfig;
import com.example.item.demo35.entity.Post;
import com.example.item.demo35.entity.PostComment;
import com.example.item.demo35.entity.PostLike;
import com.example.item.demo35.entity.User;
import com.example.item.demo35.service.HBaseService;
import com.example.item.demo35.service.PostService;
import com.example.item.demo35.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
//@ConditionalOnProperty(name = "bigdata.enabled", havingValue = "true")
public class PostServiceImpl implements PostService {

    @Autowired
    private BigDataConfig bigDataConfig;

    @Autowired
    private PostDao postDao;

    @Autowired
    private PostLikeDao postLikeDao;

    @Autowired
    private PostCommentDao postCommentDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CommentLikeDao commentLikeDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Gson gson = new Gson();

    //==============================Kafka========================
    private void sendPostEvent(String eventType, Post post) {
        if (!bigDataConfig.isEnabled()) {
            return; // 如果大数据功能未启用，不发送 Kafka 消息
        }
        try {
            // 构建一个简单的 JSON 消息
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());
            event.put("post", post);
            String message = gson.toJson(event);
            kafkaTemplate.send("post-change", message);
            System.out.println("发送 Kafka 消息: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //==============================Kafka========================

    @Override
    public boolean createPost(Integer userId, String content, List<String> images, List<String> videos, String cover) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setImages(images);
        post.setVideos(videos);
        post.setCover(cover);
        post.setLikeCount(0);
        post.setCommentCount(0);

        boolean result = postDao.createPost(post);
        System.out.println("创建帖子结果: " + result);
        if (result) {
            sendPostEvent("post_created", post);
            System.out.println("postserviceImpl阶段，已发布Kafka消息");
        }
        return result;
    }

    @Override
    public boolean updatePost(Integer postId, Integer userId, String content, List<String> images, List<String> videos, String cover) {
        Post post = postDao.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            return false;
        }

        post.setContent(content);
        post.setImages(images);
        post.setVideos(videos);
        post.setCover(cover);

        //return postDao.updatePost(post);
        boolean result = postDao.updatePost(post);
        System.out.println("修改帖子结果: " + result);
        if (result) {
            sendPostEvent("post_update", post);
            System.out.println("postserviceImpl阶段，已发布Kafka消息");
        }
        return result;
    }

    @Override
    public boolean deletePost(Integer postId, Integer userId) {
        Post post = postDao.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            return false;
        }
        boolean result = postDao.deletePost(postId);
        if (result){
            sendPostEvent("post_delete", post);
            System.out.println("postserviceImpl阶段，已发布Kafka消息");
        }
        return result;
    }

    @Override
    public Post getPostById(Integer postId, Integer currentUserId) {
        Post post = postDao.selectById(postId);
        if (post == null) {
            return null;
        }

        // 检查当前用户是否点赞帖子
        if (currentUserId != null) {
            boolean isLiked = postLikeDao.isLiked(postId, currentUserId);
            post.setIsLiked(isLiked);
        } else {
            post.setIsLiked(false);
        }

        return post;
    }

    @Override
    public List<Post> getAllPosts(Integer currentUserId) {
        List<Post> posts = postDao.selectAll(null);
        posts.forEach(post -> {
            // 检查当前用户是否点赞帖子
            if (currentUserId != null) {
                boolean isLiked = postLikeDao.isLiked(post.getId(), currentUserId);
                post.setIsLiked(isLiked);
            } else {
                post.setIsLiked(false);
            }
        });

        return posts;
    }

    @Override
    public List<Post> getUserPosts(Integer userId, Integer currentUserId) {
        List<Post> posts = postDao.selectByUserId(userId, 0, Integer.MAX_VALUE);
        posts.forEach(post -> {
            // 检查当前用户是否点赞帖子
            if (currentUserId != null) {
                boolean isLiked = postLikeDao.isLiked(post.getId(), currentUserId);
                post.setIsLiked(isLiked);
            } else {
                post.setIsLiked(false);
            }
        });

        return posts;
    }

    @Override
    public List<Post> getPostsByPage(int page, int size, Integer currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts = postDao.selectByPage(offset, size);
        posts.forEach(post -> {
            // 检查当前用户是否点赞帖子
            if (currentUserId != null) {
                boolean isLiked = postLikeDao.isLiked(post.getId(), currentUserId);
                post.setIsLiked(isLiked);
            } else {
                post.setIsLiked(false);
            }
        });

        return posts;
    }

    @Override
    public Map<String, Object> getPostStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPosts", postDao.countAll());
        stats.put("totalLikes", 0);
        stats.put("totalComments", 0);
        return stats;
    }

    @Override
    public Map<String, Object> getUserPostsWithStats(Integer userId, Integer currentUserId) {
        Map<String, Object> result = new HashMap<>();

        // 获取用户的所有帖子
        List<Post> posts = postDao.selectByUserId(userId, 0, Integer.MAX_VALUE);
        posts.forEach(post -> {
            // 检查当前用户是否点赞帖子
            if (currentUserId != null) {
                boolean isLiked = postLikeDao.isLiked(post.getId(), currentUserId);
                post.setIsLiked(isLiked);
            } else {
                post.setIsLiked(false);
            }
        });

        // 统计总点赞数
        Integer totalLikes = postDao.sumLikeCountByUserId(userId);
        if (totalLikes == null) {
            totalLikes = 0;
        }

        // 统计总评论数
        int totalComments = 0;
        for (Post post : posts) {
            totalComments += post.getCommentCount();
        }

        result.put("posts", posts);
        result.put("totalPosts", posts.size());
        result.put("totalLikes", totalLikes);
        result.put("totalComments", totalComments);

        return result;
    }

    @Override
    public boolean likePost(Integer postId, Integer userId) {
        // 检查是否已经点赞
        if (postLikeDao.isLiked(postId, userId)) {
            return false;
        }

        // 添加点赞
        boolean result = postLikeDao.likePost(postId, userId);

        if (result) {
            // 更新帖子点赞数（使用原子操作）
            postDao.incrementLikeCount(postId);
        }

        return result;
    }

    @Override
    public boolean unlikePost(Integer postId, Integer userId) {
        // 检查是否已点赞
        if (!postLikeDao.isLiked(postId, userId)) {
            return false;
        }

        // 取消点赞
        boolean result = postLikeDao.unlikePost(postId, userId);

        if (result) {
            // 更新帖子点赞数（使用原子操作）
            postDao.decrementLikeCount(postId);
        }

        return result;
    }

    @Override
    public boolean isLiked(Integer postId, Integer userId) {
        return postLikeDao.isLiked(postId, userId);
    }

    @Override
    public List<PostLike> getPostLikes(Integer postId) {
        return postLikeDao.selectByPostId(postId);
    }

    @Override
    public boolean createComment(Integer postId, Integer userId, String content, Integer parentId) {
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);

        boolean result = postCommentDao.createComment(comment);

        if (result) {
            // 更新帖子评论数（使用原子操作）
            postDao.incrementCommentCount(postId);
        }

        return result;
    }

    @Override
    public boolean updateComment(Integer commentId, Integer userId, String content) {
        PostComment comment = postCommentDao.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            return false;
        }

        comment.setContent(content);
        return postCommentDao.updateComment(comment);
    }

    @Override
    public boolean deleteComment(Integer commentId, Integer userId) {
        PostComment comment = postCommentDao.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            return false;
        }

        boolean result = postCommentDao.deleteComment(commentId);

        if (result) {
            // 更新帖子评论数（使用原子操作）
            postDao.decrementCommentCount(comment.getPostId());
        }

        return result;
    }

    @Override
    public List<PostComment> getPostComments(Integer postId) {
        List<PostComment> comments = postCommentDao.selectByPostId(postId);
        comments.forEach(comment -> comment.setIsLiked(false));
        return comments;
    }

    @Override
    public List<PostComment> getRootComments(Integer postId) {
        List<PostComment> comments = postCommentDao.selectRootComments(postId);
        comments.forEach(comment -> comment.setIsLiked(false));
        return comments;
    }

    @Override
    public List<PostComment> getReplies(Integer parentId) {
        List<PostComment> comments = postCommentDao.selectByParentId(parentId);
        comments.forEach(comment -> comment.setIsLiked(false));
        return comments;
    }

    @Override
    public List<PostComment> getUserComments(Integer userId) {
        return postCommentDao.selectByUserId(userId);
    }

    @Override
    public List<PostComment> getUserCommentsByPage(Integer userId, int page, int size) {
        return postCommentDao.selectByUserId(userId);
    }

    @Override
    public boolean adminDeletePost(Integer postId) {
        Post post = postDao.selectById(postId);
        if (post == null) {
            return false;
        }

        return postDao.deletePost(postId);
    }

    @Override
    public boolean adminDeleteComment(Integer commentId) {
        PostComment comment = postCommentDao.selectById(commentId);
        if (comment == null) {
            return false;
        }

        boolean result = postCommentDao.deleteComment(commentId);

        if (result) {
            // 更新帖子评论数（使用原子操作）
            postDao.decrementCommentCount(comment.getPostId());
        }

        return result;
    }

    @Override
    public List<PostComment> getCommentsByPage(int page, int size) {
        int offset = (page - 1) * size;
        return postCommentDao.selectAllByPage(offset, size);
    }

    @Override
    public boolean likeComment(Integer commentId, Integer userId) {
        // 检查是否已经点赞
        if (commentLikeDao.isLiked(commentId, userId)) {
            return false;
        }

        // 添加点赞
        boolean result = commentLikeDao.likeComment(commentId, userId);

        if (result) {
            // 更新评论点赞数（使用原子操作）
            postCommentDao.incrementLikeCount(commentId);
        }

        return result;
    }

    @Override
    public boolean unlikeComment(Integer commentId, Integer userId) {
        // 检查是否已点赞
        if (!commentLikeDao.isLiked(commentId, userId)) {
            return false;
        }

        // 取消点赞
        boolean result = commentLikeDao.unlikeComment(commentId, userId);

        if (result) {
            // 更新评论点赞数（使用原子操作）
            postCommentDao.decrementLikeCount(commentId);
        }

        return result;
    }

    @Override
    public boolean isCommentLiked(Integer commentId, Integer userId) {
        return commentLikeDao.isLiked(commentId, userId);
    }

    @Autowired(required = false)
    private HBaseService hbaseService;

    @Autowired(required = false)
    private RedisService redisService;

    @Override
    public List<Post> getRecommendedPosts(Integer userId, Integer currentUserId) {
        // 检查大数据功能是否启用
        if (!bigDataConfig.isEnabled()) {
            System.out.println("大数据功能未启用，返回空推荐列表");
            return new ArrayList<>();
        }

        // 检查必要的服务是否可用
        if (hbaseService == null || redisService == null) {
            System.out.println("大数据服务未初始化，返回空推荐列表");
            return new ArrayList<>();
        }

        List<Post> recommendedPosts = new ArrayList<>();

        try {
            System.out.println("开始获取用户 " + userId + " 的推荐帖子");

            List<Integer> recommendedPostIds = new ArrayList<>();

            // 如果大数据功能未启用，直接返回空列表
            if (!bigDataConfig.isEnabled()) {
                System.out.println("大数据功能未启用，返回空推荐列表");
                return recommendedPosts;
            }
             //从Redis获取推荐的帖子ID列表
            List<Integer> recommendedPostIds_redis = redisService.getRecommendedPosts(userId);
            System.out.println("===============================================从Redis获取到的推荐帖子ID列表: " + recommendedPostIds_redis);
            if (recommendedPostIds_redis == null || recommendedPostIds_redis.isEmpty()) {
                 //从HBase获取推荐的帖子ID列表（已按推荐分数降序排序）
                recommendedPostIds = hbaseService.getRecommendedPosts(userId);
                System.out.println("从HBase获取到的推荐帖子ID列表: " + recommendedPostIds);
            }else {
                recommendedPostIds = recommendedPostIds_redis;
            }

            //List<Integer> recommendedPostIds = hbaseService.getRecommendedPosts(userId);

            // 根据帖子ID获取帖子详情
            for (Integer postId : recommendedPostIds) {
                Post post = postDao.selectById(postId);
                if (post != null) {
                    // 直接添加帖子到推荐列表，不检查是否点赞
                    // 如果需要显示点赞状态，可以在前端处理
                    recommendedPosts.add(post);
                }
            }

            // 帖子列表已按照HBase返回的推荐分数顺序排列
            // 如果需要额外的排序逻辑，可以在这里添加

            System.out.println("最终获取到的推荐帖子数量: " + recommendedPosts.size());
        } catch (Exception e) {
            System.err.println("获取推荐帖子失败: " + e.getMessage());
            e.printStackTrace();
        }

        return recommendedPosts;
    }
}
