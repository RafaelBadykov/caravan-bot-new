package com.sample.caravanbot.model;

import com.truedev.kinoposk.api.model.common.Genre;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

/**
 * Возможно, что некоторые поля лишние или наоборот чего-то не хватает,
 * пока просто для тестов такой класс сделал
 *
 * */
@Getter
public class Film {

    private final int internalId;
    private final String telegramString;
    private final List<Genre> genres;

    public Film(int internalId, String telegramString, List<Genre> genres) {
        this.internalId = internalId;
        this.telegramString = telegramString;
        this.genres = genres;
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
