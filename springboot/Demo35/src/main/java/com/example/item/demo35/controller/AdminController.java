package com.example.item.demo35.controller;

import com.example.item.demo35.entity.*;
import com.example.item.demo35.service.AdminService;
import com.example.item.demo35.service.OrderService;
import com.example.item.demo35.service.PostService;
import com.example.item.demo35.service.UserService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderService orderService;

    private final AdminService adminService;
    private final UserService userService;
    private final PostService postService;
    private final JwtUtils jwtUtils;

    public AdminController(AdminService adminService, UserService userService, PostService postService, JwtUtils jwtUtils) {
        this.adminService = adminService;
        this.userService = userService;
        this.postService = postService;
        this.jwtUtils = jwtUtils;
    }

    //===============================================================================================//
    /**
     * 获取所有订单（管理员）
     */
    @GetMapping("/orders")
    public Result getAllOrders(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "receiverName", required = false) String receiverName,
            @RequestParam(value = "receiverPhone", required = false) String receiverPhone,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            // 设置默认分页参数
            if (page == null || page < 1) {
                page = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }
            if (pageSize > 100) {
                pageSize = 100;
            }

            // 构建查询参数
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("pageSize", pageSize);
            params.put("status", status);
            params.put("orderNo", orderNo);
            params.put("receiverName", receiverName);
            params.put("receiverPhone", receiverPhone);
            params.put("startDate", startDate);
            params.put("endDate", endDate);

            // 获取订单列表
            List<Order> orders = adminService.getAllOrders(params);
            System.out.println(orders);

            // 获取总数
            Long total = adminService.countAllOrders(params);

            Map<String, Object> data = new HashMap<>();
            data.put("orders", orders);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", (int) Math.ceil((double) total / pageSize));

            result.setSuccess(true);
            result.setMessage("获取订单列表成功");
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取订单列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(result);
        return result;
    }

    /**
     * 根据用户ID获取该用户的所有订单（管理员）
     */
    @GetMapping("/orders/user/{userId}")
    public Result getOrdersByUserId(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            // 检查用户是否存在
            User user = userService.selectById(userId);
            if (user == null) {
                result.setSuccess(false);
                result.setMessage("用户不存在");
                return result;
            }

            // 设置默认分页参数
            if (page == null || page < 1) {
                page = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10;
            }
            if (pageSize > 100) {
                pageSize = 100;
            }

            // 构建查询参数
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("page", page);
            params.put("pageSize", pageSize);
            params.put("status", status);

            // 获取订单列表
            List<Order> orders = adminService.getOrdersByUserId(params);

            // 获取总数
            Long total = adminService.countOrdersByUserId(userId, status);

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("userName", user.getName());
            data.put("orders", orders);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", (int) Math.ceil((double) total / pageSize));

            result.setSuccess(true);
            result.setMessage("获取用户订单列表成功");
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取用户订单列表失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取订单详情（管理员）
     */
    @GetMapping("/order/{orderNo}")
    public Result getOrderDetail(
            @PathVariable("orderNo") String orderNo,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            Order order = adminService.getOrderDetail(orderNo);
            if (order == null) {
                result.setSuccess(false);
                result.setMessage("订单不存在");
                return result;
            }

            // 获取订单商品列表
            List<OrderItem> orderItems = adminService.getOrderItems(order.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("order", order);
            data.put("items", orderItems);

            result.setSuccess(true);
            result.setMessage("获取订单详情成功");
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取订单详情失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 修改订单状态（管理员）
     * 只有当状态为1（待发货）时可以修改为2（已发货）
     * 其他状态也可以修改，但需要根据业务逻辑判断
     */
    @PutMapping("/order/status/{orderNo}")
    public Result updateOrderStatus(
            @PathVariable("orderNo") String orderNo,
            @RequestParam("status") Integer status,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            // 获取当前订单信息
            Order order = adminService.getOrderDetail(orderNo);
            if (order == null) {
                result.setSuccess(false);
                result.setMessage("订单不存在");
                return result;
            }

            // 验证状态转换的合法性
            Integer currentStatus = order.getStatus();

            // 状态转换规则
            boolean isValidTransition = isValidStatusTransition(currentStatus, status);

            if (!isValidTransition) {
                String errorMsg = String.format("无效的状态转换：当前状态为 %s，不能转换为 %s",
                        getStatusText(currentStatus), getStatusText(status));
                result.setSuccess(false);
                result.setMessage(errorMsg);
                return result;
            }

            // 特殊业务逻辑处理
            // 当状态为1（待发货）时，可以修改为2（已发货）
            if (currentStatus == 1 && status == 2) {
                // 可以在这里添加发货相关的业务逻辑，如记录发货时间等
                boolean success = adminService.updateOrderStatus(orderNo, status, reason);
                if (success) {
                    result.setSuccess(true);
                    result.setMessage("订单发货成功，状态已更新为【已发货】");
                } else {
                    result.setSuccess(false);
                    result.setMessage("订单发货失败");
                }
            }
            // 其他状态转换
            else {
                boolean success = adminService.updateOrderStatus(orderNo, status, reason);
                if (success) {
                    result.setSuccess(true);
                    result.setMessage("订单状态更新成功，当前状态：" + getStatusText(status));
                } else {
                    result.setSuccess(false);
                    result.setMessage("订单状态更新失败");
                }
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新订单状态失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 批量发货（将待发货订单批量修改为已发货）
     */
    @PostMapping("/order/refund/approve/{orderNo}")
    public Result approveRefund(
            @PathVariable("orderNo") String orderNo,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            Order order = adminService.getOrderDetail(orderNo);
            if (order == null) {
                result.setSuccess(false);
                result.setMessage("订单不存在");
                return result;
            }

            if (order.getStatus() != 5) {
                result.setSuccess(false);
                result.setMessage("当前订单不在退款处理中，不能执行退款审核");
                return result;
            }

            boolean success = orderService.approveRefund(orderNo, reason);
            result.setSuccess(success);
            result.setMessage(success ? "管理员审核退款成功" : "管理员审核退款失败");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("管理员审核退款失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/order/refund/reject/{orderNo}")
    public Result rejectRefund(
            @PathVariable("orderNo") String orderNo,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            Order order = adminService.getOrderDetail(orderNo);
            if (order == null) {
                result.setSuccess(false);
                result.setMessage("订单不存在");
                return result;
            }

            if (order.getStatus() != 5) {
                result.setSuccess(false);
                result.setMessage("当前订单不在退款处理中，不能驳回退款");
                return result;
            }

            boolean success = orderService.rejectRefund(orderNo, reason);
            result.setSuccess(success);
            result.setMessage(success ? "管理员驳回退款成功" : "管理员驳回退款失败");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("管理员驳回退款失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/order/refund/query/{orderNo}")
    public Result queryRefundStatus(
            @PathVariable("orderNo") String orderNo,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            Map<String, Object> refundStatus = orderService.queryRefundStatusForAdmin(orderNo);
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

    @PutMapping("/orders/batchShip")
    public Result batchShipOrders(
            @RequestBody List<String> orderNos,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            if (orderNos == null || orderNos.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("订单号列表不能为空");
                return result;
            }

            int successCount = 0;
            int failCount = 0;
            List<String> failedOrders = new ArrayList<>();

            for (String orderNo : orderNos) {
                Order order = adminService.getOrderDetail(orderNo);
                if (order != null && order.getStatus() == 1) {
                    boolean success = adminService.updateOrderStatus(orderNo, 2, reason);
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                        failedOrders.add(orderNo);
                    }
                } else {
                    failCount++;
                    failedOrders.add(orderNo + "(状态不正确)");
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", orderNos.size());
            data.put("successCount", successCount);
            data.put("failCount", failCount);
            data.put("failedOrders", failedOrders);

            result.setSuccess(true);
            result.setMessage(String.format("批量发货完成：成功 %d 个，失败 %d 个", successCount, failCount));
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("批量发货失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 订单统计信息（管理员）
     */
    @GetMapping("/orders/statistics")
    public Result getOrderStatistics(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权，需要管理员权限");
            return result;
        }

        try {
            Map<String, Object> statistics = adminService.getOrderStatistics(startDate, endDate);
            result.setSuccess(true);
            result.setMessage("获取订单统计成功");
            result.setData(statistics);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取订单统计失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 验证状态转换的合法性
     */
    private boolean isValidStatusTransition(Integer currentStatus, Integer newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // 定义允许的状态转换
        // 0:待付款 -> 1:待发货, 4:已取消
        // 1:待发货 -> 2:已发货, 4:已取消
        // 2:待收货 -> 3:已完成, 5:退款中
        // 3:已完成 -> 无后续状态
        // 4:已取消 -> 无后续状态
        // 5:退款中 -> 3:已完成, 6:已退款
        // 6:已退款 -> 无后续状态

        switch (currentStatus) {
            case 0: // 待付款
                return newStatus == 1 || newStatus == 4;
            case 1: // 待发货
                return newStatus == 2 || newStatus == 4;
            case 2: // 待收货
                return newStatus == 3 || newStatus == 5;
            case 3: // 已完成
                return false; // 已完成订单不能修改状态
            case 4: // 已取消
                return false; // 已取消订单不能修改状态
            case 5: // 退款中
                return newStatus == 3 || newStatus == 6;
            case 6: // 已退款
                return false;
            default:
                return false;
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待付款";
            case 1: return "待发货";
            case 2: return "待收货";
            case 3: return "已完成";
            case 4: return "已取消";
            case 5: return "退款中";
            case 6: return "已退款";
            default: return "未知";
        }
    }
    //===============================================================================================//

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result login(@RequestParam("username") String username, @RequestParam("password") String password) {
        Result result = new Result();
        Admin admin = adminService.login(username, password);
        if (admin != null) {
            String token = jwtUtils.generateToken(username);
            result.setSuccess(true);
            result.setMessage("管理员登录成功");
            result.setToken(token);
            Map<String, Object> data = new HashMap<>();
            data.put("admin", admin);
            result.setData(data);
        } else {
            result.setSuccess(false);
            result.setMessage("用户名或密码错误");
        }
        return result;
    }

    /**
     * 获取当前管理员信息
     */
    @GetMapping("/info")
    public Result getAdminInfo(@RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (token == null || token.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Token不能为空");
            return result;
        }

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtUtils.validateToken(actualToken)) {
            result.setSuccess(false);
            result.setMessage("Token无效或已过期");
            return result;
        }

        String username = jwtUtils.getUsernameFromToken(actualToken);
        Admin admin = adminService.selectByUsername(username);

        if (admin != null) {
            admin.setPassword(null);
            result.setSuccess(true);
            result.setMessage("获取管理员信息成功");
            result.setData(admin);
        } else {
            result.setSuccess(false);
            result.setMessage("管理员不存在");
        }

        return result;
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    public Result getAllUsers(@RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        List<User> users = userService.selectAllUsers();
        for (User user : users) {
            user.setPassword(null);
        }

        result.setSuccess(true);
        result.setMessage("获取用户列表成功");
        result.setData(users);

        return result;
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/user/{id}")
    public Result getUserById(@PathVariable("id") Integer id,
                               @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        User user = userService.selectById(id);
        if (user != null) {
            user.setPassword(null);
            result.setSuccess(true);
            result.setMessage("获取用户信息成功");
            result.setData(user);
        } else {
            result.setSuccess(false);
            result.setMessage("用户不存在");
        }

        return result;
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/{id}")
    public Result updateUser(@PathVariable("id") Integer id,
                             @RequestBody User user,
                             @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        user.setId(id);
        boolean success = userService.updateUser(user);

        if (success) {
            result.setSuccess(true);
            result.setMessage("更新用户成功");
        } else {
            result.setSuccess(false);
            result.setMessage("更新用户失败");
        }

        return result;
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/{id}")
    public Result deleteUser(@PathVariable("id") Integer id,
                              @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        boolean success = userService.deleteUser(id);

        if (success) {
            result.setSuccess(true);
            result.setMessage("删除用户成功");
        } else {
            result.setSuccess(false);
            result.setMessage("删除用户失败");
        }

        return result;
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/users/batch")
    public Result batchDeleteUsers(@RequestBody List<Integer> ids,
                                   @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        int successCount = 0;
        for (Integer id : ids) {
            if (userService.deleteUser(id)) {
                successCount++;
            }
        }

        result.setSuccess(true);
        result.setMessage("成功删除 " + successCount + " 个用户");

        return result;
    }

    /**
     * 获取所有管理员
     */
    @GetMapping("/list")
    public Result getAllAdmins(@RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        List<Admin> admins = adminService.selectAll();
        for (Admin admin : admins) {
            admin.setPassword(null);
        }

        result.setSuccess(true);
        result.setMessage("获取管理员列表成功");
        result.setData(admins);

        return result;
    }

    /**
     * 创建管理员
     */
    @PostMapping("/create")
    public Result createAdmin(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              @RequestParam(value = "email", required = false) String email,
                              @RequestParam(value = "phone", required = false) String phone,
                              @RequestParam(value = "level", required = false) Integer level,
                              @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                              @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            Admin admin = new Admin();
            admin.setUsername(username);
            admin.setPassword(password);
            admin.setEmail(email);
            admin.setPhone(phone);
            admin.setLevel(level != null ? level : 1);

            if (avatar != null && !avatar.isEmpty()) {
                String originalFilename = avatar.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String objectName = "admin_avatars/" + System.currentTimeMillis() + "_" + username + extension;
                String avatarUrl = com.example.item.demo35.util.AliOssUtils.uploadFile(objectName, avatar.getInputStream());
                admin.setAvatar(avatarUrl);
            }

            boolean success = adminService.createAdmin(admin);
            if (success) {
                result.setSuccess(true);
                result.setMessage("创建管理员成功");
            } else {
                result.setSuccess(false);
                result.setMessage("创建管理员失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("创建管理员失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 更新管理员信息
     */
    @PutMapping("/{id}")
    public Result updateAdmin(@PathVariable("id") Integer id,
                              @RequestBody Admin admin,
                              @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        admin.setId(id);
        boolean success = adminService.updateAdmin(admin);

        if (success) {
            result.setSuccess(true);
            result.setMessage("更新管理员成功");
        } else {
            result.setSuccess(false);
            result.setMessage("更新管理员失败");
        }

        return result;
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/{id}")
    public Result deleteAdmin(@PathVariable("id") Integer id,
                               @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        boolean success = adminService.deleteAdmin(id);

        if (success) {
            result.setSuccess(true);
            result.setMessage("删除管理员成功");
        } else {
            result.setSuccess(false);
            result.setMessage("删除管理员失败");
        }

        return result;
    }

    // ==================== 帖子管理 ====================

    /**
     * 获取所有帖子（管理员）
     */
    @GetMapping("/posts")
    public Result getAllPosts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            List<Post> posts = postService.getPostsByPage(page, size, null);

            result.setSuccess(true);
            result.setMessage("获取帖子列表成功");

            Map<String, Object> data = new HashMap<>();
            data.put("posts", posts);
            data.put("page", page);
            data.put("size", size);
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除帖子（管理员）
     */
    @DeleteMapping("/post/{id}")
    public Result deletePost(
            @PathVariable("id") Integer postId,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            // 管理员可以直接删除帖子，不需要检查userId
            boolean success = postService.adminDeletePost(postId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("删除帖子成功");
            } else {
                result.setSuccess(false);
                result.setMessage("帖子不存在或删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 批量删除帖子（管理员）
     */
    @DeleteMapping("/posts/batch")
    public Result batchDeletePosts(
            @RequestBody List<Integer> postIds,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            int successCount = 0;
            for (Integer postId : postIds) {
                if (postService.adminDeletePost(postId)) {
                    successCount++;
                }
            }

            result.setSuccess(true);
            result.setMessage("成功删除 " + successCount + " 个帖子");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("批量删除失败: " + e.getMessage());
        }

        return result;
    }

    // ==================== 评论管理 ====================

    /**
     * 获取所有评论（管理员）
     */
    @GetMapping("/comments")
    public Result getAllComments(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            List<PostComment> comments = postService.getCommentsByPage(page, size);

            result.setSuccess(true);
            result.setMessage("获取评论列表成功");

            Map<String, Object> data = new HashMap<>();
            data.put("comments", comments);
            data.put("page", page);
            data.put("size", size);
            result.setData(data);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除评论（管理员）
     */
    @DeleteMapping("/comment/{id}")
    public Result deleteComment(
            @PathVariable("id") Integer commentId,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            // 管理员可以直接删除评论，不需要检查userId
            boolean success = postService.adminDeleteComment(commentId);

            if (success) {
                result.setSuccess(true);
                result.setMessage("删除评论成功");
            } else {
                result.setSuccess(false);
                result.setMessage("评论不存在或删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 批量删除评论（管理员）
     */
    @DeleteMapping("/comments/batch")
    public Result batchDeleteComments(
            @RequestBody List<Integer> commentIds,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        if (!validateAdminToken(token)) {
            result.setSuccess(false);
            result.setMessage("未授权");
            return result;
        }

        try {
            int successCount = 0;
            for (Integer commentId : commentIds) {
                if (postService.adminDeleteComment(commentId)) {
                    successCount++;
                }
            }

            result.setSuccess(true);
            result.setMessage("成功删除 " + successCount + " 条评论");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("批量删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证管理员token
     */
    private boolean validateAdminToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        return jwtUtils.validateToken(actualToken);
    }
}
