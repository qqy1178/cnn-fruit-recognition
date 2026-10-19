package com.fruitrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitrec.common.BusinessException;
import com.fruitrec.config.FruitProperties;
import com.fruitrec.dto.ImageDetailVO;
import com.fruitrec.dto.ImageVO;
import com.fruitrec.dto.PredictResult;
import com.fruitrec.entity.FruitCategory;
import com.fruitrec.entity.ImageRecord;
import com.fruitrec.entity.RecognitionResult;
import com.fruitrec.mapper.FruitCategoryMapper;
import com.fruitrec.mapper.ImageRecordMapper;
import com.fruitrec.mapper.RecognitionResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "bmp");

    private final ImageRecordMapper imageMapper;
    private final RecognitionResultMapper resultMapper;
    private final FruitCategoryMapper categoryMapper;
    private final PythonService pythonService;
    private final FruitProperties props;

    // ==========================================================
    // 上传 → 存盘 → 调 Python → 结果入库（同步）
    // ==========================================================
    @Transactional(rollbackFor = Exception.class)
    public ImageDetailVO upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要识别的图片");
        }

        String original = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "unknown.jpg";
        String ext = extOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("只支持这些格式：" + String.join(" / ", ALLOWED_EXT));
        }

        // ---- 1. 存盘：uploads/yyyyMMdd/随机名.ext ----
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String relative = day + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = Paths.get(props.getUploadDir()).resolve(relative).toAbsolutePath();

        try {
            Files.createDirectories(target.getParent());
            // 注意：这里必须传绝对路径。transferTo 收到相对路径时会存到临时目录，不是你想的地方。
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("保存图片失败：" + e.getMessage());
        }

        try {
            // ---- 2. 图片记录入库 ----
            ImageRecord img = new ImageRecord();
            img.setFileName(original);
            img.setFilePath(relative);              // 存相对路径，换机器只改 yml
            img.setFileSize((int) file.getSize());
            img.setUserId(userId);
            img.setSource(1);                        // 1=用户上传
            int[] wh = readWidthHeight(target);
            img.setWidth(wh[0]);
            img.setHeight(wh[1]);
            imageMapper.insert(img);

            // ---- 3. 调 Python 识别 ----
            PredictResult pr = pythonService.predict(target.toString());

            // ---- 4. 5 个候选结果入库 ----
            for (PredictResult.Item item : pr.getResults()) {
                Integer categoryId = getOrCreateCategory(item.getClassIndex(), item.getNameEn(), item.getNameCn());

                RecognitionResult r = new RecognitionResult();
                r.setImageId(img.getId());
                r.setCategoryId(categoryId);
                r.setConfidence(BigDecimal.valueOf(item.getConfidence()));
                r.setRank(item.getRank());
                r.setModelVersion(pr.getModel());
                r.setCostMs(pr.getCostMs());
                resultMapper.insert(r);
            }

            log.info("图片 {} 识别完成，共 {} 个候选", img.getId(), pr.getResults().size());
            return detail(img.getId());

        } catch (RuntimeException e) {
            // 识别失败 → 删掉刚落盘的图片，并回滚数据库
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    // ==========================================================
    // 列表（分页）
    // ==========================================================
    public Page<ImageVO> list(int pageNo, int pageSize, Long userId) {
        Page<ImageRecord> page = new Page<>(pageNo, Math.min(pageSize, 100));
        LambdaQueryWrapper<ImageRecord> qw = new LambdaQueryWrapper<ImageRecord>()
                .eq(userId != null, ImageRecord::getUserId, userId)
                .orderByDesc(ImageRecord::getUploadTime);

        Page<ImageRecord> result = imageMapper.selectPage(page, qw);

        // 一次查出本页所有图的结果，避免每张图查一次（N+1）
        List<Long> ids = result.getRecords().stream().map(ImageRecord::getId).toList();
        Map<Long, RecognitionResult> topMap = ids.isEmpty() ? Map.of()
                : resultMapper.selectList(new LambdaQueryWrapper<RecognitionResult>()
                        .in(RecognitionResult::getImageId, ids)
                        .eq(RecognitionResult::getRank, 1))
                .stream().collect(Collectors.toMap(RecognitionResult::getImageId, r -> r, (a, b) -> a));

        Map<Integer, String> nameMap = categoryNameMap(topMap.values().stream()
                .map(RecognitionResult::getCategoryId).collect(Collectors.toSet()));

        Page<ImageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(img -> {
            RecognitionResult top = topMap.get(img.getId());
            return ImageVO.of(img, img.getFilePath(),
                    top == null ? null : nameMap.get(top.getCategoryId()),
                    top == null ? null : top.getConfidence().doubleValue());
        }).toList());
        return voPage;
    }

    // ==========================================================
    // 详情：一张图 + 前 5 个候选
    // ==========================================================
    public ImageDetailVO detail(Long imageId) {
        ImageRecord img = imageMapper.selectById(imageId);
        if (img == null) {
            throw new BusinessException(404, "图片不存在");
        }

        List<RecognitionResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<RecognitionResult>()
                        .eq(RecognitionResult::getImageId, imageId)
                        .orderByAsc(RecognitionResult::getRank));

        Map<Integer, FruitCategory> catMap = categoryMapper.selectBatchIds(
                        results.stream().map(RecognitionResult::getCategoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(FruitCategory::getId, c -> c));

        ImageDetailVO vo = new ImageDetailVO();
        vo.setId(img.getId());
        vo.setFileName(img.getFileName());
        vo.setUrl("/uploads/" + img.getFilePath());
        vo.setFileSize(img.getFileSize());
        vo.setUploadTime(img.getUploadTime());

        if (!results.isEmpty()) {
            RecognitionResult top = results.get(0);
            vo.setModelVersion(top.getModelVersion());
            vo.setCostMs(top.getCostMs());
            FruitCategory topCat = catMap.get(top.getCategoryId());
            vo.setTopName(topCat == null ? null : topCat.getNameCn());
            vo.setTopConfidence(top.getConfidence().doubleValue());
        }

        vo.setCandidates(results.stream().map(r -> {
            FruitCategory c = catMap.get(r.getCategoryId());
            ImageDetailVO.Candidate cd = new ImageDetailVO.Candidate();
            cd.setRank(r.getRank());
            cd.setNameCn(c == null ? null : c.getNameCn());
            cd.setNameEn(c == null ? null : c.getNameEn());
            cd.setConfidence(r.getConfidence().doubleValue());
            return cd;
        }).toList());

        return vo;
    }

    // ==========================================================
    // 工具
    // ==========================================================

    /**
     * 按 class_index 找类别；找不到就**自动建一条**。
     *
     * 为什么要这样：现在用的是 ImageNet 预训练模型，它的 class_index 是 0~999，
     * 和 fruit_category 表（将来由 Fruits-360 生成）对不上。
     * 自动扩充可以让系统在换模型前后都能跑，不用手工维护字典。
     */
    private Integer getOrCreateCategory(Integer classIndex, String nameEn, String nameCn) {
        if (classIndex == null) {
            throw new BusinessException("识别结果缺少类别索引");
        }

        // 1) 先按 class_index 找
        FruitCategory exist = categoryMapper.selectOne(
                new LambdaQueryWrapper<FruitCategory>().eq(FruitCategory::getClassIndex, classIndex));
        if (exist != null) {
            return exist.getId();
        }

        // 2) ⚠️ 再按 name_en 找。表上 name_en 是唯一索引，
        //    只按 class_index 找的话，遇到「同名但索引不同」的记录会插入失败：
        //    Duplicate entry 'orange' for key 'fruit_category.uk_name_en'
        //    （这个 bug 是端到端实测才暴露的，编译期完全看不出来）
        if (StringUtils.hasText(nameEn)) {
            FruitCategory byName = categoryMapper.selectOne(
                    new LambdaQueryWrapper<FruitCategory>().eq(FruitCategory::getNameEn, nameEn));
            if (byName != null) {
                byName.setClassIndex(classIndex);      // 顺手把索引补正
                categoryMapper.updateById(byName);
                return byName.getId();
            }
        }

        // 3) 都没有 → 新建
        FruitCategory c = new FruitCategory();
        c.setClassIndex(classIndex);
        c.setNameEn(nameEn);
        c.setNameCn(StringUtils.hasText(nameCn) ? nameCn : nameEn);
        c.setDescription("系统自动收录");
        categoryMapper.insert(c);
        return c.getId();
    }

    private Map<Integer, String> categoryNameMap(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(FruitCategory::getId, FruitCategory::getNameCn, (a, b) -> a));
    }

    /** 读宽高；失败（比如 webp）就返回 null，不影响主流程 */
    private int[] readWidthHeight(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            BufferedImage bi = ImageIO.read(in);
            if (bi != null) {
                return new int[]{bi.getWidth(), bi.getHeight()};
            }
        } catch (Exception e) {
            log.debug("读取图片尺寸失败（不影响识别）：{}", e.getMessage());
        }
        return new int[]{0, 0};
    }

    private String extOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
