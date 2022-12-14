package com.indo.admin.modules.file.service.impl;

import cn.hutool.core.util.StrUtil;
import com.indo.admin.modules.file.template.S3Template;
import com.indo.common.pojo.bo.ObjectInfo;
import com.indo.core.pojo.entity.FileInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.OutputStream;

/**
 * @author puff
 * @date 2021/8/21
 */
@Service
public class S3Service extends AbstractIFileService {
    @Resource
    private S3Template s3Template;

    @Override
    protected ObjectInfo uploadFile(MultipartFile file,String folder) {
        return s3Template.upload(file,folder);
    }

    @Override
    protected void deleteFile(String objectPath) {
        S3Object s3Object = parsePath(objectPath);
        s3Template.delete(s3Object.bucketName, s3Object.objectName);
    }

    @Override
    public void out(String id, OutputStream os) {
        FileInfo fileInfo = baseMapper.selectById(id);
        if (fileInfo != null) {
            S3Object s3Object = parsePath(fileInfo.getPath());
            s3Template.out(s3Object.bucketName, s3Object.objectName, os);
        }
    }

    @Setter
    @Getter
    private class S3Object {
        private String bucketName;
        private String objectName;
    }

    private S3Object parsePath(String path) {
        S3Object s3Object = new S3Object();
        if (StrUtil.isNotEmpty(path)) {
            int splitIndex = path.lastIndexOf("/");
            if (splitIndex != -1) {
                s3Object.bucketName = path.substring(0, splitIndex);
                s3Object.objectName = path.substring(splitIndex + 1);
            }
        }
        return s3Object;
    }
}
