package com.example.item.demo35.util;

import com.aliyun.oss.*;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;

import java.io.InputStream;

public class AliOssUtils {

    private static String endpoint = "https://oss-cn-beijing.aliyuncs.com";
    private static String accessKeyId;
    private static String accessKeySecret;
    private static String bucketName;

    /**
     * 由 OssConfig 在应用启动时注入 OSS 配置。
     * 密钥统一来自 .env / 环境变量，避免硬编码在代码中。
     */
    public static void init(String ossEndpoint, String keyId, String keySecret, String bucket) {
        endpoint = ossEndpoint;
        accessKeyId = keyId;
        accessKeySecret = keySecret;
        bucketName = bucket;
    }

    public static String uploadFile(String objectName, InputStream in) throws Exception {
//         填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
        String region = "cn-beijing";

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, clientBuilderConfiguration);

        String url = "";
        try {
            // 填写字符串。
            String content = "Hello OSS，你好世界";

            // 创建PutObjectRequest对象。
//            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new ByteArrayInputStream(content.getBytes()));
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, in);

            // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);

            // 上传字符串。
            PutObjectResult result = ossClient.putObject(putObjectRequest);

            //url = "https://cvv-springboot-test.oss-cn-beijing.aliyuncs.com/" + objectName;
            url = "https://" + bucketName + "." + endpoint.substring(endpoint.lastIndexOf("/") + 1) + "/" + objectName;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }
}