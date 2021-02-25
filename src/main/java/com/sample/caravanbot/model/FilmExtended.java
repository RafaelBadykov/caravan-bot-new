package com.sample.caravanbot.model;

import com.truedev.kinoposk.api.model.common.Genre;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
public class FilmExtended extends Film {

    private String description;
    private String webUrl;
    private List<Genre> genres;
    private List<String> facts;
    private String length;

    public FilmExtended() {
        super();
    }

    //TODO(Реализовать паттерн билдера, потому что значения дофигища)
    public FilmExtended(long filmId, String nameRu, String nameEn, String year, String rating, String posterUrl,
                        String description, String webUrl, List<Genre> genres, List<String> facts, String length) {
        super(filmId, nameRu, nameEn, year, rating, posterUrl);
        this.description = description;
        this.webUrl = webUrl;
        this.genres = genres;
        this.facts = facts;
        this.length = length;
    }

    public String getExtendedDescription() {
        return String.format("%s\nФакты:\n●%s\n\nПродолжительность фильма: %s\n\nЖанры: %s",
                toString(),
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

    private List<String> getGenreNames() {
        return genres.stream()
                .filter(Objects::nonNull)
                .map(Genre::getGenre)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("%s [%s]\nОписание:\n%s\n%s", getFilmName(), getYear(), description, getPosterUrl());
    }
}
