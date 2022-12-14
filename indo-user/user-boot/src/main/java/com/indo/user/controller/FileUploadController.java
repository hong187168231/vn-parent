package com.indo.user.controller;

import com.indo.admin.api.FileFeignClient;
import com.indo.common.result.Result;
import com.indo.common.web.exception.BizException;
import com.indo.user.pojo.vo.FileUploadVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * <p>
 * 文件上传
 * </p>
 *
 * @author puff
 * @since 2021-12-17
 */
@Api(tags = "文件上传")
@RestController
@RequestMapping("/api/v1/file")
@Slf4j
public class FileUploadController {


    @Resource
    private FileFeignClient fileFeignClient;

    /**
     * app文件上传
     *
     * @param file
     * @return
     */
    @ApiOperation(value = "文件上传接口", httpMethod = "POST")
    @PostMapping("/upload")
    public Result<FileUploadVo> upload(@RequestParam("file") MultipartFile file) {
        Result<String> result = fileFeignClient.upload(file);
        if (Result.success().getCode().equals(result.getCode())) {
            return Result.success(new FileUploadVo(result.getData()));
        } else {
            return Result.failed("No client with requested ");
        }
    }

}
