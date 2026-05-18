package com.bsuir.exhibition.service;

import com.bsuir.exhibition.config.GigachatProperties;
import com.bsuir.exhibition.dto.AiChatRequest;
import com.bsuir.exhibition.dto.AiChatResponse;
import com.bsuir.exhibition.exception.AiProxyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final RestClient restClient;
    private final GigachatProperties gigachatProperties;
    private final ObjectMapper objectMapper;

    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAtEpochMs;

    public AiChatResponse chat(AiChatRequest request) {
        if (!StringUtils.hasText(gigachatProperties.getAuthorizationKey())) {
            throw new AiProxyException("GigaChat не настроен: задайте gigachat.authorization-key (строка вида «Basic …» или только Base64 от client_id:client_secret)");
        }

        String token = obtainAccessToken();

        String systemPrompt = buildSystemPrompt(request.context());
        Map<String, Object> requestBody = Map.of(
                "model", gigachatProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", request.message())
                )
        );

        try {
            String raw = restClient.post()
                    .uri(gigachatProperties.getChatUrl())
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "";
                        log.warn("GigaChat chat HTTP {}: {}", res.getStatusCode(), truncate(errBody, 800));
                        throw new AiProxyException(
                                "GigaChat chat " + res.getStatusCode() + ": " + truncate(errBody, 400)
                        );
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            JsonNode choice = root.path("choices").path(0).path("message").path("content");
            if (choice.isMissingNode() || choice.asText().isBlank()) {
                throw new AiProxyException("Пустой ответ от GigaChat (нет choices[0].message.content). Тело: " + truncate(raw, 500));
            }
            return new AiChatResponse(choice.asText().trim());
        } catch (AiProxyException e) {
            throw e;
        } catch (RestClientResponseException e) {
            String errText = e.getResponseBodyAsString();
            log.warn("GigaChat chat HTTP {}: {}", e.getStatusCode(), truncate(errText, 800));
            throw new AiProxyException("GigaChat chat " + e.getStatusCode() + ": " + truncate(errText, 400));
        } catch (Exception e) {
            log.warn("GigaChat chat error", e);
            throw new AiProxyException("Не удалось получить ответ от нейросети: " + rootCauseMessage(e), e);
        }
    }

    private static String rootCauseMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() != null ? c.getMessage() : c.getClass().getSimpleName();
    }

    private String buildSystemPrompt(String context) {
        String base = "Ты эксперт по искусству и гид на интерактивной выставке. Отвечай кратко, по делу, на русском языке.";
        if (StringUtils.hasText(context)) {
            return base + " Контекст работы: " + context + ".";
        }
        return base;
    }

    private synchronized String obtainAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiresAtEpochMs - 60_000L) {
            return cachedAccessToken;
        }

        String authHeader = normalizeAuthorizationHeader(gigachatProperties.getAuthorizationKey());

        try {
            String raw = restClient.post()
                    .uri(gigachatProperties.getOauthUrl())
                    .header("Authorization", authHeader)
                    .header("RqUID", gigachatProperties.getRqUid())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("scope=GIGACHAT_API_PERS&grant_type=client_credentials")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        String errBody = res.getBody() != null ? new String(res.getBody().readAllBytes()) : "";
                        log.warn("GigaChat OAuth HTTP {}: {}", res.getStatusCode(), truncate(errBody, 800));
                        throw new AiProxyException(
                                "GigaChat OAuth " + res.getStatusCode() + ": " + truncate(errBody, 400)
                        );
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String token = root.path("access_token").asText(null);
            if (!StringUtils.hasText(token)) {
                String msg = root.path("message").asText("");
                if (StringUtils.hasText(msg)) {
                    throw new AiProxyException("OAuth отказ: " + msg);
                }
                throw new AiProxyException("В ответе OAuth нет access_token. Ответ: " + truncate(raw, 500));
            }

            cachedAccessToken = token;

            long expiresAt = root.path("expires_at").asLong(0);
            if (expiresAt > 1_000_000_000_000L) {
                tokenExpiresAtEpochMs = expiresAt;
            } else {
                long expiresInSec = root.path("expires_in").asLong(600);
                tokenExpiresAtEpochMs = now + expiresInSec * 1000L;
            }

            return cachedAccessToken;
        } catch (AiProxyException e) {
            throw e;
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.warn("GigaChat OAuth HTTP {}: {}", e.getStatusCode(), truncate(body, 800));
            throw new AiProxyException("GigaChat OAuth " + e.getStatusCode() + ": " + truncate(body, 400));
        } catch (Exception e) {
            log.warn("GigaChat OAuth error", e);
            throw new AiProxyException("Не удалось авторизоваться в GigaChat: " + rootCauseMessage(e), e);
        }
    }

    /**
     * Ключ из личного кабинета Сбера — строка «Basic …»; иногда передают только Base64 без префикса.
     */
    static String normalizeAuthorizationHeader(String key) {
        if (key == null) {
            return "";
        }
        String t = key.trim();
        if (t.isEmpty()) {
            return t;
        }
        if (t.length() >= 6 && t.regionMatches(true, 0, "Basic ", 0, 6)) {
            return t;
        }
        return "Basic " + t;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }
}
