package com.fruitrec.dto;

import com.fruitrec.entity.ImageRecord;
import lombok.Data;

import java.time.LocalDateTime;

/** 列表页用的图片信息（不含候选详情） */
@Data
public class ImageVO {

    private Long id;
    private String fileName;
    /** 前端直接拿去 <img :src="url"> */
    private String url;
    private Integer fileSize;
    private LocalDateTime uploadTime;
    /** 最终识别结果中文名；识别失败为 null */
    private String topName;
    private Double topConfidence;

    public static ImageVO of(ImageRecord img, String relativePath, String topName, Double topConfidence) {
        ImageVO vo = new ImageVO();
        vo.id = img.getId();
        vo.fileName = img.getFileName();
        vo.url = "/uploads/" + relativePath;
        vo.fileSize = img.getFileSize();
        vo.uploadTime = img.getUploadTime();
        vo.topName = topName;
        vo.topConfidence = topConfidence;
        return vo;
    }
}
