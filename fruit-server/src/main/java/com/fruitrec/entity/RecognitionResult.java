package com.fruitrec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 识别结果。一张图 5 行，rank 1~5；rank=1 是最终结果。 */
@Data
@TableName("recognition_result")
public class RecognitionResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long imageId;

    private Integer categoryId;

    private BigDecimal confidence;

    /** ⚠️ rank 是 MySQL 8 保留字，必须加反引号 */
    @TableField("`rank`")
    private Integer rank;

    private String modelVersion;

    private Integer costMs;

    private LocalDateTime createTime;
}
