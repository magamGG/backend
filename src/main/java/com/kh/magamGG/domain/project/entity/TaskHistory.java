package com.kh.magamGG.domain.project.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TASK_HISTORY")
@Getter
@NoArgsConstructor
public class TaskHistory {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TASK_HISTORY_NO")
	private Long taskHistoryNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TASK_NO", nullable = false)
	private KanbanCard kanbanCard;
	
	@Column(name = "BEFORE_TASK_HISTORY", length = 1000)
	private String beforeTaskHistory;
	
	@Column(name = "AFTER_TASK_HISTORY", length = 1000)
	private String afterTaskHistory;
	
	@Column(name = "TASK_HISTORY_UPDATED_AT")
	private LocalDateTime taskHistoryUpdatedAt;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
}
