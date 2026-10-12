package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostDao {
    boolean createPost(Post post);

    boolean updatePost(Post post);

    boolean deletePost(@Param("id") Integer id);

    Post selectById(@Param("id") Integer id);

    List<Post> selectAll(@Param("userId") Integer userId);

    List<Post> selectByUserId(@Param("userId") Integer userId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    List<Post> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    int countByUserId(@Param("userId") Integer userId);

    Integer countAll();

    int sumLikeCountByUserId(@Param("userId") Integer userId);

    Integer countPosts();

    boolean incrementLikeCount(@Param("id") Integer id);

    boolean decrementLikeCount(@Param("id") Integer id);

    boolean incrementCommentCount(@Param("id") Integer id);

    boolean decrementCommentCount(@Param("id") Integer id);
}
