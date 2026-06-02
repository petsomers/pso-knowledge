# PSO Knowledge

AI-driven personal knowledge base organizer built with Spring Boot and Spring AI. It monitors an Obsidian vault's `/Inbox` folder, analyzes new markdown notes using a local LLM (Ollama), and automatically organizes them into categories: **People**, **Projects**, **Concepts**, and **Stories**.

## What it does

1. **Watches** the `Inbox/` folder for new `.md` files
2. **Analyzes** content via Ollama to extract metadata (category, tags, people, projects, stories)
3. **Creates stubs** for detected entities that don't exist yet
4. **Links** entity mentions as `[[Obsidian Links]]` in the note body
5. **Moves** the processed note to its target category folder
6. **Sanitizes** metadata nightly (3 AM) and cross-checks consistency weekly (Sunday 4 AM)
7. **Regenerates** `_index.md` files per category after each change

## Prerequisites

- Java 25+
- [Ollama](https://ollama.ai) running locally with the `gemma4:31b-cloud` model pulled
- Maven 3.9+

## Build

```bash
mvn clean package
```

## Run

Start Ollama first:

```bash
ollama serve
```

Then run the application:

```bash
mvn spring-boot:run
```

Or via the packaged jar:

```bash
java -jar target/pso-knowledge-0.0.1-SNAPSHOT.jar
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
vault:
  path: "./MyKnowledgeBase"    # Path to your Obsidian vault

spring:
  ai:
    ollama:
      base-url: "http://localhost:11434"
      chat:
        options:
          model: "gemma4:31b-cloud"
          temperature: 0.1
```

## API Endpoints

- `POST /api/sanitize` — trigger metadata sanitization manually
- `POST /api/sanitize/full` — trigger full sanitize with cross-check
