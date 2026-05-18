package com.bsuir.exhibition.controller;

import com.bsuir.exhibition.dto.AiChatRequest;
import com.bsuir.exhibition.dto.AiChatResponse;
import com.bsuir.exhibition.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "ИИ-ассистент", description = "Прокси к GigaChat без раскрытия ключей на клиенте")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    @Operation(summary = "Чат с ИИ о картине")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiService.chat(request);
    }
}
