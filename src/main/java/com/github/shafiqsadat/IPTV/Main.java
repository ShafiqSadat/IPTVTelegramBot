package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.PropertiesReader;
import com.github.shafiqsadat.IPTV.utils.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import redis.clients.jedis.exceptions.JedisException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down IPTV Bot...");
            RedisManager.close();
            logger.info("IPTV Bot shut down successfully");
        }));

        try {
            PropertiesReader config = PropertiesReader.getInstance();
            String botToken = config.getBotToken();
            String botUsername = config.getBotUsername();

            RedisManager.init();

            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new IPTVBot(botToken, botUsername));

            logger.info("IPTV Bot is now running and ready to serve users!");
            logger.info("Press Ctrl+C to stop the bot");

            Thread.currentThread().join();
        } catch (IllegalStateException e) {
            logger.error("Configuration error: {}", e.getMessage());
            System.exit(1);
        } catch (JedisException e) {
            logger.error("Could not connect to Redis ({}). Make sure Redis is running and check "
                    + "redisHost/redisPort/redisPassword in local.properties.", e.getMessage());
            System.exit(1);
        } catch (TelegramApiException e) {
            logger.error("Failed to start IPTV Bot", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.info("Bot interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}
