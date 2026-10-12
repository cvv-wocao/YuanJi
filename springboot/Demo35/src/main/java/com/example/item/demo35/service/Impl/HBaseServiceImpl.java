
package com.example.item.demo35.service.Impl;

import com.example.item.demo35.service.HBaseService;
import org.apache.hadoop.hbase.TableName;
import com.example.item.demo35.config.HBaseConfig;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HBase服务实现类
 */
@Service
@ConditionalOnProperty(name = "bigdata.enabled", havingValue = "true")
public class HBaseServiceImpl implements HBaseService {

    private static final Logger logger = LoggerFactory.getLogger(HBaseServiceImpl.class);

    private static final String TABLE_NAME = "post_recommend";
    private static final String COLUMN_FAMILY = "r";
    private static final String COLUMN_QUALIFIER = "score";

    @Autowired(required = false)
    private Connection hbaseConnection;

    @Override
    public List<Integer> getRecommendedPosts(Integer userId) {
        List<Integer> recommendedPosts = new ArrayList<>();

        try (Table table = hbaseConnection.getTable(TableName.valueOf(TABLE_NAME))) {
            // 构建扫描器，只获取当前用户的推荐数据
            Scan scan = new Scan();
            // 设置行键前缀为userId + ":"
            String rowPrefix = userId + ":";
            scan.setRowPrefixFilter(Bytes.toBytes(rowPrefix));
            scan.addFamily(Bytes.toBytes(COLUMN_FAMILY));

            // 执行扫描
            ResultScanner scanner = table.getScanner(scan);

            // 用于存储推荐结果及其分数
            List<PostScore> postScores = new ArrayList<>();

            for (Result result : scanner) {
                try {
                    // 解析行键，获取帖子ID
                    String rowKey = Bytes.toString(result.getRow());
                    String[] parts = rowKey.split(":");
                    if (parts.length >= 2) {
                        Integer postId = Integer.parseInt(parts[1]);

                        // 获取推荐分数
                        byte[] scoreBytes = result.getValue(Bytes.toBytes(COLUMN_FAMILY), Bytes.toBytes(COLUMN_QUALIFIER));
                        if (scoreBytes != null) {
                            float score = Bytes.toFloat(scoreBytes);
                            postScores.add(new PostScore(postId, score));
                        }
                    }
                } catch (Exception e) {
                    logger.error("解析HBase数据失败: {}", e.getMessage());
                }
            }

            // 按分数降序排序
            Collections.sort(postScores, Comparator.comparing(PostScore::getScore).reversed());

            // 提取帖子ID列表
            for (PostScore postScore : postScores) {
                recommendedPosts.add(postScore.getPostId());
            }

            logger.info("用户 {} 的推荐帖子列表: {}", userId, recommendedPosts);

        } catch (IOException e) {
            logger.error("从HBase获取推荐数据失败: {}", e.getMessage());
        }

        return recommendedPosts;
    }

    /**
     * 内部类，用于存储帖子ID和推荐分数
     */
    private static class PostScore {
        private Integer postId;
        private float score;

        public PostScore(Integer postId, float score) {
            this.postId = postId;
            this.score = score;
        }

        public Integer getPostId() {
            return postId;
        }

        public float getScore() {
            return score;
        }
    }
}
