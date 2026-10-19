package com.fruitrec.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitrec.common.Result;
import com.fruitrec.dto.ImageDetailVO;
import com.fruitrec.dto.ImageVO;
import com.fruitrec.interceptor.JwtInterceptor;
import com.fruitrec.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /** 上传并识别。同步：接口返回时结果已经算好了 */
    @PostMapping("/upload")
    public Result<ImageDetailVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId) {
        return Result.ok(imageService.upload(file, userId));
    }

    /** 分页列表。普通用户只看自己的，管理员看全部 */
    @GetMapping("/list")
    public Result<Page<ImageVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute(JwtInterceptor.ATTR_USER_ID) Long userId,
            @RequestAttribute(JwtInterceptor.ATTR_ROLE) Integer role) {
        Long filterUserId = (role != null && role == 1) ? null : userId;
        return Result.ok(imageService.list(page, size, filterUserId));
    }

    /** 详情：图片 + 前 5 个候选 */
    @GetMapping("/{id}")
    public Result<ImageDetailVO> detail(@PathVariable Long id) {
        return Result.ok(imageService.detail(id));
    }
}
