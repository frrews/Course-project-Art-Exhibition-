package com.bsuir.exhibition.controller;

import com.bsuir.exhibition.dto.CommentResponse;
import com.bsuir.exhibition.entity.Painting;
import com.bsuir.exhibition.service.FavoriteService;
import com.bsuir.exhibition.service.PaintingCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Избранное и профиль")
public class UserController {

    private final FavoriteService favoriteService;
    private final PaintingCommentService commentService;

    @PostMapping("/favorites/{paintingId}")
    @Operation(summary = "Добавить/Удалить картину в избранное", description = "Нужен JWT токен")
    public ResponseEntity<String> toggleFavorite(
            @PathVariable String paintingId,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        String result = favoriteService.toggleFavorite(currentUser.getUsername(), paintingId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/favorites")
    @Operation(summary = "Получить избранные картины", description = "Возвращает список картин. Нужен JWT токен")
    public ResponseEntity<Set<Painting>> getFavorites(
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        Set<Painting> favorites = favoriteService.getUserFavorites(currentUser.getUsername());
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/comments")
    @Operation(summary = "Мои комментарии", description = "Список комментариев текущего пользователя. Нужен JWT")
    public ResponseEntity<?> getMyComments(@AuthenticationPrincipal UserDetails currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Войдите в аккаунт");
        }
        List<CommentResponse> comments = commentService.getMyComments(currentUser.getUsername());
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Удалить свой комментарий", description = "Нужен JWT")
    public ResponseEntity<?> deleteComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Войдите в аккаунт");
        }
        commentService.deleteComment(commentId, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

}

