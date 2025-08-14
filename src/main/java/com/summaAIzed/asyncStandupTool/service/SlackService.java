package com.summaAIzed.asyncStandupTool.service;

import com.summaAIzed.asyncStandupTool.model.Member;
import com.summaAIzed.asyncStandupTool.model.StandupEntry;
import com.summaAIzed.asyncStandupTool.repository.MemberRepository;
import com.summaAIzed.asyncStandupTool.repository.StandupEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SlackService {

    private static WebClient webClient;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private StandupEntryRepository standupEntryRepository;
    private final String botToken;

    public SlackService(
            @Value("${slack.bot.token}") String botToken,
            WebClient.Builder builder
    ) {
        this.botToken = botToken;
        this.webClient = builder
                .baseUrl("https://slack.com/api")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .build();
    }

    // Build modal JSON
    public Map<String, Object> buildStandupModal() {
        return Map.of(
                "type", "modal",
                "callback_id", "standup_modal",
                "title", Map.of("type", "plain_text", "text", "Daily Standup"),
                "submit", Map.of("type", "plain_text", "text", "Submit"),
                "blocks", List.of(
                        Map.of(
                                "type", "input",
                                "block_id", "yesterday",
                                "element", Map.of("type", "plain_text_input", "action_id", "input"),
                                "label", Map.of("type", "plain_text", "text", "What did you work on yesterday?")
                        ),
                        Map.of(
                                "type", "input",
                                "block_id", "today",
                                "element", Map.of("type", "plain_text_input", "action_id", "input"),
                                "label", Map.of("type", "plain_text", "text", "What are you working on today?")
                        ),
                        Map.of(
                                "type", "input",
                                "block_id", "blockers",
                                "element", Map.of("type", "plain_text_input", "action_id", "input", "multiline", true),
                                "label", Map.of("type", "plain_text", "text", "Any blockers?")
                        )
                )
        );
    }

    // Open modal via Slack API
    public void openModal(String triggerId) {
        Map<String, Object> modal = buildStandupModal();

        webClient.post()
                .uri("https://slack.com/api/views.open")
                .header("Authorization", "Bearer " + botToken)
                .bodyValue(Map.of(
                        "trigger_id", triggerId,
                        "view", modal
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> System.out.println("Modal response: " + response));
    }

    private void saveStandupEntry(String slackUserId, String displayName, String yesterday, String today, String blockers) {
        // Find or create member
        Member member = memberRepository.findBySlackUserId(slackUserId);
        if (member == null) {
            member = new Member();
            member.setSlackUserId(slackUserId);
            member.setDisplayName(displayName);
            memberRepository.save(member);
        }

        // Save standup entry
        StandupEntry entry = new StandupEntry();
        entry.setMember(member);
        entry.setDate(LocalDate.now());
        entry.setYesterday(yesterday);
        entry.setToday(today);
        entry.setBlockers(blockers);

        standupEntryRepository.save(entry);
    }

    /** Call Slack chat.postMessage */
    // SlackService.java
    public static String postMessage(String channelOrUserId, String text) {
        String resp = webClient.post()
                .uri("/chat.postMessage")
                .bodyValue(Map.of("channel", channelOrUserId, "text", text, "mrkdwn", true))
                .retrieve()
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class).map(body ->
                                new RuntimeException("Slack HTTP " + r.statusCode() + ": " + body)))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

        if (resp == null || !resp.contains("\"ok\":true")) {
            throw new RuntimeException("Slack API returned non-ok: " + resp);
        }
        return resp;
    }

    public void deleteAllEntries(){
        standupEntryRepository.deleteAll();
    }
}
