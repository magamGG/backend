package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.KanbanCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

    @Query("SELECT kc FROM KanbanCard kc " +
           "JOIN kc.kanbanBoard kb " +
           "WHERE kb.project.projectNo = :projectNo")
    List<KanbanCard> findByProjectNo(@Param("projectNo") Long projectNo);

    List<KanbanCard> findByKanbanBoard_Project_ProjectNo(Long projectNo);
}
