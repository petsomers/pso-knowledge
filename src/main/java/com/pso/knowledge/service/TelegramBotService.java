package com.pso.knowledge.service;

import com.pso.knowledge.config.TelegramProperties;
import com.pso.knowledge.config.VaultProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TelegramBotService implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final DateTimeFormatter FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private final TelegramProperties telegramProperties;
    private final Path inboxPath;
    private final KnowledgeBaseSearchService searchService;
    private final TelegramClient telegramClient;

    public TelegramBotService(TelegramProperties telegramProperties, VaultProperties vault,
                              KnowledgeBaseSearchService searchService) {
        this.telegramProperties = telegramProperties;
        this.inboxPath = Path.of(vault.path()).resolve("Inbox");
        this.searchService = searchService;
        this.telegramClient = new OkHttpTelegramClient(telegramProperties.token());
    }

    @Override
    public String getBotToken() {
        return telegramProperties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update);
            }
        }
    }

    private void handleMessage(Update update) {
        long userId = update.getMessage().getFrom().getId();
        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (!telegramProperties.allowedUserIds().contains(userId)) {
            sendReply(chatId, "⛔ Unauthorized");
            return;
        }

        if (text.startsWith("?")) {
            String question = text.substring(1).trim();
            String answer = searchService.search(question);
            sendReply(chatId, answer);
        } else {
            storeMessage(chatId, text);
        }
    }

    private void storeMessage(long chatId, String text) {
        try {
            Files.createDirectories(inboxPath);
            String filename = LocalDateTime.now().format(FILENAME_FORMAT) + ".md";
            Files.writeString(inboxPath.resolve(filename), text);
            sendReply(chatId, "✅ Info received");
        } catch (IOException e) {
            log.error("Failed to write inbox file", e);
            sendReply(chatId, "❌ Failed to store message");
        }
    }

    private void sendReply(long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (Exception e) {
            log.error("Failed to send reply", e);
        }
    }
}
