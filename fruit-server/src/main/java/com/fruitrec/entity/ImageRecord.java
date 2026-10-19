package com.fruitrec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 图片记录。类名不叫 Image，避免和 java.awt.Image 混淆。 */
@Data
@TableName("image")
public class ImageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String filePath;

    private Integer fileSize;

    private Integer width;

    private Integer height;

    /** 离线批处理的图为 null */
    private Long userId;

    /** 1=用户上传 2=离线批处理 */
    private Integer source;

    private LocalDateTime uploadTime;
}
