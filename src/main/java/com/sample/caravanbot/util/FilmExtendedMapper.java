package com.sample.caravanbot.util;

import com.sample.caravanbot.model.FilmExtended;
import com.truedev.kinoposk.api.model.movie.Film;

public class FilmExtendedMapper {

    public static FilmExtended mapToFilmExtendedEntity(Film film) {
        if (film != null) {
            return new FilmExtended(
                    film.getData().getKinopoiskId(),
                    film.getData().getNameRu(),
                    film.getData().getNameEn(),
                    film.getData().getYear(),
                    film.getData().getRatingMpaa(),
                    film.getData().getPosterUrl(),
                    film.getData().getDescription(),
                    film.getData().getWebUrl(),
                    film.getData().getGenres(),
                    film.getData().getFacts(),
                    film.getData().getFilmLength()
            );
        }
        return new FilmExtended();
    }
}
