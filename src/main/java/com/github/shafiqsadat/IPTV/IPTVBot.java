package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.Constants;
import com.github.shafiqsadat.IPTV.utils.PropertiesReader;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class IPTVBot extends TelegramLongPollingBot {

    //constructor for TelegramBot
    public IPTVBot(String token) {
        super(token);
    }

    // all bot updates will be received here as Update object
    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            var messageText = update.getMessage().getText();
            var chatId = update.getMessage().getChatId();
            var messageID = update.getMessage().getMessageId();
            if(messageText.equals("/start")){
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                row1.add(InlineKeyboardButton.builder().text("Location").callbackData("location").build());
                row1.add(InlineKeyboardButton.builder().text("Inline").switchInlineQueryCurrentChat("").build());
                rows.add(row1);
                markup.setKeyboard(rows);
                SendPhoto sendPhoto = SendPhoto.builder().photo(new InputFile().setMedia(Constants.START_IMAGE_URL)).chatId(chatId).caption("Welcome").replyMarkup(markup).build();
                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if(update.hasMessage() && update.getMessage().hasLocation()) {
            System.out.println(update.getMessage().getLocation().getLatitude());
            System.out.println(update.getMessage().getLocation().getLongitude());
        }
    }

    // this method must return bot's username
    @Override
    public String getBotUsername() {
        return PropertiesReader.getInstance().getBotUsername();
    }
}
