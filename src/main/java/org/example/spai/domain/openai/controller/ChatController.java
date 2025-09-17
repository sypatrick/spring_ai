package org.example.spai.domain.openai.controller;

import lombok.RequiredArgsConstructor;
import org.example.spai.domain.openai.entity.ChatEntity;
import org.example.spai.domain.openai.service.ChatService;
import org.example.spai.domain.openai.service.OpenAiService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final OpenAiService openAiService;
    private final ChatService chatService;

    @ResponseBody
    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body){
        return openAiService.generate(body.get("text"));
    }

    @ResponseBody
    @PostMapping("/chat/stream")
    public Flux<String> streamChat(@RequestBody Map<String, String> body){
        return openAiService.generateStream(body.get("text"));
    }

    @GetMapping("/")
    public String chatPage(){
        return "chat";
    }

    @ResponseBody
    @PostMapping("/chat/history/{userId}")
    public List<ChatEntity> getChatHistory(@PathVariable("userId") String userId){
        return chatService.readAllChats(userId);
    }
}
