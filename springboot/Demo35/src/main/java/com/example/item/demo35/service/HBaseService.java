
package com.example.item.demo35.service;

import java.util.List;

/**
 * HBase服务接口
 */
public interface HBaseService {

    /**
     * 获取用户推荐的帖子ID列表
     * @param userId 用户ID
     * @return 推荐的帖子ID列表
     */
    List<Integer> getRecommendedPosts(Integer userId);
}
