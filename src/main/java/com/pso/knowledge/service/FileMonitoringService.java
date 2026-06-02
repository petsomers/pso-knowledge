package com.pso.knowledge.service;

import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class FileMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(FileMonitoringService.class);
    private static final int STABILITY_WAIT_MS = 500;
    private static final int MAX_RETRIES = 10;

    private final Path inboxPath;
    private final NoteProcessingOrchestrator orchestrator;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FileMonitoringService(VaultProperties vault, NoteProcessingOrchestrator orchestrator) {
        this.inboxPath = Path.of(vault.path()).resolve("Inbox");
        this.orchestrator = orchestrator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startWatching() throws IOException {
        Files.createDirectories(inboxPath);
        log.info("Monitoring inbox: {}", inboxPath.toAbsolutePath());
        processExistingFiles();
        executor.submit(this::watchLoop);
    }

    private void processExistingFiles() throws IOException {
        try (var stream = Files.list(inboxPath)) {
            stream.filter(p -> p.toString().endsWith(".md")).forEach(this::processWhenReady);
        }
    }

    private void watchLoop() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            inboxPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            while (!Thread.currentThread().isInterrupted()) {
                var key = watcher.take();
                for (var event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        var file = inboxPath.resolve((Path) event.context());
                        if (file.toString().endsWith(".md")) {
                            processWhenReady(file);
                        }
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Watch service failed", e);
        }
    }

    private void processWhenReady(Path file) {
        try {
            if (Files.notExists(file)) return;
            waitUntilStable(file);
            if (Files.notExists(file)) return;
            orchestrator.process(file);
        } catch (Exception e) {
            log.error("Failed to process {}", file.getFileName(), e);
        }
    }

    private void waitUntilStable(Path file) throws InterruptedException {
        long previousSize = -1;
        for (int i = 0; i < MAX_RETRIES; i++) {
            Thread.sleep(STABILITY_WAIT_MS);
            try {
                long currentSize = Files.size(file);
                if (currentSize == previousSize && currentSize > 0) return;
                previousSize = currentSize;
            } catch (IOException _) {
                // file not yet accessible
            }
        }
    }
}
