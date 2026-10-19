package com.fruitrec.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 application.yml 里的 fruit.* 配置。
 * 好处：路径、超时时间不写死在 Java 代码里，换机器只改 yml。
 */
@Data
@ConfigurationProperties(prefix = "fruit")
public class FruitProperties {

    private Python python = new Python();
    private Jwt jwt = new Jwt();
    private String uploadDir;

    @Data
    public static class Python {
        private String exe = "python";
        private String script;
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Jwt {
        private String secret;
        private int expireHours = 24;
    }
}
