package com.sample.caravanbot;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sample.caravanbot.model.Film;
import com.sample.caravanbot.model.FilmExtended;
import com.sample.caravanbot.model.Genres;
import com.sample.caravanbot.model.SimilarResult;
import com.sample.caravanbot.util.FilmExtendedMapper;
import com.sample.caravanbot.util.FilmMapper;
import com.truedev.kinoposk.api.model.common.Genre;
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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CaravanTelegramBot extends TelegramWebhookBot {

    private static final Pattern PATTERN_ID = Pattern.compile("^\\d+$");
    private static final Pattern PATTERN_SIMILAR = Pattern.compile("s/\\d+$");
    private static final Pattern PATTERN_DETAILED = Pattern.compile("d/\\d+$");
    private Genres genres;

    private String webhookPath;
    private String botUserName;
    private String botToken;
    private String apiToken;

    private long chatId = -1;

    //TODO("С answer и lastClickedFilm надо что-то сделать,
    // чтобы норм старые отправленные сообщения работали,
    // или удалять старые, как вариант")
    private Message answer;
    private FilmExtended lastClickedFilm;

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
        if (isUserSendNotEmptyMessage(update)) {
            Message message = update.getMessage();

            initDefaults(message);
            defineCommand(message);
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            if (PATTERN_ID.matcher(callbackData).matches()) {
                FilmExtended film = getFilmById(callbackData);
                SendMessage sendMessage = new SendMessage()
                        .setText(film.toString())
                        .setReplyMarkup(getFilmKeyboardMarkup(callbackData));
                lastClickedFilm = film;
                this.answer = printMessage(sendMessage);
            } else if (PATTERN_DETAILED.matcher(callbackData).matches()) {
                EditMessageText editMessageText = new EditMessageText()
                        .setChatId(answer.getChatId())
                        .setMessageId(answer.getMessageId())
                        .setReplyMarkup(getFilmKeyboardMarkup(lastClickedFilm.getFilmId()))
                        .setText(lastClickedFilm.getExtendedDescription());
                execute(editMessageText);
            } else if (PATTERN_SIMILAR.matcher(callbackData).matches()) {
                List<Film> films = getFilmsByFilters(lastClickedFilm.getGenres());
                StringBuilder sb = new StringBuilder();
                films.forEach(film -> sb.append(film).append("\n\n"));
                SendMessage sendMessage = new SendMessage()
                        .setReplyMarkup(getFilmsButtons(films))
                        .setText(sb.toString());

                printMessage(sendMessage);
            }

        }
        return null;
    }

    private InlineKeyboardMarkup getFilmKeyboardMarkup(long filmId) {
        return getFilmKeyboardMarkup(String.valueOf(filmId));
    }

    private void defineCommand(Message message) {
        String messageText = message.getText();
        switch (messageText) {
            case "/start": {
                printMessage("Введите название фильма");
                break;
            }
            case "/info": {
                printMessage("Info");
                break;
            }
            case "/random": {

            }
            default: {
                searchFilm(messageText);
                break;
            }
        }
    }

    private void searchFilm(String filmName) {
        List<Film> films = getFilmsByKeyword(filmName);
        if (films.size() == 0) {
            printMessage(String.format("По вашему запросу \"%s\" ничего не найдено.", filmName));
        } else {
            StringBuilder sb = new StringBuilder();
            films.forEach(film -> sb.append(film).append("\n\n"));

            SendMessage sendMessage = new SendMessage()
                    .setReplyMarkup(getFilmsButtons(films))
                    .setText(sb.toString());

            printMessage(sendMessage);
        }
    }

    private void initDefaults(Message message) throws FileNotFoundException {
        if (chatId == -1) {
            chatId = message.getChatId();
        }

        if (genres == null) {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader("src/genres.json"));
            genres = gson.fromJson(reader, Genres.class);
        }
    }

    private boolean isUserSendNotEmptyMessage(Update update) {
        return update.getMessage() != null && update.getMessage().hasText();
    }

    private FilmExtended getFilmById(String filmId) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        return FilmExtendedMapper.mapToFilmExtendedEntity(
                kinopoiskApiService.getFilm(Integer.parseInt(filmId), new ArrayList<>()).getOrNull()
        );
    }

    private InlineKeyboardMarkup getFilmsButtons(List<Film> films) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        for (int i = 0; i < films.size(); i++) {
            keyboardRow.add(new InlineKeyboardButton()
                    .setCallbackData(String.valueOf(films.get(i).getFilmId()))
                    .setText(String.valueOf(i + 1)));
        }

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getFilmKeyboardMarkup(String filmId) {
        FilmExtended film = getFilmById(filmId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> firstKeyboardRow = Arrays.asList(
                new InlineKeyboardButton()
                        .setCallbackData("s/" + film.getFilmId())
                        .setText("Похожее"),
                new InlineKeyboardButton()
                        .setCallbackData("d/" + film.getFilmId())
                        .setText("Подробнее")
        );

        List<InlineKeyboardButton> secondKeyboardRow = Collections.singletonList(
                new InlineKeyboardButton()
                        .setUrl(film.getWebUrl())
                        .setText("На Кинопоиск")
        );

        inlineKeyboardMarkup.setKeyboard(Arrays.asList(firstKeyboardRow, secondKeyboardRow));
        return inlineKeyboardMarkup;
    }

    private void printMessage(String text) {
        printMessage(new SendMessage().setText(text));
    }

    private Message printMessage(SendMessage sendMessage) {
        sendMessage.setChatId(chatId);
        try {
            return execute(sendMessage);
        } catch (TelegramApiException ignored) {
            //TODO("add exceptions to log mb")
        }
        return null;
    }

    private List<Film> getFilmsByKeyword(String keyword) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        SearchResult searchResult = kinopoiskApiService.searchByKeyword(keyword, 1).getOrNull();
        if (searchResult != null) {
            List<SearchItem> films = searchResult.getFilms();
            int filmsQuantity = films.size();

            return films.stream()
                    .map(FilmMapper::mapToFilmEntity)
                    .collect(Collectors.toList())
                    .subList(0, Math.min(filmsQuantity, 8));
        }
        return Collections.emptyList();
    }

    private List<Film> getFilmsByFilters(List<Genre> genres) throws IOException {
        StringBuilder param = new StringBuilder();
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
                .setHeader("X-API-KEY", apiToken)
                .build();
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        Gson g = new Gson();
        SimilarResult searchResult = g.fromJson(responseString, SimilarResult.class);

        if (searchResult != null) {
            return searchResult.getFilms().subList(0, Math.min(searchResult.getFilms().size(), 8));
        }

        return Collections.emptyList();
    }
}

