package com.pso.knowledge.config;

import com.pso.knowledge.service.VaultIngestionService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class VectorStoreConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel, VaultProperties vault) {
        var store = SimpleVectorStore.builder(embeddingModel).build();
        Path storeFile = Path.of(vault.path()).resolve(".vectorstore.json");
        if (Files.exists(storeFile)) {
            store.load(storeFile.toFile());
        }
        return store;
    }

    @Bean
    Path vectorStoreFile(VaultProperties vault) {
        return Path.of(vault.path()).resolve(".vectorstore.json");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnFirstRun(ApplicationReadyEvent event) {
        var vault = event.getApplicationContext().getBean(VaultProperties.class);
        Path storeFile = Path.of(vault.path()).resolve(".vectorstore.json");
        if (!Files.exists(storeFile)) {
            event.getApplicationContext().getBean(VaultIngestionService.class).ingestAll();
        }
    }
}
