package com.modernizeai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ModernizeAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModernizeAiApplication.class, args);
    }
}
