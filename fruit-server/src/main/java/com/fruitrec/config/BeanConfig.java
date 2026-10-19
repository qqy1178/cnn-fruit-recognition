package com.fruitrec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

    /**
     * 只用 BCrypt 做密码哈希，没引整套 Spring Security。
     * 好处：不用配 SecurityFilterChain，登录鉴权自己用拦截器做，简单可控。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
