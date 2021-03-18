package com.sample.caravanbot.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class FilmEntity {

    @Setter
    private int number;

    private final long kinopoiskId;
    private final String nameRu;
    private final String nameEn;
    private final String year;
    private final String rating;
    private final String posterUrl;
    private final String description;
    private final String webUrl;
    private final List<GenreEntity> genres;
    private final List<String> facts;
    private final String length;

    @Override
    public String toString() {
        if (kinopoiskId == -1) {
            return "";
        }
        return String.format("%s. %s [%s] Рейтинг: %s", number, getFilmName(), year, rating);
    }

    protected String getFilmName() {
        String filmName = "";
        if (nameRu != null) {
            String trimmedRuName = nameRu.trim();
            if (!trimmedRuName.isEmpty() && !"null".equals(trimmedRuName)) {
                filmName = trimmedRuName + " \\";
            }
        }
        if (nameEn != null) {
            String trimmedEnName = nameEn.trim();
            if (!trimmedEnName.isEmpty() && !"null".equals(trimmedEnName)) {
                if (!filmName.isEmpty()) {
                    filmName += " ";
                }
                filmName += trimmedEnName;
            }
        }
        return filmName;
    }

    public String getExtendedToString() {
        return String.format("%s [%s]\nОписание:\n%s\n%s", getFilmName(), getYear(), description, getPosterUrl());
    }

    public String getExtendedDescription() {
        return String.format("%s\nФакты:\n●%s\n\nПродолжительность фильма: %s\n\nЖанры: %s",
                getExtendedToString(),
                getFormattedCollection("\n●", facts),
                length,
                getFormattedCollection(", ", getGenreNames()));
    }

    private String getFormattedCollection(String delimiter, List<String> collection) {
        if (collection.isEmpty()) {
            return "Отсутствуют";
        }
        return String.join(delimiter, collection.subList(0, Math.min(collection.size(), 5)));
    }

    public List<String> getGenreNames() {
        return genres.stream()
                .map(GenreEntity::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}