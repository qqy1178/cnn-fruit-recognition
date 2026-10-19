package com.fruitrec.service;

import com.fruitrec.common.BusinessException;
import com.fruitrec.dto.LoginResponse;
import com.fruitrec.entity.User;
import com.fruitrec.mapper.UserMapper;
import com.fruitrec.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试（T01–T04）。
 * 依赖全部 mock，不连数据库、不起 Spring 容器。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private UserService userService;

    private User user(int status) {
        User u = new User();
        u.setId(1L);
        u.setUsername("smoke1019");
        u.setPassword("$2a$10$hashed");
        u.setNickname("测试用户");
        u.setRole(0);
        u.setStatus(status);
        return u;
    }

    @Test
    @DisplayName("T01 正确密码 → 返回 token 与用户信息")
    void login_success() {
        when(userMapper.selectOne(any())).thenReturn(user(1));
        when(passwordEncoder.matches("123456", "$2a$10$hashed")).thenReturn(true);
        when(jwtUtil.generate(1L, "smoke1019", 0)).thenReturn("fake-token");

        LoginResponse resp = userService.login("smoke1019", "123456");

        assertEquals("fake-token", resp.getToken());
        assertEquals("smoke1019", resp.getUser().getUsername());
        verify(jwtUtil).generate(1L, "smoke1019", 0);
    }

    @Test
    @DisplayName("T02 密码错误 → 401，且不签发 token")
    void login_wrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(1));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.login("smoke1019", "wrong"));

        assertEquals(401, e.getCode());
        verify(jwtUtil, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("T03 用户不存在 → 与密码错误同一提示（防用户名枚举）")
    void login_userNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.login("nobody", "123456"));

        assertEquals(401, e.getCode());
        assertEquals("用户名或密码错误", e.getMessage());
    }

    @Test
    @DisplayName("T04 账号被禁用 → 403")
    void login_disabled() {
        when(userMapper.selectOne(any())).thenReturn(user(0));
        when(passwordEncoder.matches("123456", "$2a$10$hashed")).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.login("smoke1019", "123456"));

        assertEquals(403, e.getCode());
    }

    @Test
    @DisplayName("T05 注册重名 → 400，且不写库")
    void register_duplicate() {
        when(userMapper.selectOne(any())).thenReturn(user(1));

        BusinessException e = assertThrows(BusinessException.class,
                () -> userService.register("smoke1019", "123456", "昵称"));

        assertEquals(400, e.getCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("T06 注册成功 → 密码加密后写入，普通用户角色")
    void register_success() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$newhash");

        userService.register("newuser", "123456", null);

        verify(userMapper).insert(argThat(u ->
                "$2a$10$newhash".equals(u.getPassword())
                        && "newuser".equals(u.getNickname())   // 昵称为空时默认用用户名
                        && u.getRole() == 0
                        && u.getStatus() == 1));
    }
}
