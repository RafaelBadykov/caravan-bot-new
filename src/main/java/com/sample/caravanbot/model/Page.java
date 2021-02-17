package com.sample.caravanbot.model;

import lombok.Getter;
import java.util.List;

@Getter
public class Page {
    private final int pageCount;
    private final List<Film> films;

    public Page(int pageCount, List<Film> films) {
        this.pageCount = pageCount;
        this.films = films;
    }
}
