package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentLikeDao {
    boolean likeComment(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    boolean unlikeComment(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    boolean isLiked(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    List<CommentLike> selectByCommentId(@Param("commentId") Integer commentId);

    List<CommentLike> selectByUserId(@Param("userId") Integer userId);

    int countByCommentId(@Param("commentId") Integer commentId);

    boolean incrementLikeCount(@Param("id") Integer id);

    boolean decrementLikeCount(@Param("id") Integer id);
}
