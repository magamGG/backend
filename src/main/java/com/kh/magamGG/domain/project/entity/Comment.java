package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "COMMENT")
public class Comment {
	
	@Id
	@Column(name = "COMMENT_NO")
	private Long commentNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TASK_NO", nullable = false)
	private KanbanCard kanbanCard;
	
	@Column(name = "COMMENT_CONTENT", length = 1000)
	private String commentContent;
}
