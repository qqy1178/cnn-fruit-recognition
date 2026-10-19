package com.fruitrec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 水果类别字典：模型输出的数字索引 ↔ 中文名的桥梁 */
@Data
@TableName("fruit_category")
public class FruitCategory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String nameCn;

    private String nameEn;

    /** 模型输出层索引，必须与训练时 ImageFolder 的类别顺序一致 */
    private Integer classIndex;

    private String description;

    private String coverImage;

    private LocalDateTime createTime;
}
