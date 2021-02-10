package com.sample.caravanbot.model;

import com.truedev.kinoposk.api.model.common.Genre;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

/**
 * Возможно, что некоторые поля лишние или наоборот чего-то не хватает,
 * пока просто для тестов такой класс сделал
 *
 * */
public class Film {

    private final int id;
    private final int kinopoiskId;
    private final String telegramString;
    private final List<Genre> genres;

    public Film(int id, int kinopoiskId, String telegramString, List<Genre> genres) {
        this.id = id;
        this.kinopoiskId = kinopoiskId;
        this.telegramString = telegramString;
        this.genres = genres;
    }

    public int getId() {
        return id;
    }

    public int getKinopoiskId() {
        return kinopoiskId;
    }

    public String getTelegramString() {
        return telegramString;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    /**
     * Или хранить все поля, которые нужны для
     * @see Film#telegramString
     * и в toString() формировать эту строку или в
     * @see com.sample.caravanbot.CaravanTelegramBot#onWebhookUpdateReceived(Update)
     * поменять вывод или это в каком-то отдельном сервисе делать, иначе это фигня какая-то
     * */
    @Override
    public String toString() {
        return telegramString;
    }
}
