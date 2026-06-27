package com.neusoft.cloud_brain_diagnosis.controller;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import com.neusoft.cloud_brain_diagnosis.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileUploadController Web层测试
 * 覆盖：文件上传成功、文件为空、文件过大、格式不支持
 */
@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(any())).thenReturn(true);
        when(jwtUtil.getRoleFromToken(any())).thenReturn(RoleEnum.DOCTOR.getCode());
    }

    @Test
    void upload_ShouldSucceed_WithJpgFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.startsWith("/upload/")));
    }

    @Test
    void upload_ShouldSucceed_WithPngFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "png data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.endsWith(".png")));
    }

    @Test
    void upload_ShouldSucceed_WithWebpFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.webp",
                "image/webp",
                "webp data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.endsWith(".webp")));
    }

    @Test
    void upload_ShouldSucceed_WithJpegExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.JPEG",
                MediaType.IMAGE_JPEG_VALUE,
                "jpeg uppercase".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void upload_ShouldReturnError_WhenFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(emptyFile)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件不能为空"));
    }

    @Test
    void upload_ShouldReturnError_WhenFileIsNull() throws Exception {
        mockMvc.perform(multipart("/api/file/upload")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件不能为空"));
    }

    @Test
    void upload_ShouldReturnError_WhenFileTooLarge() throws Exception {
        // 超过 2MB 的文件（故意用大字节数组模拟）
        byte[] largeContent = new byte[3 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                largeContent
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(largeFile)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("头像大小不能超过2MB"));
    }

    @Test
    void upload_ShouldReturnError_WhenFileSizeExceeds2MB() throws Exception {
        // 精确测试 2MB 边界：2MB + 1 byte
        byte[] justOver2MB = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "just_over.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                justOver2MB
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("头像大小不能超过2MB"));
    }

    @Test
    void upload_ShouldReturnError_WhenUnsupportedFormat_Pdf() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "doc.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(pdfFile)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅支持 JPG、PNG 或 WebP 图片"));
    }

    @Test
    void upload_ShouldReturnError_WhenUnsupportedFormat_Gif() throws Exception {
        MockMultipartFile gifFile = new MockMultipartFile(
                "file",
                "animation.gif",
                "image/gif",
                "gif data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(gifFile)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅支持 JPG、PNG 或 WebP 图片"));
    }

    @Test
    void upload_ShouldReturnError_WhenUnsupportedFormat_Svg() throws Exception {
        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "image.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(svgFile)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅支持 JPG、PNG 或 WebP 图片"));
    }

    @Test
    void upload_ShouldReturnError_WhenUnsupportedFormat_NoExtension() throws Exception {
        MockMultipartFile noExtFile = new MockMultipartFile(
                "file",
                "noextension",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "data".getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(noExtFile)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅支持 JPG、PNG 或 WebP 图片"));
    }
}
