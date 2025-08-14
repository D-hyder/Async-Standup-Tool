package com.summaAIzed.asyncStandupTool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summaAIzed.asyncStandupTool.model.StandupEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class AiSummaryService {

    private final WebClient client;
    private final String model;

    public AiSummaryService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            WebClient.Builder builder
    ) {
        this.model = model;
        this.client = builder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .build();
    }

    // AiSummaryService.java (replace summarize(...) with this version)
    public String summarize(List<StandupEntry> entries) throws JsonProcessingException {
        if (entries == null || entries.isEmpty()) return "_No standups today to summarize._";

        String facts = entries.stream()
                .sorted(Comparator.comparing(e -> e.getMember().getSlackUserId()))
                .map(e -> String.format(
                        "user:%s name:%s | yesterday:%s | today:%s | blockers:%s",
                        e.getMember().getSlackUserId(),
                        nz(e.getMember().getDisplayName()),
                        trim(nz(e.getYesterday()).replaceAll("\\s*\\d+%%", ""), 280),
                        trim(nz(e.getToday()).replaceAll("\\s*\\d+%%", ""), 280),
                        normBlocker(nz(e.getBlockers()))
                ))
                .collect(java.util.stream.Collectors.joining("\n"));

        String system = """
            You are a standup assistant. Return VALID JSON only (no prose outside JSON).
            Output a single JSON object with keys:
            - "themes": [{ "title": string, "insight": string }]     // 2–3 items max; each field ≤ 12 words
            - "blockers": [{ "title": string, "owners": [string], "suggestion": string }] // ≤ 2 items; each field ≤ 10 words
            - "priorities": [string]                                  // 2–3 items; each ≤ 9 words; include owner @U… and stage/%
            - "encouragement": string                                 // 10–16 words, friendly, tied to today
            Hard limits:
            - Entire JSON ≤ 180 tokens.
            - Omit empty sections (use [] or "").
            - Do not repeat input verbatim; be concise.
            """;


        Map<String,Object> body = new HashMap<>(Map.of(
                "model", model,                               // "gpt-5-nano" or "gpt-5-mini" (both OK)
                "max_completion_tokens", 500,                            // sane cap
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role","system","content", system),
                        Map.of("role","user","content",
                                "Summarize today's standups into that JSON using these lines (one per person):\n" + facts)
                )
        ));

        String resp = client.post().uri("/chat/completions")
                .bodyValue(body).retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

        log.info("OpenAI raw response: {}", resp);

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(resp);
        JsonNode choice0 = root.path("choices").path(0);
        String finish = choice0.path("finish_reason").asText("");
        String content = choice0.path("message").path("content").asText("");

        if (content.isBlank() && "length".equals(finish)) {
            // Retry once with an even more compact schema
            log.warn("AI summary truncated at {} tokens; retrying compact schema.", root.path("usage").path("completion_tokens").asInt());

            String compactSystem = """
    You are a standup assistant. Return VALID JSON only.
    Output: {"themes":[{"title":string,"insight":string}],"blockers":[{"title":string,"owners":[string],"suggestion":string}],"priorities":[string],"encouragement":string}
    Hard limits: total ≤ 120 tokens; at most 2 themes, 1 blocker, 2 priorities; each value ≤ 8 words.
    """;

            body.put("messages", List.of(
                    Map.of("role","system","content", compactSystem),
                    Map.of("role","user","content",
                            "Summarize today's standups into that JSON. Lines:\n" + facts)
            ));
            body.put("max_tokens", 350);

            resp = client.post().uri("/chat/completions")
                    .bodyValue(body).retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.info("OpenAI compact response: {}", resp);
            root = om.readTree(resp);
            content = root.path("choices").path(0).path("message").path("content").asText("");
        }

        if (content.isBlank()) return "(AI summary unavailable)";
        JsonNode json = om.readTree(content);
        return renderSlackMarkdown(json);
    }


    private static String normBlocker(String s) {
        String x = s.trim().toLowerCase();
        if (x.isBlank() || x.equals("none") || x.equals("n/a") || x.equals("no") || x.equals("—") || x.equals("-"))
            return "";
        return s;
    }


    // Slack-friendly formatter
    private String renderSlackMarkdown(JsonNode j) {
        StringBuilder sb = new StringBuilder();

        // THEMES
        JsonNode themes = j.path("themes");
        if (themes.isArray() && themes.size() > 0) {
            for (JsonNode t : themes) {
                String title = t.path("title").asText("").trim();
                String insight = t.path("insight").asText("").trim();
                if (!title.isBlank() && !insight.isBlank()) {
                    sb.append("- *").append(title).append("*: ").append(insight).append("\n");
                }
            }
            sb.append("\n");
        }

        // BLOCKERS (skip entirely if empty)
        JsonNode blockers = j.path("blockers");
        List<String> blockerLines = new ArrayList<>();
        if (blockers.isArray()) {
            for (JsonNode b : blockers) {
                String title = b.path("title").asText("").trim();
                String suggestion = b.path("suggestion").asText("").trim();

                // owners are optional
                List<String> owners = new ArrayList<>();
                if (b.path("owners").isArray()) {
                    b.path("owners").forEach(o -> {
                        String id = o.asText("").trim();
                        if (!id.isEmpty()) owners.add("<@" + id + ">");
                    });
                }

                // Skip garbage/empty titles
                if (title.isBlank()) continue;

                String who = owners.isEmpty() ? "" : " — " + String.join(", ", owners);
                blockerLines.add("• " + title + who + (suggestion.isBlank() ? "" : "\n  ↳ Suggestion: " + suggestion));
            }
        }
        if (!blockerLines.isEmpty()) {
            sb.append("*Blockers*\n");
            blockerLines.forEach(line -> sb.append(line).append("\n"));
            sb.append("\n");
        }

        // PRIORITIES
        JsonNode priorities = j.path("priorities");
        List<String> prios = new ArrayList<>();
        if (priorities.isArray()) {
            priorities.forEach(p -> {
                String v = p.asText("").trim();
                if (!v.isBlank()) prios.add(v);
            });
        }
        if (!prios.isEmpty()) {
            sb.append("*Top Priorities*\n");
            for (int i = 0; i < prios.size(); i++) {
                sb.append(i + 1).append(". ").append(prios.get(i)).append("\n");
            }
            sb.append("\n");
        }

        // ENCOURAGEMENT
        String encouragement = j.path("encouragement").asText("").trim();
        if (!encouragement.isBlank()) {
            sb.append("_").append(encouragement).append("_");
        }

        return sb.toString().trim();
    }


    private static String nz(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static String trim(String s, int max) {
        if (s == null) return "—";
        s = s.trim();
        return (s.length() <= max) ? s : s.substring(0, max - 1) + "…";
    }
}
