package com.fruitrec.dto;

import com.fruitrec.entity.User;
import lombok.Data;

/**
 * 返回给前端的用户信息。
 * ⚠️ 存在的唯一理由：**绝不能把 password 字段返回出去**。
 *    直接返回 User 实体会把 BCrypt 哈希泄漏给前端（F12 就能看见）。
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;

    public static UserVO from(User u) {
        UserVO vo = new UserVO();
        vo.id = u.getId();
        vo.username = u.getUsername();
        vo.nickname = u.getNickname();
        vo.avatar = u.getAvatar();
        vo.role = u.getRole();
        return vo;
    }
}
