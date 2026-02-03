package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "kanban_card")
@Getter
@NoArgsConstructor
public class KanbanCard {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "KANBAN_CARD_NO")
	private Long kanbanCardNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "BOARD_NO", nullable = false)
	private KanbanBoard kanbanBoard;
	
	@Column(name = "KANBAN_CARD_NAME", nullable = false, length = 50)
	private String kanbanCardName;
	
	@Column(name = "KANBAN_CARD_STATUS", nullable = false, columnDefinition = "VARCHAR(1) DEFAULT 'N'")
	private String kanbanCardStatus;
	
	@Column(name = "KANBAN_CARD_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime kanbanCardCreatedAt;
	
	@Column(name = "KANBAN_CARD_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private LocalDateTime kanbanCardUpdatedAt;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROJECT_MEMBER_NO", nullable = false)
	private ProjectMember projectMember;
	
	@Column(name = "KANBAN_CARD_STARTED_AT")
	private LocalDate kanbanCardStartedAt;
	
	@Column(name = "KANBAN_CARD_ENDED_AT")
	private LocalDate kanbanCardEndedAt;
	
	@OneToMany(mappedBy = "kanbanCard", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	public void setKanbanBoard(KanbanBoard kanbanBoard) { this.kanbanBoard = kanbanBoard; }
	public void setKanbanCardName(String kanbanCardName) { this.kanbanCardName = kanbanCardName; }
	public void setKanbanCardStatus(String kanbanCardStatus) { this.kanbanCardStatus = kanbanCardStatus; }
	public void setProjectMember(ProjectMember projectMember) { this.projectMember = projectMember; }
	public void setKanbanCardStartedAt(LocalDate kanbanCardStartedAt) { this.kanbanCardStartedAt = kanbanCardStartedAt; }
	public void setKanbanCardEndedAt(LocalDate kanbanCardEndedAt) { this.kanbanCardEndedAt = kanbanCardEndedAt; }
}
