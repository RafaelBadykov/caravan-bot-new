package com.sample.caravanbot.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SimilarResult {
    private int pagesCount;
    private List<Film> films;

    public SimilarResult(int pagesCount, List<Film> films) {
        this.pagesCount = pagesCount;
        this.films = films;
    }
}
