package com.sample.caravanbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.caravanbot.model.FilmEntity;
import com.sample.caravanbot.model.GenreEntity;
import com.sample.caravanbot.util.FilmMapper;
import com.sample.caravanbot.util.KinopoiskApiServiceForSimilarFilms;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchItem;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchResult;
import com.truedev.kinoposk.api.service.KinopoiskApiService;
import lombok.SneakyThrows;
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

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CaravanTelegramBot extends TelegramWebhookBot {

    private static final Pattern PATTERN_ID = Pattern.compile("^\\d+$");
    private static final Pattern PATTERN_SIMILAR = Pattern.compile("s/\\d+$");
    private static final Pattern PATTERN_DETAILED = Pattern.compile("d/\\d+$");
    private Map<String, Integer> genres;

    private String webhookPath;
    private String botUserName;
    private String botToken;
    private String apiToken;

    private long chatId = -1;

    //TODO("С answer и lastClickedFilm надо что-то сделать,
    // чтобы норм старые отправленные сообщения работали,
    // или удалять старые, как вариант")
    private Message answer;
    private FilmEntity lastClickedFilm;

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
                FilmEntity film = getFilmById(callbackData);
                SendMessage sendMessage = new SendMessage()
                        .setText(film.getExtendedToString())
                        .setReplyMarkup(getFilmKeyboardMarkup(callbackData));
                lastClickedFilm = film;
                answer = printMessage(sendMessage);
            } else if (PATTERN_DETAILED.matcher(callbackData).matches()) {
                EditMessageText editMessageText = new EditMessageText()
                        .setChatId(answer.getChatId())
                        .setMessageId(answer.getMessageId())
                        .setReplyMarkup(getFilmKeyboardMarkup(lastClickedFilm.getKinopoiskId()))
                        .setText(lastClickedFilm.getExtendedDescription());
                execute(editMessageText);
            } else if (PATTERN_SIMILAR.matcher(callbackData).matches()) {
                List<FilmEntity> films = getFilmsByFilters();
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
        List<FilmEntity> films = getFilmsByKeyword(filmName);
        if (films.size() == 0) {
            printMessage(String.format("По вашему запросу \"%s\" ничего не найдено.", filmName));
        } else if (films.size() == 1) {
            String filmId = String.valueOf(films.get(0).getKinopoiskId());
            FilmEntity film = getFilmById(filmId);
            SendMessage sendMessage = new SendMessage()
                    .setText(film.getExtendedToString())
                    .setReplyMarkup(getFilmKeyboardMarkup(filmId));
            lastClickedFilm = film;
            answer = printMessage(sendMessage);
        } else {
            StringBuilder sb = new StringBuilder();
            films.forEach(film -> sb.append(film).append("\n\n"));

            SendMessage sendMessage = new SendMessage()
                    .setReplyMarkup(getFilmsButtons(films))
                    .setText(sb.toString());

            printMessage(sendMessage);
        }
    }

    private void initDefaults(Message message) throws IOException {
        if (chatId == -1) {
            chatId = message.getChatId();
        }

        if (genres == null) {
            ObjectMapper mapper = new ObjectMapper();
            GenreEntity[] genres = mapper.readValue(new FileReader("src/genres.json"), GenreEntity[].class);
            this.genres = Arrays.stream(genres).collect(Collectors.toMap(GenreEntity::getName, GenreEntity::getId));
        }
    }

    private boolean isUserSendNotEmptyMessage(Update update) {
        return update.getMessage() != null && update.getMessage().hasText();
    }

    private FilmEntity getFilmById(String filmId) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        return FilmMapper.mapToFilmEntity(
                kinopoiskApiService.getFilm(Integer.parseInt(filmId), new ArrayList<>()).getOrNull()
        );
    }

    private InlineKeyboardMarkup getFilmsButtons(List<FilmEntity> films) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();

        for (int i = 0; i < films.size(); i++) {
            keyboardRow.add(new InlineKeyboardButton()
                    .setCallbackData(String.valueOf(films.get(i).getKinopoiskId()))
                    .setText(String.valueOf(i + 1)));
        }

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup getFilmKeyboardMarkup(String filmId) {
        FilmEntity film = getFilmById(filmId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<InlineKeyboardButton> firstKeyboardRow = Arrays.asList(
                new InlineKeyboardButton()
                        .setCallbackData("s/" + film.getKinopoiskId())
                        .setText("Похожее"),
                new InlineKeyboardButton()
                        .setCallbackData("d/" + film.getKinopoiskId())
                        .setText("Подробнее")
        );

        List<InlineKeyboardButton> secondKeyboardRow = Collections.singletonList(
                new InlineKeyboardButton()
                        .setUrl("https://nono.games/film/" + filmId)
                        .setText("Смотреть фильм")
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

    private List<FilmEntity> getFilmsByKeyword(String keyword) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        SearchResult searchResult = kinopoiskApiService.searchByKeyword(keyword, 1).getOrNull();
        if (searchResult != null) {
            List<SearchItem> films = searchResult.getFilms();
            int filmsQuantity = films.size();
            AtomicInteger atomicInteger = new AtomicInteger();
            return films.stream()
                    .map(film -> FilmMapper.mapToFilmEntity(film, atomicInteger.incrementAndGet()))
                    .collect(Collectors.toList())
                    .subList(0, Math.min(filmsQuantity, 8));
        }
        return Collections.emptyList();
    }

    private List<FilmEntity> getFilmsByFilters() {
        KinopoiskApiServiceForSimilarFilms service = new KinopoiskApiServiceForSimilarFilms();
        SearchResult searchResult = service.searchByKeyword(getGenresIdsAsString()).getOrNull();

        if (searchResult != null) {
            AtomicInteger number = new AtomicInteger();
            return searchResult.getFilms()
                    .stream()
                    .map(film -> FilmMapper.mapToFilmEntity(film, 0))
                    .filter(filmEntity -> isNotSame(filmEntity, number))
                    .collect(Collectors.toList())
                    .subList(0, Math.min(searchResult.getFilms().size(), 8));

        }
        return Collections.emptyList();
    }

    private String getGenresIdsAsString() {
        return lastClickedFilm
                .getGenreNames()
                .stream()
                .mapToInt(genre -> genres.get(genre))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining("%2C"));
    }

    private boolean isNotSame(FilmEntity similarFilm, AtomicInteger atomicInteger) {
        boolean isNotSame = similarFilm != null && similarFilm.getKinopoiskId() != lastClickedFilm.getKinopoiskId();
        if (isNotSame) {
            similarFilm.setNumber(atomicInteger.incrementAndGet());
        }
        return isNotSame;
    }
}

