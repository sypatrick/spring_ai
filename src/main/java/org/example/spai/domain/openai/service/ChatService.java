package org.example.spai.domain.openai.service;

import lombok.RequiredArgsConstructor;
import org.example.spai.domain.openai.entity.ChatEntity;
import org.example.spai.repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    /**
     *  편의상 dto로 response 리턴하지 않음.
     */
    @Transactional(readOnly = true)
    public List<ChatEntity> readAllChats(String userId){
        return chatRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }
}
