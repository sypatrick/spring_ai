package org.example.spai.domain.openai.controller;

import lombok.RequiredArgsConstructor;
import org.example.spai.domain.openai.service.OpenAiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final OpenAiService openAiService;

    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body){
        return openAiService.generate(body.get("text"));
    }

    @PostMapping("/chat/stream")
    public Flux<String> streamChat(@RequestBody Map<String, String> body){
        return openAiService.generateStream(body.get("text"));
    }
}
