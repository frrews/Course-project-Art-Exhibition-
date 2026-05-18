package com.bsuir.exhibition.service;

import com.bsuir.exhibition.entity.Painting;
import com.bsuir.exhibition.entity.User;
import com.bsuir.exhibition.repository.PaintingRepository;
import com.bsuir.exhibition.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserRepository userRepository;
    private final PaintingRepository paintingRepository;

    public String toggleFavorite(String userEmail, String paintingId) {
        User user = userRepository.findUserByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Painting painting = paintingRepository.findById(paintingId)
                .orElseThrow(() -> new IllegalArgumentException("Картина с таким ID не найдена"));

        Set<Painting> favorites = user.getFavoritePaintings();

        if (favorites.contains(painting)) {
            favorites.remove(painting);
            userRepository.save(user);
            return "Картина удалена из избранного";
        } else {
            favorites.add(painting);
            userRepository.save(user);
            return "Картина добавлена в избранное";
        }
    }

    public Set<Painting> getUserFavorites(String userEmail) {
        User user = userRepository.findUserByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        return user.getFavoritePaintings();
    }
}