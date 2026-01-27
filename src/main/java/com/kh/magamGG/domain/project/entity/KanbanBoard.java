package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "KANBAN_BOARD")
@Getter
@NoArgsConstructor
public class KanbanBoard {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "KANBAN_BOARD_NO")
	private Long kanbanBoardNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROJECT_NO", nullable = false)
	private Project project;
	
	@Column(name = "KANBAN_BOARD_NAME", length = 100)
	private String kanbanBoardName;
	
	@Column(name = "KANBAN_BOARD_ORDER", nullable = false)
	private Integer kanbanBoardOrder;
	
	@Column(name = "KANBAN_BOARD_STATUS", nullable = false, columnDefinition = "VARCHAR(1) DEFAULT 'Y'")
	private String kanbanBoardStatus;
	
	@OneToMany(mappedBy = "kanbanBoard", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KanbanCard> kanbanCards = new ArrayList<>();
}
