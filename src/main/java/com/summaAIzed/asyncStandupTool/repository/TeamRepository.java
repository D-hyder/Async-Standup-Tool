package com.summaAIzed.asyncStandupTool.repository;

import com.summaAIzed.asyncStandupTool.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Team findBySlackChannelId(String slackChannelId);
}
