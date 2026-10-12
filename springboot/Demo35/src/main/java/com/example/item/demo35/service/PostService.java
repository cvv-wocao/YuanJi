package com.example.item.demo35.service;

import com.example.item.demo35.entity.Post;
import com.example.item.demo35.entity.PostComment;
import com.example.item.demo35.entity.PostLike;

import java.util.List;
import java.util.Map;

public interface PostService {

    // 帖子相关
    boolean createPost(Integer userId, String content, List<String> images, List<String> videos, String cover);

    boolean updatePost(Integer postId, Integer userId, String content, List<String> images, List<String> videos, String cover);

    boolean deletePost(Integer postId, Integer userId);

    Post getPostById(Integer postId, Integer currentUserId);

    List<Post> getAllPosts(Integer currentUserId);

    List<Post> getUserPosts(Integer userId, Integer currentUserId);

    List<Post> getPostsByPage(int page, int size, Integer currentUserId);

    Map<String, Object> getPostStatistics();

    Map<String, Object> getUserPostsWithStats(Integer userId, Integer currentUserId);

    // 点赞相关
    boolean likePost(Integer postId, Integer userId);

    boolean unlikePost(Integer postId, Integer userId);

    boolean isLiked(Integer postId, Integer userId);

    List<PostLike> getPostLikes(Integer postId);

    // 评论相关
    boolean createComment(Integer postId, Integer userId, String content, Integer parentId);

    boolean updateComment(Integer commentId, Integer userId, String content);

    boolean deleteComment(Integer commentId, Integer userId);

    List<PostComment> getPostComments(Integer postId);

    List<PostComment> getRootComments(Integer postId);

    List<PostComment> getReplies(Integer parentId);

    List<PostComment> getUserComments(Integer userId);

    List<PostComment> getUserCommentsByPage(Integer userId, int page, int size);

    // 管理员专用方法
    boolean adminDeletePost(Integer postId);

    boolean adminDeleteComment(Integer commentId);

    List<PostComment> getCommentsByPage(int page, int size);

    // 评论点赞相关
    boolean likeComment(Integer commentId, Integer userId);

    boolean unlikeComment(Integer commentId, Integer userId);

    boolean isCommentLiked(Integer commentId, Integer userId);

    // 推荐系统相关
    List<Post> getRecommendedPosts(Integer userId, Integer currentUserId);

}
