package com.sample.caravanbot;

import com.sample.caravanbot.model.Film;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchItem;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchResult;
import com.truedev.kinoposk.api.service.KinopoiskApiService;
import org.apache.logging.log4j.util.Strings;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//TODO(refactor(create new classes/services...) + add keyboard menu)
public class CaravanTelegramBot extends TelegramWebhookBot {

    private String webhookPath;
    private String botUserName;
    private String botToken;
    private String apiToken;

    public CaravanTelegramBot(DefaultBotOptions options) {
        super(options);
    }

    public void setWebhookPath(String webhookPath) {
        this.webhookPath = webhookPath;
    }

    public void setBotUserName(String botUserName) {
        this.botUserName = botUserName;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            try {
                //TODO(придумать, как разграничить логику или сделать что-то такое https://imgur.com/a/PKVbZlZ)
                if ("/start".equals(message.getText())) {
                    printMessage(message, "Введите название филмьма");
                } else {
                    List<Film> films = getFilms(message);
                    if (films.isEmpty()) {
                        printMessage(message, String.format("По запросу \"%s\" ничего не найдено.", message.getText()));
                    } else {
                        SendMessage sendMessage = new SendMessage(message.getChatId(), Strings.join(films, '\n'));
                        sendMessage.setReplyMarkup(getInlineKeyboardMarkup(films));
                        execute(sendMessage);
                    }
                }

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private InlineKeyboardMarkup getInlineKeyboardMarkup(List<Film> films) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        //TODO(реализовать поиск похожеих фильмов при нажатии на выбранный фильм)
        //TODO(В запросе 20 значений всего, можно ещё через page менять, но и 20, мне кажется, норм, но надо
        // добавить пагинацию, потмоу что 8 кнопок максимум, но и 8 кнопок перебор /)
        for (int i = 0; films.size() > i && i < 5; i++) {
            keyboardRow.add(new InlineKeyboardButton().setText(String.valueOf(films.get(i).getKinopoiskId()))
                    .setCallbackData("not implemented"));
        }

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private void printMessage(Message message, String text) throws TelegramApiException {
        execute(new SendMessage(message.getChatId(), text));
    }

    private List<Film> getFilms(Message message) {
        List<SearchItem> films = getFilmsByKeyword(message);
        if (films != null && films.size() > 0) {
            AtomicInteger index = new AtomicInteger(1);
            return films.stream()
                    .filter(this::isFilmHasRuName)
                    .map(film -> getFilm(film, index.getAndIncrement()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean isFilmHasRuName(SearchItem searchItem) {
        return searchItem != null && searchItem.getNameRu() != null && !searchItem.getNameRu().isEmpty();
    }

    private Film getFilm(SearchItem searchItem, int index) {
        String telegramString = String.format("%d. %s\\%s [%sг.]", index, searchItem.getNameRu(), searchItem.getNameEn(), searchItem.getYear());
        return new Film(index, searchItem.getKinopoiskId(), telegramString, searchItem.getGenres());
    }

    private List<SearchItem> getFilmsByKeyword(Message message) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        SearchResult searchResult = kinopoiskApiService.searchByKeyword(message.getText(), 1).getOrNull();
        if (searchResult != null) {
            return searchResult.getFilms();
        }
        return null;
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotPath() {
        return webhookPath;
    }
}
