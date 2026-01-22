package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "KANBAN_CARD")
public class KanbanCard {
	
	@Id
	@Column(name = "TASK_NO")
	private Long taskNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "BOARD_NO", nullable = false)
	private KanbanBoard kanbanBoard;
	
	@Column(name = "TASK_NAME", nullable = false, length = 50)
	private String taskName;
	
	@Column(name = "TASK_STATUS", nullable = false, length = 20)
	private String taskStatus;
	
	@Column(name = "TASK_CREATED_AT", nullable = false)
	private LocalDateTime taskCreatedAt;
	
	@Column(name = "TASK_UPDATED_AT")
	private LocalDateTime taskUpdatedAt;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROJECT_MEMBER_NO", nullable = false)
	private ProjectMember projectMember;
	
	@Column(name = "Field", length = 255)
	private String field;
	
	@Column(name = "Field2", length = 255)
	private String field2;
	
	@OneToMany(mappedBy = "kanbanCard", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();
	
	@OneToMany(mappedBy = "kanbanCard", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TaskHistory> taskHistories = new ArrayList<>();
}
