package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class EntityManagementService {

    private static final Logger log = LoggerFactory.getLogger(EntityManagementService.class);

    private final Path personenPath;
    private final Path projectenPath;
    private final Path storiesPath;
    private final Path conceptsPath;

    public EntityManagementService(VaultProperties vault) {
        this.personenPath = Path.of(vault.path()).resolve("People");
        this.projectenPath = Path.of(vault.path()).resolve("Projects");
        this.storiesPath = Path.of(vault.path()).resolve("Stories");
        this.conceptsPath = Path.of(vault.path()).resolve("Concepts");
    }

    public void createMissingEntities(List<String> people, List<String> projects, List<String> stories, List<String> concepts) throws IOException {
        Files.createDirectories(personenPath);
        Files.createDirectories(projectenPath);
        Files.createDirectories(storiesPath);
        Files.createDirectories(conceptsPath);

        for (String name : people) {
            createIfMissing(personenPath.resolve(name + ".md"), personStub(name));
        }
        for (String name : projects) {
            createIfMissing(projectenPath.resolve(name + ".md"), projectStub(name));
        }
        for (String name : stories) {
            createIfMissing(storiesPath.resolve(name + ".md"), storyStub(name));
        }
        for (String name : concepts) {
            createIfMissing(conceptsPath.resolve(name + ".md"), conceptStub(name));
        }
    }

    private void createIfMissing(Path file, String content) throws IOException {
        if (Files.notExists(file)) {
            Files.writeString(file, content);
            log.info("Created stub: {}", file.getFileName());
        }
    }

    private String personStub(String name) {
        return """
                ---
                type: persoon
                tags: [automatisch-aangemaakt]
                ---
                # %s
                """.formatted(name);
    }

    private String projectStub(String name) {
        return """
                ---
                type: project
                tags: [automatisch-aangemaakt]
                ---
                # %s
                """.formatted(name);
    }

    private String storyStub(String name) {
        return """
                ---
                type: story
                tags: [automatisch-aangemaakt]
                ---
                # %s
                """.formatted(name);
    }

    private String conceptStub(String name) {
        return """
                ---
                type: concept
                tags: [automatisch-aangemaakt]
                ---
                # %s
                """.formatted(name);
    }
}
