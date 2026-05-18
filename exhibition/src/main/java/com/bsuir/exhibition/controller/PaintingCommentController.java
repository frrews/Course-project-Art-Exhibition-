package com.bsuir.exhibition.controller;

import com.bsuir.exhibition.dto.CommentResponse;
import com.bsuir.exhibition.dto.CreateCommentRequest;
import com.bsuir.exhibition.service.PaintingCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paintings/{paintingId}/comments")
@RequiredArgsConstructor
@Tag(name = "Комментарии к картинам", description = "Обсуждение работ в каталоге")
public class PaintingCommentController {

    private final PaintingCommentService commentService;

    @GetMapping
    @Operation(summary = "Список комментариев к картине")
    public List<CommentResponse> list(@PathVariable String paintingId) {
        return commentService.getComments(paintingId);
    }

    @PostMapping
    @Operation(summary = "Добавить комментарий", description = "Требуется JWT")
    public ResponseEntity<?> create(
            @PathVariable String paintingId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Войдите, чтобы оставить комментарий");
        }
        CommentResponse created = commentService.addComment(paintingId, currentUser.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
