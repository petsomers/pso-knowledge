package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class KnowledgeBaseSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSearchService.class);
    private static final List<String> CATEGORIES = List.of("People", "Projects", "Concepts", "Stories");
    private static final String SYSTEM_PROMPT = """
            You are a knowledge base assistant. Answer the question concisely based on the provided context.
            Keep your answer short and suitable for a chat message.
            If you cannot find the answer in the context, say so.
            """;

    private final ChatClient chatClient;
    private final Path vaultPath;

    public KnowledgeBaseSearchService(ChatClient.Builder chatClientBuilder, VaultProperties vault) {
        this.chatClient = chatClientBuilder.build();
        this.vaultPath = Path.of(vault.path());
    }

    public String search(String question) {
        String context = loadVaultContent();
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("CONTEXT:\n" + context + "\n\nQUESTION: " + question)
                .call()
                .content();
    }

    private String loadVaultContent() {
        var sb = new StringBuilder();
        for (String category : CATEGORIES) {
            Path dir = vaultPath.resolve(category);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.toString().endsWith(".md")).sorted().forEach(p -> {
                    try {
                        sb.append("--- ").append(category).append("/").append(p.getFileName()).append(" ---\n");
                        sb.append(Files.readString(p)).append("\n\n");
                    } catch (IOException e) {
                        log.warn("Failed to read {}", p, e);
                    }
                });
            } catch (IOException e) {
                log.warn("Failed to list {}", dir, e);
            }
        }
        return sb.toString();
    }
}
