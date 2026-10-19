package com.fruitrec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    /** 前端存起来，之后每个请求带在 Header: Authorization: Bearer <token> */
    private String token;

    private UserVO user;
}
