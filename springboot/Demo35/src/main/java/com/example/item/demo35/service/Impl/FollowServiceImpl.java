package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.FollowDao;
import com.example.item.demo35.Dao.UserDao;
import com.example.item.demo35.entity.Follow;
import com.example.item.demo35.entity.User;
import com.example.item.demo35.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowDao followDao;

    @Autowired
    private UserDao userDao;

    @Override
    public boolean follow(Integer followerId, Integer followingId) {
        System.out.println("=== 开始关注 ===");
        System.out.println("followerId: " + followerId);
        System.out.println("followingId: " + followingId);

        // 不能关注自己
        if (followerId.equals(followingId)) {
            System.out.println("失败: 不能关注自己");
            return false;
        }

        // 检查被关注用户是否存在
        User followingUser = userDao.selectById(followingId);
        System.out.println("被关注用户: " + (followingUser != null ? followingUser.getName() : "不存在"));
        
        if (followingUser == null) {
            System.out.println("失败: 被关注用户不存在");
            return false;
        }

        // 检查是否已经关注
        boolean alreadyFollowing = followDao.isFollowing(followerId, followingId);
        System.out.println("是否已经关注: " + alreadyFollowing);
        
        if (alreadyFollowing) {
            System.out.println("失败: 已经关注过该用户");
            return false;
        }

        System.out.println("准备插入数据库...");
        boolean result = followDao.follow(followerId, followingId);
        System.out.println("插入结果: " + result);
        System.out.println("=== 关注流程结束 ===");
        
        return result;
    }

    @Override
    public boolean unfollow(Integer followerId, Integer followingId) {
        // 检查是否已关注
        if (!followDao.isFollowing(followerId, followingId)) {
            return false;
        }

        return followDao.unfollow(followerId, followingId);
    }

    @Override
    public boolean isFollowing(Integer followerId, Integer followingId) {
        return followDao.isFollowing(followerId, followingId);
    }

    @Override
    public List<Follow> getFollowers(Integer followingId) {
        return followDao.selectFollowers(followingId);
    }

    @Override
    public List<Follow> getFollowing(Integer followerId) {
        return followDao.selectFollowing(followerId);
    }

    @Override
    public int getFollowerCount(Integer followingId) {
        return followDao.countFollowers(followingId);
    }

    @Override
    public int getFollowingCount(Integer followerId) {
        return followDao.countFollowing(followerId);
    }

    @Override
    public Map<String, Object> getFollowStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("followerCount", getFollowerCount(userId));
        stats.put("followingCount", getFollowingCount(userId));
        return stats;
    }

    @Override
    public List<User> getMutualFollows(Integer userId) {
        // 获取我关注的人
        List<Follow> followingList = getFollowing(userId);
        List<User> mutualFollows = new ArrayList<>();

        for (Follow follow : followingList) {
            // 检查对方是否也关注我
            if (isFollowing(follow.getFollowingId(), userId)) {
                User user = userDao.selectById(follow.getFollowingId());
                if (user != null) {
                    mutualFollows.add(user);
                }
            }
        }

        return mutualFollows;
    }

    @Override
    public boolean isMutualFollow(Integer userId1, Integer userId2) {
        // 检查userId1是否关注了userId2，且userId2是否关注了userId1
        boolean user1FollowsUser2 = isFollowing(userId1, userId2);
        boolean user2FollowsUser1 = isFollowing(userId2, userId1);
        return user1FollowsUser2 && user2FollowsUser1;
    }
}