package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class IndexService {

    private static final Logger log = LoggerFactory.getLogger(IndexService.class);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("^summary:\\s*\"?([^\"\\n]+)\"?", Pattern.MULTILINE);

    private final Path vaultPath;

    public IndexService(VaultProperties vault) {
        this.vaultPath = Path.of(vault.path());
    }

    public void regenerateAll() {
        List.of("People", "Projects", "Concepts", "Stories").forEach(folder -> {
            Path dir = vaultPath.resolve(folder);
            if (Files.isDirectory(dir)) {
                regenerateIndex(dir, folder);
            }
        });
    }

    public void regenerateIndex(Path dir, String title) {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> entries = files
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.getFileName().toString().equals("_index.md"))
                    .sorted()
                    .toList();

            var sb = new StringBuilder();
            sb.append("# ").append(title).append("\n\n");

            for (Path entry : entries) {
                String name = entry.getFileName().toString().replace(".md", "");
                String summary = extractSummary(entry);
                sb.append("- [[").append(name).append("]]");
                if (summary != null) {
                    sb.append(" — ").append(summary);
                }
                sb.append("\n");
            }

            Files.writeString(dir.resolve("_index.md"), sb.toString());
            log.info("Regenerated index for {}", title);
        } catch (IOException e) {
            log.error("Failed to generate index for {}", title, e);
        }
    }

    private String extractSummary(Path file) {
        try {
            String content = Files.readString(file);
            Matcher matcher = SUMMARY_PATTERN.matcher(content);
            return matcher.find() ? matcher.group(1).trim() : null;
        } catch (IOException _) {
            return null;
        }
    }
}
