package com.fruitrec.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitrec.common.BusinessException;
import com.fruitrec.dto.LoginResponse;
import com.fruitrec.dto.UserVO;
import com.fruitrec.entity.User;
import com.fruitrec.mapper.UserMapper;
import com.fruitrec.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String username, String rawPassword) {
        User u = findByUsername(username);
        // ⚠️ 用户不存在和密码错误返回同一句话，避免被枚举出哪些用户名存在
        if (u == null || !passwordEncoder.matches(rawPassword, u.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        return new LoginResponse(token, UserVO.from(u));
    }

    public void register(String username, String rawPassword, String nickname) {
        if (findByUsername(username) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));   // 存哈希，不存明文
        u.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        u.setRole(0);
        u.setStatus(1);
        userMapper.insert(u);
    }

    public User findById(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return u;
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
