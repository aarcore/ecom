package com.ait.admin.rest;

import com.ait.common.api.R;
import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;
import com.ait.logging.annotation.OperationLog;
import com.ait.oss.config.StorageProperties;
import com.ait.oss.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
@Tag(name = "Admin · 文件上传")
public class AdminFileController {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    @PostMapping("/upload")
    @OperationLog(module = "文件", action = "上传文件", saveParams = false)
    @Operation(summary = "上传图片 / 附件")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "common") String folder) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要上传的文件");
        }
        if (file.getSize() > storageProperties.getMaxSize()) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "文件大小超过限制（" + storageProperties.getMaxSize() / 1024 / 1024 + "MB）");
        }

        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!storageProperties.getAllowedExtensions().contains(extension)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "不支持的文件类型，允许: " + String.join(", ", storageProperties.getAllowedExtensions()));
        }

        String key = sanitizeFolder(folder) + "/" + LocalDate.now().format(MONTH)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try (InputStream stream = file.getInputStream()) {
            String url = storageService.store(key, stream, file.getSize(), file.getContentType());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", url);
            payload.put("key", key);
            payload.put("name", original);
            payload.put("size", file.getSize());
            return R.ok(payload);
        } catch (IOException ex) {
            throw new BizException(ResultCode.ERROR, "文件上传失败: " + ex.getMessage());
        }
    }

    @GetMapping("/config")
    @Operation(summary = "存储配置概览")
    public R<Map<String, Object>> config() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", storageProperties.getType());
        payload.put("backend", storageService.backend());
        payload.put("publicBaseUrl", storageProperties.getPublicBaseUrl());
        payload.put("maxSize", storageProperties.getMaxSize());
        payload.put("allowedExtensions", storageProperties.getAllowedExtensions());
        return R.ok(payload);
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Restricts the folder to a small set of known prefixes. */
    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isEmpty()) {
            return "common";
        }
        String cleaned = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        return Arrays.asList("product", "banner", "avatar", "category", "brand", "common").contains(cleaned)
                ? cleaned : "common";
    }
}
