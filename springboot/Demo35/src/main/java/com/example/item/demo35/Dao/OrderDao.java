package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderDao {

    List<Order> selectAllOrders(@Param("params") Map<String, Object> params,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);


    Long countAllOrders(@Param("params") Map<String, Object> params);

    List<Order> selectOrdersByUserId(@Param("params") Map<String, Object> params,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);

    Long countOrdersByUserId(@Param("userId") Integer userId,
                             @Param("status") Integer status);

    long countTotalOrders(@Param("startTime") Date startTime,
                          @Param("endTime") Date endTime);

    BigDecimal sumTotalAmount(@Param("startTime") Date startTime,
                              @Param("endTime") Date endTime);

    List<Map<String, Object>> countOrdersGroupByStatusWithDate(@Param("startTime") Date startTime, @Param("endTime") Date endTime);



    List<Order> selectByUserId(@Param("userId") Integer userId, @Param("status") Integer status);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    Order selectById(@Param("id") Integer id);

    boolean insert(Order order);

    boolean update(Order order);

    boolean updateStatus(@Param("orderNo") String orderNo, @Param("status") Integer status);

    boolean delete(@Param("orderNo") String orderNo);
}
