package com.example.item.demo35.controller;

import com.example.item.demo35.entity.*;
import com.example.item.demo35.service.ProductService;
import com.example.item.demo35.util.AliOssUtils;
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
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

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
        // 这里需要从数据库或缓存中根据username获取userId
        // 暂时返回null，需要根据实际情况修改
        return jwtUtils.getUserIdFromToken(actualToken);
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

    @GetMapping("/categories")
    public Result getCategories() {
        Result result = new Result();
        try {
            List<ProductCategory> categories = productService.getCategories();
            result.setSuccess(true);
            result.setMessage("获取分类列表成功");
            result.setData(categories);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取分类列表失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/list")
    public Result getProductList(
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Result result = new Result();
        try {
            List<Product> products = productService.getProductList(categoryId, keyword, page, pageSize);
            result.setSuccess(true);
            result.setMessage("获取商品列表成功");
            result.setData(products);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取商品列表失败: " + e.getMessage());
        }
        System.out.println(result);
        return result;
    }

    @GetMapping("/detail/{id}")
    public Result getProductDetail(@PathVariable Integer id) {
        Result result = new Result();
        try {
            Product product = productService.getProductDetail(id);
            if (product != null) {
                result.setSuccess(true);
                result.setMessage("获取商品详情成功");
                result.setData(product);
            } else {
                result.setSuccess(false);
                result.setMessage("商品不存在");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取商品详情失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/hot")
    public Result getHotProducts(@RequestParam(name = "limit", defaultValue = "10") Integer limit) {
        Result result = new Result();
        try {
            List<Product> products = productService.getHotProducts(limit);
            result.setSuccess(true);
            result.setMessage("获取热门商品成功");
            result.setData(products);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取热门商品失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/new")
    public Result getNewProducts(@RequestParam(name = "limit", defaultValue = "10") Integer limit) {
        Result result = new Result();
        try {
            List<Product> products = productService.getNewProducts(limit);
            result.setSuccess(true);
            result.setMessage("获取新品推荐成功");
            result.setData(products);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取新品推荐失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/recommend")
    public Result getRecommendProducts(@RequestParam(name = "limit", defaultValue = "10") Integer limit) {
        Result result = new Result();
        try {
            List<Product> products = productService.getRecommendProducts(limit);
            result.setSuccess(true);
            result.setMessage("获取推荐商品成功");
            result.setData(products);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取推荐商品失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/favorite/add")
    public Result addFavorite(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer productId) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = productService.addFavorite(userId, productId);
            if (success) {
                result.setSuccess(true);
                result.setMessage("添加收藏成功");
            } else {
                result.setSuccess(false);
                result.setMessage("添加收藏失败或已收藏");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("添加收藏失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/favorite/remove")
    public Result removeFavorite(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer productId) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = productService.removeFavorite(userId, productId);
            if (success) {
                result.setSuccess(true);
                result.setMessage("取消收藏成功");
            } else {
                result.setSuccess(false);
                result.setMessage("取消收藏失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("取消收藏失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/favorite/list")
    public Result getFavoriteList(@RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            List<ProductFavorite> favorites = productService.getFavoriteList(userId);
            result.setSuccess(true);
            result.setMessage("获取收藏列表成功");
            result.setData(favorites);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取收藏列表失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/review/add")
    public Result addReview(
            @RequestHeader("Authorization") String token,
            @RequestBody ProductReview review) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            review.setUserId(userId);
            boolean success = productService.addReview(review);
            if (success) {
                result.setSuccess(true);
                result.setMessage("添加评价成功");
            } else {
                result.setSuccess(false);
                result.setMessage("添加评价失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("添加评价失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/review/list")
    public Result getReviewList(
            @RequestParam Integer productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Result result = new Result();
        try {
            List<ProductReview> reviews = productService.getReviewList(productId, page, pageSize);
            result.setSuccess(true);
            result.setMessage("获取评价列表成功");
            result.setData(reviews);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取评价列表失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 单独上传商品图片接口
     */
    @PostMapping("/upload/image")
    public Result uploadProductImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            // 验证文件是否为空
            if (file == null || file.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("文件不能为空");
                return result;
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.setSuccess(false);
                result.setMessage("只支持图片文件");
                return result;
            }

            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            // 检查文件名是否包含扩展名
            if (originalFilename != null && !originalFilename.isEmpty() && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                // 如果文件名没有扩展名，尝试从内容类型中推断
                if (contentType != null) {
                    switch (contentType) {
                        case "image/jpeg":
                            extension = ".jpg";
                            break;
                        case "image/png":
                            extension = ".png";
                            break;
                        case "image/gif":
                            extension = ".gif";
                            break;
                        case "image/webp":
                            extension = ".webp";
                            break;
                        default:
                            extension = ".jpg"; // 默认使用jpg
                            break;
                    }
                } else {
                    extension = ".jpg"; // 默认使用jpg
                }
            }

            // 生成唯一的文件名
            String objectName = "products/" + System.currentTimeMillis() + "_" + System.nanoTime() + extension;

            // 上传文件到阿里云OSS
            String imageUrl = AliOssUtils.uploadFile(objectName, file.getInputStream());

            if (imageUrl == null || imageUrl.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("图片上传失败");
                return result;
            }

            // 返回上传成功的图片URL
            result.setSuccess(true);
            result.setMessage("上传成功");
            Map<String, Object> data = new HashMap<>();
            data.put("url", imageUrl);
            result.setData(data);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("上传失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 创建商品 - 支持多图上传
     */
    @PostMapping("/create")
    public Result createProduct(
            @RequestParam("name") String name,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("price") String price,
            @RequestParam(value = "originalPrice", required = false) String originalPrice,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "specifications", required = false) String specifications,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            // 验证分类是否存在
            if (categoryId == null) {
                result.setSuccess(false);
                result.setMessage("商品分类不能为空");
                return result;
            }

            // 检查分类是否有效
            List<ProductCategory> categories = productService.getCategories();
            boolean categoryExists = categories.stream().anyMatch(c -> c.getId().equals(categoryId));
            if (!categoryExists) {
                result.setSuccess(false);
                result.setMessage("商品分类不存在");
                return result;
            }

            // 上传商品图片到阿里云OSS
            List<String> imageUrls = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    String originalFilename = image.getOriginalFilename();
                    String extension = "";

                    // 检查文件名是否包含扩展名
                    if (originalFilename != null && !originalFilename.isEmpty() && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    } else {
                        // 如果文件名没有扩展名，尝试从内容类型中推断
                        String contentType = image.getContentType();
                        if (contentType != null) {
                            switch (contentType) {
                                case "image/jpeg":
                                    extension = ".jpg";
                                    break;
                                case "image/png":
                                    extension = ".png";
                                    break;
                                case "image/gif":
                                    extension = ".gif";
                                    break;
                                case "image/webp":
                                    extension = ".webp";
                                    break;
                                default:
                                    extension = ".jpg"; // 默认使用jpg
                                    break;
                            }
                        } else {
                            extension = ".jpg"; // 默认使用jpg
                        }
                    }

                    String objectName = "products/" + System.currentTimeMillis() + "_img_" + imageUrls.size() + extension;

                    String imageUrl = AliOssUtils.uploadFile(objectName, image.getInputStream());
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        imageUrls.add(imageUrl);
                    }
                }
            }

            // 创建商品对象
            Product product = new Product();
            product.setName(name);
            product.setCategoryId(categoryId);
            product.setPrice(new java.math.BigDecimal(price));
            if (originalPrice != null && !originalPrice.isEmpty()) {
                product.setOriginalPrice(new java.math.BigDecimal(originalPrice));
            }
            product.setStock(stock);
            product.setDescription(description);
            // 设置规格，如果为空则使用空JSON对象
            product.setSpecifications(specifications != null && !specifications.isEmpty() ? specifications : "{}");

            // 将图片URL列表转换为JSON字符串存储
            if (!imageUrls.isEmpty()) {
                // 使用Gson或其他JSON库将列表转换为字符串
                com.google.gson.Gson gson = new com.google.gson.Gson();
                product.setImages(gson.toJson(imageUrls));
                // 设置第一张图片为缩略图
                product.setThumbnail(imageUrls.get(0));
            }

            // 设置商品状态
            product.setStatus(1);
            product.setIsHot(0);
            product.setIsNew(0);
            product.setIsRecommend(0);
            // 设置初始销量为0
            product.setSales(0);

            // 保存商品到数据库
            boolean success = productService.createProduct(product);

            if (success) {
                result.setSuccess(true);
                result.setMessage("商品创建成功");
                result.setData(product);
            } else {
                result.setSuccess(false);
                result.setMessage("商品创建失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("商品创建失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除商品 - 需要管理员权限
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteProduct(
            @PathVariable("id") Integer id,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            boolean success = productService.deleteProduct(id);
            if (success) {
                result.setSuccess(true);
                result.setMessage("商品删除成功");
            } else {
                result.setSuccess(false);
                result.setMessage("商品不存在或删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("商品删除失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 更新商品 - 需要管理员权限
     * 前端会将所有数据发送过来，包括修改了的和没有修改过的，所有数据统一在数据库中更新
     */
    @PutMapping("/update/{id}")
    public Result updateProduct(
            @PathVariable("id") Integer id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "categoryId") Integer categoryId,
            @RequestParam(value = "price") String price,
            @RequestParam(value = "originalPrice", required = false) String originalPrice,
            @RequestParam(value = "stock") Integer stock,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "specifications", required = false) String specifications,
            @RequestParam(value = "images", required = false) String images,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            // 创建商品对象并设置基本属性
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setCategoryId(categoryId);
            product.setPrice(new java.math.BigDecimal(price));
            if (originalPrice != null && !originalPrice.isEmpty()) {
                product.setOriginalPrice(new java.math.BigDecimal(originalPrice));
            }
            product.setStock(stock);
            product.setDescription(description);
            product.setSpecifications(specifications != null && !specifications.isEmpty() ? specifications : "{}");

            // 处理图片
            List<String> imageUrls = new ArrayList<>();

            // 处理URL形式的图片
            if (images != null && !images.isEmpty()) {
                // 将JSON字符串转换为URL列表
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String[] urlArray = gson.fromJson(images, String[].class);
                if (urlArray != null) {
                    for (String imageUrl : urlArray) {
                        // 检查是否是URL地址
                        if (imageUrl != null && !imageUrl.isEmpty() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                            imageUrls.add(imageUrl);
                        }
                    }
                }
            }

            // 处理图片文件，上传到阿里云OSS
            if (imageFiles != null && !imageFiles.isEmpty()) {
                for (MultipartFile image : imageFiles) {
                    String originalFilename = image.getOriginalFilename();
                    String extension = "";

                    // 检查文件名是否包含扩展名
                    if (originalFilename != null && !originalFilename.isEmpty() && originalFilename.contains(".")) {
                        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                    } else {
                        // 如果文件名没有扩展名，尝试从内容类型中推断
                        String contentType = image.getContentType();
                        if (contentType != null) {
                            switch (contentType) {
                                case "image/jpeg":
                                    extension = ".jpg";
                                    break;
                                case "image/png":
                                    extension = ".png";
                                    break;
                                case "image/gif":
                                    extension = ".gif";
                                    break;
                                case "image/webp":
                                    extension = ".webp";
                                    break;
                                default:
                                    extension = ".jpg"; // 默认使用jpg
                                    break;
                            }
                        } else {
                            extension = ".jpg"; // 默认使用jpg
                        }
                    }

                    String objectName = "products/" + System.currentTimeMillis() + "_img_" + imageUrls.size() + extension;
                    String imageUrl = AliOssUtils.uploadFile(objectName, image.getInputStream());
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        imageUrls.add(imageUrl);
                    }
                }
            }

            // 将图片URL列表转换为JSON字符串存储
            if (!imageUrls.isEmpty()) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                product.setImages(gson.toJson(imageUrls));
                // 设置第一张图片为缩略图
                product.setThumbnail(imageUrls.get(0));
            }

            // 更新商品
            boolean success = productService.updateProduct(product);
            if (success) {
                result.setSuccess(true);
                result.setMessage("商品更新成功");
                result.setData(product);
            } else {
                result.setSuccess(false);
                result.setMessage("商品更新失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("商品更新失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 添加商品分类 - 需要管理员权限
     */
    @PostMapping("/category/add")
    public Result addCategory(
            @RequestParam("name") String name,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            // 检查分类名称是否为空
            if (name == null || name.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMessage("分类名称不能为空");
                return result;
            }

            // 创建商品分类对象
            ProductCategory category = new ProductCategory();
            category.setName(name.trim());

            // 添加商品分类
            boolean success = productService.addCategory(category);
            if (success) {
                result.setSuccess(true);
                result.setMessage("商品分类添加成功");
                result.setData(category);
            } else {
                result.setSuccess(false);
                result.setMessage("商品分类添加失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("商品分类添加失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除商品分类 - 需要管理员权限
     */
    @DeleteMapping("/category/delete/{id}")
    public Result deleteCategory(
            @PathVariable("id") Integer id,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            // 检查分类ID是否为空
            if (id == null) {
                result.setSuccess(false);
                result.setMessage("分类ID不能为空");
                return result;
            }

            // 删除商品分类
            boolean success = productService.deleteCategory(id);
            if (success) {
                result.setSuccess(true);
                result.setMessage("商品分类删除成功");
            } else {
                result.setSuccess(false);
                result.setMessage("商品分类不存在或删除失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("商品分类删除失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 设置商品为热销 - 需要管理员权限
     */
    @PostMapping("/setHot/{id}")
    public Result setProductHot(
            @PathVariable("id") Integer id,
            @RequestParam("isHot") Boolean isHot,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            boolean success = productService.setProductHot(id, isHot);
            if (success) {
                result.setSuccess(true);
                result.setMessage(isHot ? "商品已设置为热销" : "商品已取消热销");
            } else {
                result.setSuccess(false);
                result.setMessage("商品不存在或设置失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("设置商品热销状态失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 设置商品为新品 - 需要管理员权限
     */
    @PostMapping("/setNew/{id}")
    public Result setProductNew(
            @PathVariable("id") Integer id,
            @RequestParam("isNew") Boolean isNew,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            boolean success = productService.setProductNew(id, isNew);
            if (success) {
                result.setSuccess(true);
                result.setMessage(isNew ? "商品已设置为新品" : "商品已取消新品");
            } else {
                result.setSuccess(false);
                result.setMessage("商品不存在或设置失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("设置商品新品状态失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 设置商品为推荐 - 需要管理员权限
     */
    @PostMapping("/setRecommend/{id}")
    public Result setProductRecommend(
            @PathVariable("id") Integer id,
            @RequestParam("isRecommend") Boolean isRecommend,
            @RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            // 验证管理员登录
            if (!validateAdminToken(token)) {
                result.setSuccess(false);
                result.setMessage("未授权，需要管理员权限");
                return result;
            }

            boolean success = productService.setProductRecommend(id, isRecommend);
            if (success) {
                result.setSuccess(true);
                result.setMessage(isRecommend ? "商品已设置为推荐" : "商品已取消推荐");
            } else {
                result.setSuccess(false);
                result.setMessage("商品不存在或设置失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("设置商品推荐状态失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
}
