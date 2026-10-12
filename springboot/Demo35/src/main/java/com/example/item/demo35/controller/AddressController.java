package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Address;
import com.example.item.demo35.service.AddressService;
import com.example.item.demo35.service.UserService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

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
        return userService.selectIdByName(username);
    }

    @PostMapping("/add")
    public Result addAddress(
            @RequestHeader("Authorization") String token,
            @RequestBody Address address) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = addressService.addAddress(userId, address);
            if (success) {
                result.setSuccess(true);
                result.setMessage("添加地址成功");
            } else {
                result.setSuccess(false);
                result.setMessage("添加地址失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("添加地址失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/list")
    public Result getAddressList(@RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            List<Address> addresses = addressService.getAddressList(userId);
            System.out.println(addresses);
            result.setSuccess(true);
            result.setMessage("获取地址列表成功");
            result.setData(addresses);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取地址列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/detail/{id}")
    public Result getAddressDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer id) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            Address address = addressService.getAddressDetail(userId, id);
            if (address != null) {
                result.setSuccess(true);
                result.setMessage("获取地址详情成功");
                result.setData(address);
            } else {
                result.setSuccess(false);
                result.setMessage("地址不存在");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取地址详情失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/update")
    public Result updateAddress(
            @RequestHeader("Authorization") String token,
            @RequestBody Address address) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = addressService.updateAddress(userId, address);
            if (success) {
                result.setSuccess(true);
                result.setMessage("更新地址成功");
            } else {
                result.setSuccess(false);
                result.setMessage("更新地址失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新地址失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/delete")
    public Result deleteAddress(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer id) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = addressService.deleteAddress(userId, id);
            if (success) {
                result.setSuccess(true);
                result.setMessage("删除地址成功");
            } else {
                result.setSuccess(false);
                result.setMessage("删除地址失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除地址失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/setDefault")
    public Result setDefaultAddress(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer id) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = addressService.setDefaultAddress(userId, id);
            if (success) {
                result.setSuccess(true);
                result.setMessage("设置默认地址成功");
            } else {
                result.setSuccess(false);
                result.setMessage("设置默认地址失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("设置默认地址失败: " + e.getMessage());
        }
        return result;
    }
}
