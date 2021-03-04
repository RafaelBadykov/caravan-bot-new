package com.sample.caravanbot.util;

import com.sample.caravanbot.model.FilmEntity;
import com.sample.caravanbot.model.GenreEntity;
import com.truedev.kinoposk.api.model.common.Genre;
import com.truedev.kinoposk.api.model.movie.Common;
import com.truedev.kinoposk.api.model.movie.Film;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FilmMapper {

    public static FilmEntity mapToFilmEntity(SearchItem film, int number) {
        if (film != null) {
            return FilmEntity.builder()
                    .number(number)
                    .kinopoiskId(film.getKinopoiskId())
                    .nameRu(film.getNameRu())
                    .nameEn(film.getNameEn())
                    .year(film.getYear())
                    .rating(film.getRating())
                    .posterUrl(film.getPosterUrl())
                    .build();
        }
        return FilmEntity.builder().kinopoiskId(-1).build();
    }

    public static FilmEntity mapToFilmEntity(Film film) {
        if (film != null) {
            Common data = film.getData();
            return FilmEntity.builder()
                    .number(1)
                    .kinopoiskId(data.getKinopoiskId())
                    .nameRu(data.getNameRu())
                    .nameEn(data.getNameEn())
                    .description(data.getDescription())
                    .year(data.getYear())
                    .rating(data.getRatingMpaa())
                    .posterUrl(data.getPosterUrl())
                    .genres(mapToGenre(data.getGenres()))
                    .facts(data.getFacts())
                    .length(data.getFilmLength())
                    .build();
        }
        return FilmEntity.builder().kinopoiskId(-1).build();
    }

    @NotNull
    private static List<GenreEntity> mapToGenre(List<Genre> genres) {
        return genres.stream()
                .filter(Objects::nonNull)
                .map(genre -> new GenreEntity(genre.getId(), genre.getGenre()))
                .collect(Collectors.toList());
    }
}
