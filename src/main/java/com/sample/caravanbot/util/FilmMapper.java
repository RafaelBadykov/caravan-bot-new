package com.sample.caravanbot.util;

import com.sample.caravanbot.model.Film;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchItem;

public class FilmMapper {

    public static Film mapToFilmEntity(SearchItem film) {
        if (film != null) {
            return new Film(
                    film.getKinopoiskId(),
                    film.getNameRu(),
                    film.getNameEn(),
                    film.getYear(),
                    film.getRating(),
                    film.getPosterUrl()
            );
        }
        return new Film();
    }
}
