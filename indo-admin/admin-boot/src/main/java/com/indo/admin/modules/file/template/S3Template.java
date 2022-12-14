package com.indo.admin.modules.file.template;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import com.indo.admin.modules.file.properties.AwsS3Properties;
import com.indo.common.pojo.bo.ObjectInfo;
import com.indo.common.utils.CommonFunction;
import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.UUID;

/**
 * aws s3配置
 *
 * @author puff
 * @date 2021/2/11
 */
@Component
public class S3Template implements InitializingBean {
    private static final String DEF_CONTEXT_TYPE = "application/octet-stream";
    private static final String PATH_SPLIT = "/";

    @Autowired
    private AwsS3Properties s3;

    private AmazonS3 amazonS3;

    @Override
    public void afterPropertiesSet() {
        ClientConfiguration config = new ClientConfiguration();
        config.setProtocol(Protocol.HTTPS);//访问协议
        AWSCredentials credentials = new BasicAWSCredentials(s3.getAccessKey(), s3.getAccessKeySecret());

        this.amazonS3 = AmazonS3Client.builder()
                .withClientConfiguration(config)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(s3.getRegion())
                .disableChunkedEncoding()
                .withPathStyleAccessEnabled(true)
                .build();
    }

    @SneakyThrows
    public ObjectInfo upload(String fileName, String folder, InputStream is) {
        return upload(s3.getBucketName(), fileName, folder, is, is.available(), DEF_CONTEXT_TYPE);
    }

    @SneakyThrows
    public ObjectInfo upload(MultipartFile file, String folder) {
        return upload(s3.getBucketName(), file.getOriginalFilename(), folder, file.getInputStream()
                , ((Long) file.getSize()).intValue(), file.getContentType());
    }

    @SneakyThrows
    public ObjectInfo upload(String bucketName, String fileName, String folder, InputStream is) {
        return upload(bucketName, fileName, folder, is, is.available(), DEF_CONTEXT_TYPE);
    }

    /**
     * 上传对象
     *
     * @param bucketName  bucket名称
     * @param objectName  对象名
     * @param is          对象流
     * @param size        大小
     * @param contentType 类型
     */
    private ObjectInfo upload(String bucketName, String objectName, String folder, InputStream is, int size, String contentType) {

        String filePrefix = objectName.substring(0,objectName.lastIndexOf("."));
        String fileSuffix = objectName.substring(objectName.lastIndexOf("."));
        String key = folder + PATH_SPLIT + getKeyCode() + fileSuffix.toLowerCase();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contentType);
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName, key, is, objectMetadata);
        putObjectRequest.getRequestClientOptions().setReadLimit(size + 1);
        amazonS3.putObject(putObjectRequest);
        ObjectInfo obj = new ObjectInfo();
        obj.setObjectPath(PATH_SPLIT + key);
        obj.setObjectUrl(s3.getAwsS3PrefixUrl() + obj.getObjectPath());
        return obj;
    }

    public void delete(String objectName) {
        delete(s3.getBucketName(), objectName);
    }

    public void delete(String bucketName, String objectName) {
        amazonS3.deleteObject(bucketName, objectName);
    }

    /**
     * 获取预览地址
     *
     * @param bucketName bucket名称
     * @param objectName 对象名
     * @param expires    有效时间(分钟)，最大7天有效
     * @return
     */
    public String getViewUrl(String bucketName, String objectName, int expires) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, expires);
        URL url = amazonS3.generatePresignedUrl(bucketName, objectName, cal.getTime());
        return url.toString();
    }

    public void out(String objectName, OutputStream os) {
        out(s3.getBucketName(), objectName, os);
    }

    /**
     * 输出对象
     *
     * @param bucketName bucket名称
     * @param objectName 对象名
     * @param os         输出流
     */
    @SneakyThrows
    public void out(String bucketName, String objectName, OutputStream os) {
        S3Object s3Object = amazonS3.getObject(bucketName, objectName);
        try (
                S3ObjectInputStream s3is = s3Object.getObjectContent();
        ) {
            IOUtils.copy(s3is, os);
        }
    }


    /**
     * s3 生成对象 唯一的 key
     *
     * @return
     */
    public String getKeyCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(UUID.randomUUID());
        builder.append(CommonFunction.inviteCode().toLowerCase());
        return builder.toString().replaceAll("-", "");
    }
}
