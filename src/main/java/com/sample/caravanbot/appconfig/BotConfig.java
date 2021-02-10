package com.sample.caravanbot.appconfig;

import com.sample.caravanbot.CaravanTelegramBot;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "telegrambot")
public class BotConfig {

    private String webhookPath;
    private String botUserName;
    private String botToken;
    private String apiToken;

    @Bean
    public CaravanTelegramBot CaravanBot(){
        DefaultBotOptions options = ApiContext.getInstance(DefaultBotOptions.class);

        CaravanTelegramBot caravanTelegramBot = new CaravanTelegramBot(options);
        caravanTelegramBot.setWebhookPath(webhookPath);
        caravanTelegramBot.setBotUserName(botUserName);
        caravanTelegramBot.setBotToken(botToken);
        caravanTelegramBot.setApiToken(apiToken);

        return caravanTelegramBot;
    }
}
