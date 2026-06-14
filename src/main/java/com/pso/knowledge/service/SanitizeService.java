package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
public class SanitizeService {

    private static final Logger log = LoggerFactory.getLogger(SanitizeService.class);

    private static final String SANITIZE_PROMPT = """
            You are a metadata sanitizer for a markdown knowledge base. You will receive a markdown file.
            
            Your task: clean up and standardize the YAML frontmatter metadata. Rules:
            - Ensure all tags are lowercase, deduplicated, and relevant
            - Remove empty or meaningless tags
            - Fix formatting issues in the frontmatter (proper YAML syntax)
            - Ensure the summary is a single clear sentence
            - Ensure category is exactly one of: "People", "Projects", "Concepts", or "Stories"
            - Do NOT modify the body content below the frontmatter
            - If the file has no frontmatter, leave it unchanged
            - Output ONLY the complete file (frontmatter + body). No explanation.
            """;

    private static final String CROSS_CHECK_PROMPT = """
            You are a knowledge base consistency editor. You will receive ALL files from a folder in a knowledge base.
            
            Your task: ensure consistent tagging, terminology, and cross-references across all files.
            
            Rules:
            - Normalize tags: use the same tag name for the same concept across all files (e.g. don't mix "dev" and "developer")
            - Ensure people working on the same project reference each other where relevant
            - Fix conflicting information (dates, roles, descriptions) — prefer the most detailed version
            - Keep each file's structure intact
            - Do NOT invent new information, only harmonize what exists
            
            Output ALL files separated by the marker "===FILE: {filename}===" on its own line before each file.
            Output ONLY the files. No explanation.
            """;

    private final ChatClient chatClient;
    private final IndexService indexService;
    private final VaultIngestionService ingestionService;
    private final Path vaultPath;

    public SanitizeService(ChatClient.Builder chatClientBuilder, IndexService indexService,
                           VaultIngestionService ingestionService, VaultProperties vault) {
        this.chatClient = chatClientBuilder.build();
        this.indexService = indexService;
        this.ingestionService = ingestionService;
        this.vaultPath = Path.of(vault.path());
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledSanitize() {
        sanitize();
    }

    @Scheduled(cron = "0 0 4 * * SUN")
    public void scheduledFullSanitize() {
        fullSanitize();
    }

    public int fullSanitize() {
        int count = sanitize();
        List<Path> dirs = List.of(
                vaultPath.resolve("People"),
                vaultPath.resolve("Projects"),
                vaultPath.resolve("Concepts"),
                vaultPath.resolve("Stories")
        );
        for (Path dir : dirs) {
            if (Files.isDirectory(dir)) {
                crossCheckDirectory(dir);
            }
        }
        indexService.regenerateAll();
        ingestionService.save();
        log.info("Full sanitize with cross-check completed");
        return count;
    }

    public int sanitize() {
        List<Path> dirs = List.of(
                vaultPath.resolve("People"),
                vaultPath.resolve("Projects"),
                vaultPath.resolve("Concepts"),
                vaultPath.resolve("Stories")
        );
        int count = 0;
        for (Path dir : dirs) {
            if (Files.isDirectory(dir)) {
                count += sanitizeDirectory(dir);
            }
        }
        log.info("Sanitized {} files", count);
        indexService.regenerateAll();
        ingestionService.save();
        return count;
    }

    private int sanitizeDirectory(Path dir) {
        int count = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.getFileName().toString().equals("_index.md")).toList()) {
                try {
                    String content = Files.readString(file);
                    String sanitized = chatClient.prompt()
                            .system(SANITIZE_PROMPT)
                            .user(content)
                            .call()
                            .content();
                    if (!sanitized.equals(content)) {
                        Files.writeString(file, sanitized);
                        ingestionService.ingestFile(file);
                        log.info("Sanitized: {}", file.getFileName());
                        count++;
                    }
                } catch (Exception e) {
                    log.error("Failed to sanitize {}", file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to list directory {}", dir, e);
        }
        return count;
    }

    private void crossCheckDirectory(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> mdFiles = files.filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.getFileName().toString().equals("_index.md"))
                    .sorted().toList();

            if (mdFiles.size() < 2) return;

            var input = new StringBuilder();
            for (Path file : mdFiles) {
                input.append("===FILE: ").append(file.getFileName()).append("===\n");
                input.append(Files.readString(file)).append("\n");
            }

            String result = chatClient.prompt()
                    .system(CROSS_CHECK_PROMPT)
                    .user(input.toString())
                    .call()
                    .content();

            // Parse output back into individual files
            String[] parts = result.split("===FILE: ");
            for (String part : parts) {
                if (part.isBlank()) continue;
                int endOfName = part.indexOf("===");
                if (endOfName < 0) continue;
                String filename = part.substring(0, endOfName).trim();
                String content = part.substring(endOfName + 3).stripLeading();
                Path filePath = dir.resolve(filename);
                Files.writeString(filePath, content);
                ingestionService.ingestFile(filePath);
            }
            log.info("Cross-checked {} files in {}", mdFiles.size(), dir.getFileName());
        } catch (Exception e) {
            log.error("Cross-check failed for {}", dir, e);
        }
    }
}
