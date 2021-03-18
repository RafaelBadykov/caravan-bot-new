package com.sample.caravanbot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenreEntity {
    private int id;
    private String name;

    @SuppressWarnings("unused")
    public GenreEntity() {
    }

    public GenreEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
