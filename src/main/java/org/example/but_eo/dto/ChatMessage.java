package org.example.but_eo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatMessage {

    private String chatRoomId; //채팅방 아이디
    private String senderId; //메세지 전송자 아이디
    private String senderName; //메세지 전송자 닉네임
    private String content; //메세지 내용
    private LocalDateTime timestamp; //메세지 전송시간

}
