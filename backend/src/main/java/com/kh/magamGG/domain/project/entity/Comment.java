package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "COMMENT")
@Getter
@NoArgsConstructor
public class Comment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "COMMENT_NO")
	private Long commentNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "KANBAN_CARD_NO", nullable = false)
	private KanbanCard kanbanCard;
	
	@Column(name = "COMMENT_CONTENT", length = 1000)
	private String commentContent;
}
