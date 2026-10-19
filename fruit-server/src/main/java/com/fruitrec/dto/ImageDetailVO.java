package com.fruitrec.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 详情页：一张图 + 前 5 个候选 */
@Data
public class ImageDetailVO {

    private Long id;
    private String fileName;
    private String url;
    private Integer fileSize;
    private LocalDateTime uploadTime;

    private String modelVersion;
    private Integer costMs;

    /** 最终结果（rank=1） */
    private String topName;
    private Double topConfidence;

    /** 前 5 个候选 */
    private List<Candidate> candidates;

    @Data
    public static class Candidate {
        private Integer rank;
        private String nameCn;
        private String nameEn;
        private Double confidence;
    }
}
