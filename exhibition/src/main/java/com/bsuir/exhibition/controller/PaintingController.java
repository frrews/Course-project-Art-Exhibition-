package com.bsuir.exhibition.controller;

import com.bsuir.exhibition.entity.Painting;
import com.bsuir.exhibition.service.PaintingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paintings")
@RequiredArgsConstructor
@Tag(name = "Каталог картин", description = "API для управления интерактивной выставкой искусств")
public class PaintingController {

    private final PaintingService paintingService;

    @GetMapping
    @Operation(summary = "Получить все картины", description = "Сортировка по названию (А→Я)")
    public List<Painting> getAllPaintings() {
        return paintingService.get();
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск с фильтрами", description = "Текст по названию, автору и описанию; фильтры по стилю и автору; сортировка")
    public List<Painting> searchPaintings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String author,
            @RequestParam(required = false, defaultValue = "TITLE_ASC") String sort
    ) {
        return paintingService.search(q, style, author, sort);
    }

    @GetMapping("/meta/styles")
    @Operation(summary = "Список стилей", description = "Уникальные значения поля style для фильтров")
    public List<String> paintingStyles() {
        return paintingService.distinctStyles();
    }

    @PostMapping
    @Operation(summary = "Добавить новую картину", description = "Принимает JSON с данными картины и сохраняет в БД")
    public Painting addPainting(@RequestBody Painting painting) {
        return paintingService.post(painting);
    }
}