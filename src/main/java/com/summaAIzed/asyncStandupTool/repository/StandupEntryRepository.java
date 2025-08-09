package com.summaAIzed.asyncStandupTool.repository;

import com.summaAIzed.asyncStandupTool.model.Member;
import com.summaAIzed.asyncStandupTool.model.StandupEntry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StandupEntryRepository extends JpaRepository<StandupEntry, Long> {
    List<StandupEntry> findByDate(LocalDate date);
    List<StandupEntry> findByMemberAndDate(Member member, LocalDate date);
}
