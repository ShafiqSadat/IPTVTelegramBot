package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.Constants;
import com.github.shafiqsadat.IPTV.utils.FileDownloader;
import com.github.shafiqsadat.IPTV.utils.IPTVModel;
import com.github.shafiqsadat.IPTV.utils.IPTVParser;
import com.github.shafiqsadat.IPTV.utils.MessageTemplates;
import com.github.shafiqsadat.IPTV.utils.RedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IPTVBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(IPTVBot.class);
    private static final String REDIS_USER_KEY = "IPTVBOT_USERS";
    private static final String REDIS_COUNTRY_WAIT_PREFIX = "waitForGetByCountryName:";
    private static final int COUNTRY_WAIT_SECONDS = 300;
    private static final String BACK_BUTTON = "🔙 Back";

    private final String botUsername;

    public IPTVBot(String token, String botUsername) {
        super(token);
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            logger.error("Unhandled error processing update {}", update.getUpdateId(), e);
        }
    }

    private void handleTextMessage(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();
        User from = message.getFrom();
        logger.info("User {} sent message: {}", from.getId(), text);

        try (Jedis redis = RedisManager.getJedis()) {
            if (text.equals("/start") || text.equals("/help")) {
                handleStartCommand(chatId, messageId, from, redis);
            } else if (text.startsWith("Language: ")) {
                downloadAndSendIPTV(extractName(text, "Language: "), "Language", chatId, messageId,
                        this::getIPTVByLanguage);
            } else if (text.startsWith("Category: ")) {
                downloadAndSendIPTV(extractName(text, "Category: "), "Category", chatId, messageId,
                        this::getIPTVByCategory);
            } else if (text.startsWith("Region: ")) {
                downloadAndSendIPTV(extractName(text, "Region: "), "Region", chatId, messageId,
                        this::getIPTVByRegion);
            } else if (text.equals(BACK_BUTTON)) {
                handleBackToMenu(chatId, from, redis);
            } else if (isWaitingForCountryName(redis, from.getId())) {
                redis.del(REDIS_COUNTRY_WAIT_PREFIX + from.getId());
                downloadAndSendIPTV(text.trim(), "Country", chatId, messageId, this::getIPTVByCountry);
            }
        } catch (Exception e) {
            logger.error("Error handling text message from user {}", from.getId(), e);
            sendErrorMessage(chatId, messageId);
        }
    }

    private void handleStartCommand(Long chatId, Integer messageId, User from, Jedis redis)
            throws TelegramApiException {
        if (redis.sadd(REDIS_USER_KEY, from.getId().toString()) == 1) {
            logger.info("New user registered: {}", from.getId());
        }

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(Constants.START_IMAGE_URL));
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(MessageTemplates.getWelcomeMessage(from.getFirstName(), from.getLastName()));
        sendPhoto.setReplyMarkup(createMainMenuKeyboard());
        sendPhoto.setReplyToMessageId(messageId);
        execute(sendPhoto);
    }

    private void handleBackToMenu(Long chatId, User from, Jedis redis) throws TelegramApiException {
        redis.del(REDIS_COUNTRY_WAIT_PREFIX + from.getId());

        ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
        removeKeyboard.setRemoveKeyboard(true);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(MessageTemplates.getBackToMainMenuMessage());
        sendMessage.setReplyMarkup(removeKeyboard);
        execute(sendMessage);

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setPhoto(new InputFile(Constants.START_IMAGE_URL));
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(MessageTemplates.getWelcomeMessage(from.getFirstName(), from.getLastName()));
        sendPhoto.setReplyMarkup(createMainMenuKeyboard());
        execute(sendPhoto);
    }

    private void downloadAndSendIPTV(String name, String type, Long chatId, Integer messageId,
                                     IPTVFetcher fetcher) {
        CompletableFuture.runAsync(() -> {
            try {
                sendChatAction(ActionType.TYPING, chatId);
                sendMessageText(MessageTemplates.getDownloadingMessage(), chatId, messageId);

                IPTVModel model = fetcher.fetch(name);
                if (model == null) {
                    sendMessageText(MessageTemplates.getNoChannelsFoundMessage(), chatId, messageId);
                    return;
                }
                sendPlaylist(type, model, chatId, messageId);
            } catch (Exception e) {
                logger.error("Error sending {} playlist '{}' to chat {}", type, name, chatId, e);
                sendErrorMessage(chatId, messageId);
            }
        });
    }

    private void sendPlaylist(String type, IPTVModel model, Long chatId, Integer messageId)
            throws IOException, TelegramApiException {
        sendChatAction(ActionType.UPLOADDOCUMENT, chatId);

        Path file = FileDownloader.downloadToTempFile(model.getStreamLink());
        try {
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setDocument(new InputFile(file.toFile(), model.getName() + ".m3u"));
            sendDocument.setCaption(MessageTemplates.getChannelInfoCaption(
                    type, model.getName(), model.getCount(), model.getStreamLink()));
            sendDocument.setReplyToMessageId(messageId);
            sendDocument.setParseMode(ParseMode.MARKDOWNV2);
            execute(sendDocument);
        } finally {
            Files.deleteIfExists(file);
        }
        logger.info("Sent {} playlist '{}' to chat {}", type, model.getName(), chatId);
    }

    private IPTVModel getIPTVByLanguage(String name) throws IOException {
        return findByName(IPTVParser.getIPTVListByLanguages(), name);
    }

    private IPTVModel getIPTVByCategory(String name) throws IOException {
        return findByName(IPTVParser.getIPTVListByCategories(), name);
    }

    private IPTVModel getIPTVByRegion(String name) throws IOException {
        return findByName(IPTVParser.getIPTVListByRegion(), name);
    }

    private IPTVModel getIPTVByCountry(String query) throws IOException {
        List<IPTVModel> countries = IPTVParser.getIPTVListByCountries();
        String lowerQuery = query.toLowerCase();
        return countries.stream()
                .filter(model -> stripFlag(model.getName()).equalsIgnoreCase(query))
                .findFirst()
                .or(() -> countries.stream()
                        .filter(model -> model.getName().toLowerCase().contains(lowerQuery))
                        .findFirst())
                .orElse(null);
    }

    private static IPTVModel findByName(List<IPTVModel> list, String name) {
        return list.stream()
                .filter(model -> model.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private static String stripFlag(String countryName) {
        return countryName.replaceFirst("^[^\\p{L}]+", "");
    }

    private static String extractName(String text, String prefix) {
        return text.substring(prefix.length()).replaceFirst("\\s*\\([\\d,]+\\)$", "").trim();
    }

    private boolean isWaitingForCountryName(Jedis redis, Long userId) {
        return "true".equals(redis.get(REDIS_COUNTRY_WAIT_PREFIX + userId));
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        User from = callbackQuery.getFrom();
        Long chatId = callbackQuery.getMessage().getChatId();
        logger.info("User {} pressed button: {}", from.getId(), callbackData);

        answerCallbackQuery(callbackQuery.getId());

        if (!(callbackQuery.getMessage() instanceof Message message)) {
            logger.warn("Message for callback query from user {} is not accessible", from.getId());
            return;
        }
        Integer messageId = message.getMessageId();

        try {
            switch (callbackData) {
                case "getIPTV" -> showIPTVOptions(chatId, messageId);
                case "howITWorks" -> editCaption(chatId, messageId,
                        MessageTemplates.getHowItWorksMessage(), createBackButton());
                case "whatISIPTV" -> editCaption(chatId, messageId,
                        MessageTemplates.getWhatIsIPTVMessage(), createBackButton());
                case "getCategory" -> showSelectionList(chatId, IPTVParser.getIPTVListByCategories(),
                        "Category", MessageTemplates.getSelectCategoryMessage());
                case "getByLanguage" -> showSelectionList(chatId, IPTVParser.getIPTVListByLanguages(),
                        "Language", MessageTemplates.getSelectLanguageMessage());
                case "getByRegion" -> showSelectionList(chatId, IPTVParser.getIPTVListByRegion(),
                        "Region", MessageTemplates.getSelectRegionMessage());
                case "getByCountry" -> showCountrySearch(chatId, messageId, from.getId());
                case "goBack" -> editCaption(chatId, messageId,
                        MessageTemplates.getWelcomeMessage(from.getFirstName(), from.getLastName()),
                        createMainMenuKeyboard());
                default -> logger.warn("Unknown callback data: {}", callbackData);
            }
        } catch (Exception e) {
            logger.error("Error handling callback '{}' from user {}", callbackData, from.getId(), e);
            sendErrorMessage(chatId, messageId);
        }
    }

    private void answerCallbackQuery(String callbackQueryId) {
        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
        } catch (TelegramApiException e) {
            logger.warn("Failed to answer callback query {}", callbackQueryId, e);
        }
    }

    private void showIPTVOptions(Long chatId, Integer messageId) throws TelegramApiException {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(button("📂 By Category", "getCategory"), button("🌐 By Language", "getByLanguage")),
                List.of(button("🏳️ By Country", "getByCountry"), button("®️ By Region", "getByRegion")),
                List.of(button(BACK_BUTTON, "goBack"))));
        editCaption(chatId, messageId, "📺 Please select an option to get IPTV:", markup);
    }

    private void showSelectionList(Long chatId, List<IPTVModel> items, String prefix, String prompt)
            throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(prompt);
        sendMessage.setReplyMarkup(createSelectionKeyboard(items, prefix));
        execute(sendMessage);
    }

    private void showCountrySearch(Long chatId, Integer messageId, Long userId) {
        try (Jedis redis = RedisManager.getJedis()) {
            redis.setex(REDIS_COUNTRY_WAIT_PREFIX + userId, COUNTRY_WAIT_SECONDS, "true");
        }
        sendMessageText(MessageTemplates.getCountrySearchMessage(), chatId, messageId);
    }

    private void editCaption(Long chatId, Integer messageId, String caption, InlineKeyboardMarkup markup)
            throws TelegramApiException {
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(chatId);
        editMessageCaption.setMessageId(messageId);
        editMessageCaption.setCaption(caption);
        editMessageCaption.setReplyMarkup(markup);
        execute(editMessageCaption);
    }

    private InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(button("📺 Get IPTV", "getIPTV"), button("🧐 How it works?", "howITWorks")),
                List.of(button("👀 What is IPTV?", "whatISIPTV"))));
        return markup;
    }

    private InlineKeyboardMarkup createBackButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(button(BACK_BUTTON, "goBack"))));
        return markup;
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder().text(text).callbackData(callbackData).build();
    }

    private ReplyKeyboardMarkup createSelectionKeyboard(List<IPTVModel> items, String prefix) {
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for (IPTVModel item : items) {
            if (row.size() == 2) {
                rows.add(row);
                row = new KeyboardRow();
            }
            row.add(new KeyboardButton(selectionLabel(prefix, item)));
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton(BACK_BUTTON));
        rows.add(backRow);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        return keyboard;
    }

    private static String selectionLabel(String prefix, IPTVModel item) {
        String label = prefix + ": " + item.getName();
        return item.getCount().isBlank() ? label : label + " (" + item.getCount() + ")";
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
            logger.error("Error sending message to chat {}", chatId, e);
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
            logger.warn("Failed to send chat action to chat {}", chatId, e);
        }
    }

    @FunctionalInterface
    private interface IPTVFetcher {
        IPTVModel fetch(String name) throws IOException;
    }
}
