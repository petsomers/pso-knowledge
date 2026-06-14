package com.pso.knowledge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSearchService.class);
    private static final String SYSTEM_PROMPT = """
            You are a knowledge base assistant. Answer the question concisely based on the provided context.
            Keep your answer short and suitable for a chat message.
            If you cannot find the answer in the context, say so.
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public KnowledgeBaseSearchService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public String search(String question) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(5).build());
        String context = results.stream()
                .map(doc -> "--- " + doc.getId() + " ---\n" + doc.getText())
                .collect(Collectors.joining("\n\n"));
        log.debug("Retrieved {} documents for query", results.size());
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("CONTEXT:\n" + context + "\n\nQUESTION: " + question)
                .call()
                .content();
    }
}
