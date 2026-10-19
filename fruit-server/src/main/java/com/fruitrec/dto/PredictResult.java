package com.fruitrec.dto;

import lombok.Data;

import java.util.List;

/**
 * 对应 Python 端 predict_json.py 输出的 JSON。
 * 字段是下划线命名（cost_ms / class_index / name_cn），
 * 靠 ObjectMapper 的 SNAKE_CASE 策略自动映射，见 PythonService。
 */
@Data
public class PredictResult {

    private Boolean success;

    private String file;

    private String model;

    /** 下划线 cost_ms */
    private Integer costMs;

    /** 失败时的原因；success=true 时为 null */
    private String error;

    private List<Item> results;

    @Data
    public static class Item {
        private Integer rank;
        /** 下划线 class_index —— 用它去 fruit_category 表查中文名 */
        private Integer classIndex;
        private String nameEn;
        private String nameCn;
        private Double confidence;
    }
}
