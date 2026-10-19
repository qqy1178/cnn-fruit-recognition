package com.fruitrec.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fruitrec.common.Result;
import com.fruitrec.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录拦截器。没引 Spring Security，鉴权就这几行，简单可控。
 * 登录后把 userId / role 放进 request attribute，Controller 直接取。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLE = "role";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        // 浏览器跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response, 401, "未登录");
        }

        try {
            Claims claims = jwtUtil.parse(header.substring(7));
            request.setAttribute(ATTR_USER_ID, Long.valueOf(claims.getSubject()));
            request.setAttribute(ATTR_ROLE, claims.get("role", Integer.class));
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return reject(response, 401, "登录已过期，请重新登录");
        }
    }

    /**
     * ⚠️ 这里必须自己写响应，**不能 throw BusinessException**。
     *
     * 原因：拦截器 preHandle 里抛的异常**不走 @RestControllerAdvice**
     * （那是给 Controller 层用的），会直接冒到 Servlet 容器，
     * 前端收到一页 500 的 HTML 错误页，而不是 {"code":401,...}。
     * 这是个很容易踩的坑，答辩时也能讲。
     */
    private boolean reject(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, msg)));
        return false;
    }
}
