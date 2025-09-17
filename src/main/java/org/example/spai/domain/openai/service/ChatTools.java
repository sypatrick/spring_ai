package org.example.spai.domain.openai.service;

import org.example.spai.domain.openai.dto.UserResponseDto;
import org.springframework.ai.tool.annotation.Tool;

public class ChatTools {

    @Tool(description = "User personal information : name, age, address, phone, etc")
    public UserResponseDto getUserInfoTool(){
        return new UserResponseDto("pat", 35L, "seoul", "010-1000-0000", "frank");
    }
}
