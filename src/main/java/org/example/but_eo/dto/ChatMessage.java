package org.example.but_eo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String messageId;

    @JsonProperty("chatroomId")
    private String chat_id;
    private String sender;
    private String nickName;
    private String message;
    private String createdAt;

//    private String chatRoomId; //채팅방 아이디
//    private String senderId; //메세지 전송자 아이디
//    private String senderName; //메세지 전송자 닉네임
//    private String content; //메세지 내용
//    private LocalDateTime timestamp; //메세지 전송시간

}
