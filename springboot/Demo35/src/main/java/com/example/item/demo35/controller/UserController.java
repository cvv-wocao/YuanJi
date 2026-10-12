package com.example.item.demo35.controller;

import com.example.item.demo35.service.UserService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import com.example.item.demo35.util.AliOssUtils;
import com.example.item.demo35.entity.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {

    //@Resource
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public UserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @GetMapping("/info")
    public Result getUserInfo(@RequestHeader("Authorization") String token) {
        Result result = new Result();

        // 验证token是否有效
        if (token == null || token.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Token不能为空");
            return result;
        }

        // 去掉 "Bearer " 前缀（如果存在）
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        // 验证token
        if (!jwtUtils.validateToken(actualToken)) {
            result.setSuccess(false);
            result.setMessage("Token无效或已过期");
            return result;
        }

        // 从token中获取用户名
        String username = jwtUtils.getUsernameFromToken(actualToken);

        // 根据用户名查询用户信息
        User user = userService.selectByName(username);

        if (user != null) {
            // 返回用户信息（不包含密码）
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


    @PostMapping("/login")
    public Result login(@RequestParam("number") Integer number, @RequestParam("password") String password) {
        User user = userService.login(number,password);
        Result result = new Result();
        if(user != null) {
            String token = jwtUtils.generateToken(user.getName());
            result.setSuccess(true);
            result.setMessage("登陆成功");
            result.setToken(token);
        }
        else {
            result.setSuccess(false);
            result.setMessage("用户名或密码错误");
        }
        return result;
    }

    @PostMapping("/register")
    public Result register(
            @RequestParam("name") String name,
            @RequestParam("password") String password,
            @RequestParam("number") Integer number,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "age", required = false) Integer age,
            @RequestParam(value = "sex", required = false) String sex,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        Result result = new Result();
        try {
            User user = new User();
            user.setName(name);
            user.setPassword(password);
            user.setNumber(number);
            user.setEmail(email);
            user.setAge(age);
            user.setSex(sex);

            // 如果上传了头像，上传到 OSS
            if (avatar != null && !avatar.isEmpty()) {
                // 生成唯一文件名
                String originalFilename = avatar.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String objectName = "avatars/" + System.currentTimeMillis() + "_" + number + extension;

                // 上传文件并获取 URL
                String avatarUrl = AliOssUtils.uploadFile(objectName, avatar.getInputStream());
                System.out.println("头像已经上传阿里云.........avatarUrl: " + avatarUrl);
                user.setAvatar(avatarUrl);
                System.out.println("获取url地址成功且以保存到user对象中");
            }

            System.out.println("user对象已经创建完毕，开始注册");
            boolean success = userService.register(user);
            System.out.println("数据库已经联通");
            if (success) {
                result.setSuccess(true);
                System.out.println("数据库注册成功");
                result.setMessage("注册成功");
            } else {
                result.setSuccess(false);
                System.out.println("数据库注册失败");
                result.setMessage("注册失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("注册失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/update")
    public Result updateUser(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "number", required = false) Integer number,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "age", required = false) Integer age,
            @RequestParam(value = "sex", required = false) String sex,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();

        // 验证token是否有效
        if (token == null || token.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("Token不能为空");
            return result;
        }

        // 去掉 "Bearer " 前缀（如果存在）
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        // 验证token
        if (!jwtUtils.validateToken(actualToken)) {
            result.setSuccess(false);
            result.setMessage("Token无效或已过期");
            return result;
        }

        // 从token中获取用户名
        String username = jwtUtils.getUsernameFromToken(actualToken);

        // 根据用户名查询用户信息
        User currentUser = userService.selectByName(username);
        if (currentUser == null) {
            result.setSuccess(false);
            result.setMessage("用户不存在");
            return result;
        }

        try {
            // 创建用户对象，只更新允许修改的字段
            User user = new User();
            user.setId(currentUser.getId()); // 设置当前用户的ID

            // 如果提供了新值，则更新（不允许修改ID）
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            } else {
                user.setName(currentUser.getName());
            }

            if (password != null && !password.isEmpty()) {
                user.setPassword(password);
            } else {
                user.setPassword(currentUser.getPassword());
            }

            if (number != null) {
                user.setNumber(number);
            } else {
                user.setNumber(currentUser.getNumber());
            }

            user.setEmail(email != null ? email : currentUser.getEmail());
            user.setAge(age != null ? age : currentUser.getAge());
            user.setSex(sex != null ? sex : currentUser.getSex());

            // 如果上传了新头像，上传到 OSS
            if (avatar != null && !avatar.isEmpty()) {
                String originalFilename = avatar.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String objectName = "avatars/" + System.currentTimeMillis() + "_" + user.getNumber() + extension;
                String avatarUrl = AliOssUtils.uploadFile(objectName, avatar.getInputStream());
                user.setAvatar(avatarUrl);
            } else {
                user.setAvatar(currentUser.getAvatar());
            }

            // 更新用户信息
            boolean success = userService.updateUser(user);

            if (success) {
                result.setSuccess(true);
                result.setMessage("修改成功");
                // 返回更新后的用户信息（不包含密码）
                user.setPassword(null);
                result.setData(user);
            } else {
                result.setSuccess(false);
                result.setMessage("修改失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("修改失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取所有用户列表
     */
    @GetMapping("/list")
    public Result getAllUsers(@RequestHeader(value = "Authorization", required = false) String token) {
        Result result = new Result();

        try {
            // 如果提供了token，验证是否有效
            if (token != null && !token.isEmpty()) {
                String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
                if (!jwtUtils.validateToken(actualToken)) {
                    result.setSuccess(false);
                    result.setMessage("Token无效或已过期");
                    return result;
                }
            }

            // 获取所有用户列表
            var users = userService.selectAllUsers();

            // 移除密码信息
            users.forEach(user -> user.setPassword(null));

            result.setSuccess(true);
            result.setMessage("获取用户列表成功");
            result.setData(users);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取用户列表失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    public Result getUserById(@PathVariable("id") Integer id) {
        Result result = new Result();

        try {
            // 根据ID查询用户信息
            User user = userService.selectById(id);

            if (user != null) {
                // 移除密码信息
                user.setPassword(null);
                result.setSuccess(true);
                result.setMessage("获取用户信息成功");
                result.setData(user);
            } else {
                result.setSuccess(false);
                result.setMessage("用户不存在");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取用户信息失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

}
