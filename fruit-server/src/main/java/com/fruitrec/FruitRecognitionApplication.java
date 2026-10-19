package com.fruitrec;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.fruitrec.mapper")
public class FruitRecognitionApplication {
    public static void main(String[] args) {
        SpringApplication.run(FruitRecognitionApplication.class, args);
    }
}
