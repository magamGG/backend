package com.kh.magamGG.domain.project.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "COMMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

	@Column(name = "COMMENT_STATUS", nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'active'")
	private String commentStatus;

	@Column(name = "COMMENT_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime commentCreatedAt;
}
