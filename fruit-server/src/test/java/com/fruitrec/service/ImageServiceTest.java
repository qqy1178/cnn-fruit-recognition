package com.fruitrec.service;

import com.fruitrec.common.BusinessException;
import com.fruitrec.config.FruitProperties;
import com.fruitrec.mapper.FruitCategoryMapper;
import com.fruitrec.mapper.ImageRecordMapper;
import com.fruitrec.mapper.RecognitionResultMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 图片服务单元测试（T06–T07）。
 * 上传主链路需要真实文件与 Python 进程，这里只测「入口校验」与「查不到」两条分支——
 * 这正是 TDD 里先写的边界用例。
 */
@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRecordMapper imageMapper;
    @Mock
    private RecognitionResultMapper resultMapper;
    @Mock
    private FruitCategoryMapper categoryMapper;
    @Mock
    private PythonService pythonService;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        FruitProperties props = new FruitProperties();
        props.setUploadDir(System.getProperty("java.io.tmpdir") + "/fruit-test");
        imageService = new ImageService(imageMapper, resultMapper, categoryMapper, pythonService, props);
    }

    @Test
    @DisplayName("T06 非法扩展名 → 拒绝，且不写库、不调 Python")
    void upload_rejects_bad_extension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hack.exe", "application/octet-stream", new byte[]{1, 2, 3});

        BusinessException e = assertThrows(BusinessException.class,
                () -> imageService.upload(file, 1L));

        assertTrue(e.getMessage().contains("只支持"));
        verify(imageMapper, never()).insert(any());
        verify(pythonService, never()).predict(any());
    }

    @Test
    @DisplayName("空文件 → 拒绝，且不写库")
    void upload_rejects_empty_file() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.jpg", "image/jpeg", new byte[0]);

        BusinessException e = assertThrows(BusinessException.class,
                () -> imageService.upload(file, 1L));

        assertTrue(e.getMessage().contains("请选择"));
        verify(imageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("T07 详情查不到 → 404 业务异常")
    void detail_not_found() {
        when(imageMapper.selectById(99L)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> imageService.detail(99L));

        assertEquals(404, e.getCode());
        verify(resultMapper, never()).selectList(any());
    }
}
