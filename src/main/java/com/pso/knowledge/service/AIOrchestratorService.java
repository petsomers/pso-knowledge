package com.pso.knowledge.service;

import com.pso.knowledge.domain.NoteAnalysis;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class AIOrchestratorService {

    private static final String SYSTEM_PROMPT = """
            You are a knowledge base analyst. Analyze the provided markdown note and extract structured metadata.
            
            Rules:
            - category: MUST be exactly one of: "People", "Projects", "Concepts", or "Stories"
              - Use "People" only if the note is primarily ABOUT a specific person
              - Use "Projects" only if the note is primarily ABOUT a specific project
              - Use "Stories" if the note is primarily a story, anecdote, or personal narrative
              - Use "Concepts" for everything else (meeting notes, ideas, technical concepts, companies, entities, etc.)
            - tags: lowercase, relevant topic tags (max 5)
            - detectedPeople: full names of people MENTIONED in the text (e.g. "Jan de Vries"). Only real person names, not roles or titles.
            - detectedProjects: project names MENTIONED in the text. Only named projects, not generic concepts.
            - detectedStories: titles of stories or anecdotes found in the text. A story is a narrative about an event, experience, or memorable moment. Give each a short descriptive title.
            - subjectPerson: if category is "People", the full name of the person this note is ABOUT. Otherwise null.
            - subjectName: the name of the subject this note is about. This becomes the filename.
              - For "People": the person's full name
              - For "Projects": the project name
              - For "Concepts": a concise concept title (e.g. "Domain Driven Design", "Kubernetes Networking", "Soccer Club X")
              - For "Stories": a short descriptive title for the story (e.g. "The Camping Trip Disaster", "First Day at Sony")
            - summarySentence: one sentence summary of the note's content
            
            Respond ONLY with valid JSON matching the specified format. No markdown, no explanation.
            """;

    private final ChatClient chatClient;
    private final BeanOutputConverter<NoteAnalysis> converter = new BeanOutputConverter<>(NoteAnalysis.class);

    public AIOrchestratorService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    private static final String MERGE_PROMPT = """
            You are a knowledge base editor. You will receive an EXISTING markdown file and NEW information about the same subject.
            
            Your task: produce a single, merged markdown file that integrates the new information into the existing content.
            
            Rules:
            - Preserve all existing information
            - Add only genuinely new facts from the new information
            - Do NOT duplicate information that is already present
            - Keep the same markdown structure and style as the existing file
            - Keep any existing YAML frontmatter at the top
            - If the existing file is just a stub, expand it with the new information
            - Output ONLY the final merged markdown. No explanation.
            """;

    public String mergeContent(String existing, String newInfo) {
        return chatClient.prompt()
                .system(MERGE_PROMPT)
                .user("EXISTING FILE:\n" + existing + "\n\nNEW INFORMATION:\n" + newInfo)
                .call()
                .content();
    }

    public NoteAnalysis analyze(String content) {
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(content + "\n\n" + converter.getFormat())
                .call()
                .content();
        return converter.convert(response);
    }
}
