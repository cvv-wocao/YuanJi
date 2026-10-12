package com.example.item.demo35.service;

import com.example.item.demo35.entity.Follow;
import com.example.item.demo35.entity.User;

import java.util.List;
import java.util.Map;

public interface FollowService {

    // 关注用户
    boolean follow(Integer followerId, Integer followingId);

    // 取消关注
    boolean unfollow(Integer followerId, Integer followingId);

    // 检查是否关注
    boolean isFollowing(Integer followerId, Integer followingId);

    // 获取粉丝列表（关注我的人）
    List<Follow> getFollowers(Integer followingId);

    // 获取关注列表（我关注的人）
    List<Follow> getFollowing(Integer followerId);

    // 获取粉丝数
    int getFollowerCount(Integer followingId);

    // 获取关注数
    int getFollowingCount(Integer followerId);

    // 获取当前用户的关注统计信息
    Map<String, Object> getFollowStats(Integer userId);

    // 获取互相关注的用户列表
    List<User> getMutualFollows(Integer userId);

    // 检查是否与指定用户互相关注
    boolean isMutualFollow(Integer userId1, Integer userId2);
}