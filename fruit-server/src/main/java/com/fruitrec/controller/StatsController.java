package com.fruitrec.controller;

import com.fruitrec.common.Result;
import com.fruitrec.dto.CategoryStatVO;
import com.fruitrec.mapper.RecognitionResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final RecognitionResultMapper resultMapper;

    /** 各类别识别数量 —— 前端拿去画柱状图/饼图 */
    @GetMapping("/category")
    public Result<List<CategoryStatVO>> byCategory() {
        return Result.ok(resultMapper.statByCategory());
    }
}
