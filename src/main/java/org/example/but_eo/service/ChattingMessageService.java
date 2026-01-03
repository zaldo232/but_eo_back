package org.example.but_eo.service;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.ChatMessage;
import org.example.but_eo.entity.ChattingMessage;
import org.example.but_eo.repository.ChattingMessageRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChattingMessageService {

    private final ChattingMessageRepository chattingMessageRepository;

}
