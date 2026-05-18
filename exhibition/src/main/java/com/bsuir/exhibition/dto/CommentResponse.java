package com.bsuir.exhibition.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String username,
        String paintingId,
        String paintingTitle,
        String paintingAuthor,
        String content,
        Instant createdAt
) {
}
