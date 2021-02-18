package com.sample.caravanbot;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sample.caravanbot.model.Genres;
import com.sample.caravanbot.model.SimilarResult;
import com.truedev.kinoposk.api.model.common.Genre;
import com.truedev.kinoposk.api.model.movie.Common;
import com.truedev.kinoposk.api.model.movie.Film;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchItem;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchResult;
import com.truedev.kinoposk.api.service.KinopoiskApiService;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CaravanTelegramBot extends TelegramWebhookBot {

    private static final Pattern PATTERN_ID = Pattern.compile("^\\d+$");
    private static final Pattern PATTERN_SIMILAR = Pattern.compile("similar\\d+$");
    private static final Pattern PATTERN_DETAILS = Pattern.compile("detailed\\d+$");
    private static final Pattern ONLY_DIGIT = Pattern.compile("[^0-9]");
    private Genres genres;

    private String webhookPath;
    private String botUserName;
    private String botToken;
    private String apiToken;
    private long chatId;
    private Message answer;

    public CaravanTelegramBot(DefaultBotOptions options) throws FileNotFoundException {
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

    @SneakyThrows
    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (genres == null) {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader("src/genres.json"));
            genres = gson.fromJson(reader, Genres.class);
        }
        if (update.getMessage() != null && update.getMessage().hasText()) {
            Message message = update.getMessage();
            try {
                //TODO(придумать, как разграничить логику или сделать что-то такое https://imgur.com/a/PKVbZlZ)
                String messageText = message.getText();
                switch (messageText) {
                    case "/start": {
                        printMessage(message, "Введите название фильма");
                        break;
                    }
                    case "/info": {
                        printMessage(message, "Info");
                        break;
                    }
                    case "/random": {

                    }
                    default: {
                        this.chatId = update.getMessage().getChatId();
                        SendMessage sendMessage = new SendMessage().setChatId(this.chatId);
                        List<SearchItem> films = getFilmsByKeyword(message.getText());
                        if (films.size() == 0) {
                            execute(sendMessage.setText(String.format("По вашему запросу \"%s\" ничего не найдено.", messageText)));
                        } else {
                            StringBuilder text = new StringBuilder("");
                            for (SearchItem film : films) {
                                text.append(getFilm(film));
                            }
                            sendMessage.setReplyMarkup(this.getSearchKeyboardMarkup1(films));
                            execute(sendMessage.setText(text.toString().replaceAll("null", "Отсутствует")));
                        }
                        break;
                    }
                }

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.hasCallbackQuery()) {
            if (PATTERN_ID.matcher(update.getCallbackQuery().getData()).matches()) {
                String fimId = update.getCallbackQuery().getData();
                Film film = getFilmById(fimId);
                Common data = film.getData();
                String text = new String("");
                text = data.getNameRu() + " " + "[" + data.getYear() + "]" + "\nОписание:\n" + (data.getDescription()) + "\n\n" + data.getPosterUrl();
                if (answer != null) {
                    EditMessageText editMessageText = new EditMessageText();
                    editMessageText.setChatId(answer.getChatId());
                    editMessageText.setMessageId(answer.getMessageId());

                    try {
                        if ((editMessageText.setReplyMarkup(getFilmKeyboardMarkup(fimId))
                                .setText(text.replaceAll("null", "Отсутствует"))).getText().equals(answer.getText()))
                            return null;
                        else execute(editMessageText.setReplyMarkup(getFilmKeyboardMarkup(fimId))
                                .setText(text.replaceAll("null", "Отсутствует")));
                    } catch (TelegramApiRequestException e) {
                        System.out.println("Massage not modified");
                    }
                } else {
                    this.answer = execute(new SendMessage().setReplyMarkup(getFilmKeyboardMarkup(fimId))
                            .setChatId(chatId)
                            .setText(text.replaceAll("null", "Отсутствует")));
                }
            }
            if (PATTERN_SIMILAR.matcher(update.getCallbackQuery().getData()).matches()) {
                String fimId = update.getCallbackQuery().getData().replaceAll(ONLY_DIGIT.toString(), "");
                Film film = getFilmById(fimId);
                Common data = film.getData();
                List<com.sample.caravanbot.model.Film> films = getFilmsByFilters(data.getGenres());
                SendMessage sendMessage = new SendMessage().setChatId(chatId);
                StringBuilder text = new StringBuilder("");
                for (com.sample.caravanbot.model.Film f : films) {
                    text.append(getFilm(f));
                }
                sendMessage.setReplyMarkup(getSearchKeyboardMarkup(films));
                execute(sendMessage.setText(text.toString().replaceAll("null", "Отсутствует")));
                answer = null;
            }
            if (PATTERN_DETAILS.matcher(update.getCallbackQuery().getData()).matches()) {
                String fimId = update.getCallbackQuery().getData().replaceAll(ONLY_DIGIT.toString(), "");
                Film film = getFilmById(fimId);
                EditMessageText editMessageText = new EditMessageText();
                editMessageText.setChatId(answer.getChatId());
                editMessageText.setMessageId(answer.getMessageId());
                Common data = film.getData();
                String text;
                text = (data.getNameRu() + " " + "[" + data.getYear() + "]" + "\n\nОписание:\n" + (data.getDescription())
                        + "\n\nЖанр: " + data.getGenres().stream()
                        .map(Genre::getGenre)
                        .collect(Collectors.toList())
                        .toString()
                        .replaceAll("[\\[\\]]", "")
                        + "\n\nДлина фильма: " + data.getFilmLength()
                        + "\n\nФакты: " + data.getFacts().stream()
                        .limit(5)
                        .collect(Collectors.toList())
                        .toString()
                        .replaceAll("[\\[\\]]", "")
                        .replaceAll("\\.,", ".\n\t")
                        + "\n" + data.getPosterUrl());
                execute(editMessageText.setReplyMarkup(getFilmKeyboardMarkup(fimId))
                        .setText(text.replaceAll("null", "Отсутствует")));
            }
        }

        return null;
    }

    private Film getFilmById(String filmId) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        return kinopoiskApiService.getFilm(Integer.parseInt(filmId), new ArrayList<>()).getOrNull();
    }

    private InlineKeyboardMarkup getSearchKeyboardMarkup1(List<SearchItem> searchItem) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        for (int i = 1; i <= searchItem.size(); i++) {
            keyboardRow.add(new InlineKeyboardButton()
                    .setCallbackData("" + searchItem.get(i - 1).getKinopoiskId())
                    .setText("" + i));
        }

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getSearchKeyboardMarkup(List<com.sample.caravanbot.model.Film> searchItem) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        for (int i = 1; i <= searchItem.size(); i++) {
            keyboardRow.add(new InlineKeyboardButton()
                    .setCallbackData("" + searchItem.get(i - 1).getFilmId())
                    .setText("" + i));
        }

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getFilmKeyboardMarkup(String filmId) {
        Film film = getFilmById(filmId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        keyboardRow.add(new InlineKeyboardButton()
                .setCallbackData("similar" + film.getData().getKinopoiskId())
                .setText("Похожее"));
        keyboardRow.add(new InlineKeyboardButton()
                .setCallbackData("detailed" + film.getData().getKinopoiskId())
                .setText("Подробнее"));
        keyboardRow.add(new InlineKeyboardButton()
                .setUrl(film.getData().getWebUrl())
                .setText("На Кинопоиск"));

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private void printMessage(Message message, String text) throws TelegramApiException {
        execute(new SendMessage(message.getChatId(), text));
    }

/*    private Page getPage(Message message, int pageCount) {
        SearchResult items = getFilmsByKeyword(message, pageCount);
        if (items.getFilms().size() > 0) {
            List<Film> films = items.getFilms().stream()
                    .filter(this::isFilmHasRuName)
                    .map(this::getFilm)
                    .collect(Collectors.toList());

            return new Page(items.getPagesCount(), films);
        }
        *//*TODO: make me better, not null*//*
        return null;
    }*/


    private boolean isFilmHasRuName(SearchItem searchItem) {
        return searchItem != null && searchItem.getNameRu() != null && !searchItem.getNameRu().isEmpty();
    }

    private String getFilm(SearchItem searchItem) {
        if (!Objects.equals(searchItem.getNameRu(), "")) {
            return String.format("● %s \\ %s [%s] Рейтинг: %s\n\n", searchItem.getNameRu(), searchItem.getNameEn(), searchItem.getYear(), searchItem.getRating());
        } else {
            return String.format("● %s [%s] Рейтинг: %s\n\n", searchItem.getNameEn(), searchItem.getYear(), searchItem.getRating());
        }
    }

    private String getFilm(com.sample.caravanbot.model.Film searchItem) {
        if (!Objects.equals(searchItem.getNameRu(), "")) {
            return String.format("● %s \\ %s [%s] Рейтинг: %s\n\n", searchItem.getNameRu(), searchItem.getNameEn(), searchItem.getYear(), searchItem.getRating());
        } else {
            return String.format("● %s [%s] Рейтинг: %s\n\n", searchItem.getNameEn(), searchItem.getYear(), searchItem.getRating());
        }
    }

    private List<SearchItem> getFilmsByKeyword(String keyword) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        SearchResult searchResult = kinopoiskApiService.searchByKeyword(keyword, 1).getOrNull();
        assert searchResult != null && searchResult.getSearchFilmsCountResult() != null;
        if (searchResult.getFilms().size() < 8) {
            return searchResult.getFilms().subList(0, searchResult.getSearchFilmsCountResult());
        } else {
            return searchResult.getFilms().subList(0, 8);
        }
    }

    private List<com.sample.caravanbot.model.Film> getFilmsByFilters(List<Genre> genres) throws IOException {
        StringBuilder param = new StringBuilder("");
        for (Genre i : genres) {
            for (Genre j : this.genres.getGenres()) {
                if (i.getGenre().equals(j.getGenre())) {
                    param.append(j.getId()).append(",");
                }
            }
        }

        String url = "https://kinopoiskapiunofficial.tech/api/v2.1/films/search-by-filters?genre=" + param;

        HttpClient client = HttpClients.custom().build();
        HttpUriRequest request = RequestBuilder.get()
                .setUri(url)
                .setHeader("X-API-KEY", " b7f34379-c0b7-4046-931f-119b3b30a7d9")
                .build();
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        Gson g = new Gson();
        SimilarResult searchResult = g.fromJson(responseString, SimilarResult.class);

        assert searchResult != null;
        if (searchResult.getFilms().size() < 8) {
            return searchResult.getFilms();
        } else {
            return searchResult.getFilms().subList(0, 8);
        }
    }
}

