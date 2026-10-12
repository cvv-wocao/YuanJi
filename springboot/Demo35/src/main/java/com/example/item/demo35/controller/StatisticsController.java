package com.example.item.demo35.controller;

import com.example.item.demo35.Dao.*;
import com.example.item.demo35.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private static final Logger log = LoggerFactory.getLogger(StatisticsController.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private PostDao postDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private PostCommentDao postCommentDao;

    @Autowired
    private OrderDao orderDao;

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> data = new HashMap<>();

        Object userCountObj = userDao.countUsers();
        Object postCountObj = postDao.countAll();
        Object productCountObj = productDao.countProducts();
        Object commentCountObj = postCommentDao.countAll();
        Object orderCountObj = orderDao.countTotalOrders(null, null);

        Integer userCount = userCountObj instanceof Number ? ((Number) userCountObj).intValue() : 0;
        Integer postCount = postCountObj instanceof Number ? ((Number) postCountObj).intValue() : 0;
        Integer productCount = productCountObj instanceof Number ? ((Number) productCountObj).intValue() : 0;
        Integer commentCount = commentCountObj instanceof Number ? ((Number) commentCountObj).intValue() : 0;
        Integer orderCount = orderCountObj instanceof Number ? ((Number) orderCountObj).intValue() : 0;

        data.put("userCount", userCount);
        data.put("postCount", postCount);
        data.put("productCount", productCount);
        data.put("commentCount", commentCount);
        data.put("orderCount", orderCount);

        return data;
    }

    @GetMapping("/orders")
    public Map<String, Object> getOrderStats() {
        Map<String, Object> data = new HashMap<>();

        Long totalOrdersLong = orderDao.countTotalOrders(null, null);
        BigDecimal totalAmount = orderDao.sumTotalAmount(null, null);

        Integer totalOrders = totalOrdersLong != null ? totalOrdersLong.intValue() : 0;

        data.put("totalOrders", totalOrders);
        data.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);

        return data;
    }

    @GetMapping("/orderStatus")
    public Map<String, Object> getOrderStatusStats() {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> statusList = orderDao.countOrdersGroupByStatusWithDate(null, null);

        log.info("订单状态统计数据, 返回条数: {}", statusList.size());
        for (Map<String, Object> item : statusList) {
            log.info("原始数据: {}", item);
        }

        int pendingPayment = 0, pendingShipment = 0, pendingReceipt = 0, completed = 0, cancelled = 0, refunding = 0, refunded = 0;

        for (Map<String, Object> item : statusList) {
            Object statusObj = item.get("status");
            if (statusObj == null) statusObj = item.get("STATUS");
            Object countObj = item.get("count");
            if (countObj == null) countObj = item.get("COUNT");

            Integer count = countObj instanceof Number ? ((Number) countObj).intValue() : 0;

            if (statusObj instanceof Boolean) {
                Boolean statusBool = (Boolean) statusObj;
                int statusInt = statusBool ? 1 : 0;
                if (statusInt == 0) pendingPayment = count;
                else pendingShipment = count;
            } else if (statusObj instanceof Number) {
                int status = ((Number) statusObj).intValue();
                switch (status) {
                    case 0: pendingPayment = count; break;
                    case 1: pendingShipment = count; break;
                    case 2: pendingReceipt = count; break;
                    case 3: completed = count; break;
                    case 4: cancelled = count; break;
                    case 5: refunding = count; break;
                    case 6: refunded = count; break;
                }
            }
        }

        List<Map<String, Object>> statusData = new ArrayList<>();
        statusData.add(createStatusItem("待支付", pendingPayment, "#ff6b6b"));
        statusData.add(createStatusItem("待发货", pendingShipment, "#ffd93d"));
        statusData.add(createStatusItem("待收货", pendingReceipt, "#6bcbff"));
        statusData.add(createStatusItem("已完成", completed, "#00ff88"));
        statusData.add(createStatusItem("已取消", cancelled, "#a78bfa"));
        statusData.add(createStatusItem("退款中", refunding, "#ff9f43"));
        statusData.add(createStatusItem("已退款", refunded, "#54a0ff"));

        data.put("statusData", statusData);
        data.put("total", pendingPayment + pendingShipment + pendingReceipt + completed + cancelled + refunding + refunded);

        return data;
    }

    private Map<String, Object> createStatusItem(String name, int value, String color) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("color", color);
        return item;
    }

    @GetMapping("/hotProducts")
    public Map<String, Object> getHotProducts() {
        Map<String, Object> data = new HashMap<>();
        List<Product> products = productDao.selectHotProducts(5);

        List<Map<String, Object>> productList = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", p.getName());
            item.put("sales", p.getSales() != null ? p.getSales() : 0);
            item.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            item.put("stock", p.getStock() != null ? p.getStock() : 0);
            productList.add(item);
        }

        data.put("products", productList);
        return data;
    }

    @GetMapping("/newProducts")
    public Map<String, Object> getNewProducts() {
        Map<String, Object> data = new HashMap<>();
        List<Product> products = productDao.selectNewProducts(5);

        List<Map<String, Object>> productList = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", p.getName());
            item.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            item.put("stock", p.getStock() != null ? p.getStock() : 0);
            item.put("sales", p.getSales() != null ? p.getSales() : 0);
            productList.add(item);
        }

        data.put("products", productList);
        return data;
    }

    @GetMapping("/productStock")
    public Map<String, Object> getProductStockStats() {
        Map<String, Object> data = new HashMap<>();
        List<Product> products = productDao.selectAllProducts();

        int lowStock = 0, normalStock = 0, highStock = 0;

        for (Product p : products) {
            Integer stock = p.getStock();
            if (stock == null) stock = 0;

            if (stock < 10) {
                lowStock++;
            } else if (stock < 50) {
                normalStock++;
            } else {
                highStock++;
            }
        }

        List<Map<String, Object>> stockData = new ArrayList<>();
        stockData.add(createStatusItem("低库存(<10)", lowStock, "#ff6b6b"));
        stockData.add(createStatusItem("正常库存(10-50)", normalStock, "#ffd93d"));
        stockData.add(createStatusItem("高库存(>50)", highStock, "#00ff88"));

        data.put("stockData", stockData);
        data.put("total", lowStock + normalStock + highStock);

        return data;
    }
}