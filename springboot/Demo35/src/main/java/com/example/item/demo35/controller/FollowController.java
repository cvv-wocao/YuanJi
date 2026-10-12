package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Follow;
import com.example.item.demo35.entity.User;
import com.example.item.demo35.service.FollowService;
import com.example.item.demo35.service.UserService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    /**
     * 关注用户
     */
    @PostMapping("/{followingId}")
    public Result followUser(
            @PathVariable("followingId") Integer followingId,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Result result = new Result();
        System.out.println("关注请求: followingId=" + followingId);

        try {
            // 验证 token
            if (token == null || token.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("未提供token，请先登录");
                return result;
            }

            Integer followerId = getUserIdFromToken(token);
            if (followerId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录或token无效");
                return result;
            }

            System.out.println("关注请求: followerId=" + followerId + ", followingId=" + followingId);

            boolean success = followService.follow(followerId, followingId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("关注成功");
                System.out.println("关注成功");
            } else {
                result.setSuccess(false);
                result.setMessage("关注失败（可能已关注、不能关注自己或被关注用户不存在）");
                System.out.println("关注失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("关注失败: " + e.getMessage());
            e.printStackTrace();
            System.out.println("关注异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/{followingId}")
    public Result unfollowUser(
            @PathVariable("followingId") Integer followingId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer followerId = getUserIdFromToken(token);
            if (followerId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean success = followService.unfollow(followerId, followingId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("取消关注成功");
            } else {
                result.setSuccess(false);
                result.setMessage("取消关注失败（可能未关注）");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消关注失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 检查是否关注
     */
    @GetMapping("/check/{followingId}")
    public Result checkFollowing(
            @PathVariable("followingId") Integer followingId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer followerId = getUserIdFromToken(token);
            if (followerId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean isFollowing = followService.isFollowing(followerId, followingId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(isFollowing);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取我的粉丝列表（关注我的人）
     */
    @GetMapping("/followers")
    public Result getMyFollowers(@RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            List<Follow> followers = followService.getFollowers(userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(followers);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取我的关注列表（我关注的人）
     */
    @GetMapping("/following")
    public Result getMyFollowing(@RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            List<Follow> following = followService.getFollowing(userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(following);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取关注统计信息
     */
    @GetMapping("/stats")
    public Result getFollowStats(@RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            Map<String, Object> stats = followService.getFollowStats(userId);

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
     * 获取互相关注的用户列表
     */
    @GetMapping("/mutual")
    public Result getMutualFollows(@RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            List<User> mutualFollows = followService.getMutualFollows(userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(mutualFollows);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取指定用户的粉丝列表
     */
    @GetMapping("/{userId}/followers")
    public Result getUserFollowers(@PathVariable("userId") Integer userId) {

        Result result = new Result();

        try {
            List<Follow> followers = followService.getFollowers(userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(followers);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取指定用户的关注列表
     */
    @GetMapping("/{userId}/following")
    public Result getUserFollowing(@PathVariable("userId") Integer userId) {

        Result result = new Result();

        try {
            List<Follow> following = followService.getFollowing(userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(following);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取指定用户的关注统计
     */
    @GetMapping("/{userId}/stats")
    public Result getUserFollowStats(@PathVariable("userId") Integer userId) {

        Result result = new Result();

        try {
            Map<String, Object> stats = followService.getFollowStats(userId);

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
     * 测试关注接口（不需要token，方便调试）
     */
    @PostMapping("/test/{followerId}/{followingId}")
    public Result testFollow(
            @PathVariable("followerId") Integer followerId,
            @PathVariable("followingId") Integer followingId) {

        Result result = new Result();

        try {
            System.out.println("=== 测试关注接口 ===");
            System.out.println("followerId: " + followerId);
            System.out.println("followingId: " + followingId);

            boolean success = followService.follow(followerId, followingId);

            result.setSuccess(success);
            result.setMessage(success ? "关注成功" : "关注失败");
            result.setData(success);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("关注异常: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 检查是否与指定用户互相关注
     */
    @GetMapping("/mutual/check/{userId}")
    public Result checkMutualFollow(
            @PathVariable("userId") Integer userId,
            @RequestHeader("Authorization") String token) {

        Result result = new Result();

        try {
            Integer currentUserId = getUserIdFromToken(token);
            if (currentUserId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            boolean isMutualFollow = followService.isMutualFollow(currentUserId, userId);

            result.setSuccess(true);
            result.setMessage("获取成功");
            result.setData(isMutualFollow);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

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
}