package com.sample.caravanbot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Film {

    private long filmId;
    private String nameRu;
    private String nameEn;
    private String year;
    private String rating;
    private String posterUrl;

    public Film() {
        this(-1, "", "", "", "", "");
    }

    public Film(long filmId, String nameRu, String nameEn, String year, String rating, String posterUrl) {
        this.filmId = filmId;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.year = year;
        this.rating = rating;
        this.posterUrl = posterUrl;
    }

    @Override
    public String toString() {
        if (filmId == -1) {
            return "";
        }
        return String.format("● %s [%s] Рейтинг: %s", getFilmName(), year, rating);
    }

    protected String getFilmName() {
        String filmName = "";
        String trimmedRuName = nameRu.trim();
        if (!trimmedRuName.isEmpty() && !"null".equals(trimmedRuName)) {
            filmName = trimmedRuName + " \\";
        }

        String trimmedEnName = nameEn.trim();
        if (!trimmedEnName.isEmpty() && !"null".equals(trimmedEnName)) {
            if (!filmName.isEmpty()) {
                filmName += " ";
            }
            filmName += trimmedEnName;
        }
        return filmName;
    }

    // мб потом как-то используем, пока так
    @SuppressWarnings("unused")
    protected String getValueOrEmpty(String value) {
        if ("".equals(value)) {
            return "Отсутствует";
        } else return value;
    }
}