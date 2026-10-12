package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Order;
import com.example.item.demo35.entity.OrderItem;
import com.example.item.demo35.service.OrderService;
import com.example.item.demo35.service.UserService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtils jwtUtils;

    private Integer getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (!jwtUtils.validateToken(actualToken)) {
            return null;
        }
        String username = jwtUtils.getUsernameFromToken(actualToken);
        System.out.println(userService.selectIdByName(username));
        return userService.selectIdByName(username);
    }

    /**
     * 根据订单ID获取订单商品列表
     */
    @GetMapping("/items/{orderId}")
    public Result getOrderItemsByOrderId(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer orderId) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            // 验证订单是否属于当前用户
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                result.setSuccess(false);
                result.setMessage("订单不存在");
                return result;
            }

            if (!order.getUserId().equals(userId)) {
                result.setSuccess(false);
                result.setMessage("无权查看此订单");
                return result;
            }

            // 获取订单商品列表
            List<OrderItem> orderItems = orderService.getOrderItemsByOrderId(orderId);

            result.setSuccess(true);
            result.setMessage("获取订单商品信息成功");
            result.setData(orderItems);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取订单商品信息失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

//============================支付订单========================
    @PostMapping("/pay")
    public Result payOrder(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> payRequest) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            String orderNo = (String) payRequest.get("orderNo");
            String paymentMethod = (String) payRequest.get("paymentMethod");

            if (orderNo == null || orderNo.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("订单号不能为空");
                return result;
            }

            if (paymentMethod == null || paymentMethod.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("请选择支付方式");
                return result;
            }

            boolean success = orderService.payOrder(userId, orderNo, paymentMethod);

            if (success) {
                result.setSuccess(true);
                result.setMessage("付款成功");
                // 返回支付成功的订单信息，便于前端更新列表
                Order order = orderService.getOrderDetail(userId, orderNo);
                result.setData(order);
            } else {
                result.setSuccess(false);
                result.setMessage("付款失败，请稍后重试");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("付款失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("支付订单的相关内容：" + result);
        return result;
    }

    @PostMapping("/create")
    public Result createOrder(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> orderRequest) {
        System.out.println("创建的订单的相关内容!!!!!!!!!!!!!!!!!!!："+orderRequest);
        Result result = new Result();
        System.out.println("==========================="+orderRequest);
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            Order order = (Order) orderService.createOrder(userId, orderRequest);
            if (order != null) {
                result.setSuccess(true);
                result.setMessage("创建订单成功");
                result.setData(order);
            } else {
                result.setSuccess(false);
                result.setMessage("创建订单失败！！！！！");
            }
        System.out.println("创建的订单的相关内容："+result);
        return result;
    }

    @GetMapping("/list")
    public Result getOrderList(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            List<Order> orders = orderService.getOrderList(userId, status, page, pageSize);
            result.setSuccess(true);
            result.setMessage("获取订单列表成功");
            result.setData(orders);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取订单列表失败: " + e.getMessage());
        }
        System.out.println("获取的订单列表的相关内容："+result);
        return result;
    }

    @GetMapping("/detail/{orderNo}")
    public Result getOrderDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable String orderNo) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            Order order = orderService.getOrderDetail(userId, orderNo);
            if (order != null) {
                result.setSuccess(true);
                result.setMessage("获取订单详情成功");
                result.setData(order);
            } else {
                result.setSuccess(false);
                result.setMessage("订单不存在");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取订单详情失败: " + e.getMessage());
        }
        System.out.println("获取的订单详情的相关内容："+result);
        return result;
    }

    @PostMapping("/cancel")
    public Result cancelOrder(
            @RequestHeader("Authorization") String token,
            @RequestParam String orderNo,
            @RequestParam(required = false) String reason) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = orderService.cancelOrder(userId, orderNo, reason);
            if (success) {
                result.setSuccess(true);
                result.setMessage("取消订单成功");
            } else {
                result.setSuccess(false);
                result.setMessage("取消订单失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消订单失败: " + e.getMessage());
        }
        System.out.println("取消的订单的相关内容："+result);
        return result;
    }

    @PostMapping("/confirm")
    public Result confirmOrder(
            @RequestHeader("Authorization") String token,
            @RequestParam String orderNo) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = orderService.confirmOrder(userId, orderNo);
            if (success) {
                result.setSuccess(true);
                result.setMessage("确认收货成功");
            } else {
                result.setSuccess(false);
                result.setMessage("确认收货失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("确认收货失败: " + e.getMessage());
        }
        System.out.println("确认收货的相关内容："+result);
        return result;
    }

    @PostMapping("/refund")
    public Result requestRefund(
            @RequestHeader("Authorization") String token,
            @RequestParam String orderNo,
            @RequestParam String reason) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = orderService.requestRefund(userId, orderNo, reason);
            if (success) {
                result.setSuccess(true);
                result.setMessage("申请退款成功");
            } else {
                result.setSuccess(false);
                result.setMessage("申请退款失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("申请退款失败: " + e.getMessage());
        }
        System.out.println("申请退款的相关内容："+result);
        return result;
    }
    @GetMapping("/refund/query/{orderNo}")
    public Result queryRefundStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable String orderNo) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }

            Map<String, Object> refundStatus = orderService.queryRefundStatus(userId, orderNo);
            if (refundStatus == null) {
                result.setSuccess(false);
                result.setMessage("订单不存在");
                return result;
            }

            result.setSuccess(true);
            result.setMessage("查询退款状态成功");
            result.setData(refundStatus);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("查询退款状态失败: " + e.getMessage());
        }
        return result;
    }
}
