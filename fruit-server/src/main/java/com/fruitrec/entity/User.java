package com.fruitrec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希。⚠️ 永远不要返回给前端 —— 见 UserVO */
    private String password;

    private String nickname;

    private String avatar;

    /** 0=普通用户 1=管理员 */
    private Integer role;

    /** 1=正常 0=禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
