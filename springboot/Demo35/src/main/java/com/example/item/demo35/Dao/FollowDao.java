package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowDao {

    boolean follow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    boolean unfollow(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    Follow selectByFollowerAndFollowing(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    List<Follow> selectFollowers(@Param("followingId") Integer followingId); // 关注我的人（我的粉丝）

    List<Follow> selectFollowing(@Param("followerId") Integer followerId); // 我关注的人

    boolean isFollowing(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    int countFollowers(@Param("followingId") Integer followingId); // 粉丝数

    int countFollowing(@Param("followerId") Integer followerId); // 关注数
}