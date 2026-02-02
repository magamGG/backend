package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, Long> {

    @Query("SELECT DISTINCT b FROM KanbanBoard b LEFT JOIN FETCH b.kanbanCards WHERE b.project.projectNo = :projectNo AND b.kanbanBoardStatus = :status ORDER BY b.kanbanBoardOrder ASC")
    List<KanbanBoard> findByProjectNoAndStatusWithCards(@Param("projectNo") Long projectNo, @Param("status") String status);

    @Query("SELECT COALESCE(MAX(b.kanbanBoardOrder), 0) FROM KanbanBoard b WHERE b.project.projectNo = :projectNo")
    Integer findMaxOrderByProjectNo(@Param("projectNo") Long projectNo);
}


