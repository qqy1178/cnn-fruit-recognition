package com.fruitrec.dto;

import lombok.Data;

/** 各类别识别数量统计 —— 答辩演示的图表数据源 */
@Data
public class CategoryStatVO {

    private String nameCn;
    private Long cnt;
}
