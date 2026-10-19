package com.fruitrec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fruitrec.dto.CategoryStatVO;
import com.fruitrec.entity.RecognitionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecognitionResultMapper extends BaseMapper<RecognitionResult> {

    /**
     * 各类别识别数量（只统计 rank=1 的最终结果）。
     * 注意 `rank` 必须加反引号 —— 它是 MySQL 8 的保留字（窗口函数 RANK()）。
     */
    @Select("""
            SELECT fc.name_cn AS nameCn, COUNT(*) AS cnt
            FROM recognition_result r
            JOIN fruit_category fc ON fc.id = r.category_id
            WHERE r.`rank` = 1
            GROUP BY fc.name_cn
            ORDER BY cnt DESC
            """)
    List<CategoryStatVO> statByCategory();
}
