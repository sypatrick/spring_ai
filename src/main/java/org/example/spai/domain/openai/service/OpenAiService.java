package org.example.spai.domain.openai.service;

import lombok.RequiredArgsConstructor;
import org.example.spai.domain.openai.entity.ChatEntity;
import org.example.spai.repository.ChatRepository;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiImageModel openAiImageModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatRepository chatRepository;

    // 1. chat model

    public String generate(String text){
        // 메시지
        SystemMessage systemMessage = new SystemMessage("");
        UserMessage userMessage = new UserMessage(text);
        AssistantMessage assistantMessage = new AssistantMessage("");

        //옵션
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini") // 기본 값이 해당 값임
                .temperature(0.7) // 기본 값 0.8
                .build();

        //프롬프트
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage, assistantMessage), options);

        //요청, 응답
        ChatResponse response = openAiChatModel.call(prompt);
        return response.getResult().getOutput().getText();

    }

    // stream으로 받는 메소드
    public Flux<String> generateStream(String text){
        // 유저&페이지별 Chat Memory 관리하기 위한 key (시큐리티와 같은 것으로 받을 수 있지만 우선 명시적으로)
        String userId = "patrick" + "_" + "1";

        // 전체 대화 저장용
        ChatEntity chatUserEntity = new ChatEntity();
        chatUserEntity.setUserId(userId);
        chatUserEntity.setType(MessageType.USER);
        chatUserEntity.setContent(text);

        // 메시지
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
        chatMemory.add(userId, new UserMessage(text)); // 신규 메시지 추가

        //옵션
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4.1-mini") // 기본 값이 해당 값임
                .temperature(0.7) // 기본 값 0.8
                .build();

        //프롬프트
        Prompt prompt = new Prompt(chatMemory.get(userId), options);

        // 응답 메시지를 저장할 임시 버퍼
        StringBuilder responseBuffer = new StringBuilder();

        //요청, 응답
        return openAiChatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText())
                .doOnNext(responseBuffer::append) // null이 제거된 값만 누적
                .doOnComplete(() -> {
                    chatMemory.add(userId, new AssistantMessage(responseBuffer.toString()));
                    chatMemoryRepository.saveAll(userId, chatMemory.get(userId));

                    //전체 대화 저장용
                    ChatEntity chatAssistantEntity = new ChatEntity();
                    chatAssistantEntity.setUserId(userId);
                    chatAssistantEntity.setType(MessageType.ASSISTANT);
                    chatAssistantEntity.setContent(responseBuffer.toString());

                    chatRepository.saveAll(List.of(chatUserEntity, chatAssistantEntity));
                });

    }

    // 2. Embedding model
    public List<float[]> generateEmbedding(List<String> texts, String Model){
        // 옵션
        EmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(Model)
                .build();

        //prompt
        EmbeddingRequest prompt = new EmbeddingRequest(texts, embeddingOptions);

        // req, res
        EmbeddingResponse response = openAiEmbeddingModel.call(prompt);

        return response.getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    // 3. Image Model
    public List<String> generateImage(String text, int count, int height, int width){ // 몇 개, 규격

        //opt
        OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
                .quality("hd")
                .N(count)
                .height(height)
                .width(width)
                .build();

        //prompt
        ImagePrompt prompt = new ImagePrompt(text, imageOptions);

        //req, res
        ImageResponse response = openAiImageModel.call(prompt);

        return response.getResults().stream()
                .map(image -> image.getOutput().getUrl())
                .toList();
    }

    // 4. Speech Model
    /**
     * TTS -> 오디오 형태
     */
    public byte[] tts(String text){
        //opt
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0f)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .build();

        //prompt
        SpeechPrompt  prompt = new SpeechPrompt(text, speechOptions);

        //req, res
        SpeechResponse response = openAiAudioSpeechModel.call(prompt);

        return response.getResult().getOutput();
    }

    /**
     * STT -> 텍스트 형태
     */
    public String stt(Resource audioFile){
        //opt
        OpenAiAudioApi.TranscriptResponseFormat responseFormat = OpenAiAudioApi.TranscriptResponseFormat.VTT; //자막 포맷, Json 등으로 할 수 도 있음
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("ko")
                .prompt("Ask not this, but ask that")
                .temperature(0f)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .responseFormat(responseFormat)
                .build();

        //prompt
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);

        //req, res
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);

        return response.getResult().getOutput();
    }

}
