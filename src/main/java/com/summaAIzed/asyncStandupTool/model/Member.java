package com.summaAIzed.asyncStandupTool.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slackUserId;   // Slack user ID
    private String displayName;   // Slack display name

    @ManyToOne
    @JoinColumn(name = "team_id")
//    @JsonBackReference
    private Team team;


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlackUserId() { return slackUserId; }
    public void setSlackUserId(String slackUserId) { this.slackUserId = slackUserId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
}
