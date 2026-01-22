package com.kh.magamGG.domain.calendar.entity;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.memo.entity.Memo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CALENDAR_EVENT")
@Getter
@NoArgsConstructor
public class CalendarEvent {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "CALENDAR_EVENT_NO")
	private Long calendarEventNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "CALENDAR_EVENT_NAME", length = 50)
	private String calendarEventName;
	
	@Column(name = "CALENDAR_EVENT_CONTENT", length = 100)
	private String calendarEventContent;
	
	@Column(name = "CALENDAR_EVENT_TYPE", length = 20)
	private String calendarEventType;
	
	@Column(name = "CALENDAR_EVENT_STARTED_AT", nullable = false)
	private LocalDate calendarEventStartedAt;
	
	@Column(name = "CALENDAR_EVENT_ENDED_AT", nullable = false)
	private LocalDate calendarEventEndedAt;
	
	@Column(name = "CALENDAR_EVENT_CREATED_AT", nullable = false)
	private LocalDateTime calendarEventCreatedAt;
	
	@Column(name = "CALENDAR_EVENT_UPDATED_AT")
	private LocalDateTime calendarEventUpdatedAt;
	
	@Column(name = "CALENDAR_EVENT_COLOR", length = 10)
	private String calendarEventColor;
	
	@OneToMany(mappedBy = "calendarEvent", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Memo> memos = new ArrayList<>();
}
