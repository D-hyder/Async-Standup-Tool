package com.summaAIzed.asyncStandupTool.repository;

import com.summaAIzed.asyncStandupTool.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findBySlackUserId(String slackUserId);
}
