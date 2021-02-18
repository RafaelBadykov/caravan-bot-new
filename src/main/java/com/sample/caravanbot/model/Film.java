package com.sample.caravanbot.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Возможно, что некоторые поля лишние или наоборот чего-то не хватает,
 * пока просто для тестов такой класс сделал
 */
@Getter
@Setter
public class Film {
    private long filmId;
    private String nameRu;
    private String nameEn;
    private String year;
    private String rating;
    private String posterUrl;

    public Film(long filmId, String nameRu,String nameEn, String year, String rating, String posterUrl) {
        this.filmId = filmId;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.year = year;
        this.rating = rating;
        this.posterUrl = posterUrl;
    }
}