package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {
    List<KanbanBoard> findByProject_ProjectNo(Long projectNo);
}


