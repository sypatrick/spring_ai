package org.example.spai.domain.openai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
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

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final OpenAiChatModel openAiChatModel;
    private final OpenAiEmbeddingModel openAiEmbeddingModel;
    private final OpenAiImageModel openAiImageModel;
    private final OpenAiAudioSpeechModel openAiAudioSpeechModel;
    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

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

        return openAiChatModel.stream(prompt)
                .mapNotNull(response -> response.getResult().getOutput().getText());
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
