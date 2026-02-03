package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.KanbanCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanCardRepository extends JpaRepository<KanbanCard, Long> {

    List<KanbanCard> findByKanbanBoard_Project_ProjectNo(Long projectNo);
}
