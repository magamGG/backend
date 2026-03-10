package com.kh.magamGG.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_no")
    private Long chatRoomNo;

    @Column(name = "chat_room_name", nullable = false, length = 50)
    private String chatRoomName;

    @Column(name = "chat_room_type", length = 30)
    private String chatRoomType; // 전체 / 프로젝트 / 개인

    @Column(
        name = "chat_room_created_at",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatRoomCreatedAt;

    @Column(
        name = "chat_room_updated_at",
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatRoomUpdatedAt;

    @Column(name = "chat_room_status", nullable = false, length = 1)
    private String chatRoomStatus; // Y / N

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> roomMembers = new ArrayList<>();

    @Column(name = "project_no", nullable = true)
    private Long projectNo;

    @Column(name = "agency_no")
    private Long agencyNo;
}


