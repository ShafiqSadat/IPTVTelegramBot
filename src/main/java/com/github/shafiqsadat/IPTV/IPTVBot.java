package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import redis.clients.jedis.Jedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * IPTV Telegram Bot with enhanced UX, error handling, and code organization
 */
public class IPTVBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(IPTVBot.class);
    private static final String REDIS_USER_KEY = "IPTVBOT_USERS";
    private static final String REDIS_COUNTRY_WAIT_PREFIX = "waitForGetByCountryName:";

    public IPTVBot(String token) {
        super(token);
        logger.info("IPTV Bot initialized successfully");
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            logger.debug("Received update: {}", update);
            System.out.println("📨 Received update from user");
            
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            logger.error("Error processing update", e);
            System.err.println("❌ Error processing update: " + e.getMessage());
            e.printStackTrace();
            handleError(update);
        }
    }

    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Integer messageId = update.getMessage().getMessageId();
        User from = update.getMessage().getFrom();

        logger.info("User {} sent message: {}", from.getId(), messageText);
        System.out.println("💬 User " + from.getId() + " sent: " + messageText);

        try (Jedis redis = RedisManager.getJedis()) {
            if (messageText.equals("/start") || messageText.equals("/help")) {
                handleStartCommand(chatId, messageId, from, redis);
            } else if (messageText.startsWith("Language: ")) {
                handleLanguageSelection(messageText, chatId, messageId);
            } else if (messageText.startsWith("Category: ")) {
                handleCategorySelection(messageText, chatId, messageId);
            } else if (messageText.startsWith("Region: ")) {
                handleRegionSelection(messageText, chatId, messageId);
            } else if (messageText.equals("🔙 Back")) {
                handleBackToMenu(chatId, messageId, from, redis);
            } else if (isWaitingForCountryName(redis, from.getId()) && !messageText.equals("🔙 Back")) {
                handleCountrySearch(messageText, chatId, messageId, redis, from.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling text message", e);
            System.err.println("❌ Error handling message: " + e.getMessage());
            e.printStackTrace();
            sendErrorMessage(chatId, messageId);
        }
    }

    private void handleStartCommand(Long chatId, Integer messageId, User from, Jedis redis) {
        try {
            // Track user
            if (!redis.sismember(REDIS_USER_KEY, from.getId().toString())) {
                redis.sadd(REDIS_USER_KEY, from.getId().toString());
                logger.info("New user registered: {}", from.getId());
            }

            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(Constants.START_IMAGE_URL));
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption(MessageTemplates.getWelcomeMessage(
                from.getFirstName(), 
                from.getLastName()
            ));
            sendPhoto.setReplyMarkup(createMainMenuKeyboard());
            sendPhoto.setReplyToMessageId(messageId);

            execute(sendPhoto);
            logger.info("Sent welcome message to user: {}", from.getId());
        } catch (TelegramApiException e) {
            logger.error("Error sending welcome message", e);
            throw new RuntimeException(e);
        }
    }

    private void handleLanguageSelection(String messageText, Long chatId, Integer messageId) {
        String languageName = extractName(messageText, "Language: ");
        downloadAndSendIPTV(languageName, "Language", chatId, messageId, this::getIPTVByLanguage);
    }

    private void handleCategorySelection(String messageText, Long chatId, Integer messageId) {
        String categoryName = extractName(messageText, "Category: ");
        downloadAndSendIPTV(categoryName, "Category", chatId, messageId, this::getIPTVByCategory);
    }

    private void handleRegionSelection(String messageText, Long chatId, Integer messageId) {
        String regionName = extractName(messageText, "Region: ");
        downloadAndSendIPTV(regionName, "Region", chatId, messageId, this::getIPTVByRegion);
    }

    private String extractName(String messageText, String prefix) {
        return messageText.replace(prefix, "").split("\\(")[0].trim();
    }

    private void downloadAndSendIPTV(String name, String type, Long chatId, Integer messageId, 
                                     IPTVFetcher fetcher) {
        CompletableFuture.runAsync(() -> {
            try {
                // Show typing action for better UX
                sendChatAction(ActionType.TYPING, chatId);
                
                // Send status message
                SendMessage statusMsg = new SendMessage();
                statusMsg.setChatId(chatId);
                statusMsg.setText(MessageTemplates.getDownloadingMessage());
                statusMsg.setReplyToMessageId(messageId);
                execute(statusMsg);
                
                // Find and download IPTV
                IPTVModel iptvModel = fetcher.fetch(name);
                
                if (iptvModel != null) {
                    // Show upload action
                    sendChatAction(ActionType.UPLOADDOCUMENT, chatId);
                    
                    File file = FileDownloader.downloadFile(
                        iptvModel.getStreamLink(), 
                        iptvModel.getName() + ".m3u"
                    );
                    
                    SendDocument sendDocument = new SendDocument();
                    sendDocument.setChatId(chatId);
                    sendDocument.setDocument(new InputFile(file));
                    sendDocument.setCaption(MessageTemplates.getChannelInfoCaption(
                        type,
                        iptvModel.getName(),
                        iptvModel.getCount(),
                        iptvModel.getStreamLink()
                    ));
                    sendDocument.setReplyToMessageId(messageId);
                    sendDocument.setParseMode("MarkdownV2");
                    
                    execute(sendDocument);
                    
                    // Clean up
                    if (file.exists()) {
                        file.delete();
                    }
                    
                    logger.info("Sent IPTV file for {} to user", name);
                    System.out.println("✅ Sent IPTV file for " + type + ": " + name);
                } else {
                    sendMessageText(MessageTemplates.getNoChannelsFoundMessage(), chatId, messageId);
                }
            } catch (Exception e) {
                logger.error("Error downloading and sending IPTV", e);
                System.err.println("❌ Error downloading IPTV: " + e.getMessage());
                e.printStackTrace();
                sendErrorMessage(chatId, messageId);
            }
        });
    }

    private IPTVModel getIPTVByLanguage(String name) throws IOException {
        return findIPTVModel(IPTVParser.getIPTVListByLanguages(), name);
    }

    private IPTVModel getIPTVByCategory(String name) throws IOException {
        return findIPTVModel(IPTVParser.getIPTVListByCategories(), name);
    }

    private IPTVModel getIPTVByRegion(String name) throws IOException {
        return findIPTVModel(IPTVParser.getIPTVListByRegion(), name);
    }

    private IPTVModel findIPTVModel(List<IPTVModel> list, String name) {
        return list.stream()
                   .filter(model -> model.getName().equalsIgnoreCase(name))
                   .findFirst()
                   .orElse(null);
    }

    private void handleBackToMenu(Long chatId, Integer messageId, User from, Jedis redis) {
        try {
            // Clear any waiting states
            redis.del(REDIS_COUNTRY_WAIT_PREFIX + from.getId());
            
            // Remove custom keyboard
            ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
            replyKeyboardRemove.setRemoveKeyboard(true);
            
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setReplyMarkup(replyKeyboardRemove);
            sendMessage.setText(MessageTemplates.getBackToMainMenuMessage());
            execute(sendMessage);
            
            // Send main menu
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setPhoto(new InputFile(Constants.START_IMAGE_URL));
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption(MessageTemplates.getWelcomeMessage(
                from.getFirstName(), 
                from.getLastName()
            ));
            sendPhoto.setReplyMarkup(createMainMenuKeyboard());
            
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.error("Error returning to main menu", e);
            throw new RuntimeException(e);
        }
    }

    private boolean isWaitingForCountryName(Jedis redis, Long userId) {
        String value = redis.get(REDIS_COUNTRY_WAIT_PREFIX + userId);
        return "true".equals(value);
    }

    private void handleCountrySearch(String messageText, Long chatId, Integer messageId, 
                                     Jedis redis, Long userId) {
        // Clear the waiting state first
        redis.del(REDIS_COUNTRY_WAIT_PREFIX + userId);
        
        CompletableFuture.runAsync(() -> {
            try {
                sendChatAction(ActionType.TYPING, chatId);
                
                List<IPTVModel> countries = IPTVParser.getIPTVListByCountries();
                IPTVModel found = null;
                
                // Try to find exact or partial match
                for (IPTVModel model : countries) {
                    if (model.getName().toLowerCase().contains(messageText.toLowerCase())) {
                        found = model;
                        break;
                    }
                }
                
                if (found != null) {
                    sendChatAction(ActionType.UPLOADDOCUMENT, chatId);
                    
                    File file = FileDownloader.downloadFile(
                        found.getStreamLink(), 
                        found.getName() + ".m3u"
                    );
                    
                    SendDocument sendDocument = new SendDocument();
                    sendDocument.setChatId(chatId);
                    sendDocument.setDocument(new InputFile(file));
                    sendDocument.setCaption(MessageTemplates.getChannelInfoCaption(
                        "Country",
                        found.getName(),
                        found.getCount(),
                        found.getStreamLink()
                    ));
                    sendDocument.setReplyToMessageId(messageId);
                    sendDocument.setParseMode("MarkdownV2");
                    
                    execute(sendDocument);
                    
                    // Clean up
                    if (file.exists()) {
                        file.delete();
                    }
                    
                    logger.info("Sent IPTV file for country: {}", found.getName());
                    System.out.println("✅ Sent IPTV file for country: " + found.getName());
                } else {
                    sendMessageText(MessageTemplates.getNoChannelsFoundMessage(), chatId, messageId);
                }
            } catch (Exception e) {
                logger.error("Error handling country search", e);
                System.err.println("❌ Error in country search: " + e.getMessage());
                e.printStackTrace();
                sendErrorMessage(chatId, messageId);
            }
        });
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        var messageAccessible = update.getCallbackQuery().getMessage();
        Integer messageId = (messageAccessible instanceof Message) ? 
            ((Message) messageAccessible).getMessageId() : null;
        User from = update.getCallbackQuery().getFrom();

        logger.info("User {} pressed button: {}", from.getId(), callbackData);

        if (messageId == null) {
            logger.warn("Message is not accessible for callback query");
            return;
        }
        
        Message message = (Message) messageAccessible;

        try (Jedis redis = RedisManager.getJedis()) {
            switch (callbackData) {
                case "getIPTV" -> showIPTVOptions(chatId, message);
                case "howITWorks" -> showHowItWorks(chatId, message);
                case "whatISIPTV" -> showWhatIsIPTV(chatId, message);
                case "getCategory" -> showCategoryList(chatId);
                case "getByLanguage" -> showLanguageList(chatId);
                case "getByCountry" -> showCountrySearch(chatId, messageId, redis, from.getId());
                case "getByRegion" -> showRegionList(chatId);
                case "goBack" -> goBackToMainMenu(chatId, message, from);
                default -> logger.warn("Unknown callback data: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error("Error handling callback query", e);
            sendErrorMessage(chatId, messageId);
        }
    }

    private void showIPTVOptions(Long chatId, Message message) throws TelegramApiException {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        rows.add(List.of(
            InlineKeyboardButton.builder().text("📂 By Category").callbackData("getCategory").build(),
            InlineKeyboardButton.builder().text("🌐 By Language").callbackData("getByLanguage").build()
        ));
        rows.add(List.of(
            InlineKeyboardButton.builder().text("🏳️ By Country").callbackData("getByCountry").build(),
            InlineKeyboardButton.builder().text("®️ By Region").callbackData("getByRegion").build()
        ));
        rows.add(List.of(
            InlineKeyboardButton.builder().text("🔙 Back").callbackData("goBack").build()
        ));
        
        markup.setKeyboard(rows);
        
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(chatId);
        editMessageCaption.setCaption("📺 Please select an option to get IPTV:");
        editMessageCaption.setReplyMarkup(markup);
        editMessageCaption.setMessageId(message.getMessageId());
        
        execute(editMessageCaption);
    }

    private void showHowItWorks(Long chatId, Message message) throws TelegramApiException {
        InlineKeyboardMarkup markup = createBackButton();
        
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(chatId);
        editMessageCaption.setCaption(MessageTemplates.getHowItWorksMessage());
        editMessageCaption.setReplyMarkup(markup);
        editMessageCaption.setMessageId(message.getMessageId());
        
        execute(editMessageCaption);
    }

    private void showWhatIsIPTV(Long chatId, Message message) throws TelegramApiException {
        InlineKeyboardMarkup markup = createBackButton();
        
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(chatId);
        editMessageCaption.setCaption(MessageTemplates.getWhatIsIPTVMessage());
        editMessageCaption.setReplyMarkup(markup);
        editMessageCaption.setMessageId(message.getMessageId());
        
        execute(editMessageCaption);
    }

    private void showCategoryList(Long chatId) throws IOException, TelegramApiException {
        List<IPTVModel> categories = IPTVParser.getIPTVListByCategories();
        ReplyKeyboardMarkup keyboard = createSelectionKeyboard(categories, "Category");
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(keyboard);
        sendMessage.setText(MessageTemplates.getSelectCategoryMessage());
        
        execute(sendMessage);
    }

    private void showLanguageList(Long chatId) throws IOException, TelegramApiException {
        List<IPTVModel> languages = IPTVParser.getIPTVListByLanguages();
        ReplyKeyboardMarkup keyboard = createSelectionKeyboard(languages, "Language");
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(keyboard);
        sendMessage.setText(MessageTemplates.getSelectLanguageMessage());
        
        execute(sendMessage);
    }

    private void showRegionList(Long chatId) throws IOException, TelegramApiException {
        List<IPTVModel> regions = IPTVParser.getIPTVListByRegion();
        ReplyKeyboardMarkup keyboard = createSelectionKeyboard(regions, "Region");
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(keyboard);
        sendMessage.setText(MessageTemplates.getSelectRegionMessage());
        
        execute(sendMessage);
    }

    private void showCountrySearch(Long chatId, Integer messageId, Jedis redis, Long userId) {
        redis.setex(REDIS_COUNTRY_WAIT_PREFIX + userId, 300, "true"); // 5 minutes expiry
        sendMessageText(MessageTemplates.getCountrySearchMessage(), chatId, messageId);
    }

    private void goBackToMainMenu(Long chatId, Message message, User from) throws TelegramApiException {
        InlineKeyboardMarkup markup = createMainMenuKeyboard();
        
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(chatId);
        editMessageCaption.setCaption(MessageTemplates.getWelcomeMessage(
            from.getFirstName(), 
            from.getLastName()
        ));
        editMessageCaption.setReplyMarkup(markup);
        editMessageCaption.setMessageId(message.getMessageId());
        
        execute(editMessageCaption);
    }

    private InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        rows.add(List.of(
            InlineKeyboardButton.builder().text("📺 Get IPTV").callbackData("getIPTV").build(),
            InlineKeyboardButton.builder().text("🧐 How it works?").callbackData("howITWorks").build()
        ));
        rows.add(List.of(
            InlineKeyboardButton.builder().text("👀 What is IPTV?").callbackData("whatISIPTV").build()
        ));
        
        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup createBackButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
            InlineKeyboardButton.builder().text("🔙 Back").callbackData("goBack").build()
        ));
        markup.setKeyboard(rows);
        return markup;
    }

    private ReplyKeyboardMarkup createSelectionKeyboard(List<IPTVModel> items, String prefix) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        int count = 0;

        for (IPTVModel item : items) {
            if (count == 2) {
                rows.add(row);
                row = new KeyboardRow();
                count = 0;
            }
            row.add(new KeyboardButton(prefix + ": " + item.getName() + " (" + item.getCount() + ")"));
            count++;
        }

        if (!row.isEmpty()) {
            rows.add(row);
        }

        // Add back button
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("🔙 Back"));
        rows.add(backRow);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        
        return keyboard;
    }

    private void sendMessageText(String text, Long chatId, Integer messageId) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(text);
            if (messageId != null) {
                sendMessage.setReplyToMessageId(messageId);
            }
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending message", e);
        }
    }

    private void sendErrorMessage(Long chatId, Integer messageId) {
        sendMessageText(MessageTemplates.getErrorMessage(), chatId, messageId);
    }

    private void sendChatAction(ActionType action, Long chatId) {
        try {
            execute(SendChatAction.builder()
                .chatId(chatId)
                .action(action.toString())
                .build());
        } catch (TelegramApiException e) {
            logger.warn("Failed to send chat action", e);
        }
    }

    private void handleError(Update update) {
        try {
            Long chatId = null;
            Integer messageId = null;
            
            if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
                messageId = update.getMessage().getMessageId();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
                var messageAccessible = update.getCallbackQuery().getMessage();
                if (messageAccessible instanceof Message) {
                    messageId = ((Message) messageAccessible).getMessageId();
                }
            }
            
            if (chatId != null) {
                sendErrorMessage(chatId, messageId);
            }
        } catch (Exception e) {
            logger.error("Error handling error", e);
        }
    }

    @Override
    public String getBotUsername() {
        return PropertiesReader.getInstance().getBotUsername();
    }

    @FunctionalInterface
    private interface IPTVFetcher {
        IPTVModel fetch(String name) throws IOException;
    }
}
