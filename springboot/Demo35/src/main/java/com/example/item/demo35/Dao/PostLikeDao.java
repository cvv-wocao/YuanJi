package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostLikeDao {
    boolean likePost(@Param("postId") Integer postId, @Param("userId") Integer userId);

    boolean unlikePost(@Param("postId") Integer postId, @Param("userId") Integer userId);

    PostLike selectByPostAndUser(@Param("postId") Integer postId, @Param("userId") Integer userId);

    List<PostLike> selectByPostId(@Param("postId") Integer postId);

    List<PostLike> selectByUserId(@Param("userId") Integer userId);

    boolean isLiked(@Param("postId") Integer postId, @Param("userId") Integer userId);

    int countByPostId(@Param("postId") Integer postId);
}
