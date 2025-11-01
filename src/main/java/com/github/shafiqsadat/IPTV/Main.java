package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.*;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) throws IOException {
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down IPTV Bot...");
            RedisManager.close();
            logger.info("IPTV Bot shut down successfully");
        }));
        
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            
            // Register the bot
            telegramBotsApi.registerBot(new IPTVBot(PropertiesReader.getInstance().getBotToken()));
            
            logger.info("✅ IPTV Bot is now running and ready to serve users!");
            logger.info("Press Ctrl+C to stop the bot");
            
            // Keep the application running
            Thread.currentThread().join();
        } catch (TelegramApiException e) {
            logger.error("❌ Failed to start IPTV Bot", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.info("Bot interrupted, shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}