package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LongTermMemoryDao {
    
    int insert(LongTermMemory memory);
    
    int update(LongTermMemory memory);
    
    int deleteById(@Param("id") Long id);
    
    LongTermMemory selectById(@Param("id") Long id);
    
    List<LongTermMemory> selectByUserId(@Param("userId") Long userId);
    
    List<LongTermMemory> selectByUserIdAndType(@Param("userId") Long userId, @Param("memoryType") String memoryType);
    
    List<LongTermMemory> searchByKeywords(@Param("userId") Long userId, @Param("keywords") String keywords);
    
    List<LongTermMemory> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    List<LongTermMemory> selectByUserIdAndThread(@Param("userId") Long userId, @Param("threadSlug") String threadSlug);
    
    int deleteByUserId(@Param("userId") Long userId);
}