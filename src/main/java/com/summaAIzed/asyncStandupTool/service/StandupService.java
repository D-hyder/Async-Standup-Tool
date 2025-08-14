package com.summaAIzed.asyncStandupTool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.summaAIzed.asyncStandupTool.model.Member;
import com.summaAIzed.asyncStandupTool.model.StandupEntry;
import com.summaAIzed.asyncStandupTool.repository.MemberRepository;
import com.summaAIzed.asyncStandupTool.repository.StandupEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;

@Service
@Slf4j
public class StandupService {

    private final MemberRepository memberRepo;
    private final StandupEntryRepository entryRepo;
    private final WebClient webClient;
    private final StandupEntryRepository standupEntryRepository;
    private final SlackService slackService;
    private final AiSummaryService aiSummaryService;

    @Value("${slack.bot.token}")
    private String botToken;

    @Value("${slack.default-channel-id:}")
    private String defaultChannelId;

    public StandupService(MemberRepository memberRepo,
                          StandupEntryRepository entryRepo,
                          WebClient.Builder builder, StandupEntryRepository standupEntryRepository, SlackService slackService, AiSummaryService aiSummaryService) {
        this.memberRepo = memberRepo;
        this.entryRepo = entryRepo;
        this.webClient = builder
                .baseUrl("https://slack.com/api")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + botToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .build();
        this.standupEntryRepository = standupEntryRepository;
        this.slackService = slackService;
        this.aiSummaryService = aiSummaryService;
    }

    /** Save a standup, DM the user, optionally post to a channel. */
    public StandupEntry saveStandup(String slackUserId, String displayName,
                                    String yesterday, String today, String blockers) {

        Member m = memberRepo.findBySlackUserId(slackUserId);
        if (m == null) {
            m = new Member();
            m.setSlackUserId(slackUserId);
            m.setDisplayName(displayName != null ? displayName : slackUserId);
            memberRepo.save(m);
        }

        StandupEntry entry = new StandupEntry();
        entry.setMember(m);
        entry.setDate(LocalDate.now());
        entry.setYesterday(yesterday);
        entry.setToday(today);
        entry.setBlockers(blockers);
        entryRepo.save(entry);

        // ✅ DM confirmation only
        String dmText = String.format(
                "*Standup received* :white_check_mark:\n*Yesterday:* %s\n*Today:* %s\n*Blockers:* %s",
                n(yesterday), n(today), n(blockers)
        );
        SlackService.postMessage(slackUserId, dmText);   // User ID => our postMessage opens a DM first

        // ❌ remove/disable any channel post here
        return entry;
    }

    /** Daily digest (simple summarization, AI hook point). */
    public void postDailyDigest(LocalDate date) throws JsonProcessingException {
        log.info("=== Daily digest for {} ===", date);

        var entries = entryRepo.findByDate(date);
        log.info("Found {} entries", entries.size());
        if (entries.isEmpty()) { log.info("No entries; skipping."); return; }

        // 🔮 AI summary
        String aiSummary = aiSummaryService.summarize(entries);

        String details = entries.stream()
                .map(e -> String.format("• <@%s> — *Yesterday:* %s | *Today:* %s | *Blockers:* %s",
                        e.getMember().getSlackUserId(), n(e.getYesterday()), n(e.getToday()), n(e.getBlockers())))
                .collect(java.util.stream.Collectors.joining("\n"));

        String msg = "*Daily Standup — " + date + "*\n\n:crystal_ball: *AI Summary*\n" +
                aiSummary + "\n\n*Details*\n" + details;

        slackService.postMessage(defaultChannelId, msg); // this already throws on failure
    }



    private static String n(String s) { return (s == null || s.isBlank()) ? "—" : s; }
}
