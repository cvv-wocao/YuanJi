package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostCommentDao {
    boolean createComment(PostComment comment);

    boolean updateComment(PostComment comment);

    boolean deleteComment(@Param("id") Integer id);

    PostComment selectById(@Param("id") Integer id);

    List<PostComment> selectByPostId(@Param("postId") Integer postId);

    Long countComments();

    Integer countAll();

    List<PostComment> selectByUserId(@Param("userId") Integer userId);

    List<PostComment> selectRootComments(@Param("postId") Integer postId);

    List<PostComment> selectByParentId(@Param("parentId") Integer parentId);

    List<PostComment> selectAllByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    boolean incrementLikeCount(@Param("id") Integer id);

    boolean decrementLikeCount(@Param("id") Integer id);
}