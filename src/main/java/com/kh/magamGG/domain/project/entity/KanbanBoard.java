package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "KANBAN_BOARD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
	
	@Builder.Default
	@OneToMany(mappedBy = "kanbanBoard", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KanbanCard> kanbanCards = new ArrayList<>();

	public void setProject(Project project) { this.project = project; }
	public void setKanbanBoardName(String kanbanBoardName) { this.kanbanBoardName = kanbanBoardName; }
	public void setKanbanBoardOrder(Integer kanbanBoardOrder) { this.kanbanBoardOrder = kanbanBoardOrder; }
	public void setKanbanBoardStatus(String kanbanBoardStatus) { this.kanbanBoardStatus = kanbanBoardStatus; }
}
