package com.fruitrec.controller;

import com.fruitrec.common.Result;
import com.fruitrec.dto.LoginRequest;
import com.fruitrec.dto.LoginResponse;
import com.fruitrec.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** POST /api/auth/login  {"username":"admin","password":"123456"} */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return Result.ok(userService.login(req.getUsername(), req.getPassword()));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid LoginRequest req) {
        userService.register(req.getUsername(), req.getPassword(), req.getNickname());
        return Result.ok();
    }
}
