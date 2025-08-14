package com.summaAIzed.asyncStandupTool.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class StandupEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    private LocalDate date;
    private String yesterday;
    private String today;
    private String blockers;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getYesterday() { return yesterday; }
    public void setYesterday(String yesterday) { this.yesterday = yesterday; }

    public String getToday() { return today; }
    public void setToday(String today) { this.today = today; }

    public String getBlockers() { return blockers; }
    public void setBlockers(String blockers) { this.blockers = blockers; }
}
