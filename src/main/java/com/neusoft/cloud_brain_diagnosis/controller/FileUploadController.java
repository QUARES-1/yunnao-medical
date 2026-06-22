package com.neusoft.cloud_brain_diagnosis.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.neusoft.cloud_brain_diagnosis.common.annotation.RequireLogin;
import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/file")
@Tag(name = "文件上传", description = "头像、图片等文件上传")
public class FileUploadController {

    @Value("${file.upload.path:D:/upload/}")
    private String uploadPath;

    @PostMapping("/upload")
    @RequireLogin(RoleEnum.DOCTOR)
    @Operation(summary = "文件上传", description = "上传头像、图片等，返回文件访问URL")
    public Result<String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("头像大小不能超过2MB");
        }

        try {
            FileUtil.mkdir(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.extName(originalFilename).toLowerCase();
            if (!Set.of("jpg", "jpeg", "png", "webp").contains(suffix)) {
                return Result.error("仅支持 JPG、PNG 或 WebP 图片");
            }
            String newFilename = IdUtil.simpleUUID() + "." + suffix;

            File destFile = new File(uploadPath + newFilename);
            file.transferTo(destFile);

            String fileUrl = "/upload/" + newFilename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
