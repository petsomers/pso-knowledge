package com.pso.knowledge.service;

import com.pso.knowledge.domain.NoteAnalysis;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MarkdownProcessorService {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\n.*?\\n---\\s*\\n", Pattern.DOTALL);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```.*?```", Pattern.DOTALL);

    public String process(String originalContent, NoteAnalysis analysis) {
        String body = stripFrontmatter(originalContent);
        String linkedBody = insertLinks(body, analysis.detectedPeople(), analysis.detectedProjects());
        return buildFrontmatter(analysis) + linkedBody;
    }

    private String stripFrontmatter(String content) {
        var matcher = FRONTMATTER_PATTERN.matcher(content);
        return matcher.find() ? content.substring(matcher.end()) : content;
    }

    private String insertLinks(String body, List<String> people, List<String> projects) {
        List<String> entities = Stream.concat(people.stream(), projects.stream())
                .sorted((a, b) -> Integer.compare(b.length(), a.length())) // longest first to avoid partial matches
                .toList();

        // Split body into code blocks and non-code segments
        var codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(body);
        var result = new StringBuilder();
        int lastEnd = 0;

        while (codeBlockMatcher.find()) {
            String segment = body.substring(lastEnd, codeBlockMatcher.start());
            result.append(linkEntitiesInSegment(segment, entities));
            result.append(codeBlockMatcher.group()); // preserve code block as-is
            lastEnd = codeBlockMatcher.end();
        }
        result.append(linkEntitiesInSegment(body.substring(lastEnd), entities));
        return result.toString();
    }

    private String linkEntitiesInSegment(String segment, List<String> entities) {
        for (String entity : entities) {
            // Match entity name not already inside [[ ]]
            String escaped = Pattern.quote(entity);
            segment = segment.replaceAll(
                    "(?<!\\[\\[)" + escaped + "(?!\\]\\]|[^\\[]*\\]\\])",
                    "[[" + entity + "]]"
            );
        }
        return segment;
    }

    private String buildFrontmatter(NoteAnalysis analysis) {
        String tags = analysis.tags().stream()
                .map(t -> "  - " + t)
                .collect(Collectors.joining("\n"));

        return """
                ---
                category: %s
                summary: "%s"
                tags:
                %s
                ---
                """.formatted(analysis.category(), analysis.summarySentence(), tags);
    }
}
