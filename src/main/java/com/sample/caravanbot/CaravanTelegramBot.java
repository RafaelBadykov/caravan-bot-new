package com.sample.caravanbot;

import com.sample.caravanbot.model.Film;
import com.sample.caravanbot.model.Page;
import com.truedev.kinoposk.api.model.movie.Common;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchItem;
import com.truedev.kinoposk.api.model.search.movie.keyword.SearchResult;
import com.truedev.kinoposk.api.service.KinopoiskApiService;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//TODO(refactor(create new classes/services...) + add keyboard menu)
public class CaravanTelegramBot extends TelegramWebhookBot {

    private static final Pattern PATTERN = Pattern.compile("^/\\d+$");
    private static final Pattern PAGE_COUNT_PATTERN = Pattern.compile("^\\d$");

    private String webhookPath;
    private String botUserName;
    private String botToken;
    private String apiToken;

    private Message message;
    private Message answer;

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
                        if (PATTERN.matcher(message.getText()).matches()) {
                            String fimId = message.getText().substring(1);
                            com.truedev.kinoposk.api.model.movie.Film film = getFilmById(fimId);
                            Common data = film.getData();

                            execute(new SendMessage()
                                    .setChatId(message.getChatId())
                                    .setText(
                                            data.getNameRu()
                                                    + "\n" + data.getDescription()
                                                    + "\n" + data.getPosterUrl())
                            );
                        } else {
                            this.message = update.getMessage();
                            Page page = getPage(message, 1);
                            if (page == null) {
                                printMessage(message, String.format("По запросу \"%s\" ничего не найдено.", messageText));
                            } else {
                                answer = execute(new SendMessage(message.getChatId(), Strings.join(page.getFilms(), '\n'))
                                        .setReplyMarkup(getPageKeyboardMarkup(page)));
                            }
                        }
                        break;
                    }
                }

            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.hasCallbackQuery()) {
            if (PAGE_COUNT_PATTERN.matcher(update.getCallbackQuery().getData()).matches()) {
                //TODO (Update message)
                try {
                    EditMessageText editMessageText = new EditMessageText();
                    editMessageText.setChatId(answer.getChatId());
                    editMessageText.setMessageId(answer.getMessageId());
                    int pageCount = Integer.parseInt(update.getCallbackQuery().getData());
                    Page page = getPage(message, pageCount);
                    if (page != null) {
                        editMessageText.setText((Strings.join(page.getFilms(), '\n')));
                        editMessageText.setReplyMarkup(getPageKeyboardMarkup(page));
                        execute(editMessageText);
                    } else {
                        throw new AssertionError();
                    }
                } catch (RuntimeException e) {
                    System.out.println("Error. Unselected request page changing!");
                }
            }
        }
        return null;
    }

    private com.truedev.kinoposk.api.model.movie.Film getFilmById(String filmId) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        return kinopoiskApiService.getFilm(Integer.parseInt(filmId), new ArrayList<>()).getOrNull();
    }

    private InlineKeyboardMarkup getPageKeyboardMarkup(Page page) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> keyboardRow = new ArrayList<>();
        List<Film> films = page.getFilms();
        //TODO(реализовать поиск похожеих фильмов при нажатии на выбранный фильм)


        for (int i = 1; page.getPageCount() >= i; i++) {
            keyboardRow.add(new InlineKeyboardButton().setText(String.valueOf(i))
                    .setCallbackData("" + i));
        }

        inlineKeyboardMarkup.setKeyboard(Collections.singletonList(keyboardRow));
        return inlineKeyboardMarkup;
    }

    private void printMessage(Message message, String text) throws TelegramApiException {
        execute(new SendMessage(message.getChatId(), text));
    }

    private Page getPage(Message message, int pageCount) {
        SearchResult items = getFilmsByKeyword(message, pageCount);
        if (items.getFilms().size() > 0) {
            List<Film> films = items.getFilms().stream()
                    .filter(this::isFilmHasRuName)
                    .map(this::getFilm)
                    .collect(Collectors.toList());

            return new Page(items.getPagesCount(), films);
        }
        /*TODO: make me better, not null*/
        return null;
    }

    private boolean isFilmHasRuName(SearchItem searchItem) {
        return searchItem != null && searchItem.getNameRu() != null && !searchItem.getNameRu().isEmpty();
    }

    private Film getFilm(SearchItem searchItem) {
        String telegramString = String.format("● %s\\%s [%sг.] /%s", searchItem.getNameRu(), searchItem.getNameEn(), searchItem.getYear(), searchItem.getKinopoiskId());
        return new Film(searchItem.getKinopoiskId(), telegramString, searchItem.getGenres());
    }

    private SearchResult getFilmsByKeyword(Message message, int pageCount) {
        KinopoiskApiService kinopoiskApiService = new KinopoiskApiService(apiToken, 15000);
        return kinopoiskApiService.searchByKeyword(message.getText(), pageCount).getOrNull();
    }
}
