package com.pso.knowledge;

import com.pso.knowledge.config.VaultProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(VaultProperties.class)
@EnableScheduling
public class PsoKnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsoKnowledgeApplication.class, args);
    }
}
