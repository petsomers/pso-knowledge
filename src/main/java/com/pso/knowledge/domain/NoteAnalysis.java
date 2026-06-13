package com.pso.knowledge.domain;

import java.util.List;

public record NoteAnalysis(
    String category,
    List<String> tags,
    List<String> detectedPeople,
    List<String> detectedProjects,
    List<String> detectedStories,
    List<String> detectedConcepts,
    String subjectPerson,
    String subjectName,
    String summarySentence
) {}
