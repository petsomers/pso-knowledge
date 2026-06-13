package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import com.pso.knowledge.domain.NoteAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class NoteProcessingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NoteProcessingOrchestrator.class);

    private final AIOrchestratorService aiService;
    private final EntityManagementService entityService;
    private final MarkdownProcessorService markdownService;
    private final IndexService indexService;
    private final Path vaultPath;

    public NoteProcessingOrchestrator(AIOrchestratorService aiService,
                                      EntityManagementService entityService,
                                      MarkdownProcessorService markdownService,
                                      IndexService indexService,
                                      VaultProperties vault) {
        this.aiService = aiService;
        this.entityService = entityService;
        this.markdownService = markdownService;
        this.indexService = indexService;
        this.vaultPath = Path.of(vault.path());
    }

    public void process(Path file) throws IOException {
        log.info("Processing: {}", file.getFileName());

        String content = Files.readString(file);
        NoteAnalysis analysis = aiService.analyze(content);
        log.info("Analysis: category={}, subject={}", analysis.category(), analysis.subjectName());

        entityService.createMissingEntities(analysis.detectedPeople(), analysis.detectedProjects(), analysis.detectedStories(), analysis.detectedConcepts());

        String processed = markdownService.process(content, analysis);

        Path targetFile = vaultPath.resolve(analysis.category()).resolve(analysis.subjectName() + ".md");
        Files.createDirectories(targetFile.getParent());

        if (Files.exists(targetFile)) {
            String existing = Files.readString(targetFile);
            String merged = aiService.mergeContent(existing, processed);
            Files.writeString(targetFile, merged);
        } else {
            Files.writeString(targetFile, processed);
        }

        Files.delete(file);
        indexService.regenerateIndex(targetFile.getParent(), analysis.category());
        log.info("Processed into {}/{}", analysis.category(), analysis.subjectName());
    }
}
