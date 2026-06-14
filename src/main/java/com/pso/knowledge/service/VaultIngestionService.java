package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class VaultIngestionService {

    private static final Logger log = LoggerFactory.getLogger(VaultIngestionService.class);
    private static final List<String> CATEGORIES = List.of("People", "Projects", "Concepts", "Stories");

    private final VectorStore vectorStore;
    private final Path vaultPath;
    private final Path vectorStoreFile;

    public VaultIngestionService(VectorStore vectorStore, VaultProperties vault, Path vectorStoreFile) {
        this.vectorStore = vectorStore;
        this.vaultPath = Path.of(vault.path());
        this.vectorStoreFile = vectorStoreFile;
    }

    public void ingestAll() {
        log.info("Starting full vault ingestion into vector store...");
        for (String category : CATEGORIES) {
            Path dir = vaultPath.resolve(category);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.toString().endsWith(".md")).forEach(this::ingestFile);
            } catch (IOException e) {
                log.warn("Failed to list {}", dir, e);
            }
        }
        save();
        log.info("Vault ingestion complete.");
    }

    public void ingestFile(Path file) {
        try {
            String content = Files.readString(file);
            String fileName = file.getFileName().toString();
            String category = file.getParent().getFileName().toString();
            String id = category + "/" + fileName;

            // Remove existing document for this file (update scenario)
            vectorStore.delete(List.of(id));

            Document doc = new Document(id, content, Map.of("category", category, "filename", fileName));
            vectorStore.add(List.of(doc));
            log.debug("Ingested: {}", id);
        } catch (IOException e) {
            log.warn("Failed to ingest {}", file, e);
        }
    }

    public void ingestFileAndSave(Path file) {
        ingestFile(file);
        save();
    }

    public void save() {
        ((SimpleVectorStore) vectorStore).save(vectorStoreFile.toFile());
    }
}
