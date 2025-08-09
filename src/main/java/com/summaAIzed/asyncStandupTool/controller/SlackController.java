package com.summaAIzed.asyncStandupTool.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summaAIzed.asyncStandupTool.model.Member;
import com.summaAIzed.asyncStandupTool.model.StandupEntry;
import com.summaAIzed.asyncStandupTool.repository.MemberRepository;
import com.summaAIzed.asyncStandupTool.repository.StandupEntryRepository;
import com.summaAIzed.asyncStandupTool.service.SlackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slack")
public class SlackController {

    @Value("${slack.bot.token}")
    private String botToken;
    @Autowired
    private SlackService slackService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Handle slash commands (/standup)
    @PostMapping("/events")
    public ResponseEntity<String> handleCommand(@RequestParam Map<String, String> params) {
        String command = params.get("command");
        String triggerId = params.get("trigger_id");

        if ("/standup".equals(command)) {
            slackService.openModal(triggerId);  // <-- Calls service to open modal
            return ResponseEntity.ok("success");        // Acknowledge command
        }
        return ResponseEntity.ok("success");
    }

    // Handle modal submission and button clicks
    @PostMapping("/interactions")
    public ResponseEntity<String> handleInteraction(@RequestParam("payload") String payloadJson) throws IOException {
        JsonNode payload = objectMapper.readTree(payloadJson);
        String type = payload.get("type").asText();

        if ("view_submission".equals(type)) {
            // Extract responses from modal
            JsonNode values = payload.get("view").get("state").get("values");

            String yesterday = values.get("yesterday").get("input").get("value").asText();
            String today = values.get("today").get("input").get("value").asText();
            String blockers = values.get("blockers").get("input").get("value").asText();

            String slackUserId = payload.get("user").get("id").asText();

            // TODO: Save to DB (Member + StandupEntry)

            return (ResponseEntity<String>) ResponseEntity.ok(); // Acknowledge
        }

        return ResponseEntity.ok("success");
    }

}
