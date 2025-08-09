package com.summaAIzed.asyncStandupTool.controller;

import com.summaAIzed.asyncStandupTool.model.Team;
import com.summaAIzed.asyncStandupTool.model.Member;
import com.summaAIzed.asyncStandupTool.model.StandupEntry;
import com.summaAIzed.asyncStandupTool.repository.TeamRepository;
import com.summaAIzed.asyncStandupTool.repository.MemberRepository;
import com.summaAIzed.asyncStandupTool.repository.StandupEntryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/test")
public class TestController {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final StandupEntryRepository standupEntryRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public TestController(TeamRepository teamRepository,
                          MemberRepository memberRepository,
                          StandupEntryRepository standupEntryRepository, EntityManager entityManager) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.standupEntryRepository = standupEntryRepository;
        this.entityManager = entityManager;
    }

    @PostMapping("/seed")
    @Transactional  // <-- Add this
    public String seedData() {
//        entityManager.createNativeQuery(
//                "TRUNCATE TABLE standup_entry, member, team RESTART IDENTITY CASCADE"
//        ).executeUpdate();

        // Now insert dummy data
        Team team = new Team();
        team.setName("Dev Team");
        team.setSlackChannelId("C123456");
        teamRepository.save(team);

        Member member = new Member();
        member.setSlackUserId("U123456");
        member.setDisplayName("John Doe");
        member.setTeam(team);
        memberRepository.save(member);

        StandupEntry entry = new StandupEntry();
        entry.setMember(member);
        entry.setDate(LocalDate.now());
        entry.setYesterday("Worked on backend APIs");
        entry.setToday("Integrate Slack bot");
        entry.setBlockers("None");
        standupEntryRepository.save(entry);

        return "Dummy data seeded!";
    }

    // Test endpoint: Fetch all entries
    @GetMapping("/entries")
    public List<StandupEntry> getEntries() {
        return standupEntryRepository.findAll();
    }
}
