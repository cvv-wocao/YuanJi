package com.example.item.demo35.service.Impl;

import com.example.item.demo35.service.RedisService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.PostConstruct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@ConditionalOnProperty(name = "bigdata.enabled", havingValue = "true")
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public List<Integer> getRecommendedPosts(Integer userId) {
        String cacheKey = "recommend:user:" + userId;
        List<String> postList = redisTemplate.opsForList().range(cacheKey, 0, -1);
        System.out.println(postList);
        if (postList == null || postList.isEmpty()) {
            return new ArrayList<>();
        }
        return postList.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }


    //===================================================================================================
    @Autowired
    private Connection hbaseConnection;   // HBase 连接（从 HBaseConfig 注入）
    private static final String HBASE_TABLE = "post_recommend";
    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("r");
    private static final byte[] COLUMN_SCORE = Bytes.toBytes("score");

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    /**
     * 启动时自动执行同步（可根据需要移除 CommandLineRunner 接口，改为手动触发）
     */
    @PostConstruct
    public void init() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        log.info("Redis 连接信息: {}", connection.getConfig("port"));
        log.info("开始同步 HBase 推荐数据到 Redis...");
        syncHBaseToRedis();
        log.info("同步完成");
    }

    /**
     * 核心同步方法：扫描 HBase 表，将数据按用户分组后写入 Redis ZSet
     */
    public void syncHBaseToRedis() {
        Table table = null;
        Scan scan = new Scan();
        scan.addColumn(COLUMN_FAMILY, COLUMN_SCORE);

        try {
            table = hbaseConnection.getTable(TableName.valueOf(HBASE_TABLE));
            ResultScanner scanner = table.getScanner(scan);

            // 使用 Multimap 临时存储每个用户的（帖子ID, 分数）对
            Multimap<String, PostScore> userPosts = ArrayListMultimap.create();

            for (Result result : scanner) {
                String rowKey = Bytes.toString(result.getRow());
                // RowKey 格式：用户ID:物品ID
                String[] parts = rowKey.split(":");
                if (parts.length != 2) {
                    log.warn("无效的 RowKey: {}", rowKey);
                    continue;
                }
                String userId = parts[0];
                String postId = parts[1];

                // 获取分数列的值
                Cell cell = result.getColumnLatestCell(COLUMN_FAMILY, COLUMN_SCORE);
                if (cell == null) {
                    continue;
                }
                byte[] valueBytes = CellUtil.cloneValue(cell);
                double score;

                // 根据实际存储格式解析分数
                // 如果存储的是 float（4 字节）
                if (valueBytes.length == 4) {
                    score = Bytes.toFloat(valueBytes);
                }
                // 如果存储的是 double（8 字节）
                else if (valueBytes.length == 8) {
                    score = Bytes.toDouble(valueBytes);
                }
                // 如果存储的是字符串
                else {
                    String scoreStr = Bytes.toString(valueBytes);
                    score = Double.parseDouble(scoreStr);
                }

                userPosts.put(userId, new PostScore(postId, score));
            }

            // 将每个用户的推荐列表写入 Redis
            for (String userId : userPosts.keySet()) {
                Collection<PostScore> posts = userPosts.get(userId);
                String redisKey = "recommend:user:" + userId;

                // 清空旧的推荐列表（如果有）
                redisTemplate.delete(redisKey);

                // 按分数降序排序后存入 List
                List<String> sortedPostIds = posts.stream()
                        .sorted((a, b) -> Double.compare(b.score, a.score))
                        .map(ps -> ps.postId)
                        .collect(Collectors.toList());

                if (!sortedPostIds.isEmpty()) {
                    redisTemplate.opsForList().rightPushAll(redisKey, sortedPostIds);
                    redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
                    log.info("用户 {} 的 {} 个推荐项已同步到 Redis", userId, sortedPostIds.size());
                }
            }

            log.info("总计同步 {} 个用户的数据", userPosts.keySet().size());

        } catch (IOException e) {
            log.error("同步 HBase 到 Redis 失败", e);
        } finally {
            if (table != null) {
                try {
                    table.close();
                } catch (IOException e) {
                    log.error("关闭 HBase 表失败", e);
                }
            }
        }
    }

    /**
     * 内部类，存储帖子ID和分数
     */
    private static class PostScore {
        String postId;
        double score;

        PostScore(String postId, double score) {
            this.postId = postId;
            this.score = score;
        }
    }
    //=============================================================================================
}
