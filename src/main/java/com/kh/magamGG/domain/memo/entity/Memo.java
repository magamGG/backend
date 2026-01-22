package com.kh.magamGG.domain.memo.entity;

import com.kh.magamGG.domain.calendar.entity.CalendarEvent;
import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "MEMO")
public class Memo {
	
	@Id
	@Column(name = "MEMO_NO")
	private Long memoNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "CALENDAR_EVENT_NO", nullable = false)
	private CalendarEvent calendarEvent;
	
	@Column(name = "MEMO_NAME", nullable = false, length = 30)
	private String memoName;
	
	@Column(name = "MEMO_TEXT", length = 255)
	private String memoText;
	
	@Column(name = "MEMO_CREATED_AT", nullable = false)
	private LocalDateTime memoCreatedAt;
	
	@Column(name = "MEMO_UPDATED_AT")
	private LocalDateTime memoUpdatedAt;
	
	@Column(name = "MEMO_TYPE", length = 12)
	private String memoType;
}
