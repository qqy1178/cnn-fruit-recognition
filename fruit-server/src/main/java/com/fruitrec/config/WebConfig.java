package com.fruitrec.config;

import com.fruitrec.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final FruitProperties props;

    /** 放行登录/注册，其余接口都要 token */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }

    /** 允许 Vue 开发服务器（5173）跨域访问 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 把上传目录映射成静态资源，前端能直接用 /uploads/xxx.jpg 显示图片 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = props.getUploadDir();
        if (dir != null && !dir.isBlank()) {
            String location = "file:" + dir.replace("\\", "/") + "/";
            registry.addResourceHandler("/uploads/**").addResourceLocations(location);
            new File(dir).mkdirs();
        }
    }
}
