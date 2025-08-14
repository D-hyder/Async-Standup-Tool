package com.summaAIzed.asyncStandupTool.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summaAIzed.asyncStandupTool.service.SlackService;
import com.summaAIzed.asyncStandupTool.service.StandupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/slack")
public class SlackController {

    @Value("${slack.bot.token}")
    private String botToken;
    @Autowired
    private SlackService slackService;
    private final StandupService standupService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackController(StandupService standupService) {
        this.standupService = standupService;
    }

    // Handle slash commands (/standup)
    @PostMapping("/events")
    public ResponseEntity<Void> handleCommand(@RequestParam Map<String, String> params) {
        String command = params.get("command");
        String triggerId = params.get("trigger_id");

        if ("/standup".equals(command)) {
            slackService.openModal(triggerId);  // <-- Calls service to open modal
            return ResponseEntity.ok().build();        // Acknowledge command
        }
        return ResponseEntity.ok().build();
    }

    // Handle modal submission and button clicks
    @PostMapping("/interactions")
    public ResponseEntity<Void> handleInteraction(@RequestParam("payload") String payloadJson) throws IOException {
        JsonNode payload = objectMapper.readTree(payloadJson);
        String type = payload.path("type").asText();

        if ("view_submission".equals(type)) {
            JsonNode values = payload.path("view").path("state").path("values");

            // these keys must match your modal block_id + action_id
            String yesterday = values.path("yesterday").path("input").path("value").asText("");
            String today     = values.path("today").path("input").path("value").asText("");
            String blockers  = values.path("blockers").path("input").path("value").asText("");

            String slackUserId = payload.path("user").path("id").asText("");
            // display name may be absent in some payloads, so fallback to id
            String displayName = payload.path("user").path("name").asText(slackUserId);

            // 🔗 save + notify (DM / channel post handled inside the service)
            standupService.saveStandup(slackUserId, displayName, yesterday, today, blockers);

            // must return within ~3s
            return ResponseEntity.ok().build();
        }

        // acknowledge all other interaction types
        return ResponseEntity.ok().build();
    }

    @PostMapping("/digest/now")
    public ResponseEntity<String> digestNow() {
        try {
            standupService.postDailyDigest(LocalDate.now());
            return ResponseEntity.ok("Digest posted.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Digest failed: " + e.getMessage());
        }
    }

    @DeleteMapping()
    public void deleteEntries() {
        slackService.deleteAllEntries();
        }

}
