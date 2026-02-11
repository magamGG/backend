package com.kh.magamGG.domain.chat.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CHAT_ROOM_MEMBER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ROOM_MEMBER_NO")
    private Long chatRoomMemberNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHAT_ROOM_NO", nullable = false)
    private ChatRoom chatRoom;

    @Column(
        name = "CHAT_ROOM_MEMBER_JOINED_AT",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatRoomMemberJoinedAt;

    // 마지막 읽은 채팅 번호 (FK 제약은 ERD에 없으므로 Long으로만 보관)
    @Column(name = "LAST_READ_CHAT_NO")
    private Long lastReadChatNo;
}


