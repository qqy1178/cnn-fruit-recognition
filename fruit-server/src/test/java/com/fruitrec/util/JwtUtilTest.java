package com.fruitrec.util;

import com.fruitrec.config.FruitProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具测试（T05）：生成 → 解析 往返一致；篡改后解析失败。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        FruitProperties props = new FruitProperties();
        props.getJwt().setSecret("fruit-recognition-test-secret-key-32b!");
        props.getJwt().setExpireHours(1);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    @DisplayName("生成后解析，subject/username/role 一致")
    void generate_then_parse() {
        String token = jwtUtil.generate(7L, "smoke1019", 1);

        Claims claims = jwtUtil.parse(token);

        assertEquals("7", claims.getSubject());
        assertEquals("smoke1019", claims.get("username", String.class));
        assertEquals(1, claims.get("role", Integer.class));
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    @DisplayName("被篡改的 token → 抛 JwtException")
    void tampered_token_rejected() {
        String token = jwtUtil.generate(1L, "u", 0);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class, () -> jwtUtil.parse(tampered));
    }
}
