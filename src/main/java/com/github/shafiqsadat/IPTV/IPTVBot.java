package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.Constants;
import com.github.shafiqsadat.IPTV.utils.FileDownloader;
import com.github.shafiqsadat.IPTV.utils.IPTVParser;
import com.github.shafiqsadat.IPTV.utils.PropertiesReader;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            var messageText = update.getMessage().getText();
            var chatId = update.getMessage().getChatId();
            var messageID = update.getMessage().getMessageId();
            var from = update.getMessage().getFrom();
            if (messageText.equals("/start")) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                row1.add(InlineKeyboardButton.builder().text("\uD83D\uDCFA Get IPTV").callbackData("getIPTV").build());
                row1.add(InlineKeyboardButton.builder().text("\uD83E\uDDD0 How it works?").callbackData("howITWorks").build());
                row2.add(InlineKeyboardButton.builder().text("\uD83D\uDC40 What is IPTV?").callbackData("whatISIPTV").build());
                rows.add(row1);
                rows.add(row2);
                markup.setKeyboard(rows);
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setPhoto(new InputFile().setMedia(Constants.START_IMAGE_URL));
                sendPhoto.setChatId(chatId);
                sendPhoto.setCaption(String.format("""
                        Hi %s, Welcome to our IPTV Bot!

                        We are thrilled to have you here. With our bot, you can access a wide range of IPTV channels and enjoy your favorite shows, movies, sports events, and more, right from the comfort of your device.

                        To get started, simply type in the commands or use the menu options provided. You can explore different categories, search for specific channels, and even customize your preferences.

                        If you have any questions or need assistance, feel free to reach out to us. We're here to help!

                        Sit back, relax, and immerse yourself in the world of IPTV with our bot. Enjoy the endless entertainment it brings!

                        Thank you for choosing our IPTV Bot. Happy streaming!""", from.getFirstName() + " " + from.getLastName()));
                sendPhoto.setReplyMarkup(markup);
                sendPhoto.setReplyToMessageId(messageID);
                try {
                    execute(sendPhoto);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (messageText.startsWith("Language: ")) {
                try {
                    var IPTVList = IPTVParser.getIPTVListByLanguages();
                    for (var iptvModel : IPTVList) {
                        if (iptvModel.getName().equals(messageText.replace("Language: ", ""))) {
                            Thread downloadThread = new Thread(() -> {
                                try {
                                    var file = FileDownloader.downloadFile(iptvModel.getStreamLink(), iptvModel.getName() + ".m3u");
                                    SendDocument sendDocument = new SendDocument();
                                    sendDocument.setChatId(chatId);
                                    sendDocument.setDocument(new InputFile().setMedia(file));
                                    sendDocument.setCaption("Language: " + iptvModel.getName() + "\n" + "Count: " + iptvModel.getCount() + "\n" + "Stream Link: `" + iptvModel.getStreamLink().replaceAll("-", "\\-") + "`");
                                    sendDocument.setReplyToMessageId(messageID);
                                    sendDocument.setParseMode("MarkdownV2");
                                    execute(sendDocument);
                                } catch (IOException | TelegramApiException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            downloadThread.start();
                            downloadThread.join(); // Wait for the download thread to finish before proceeding

                            break;
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (update.hasMessage() && update.getMessage().hasLocation()) {
            System.out.println(update.getMessage().getLocation().getLatitude());
            System.out.println(update.getMessage().getLocation().getLongitude());
        } else if (update.hasCallbackQuery()) {
            var callbackQuery = update.getCallbackQuery();
            var chatId = update.getCallbackQuery().getMessage().getChatId();
            var message = (Message) callbackQuery.getMessage();
            var inlineMessageId = update.getCallbackQuery().getInlineMessageId();
            if (update.getCallbackQuery().getData().equals("getIPTV")) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                row1.add(InlineKeyboardButton.builder().text("\uD83D\uDCC2 By Category").callbackData("getCategory").build());
                row1.add(InlineKeyboardButton.builder().text("\uD83C\uDF10 By Language").callbackData("getByLanguage").build());
                //TODO: I should find a way to show countries in a better way
//                row2.add(InlineKeyboardButton.builder().text("\uD83C\uDFF3\uFE0F By Country").callbackData("getByCountry").build());
                row2.add(InlineKeyboardButton.builder().text("®\uFE0F By Region").callbackData("getByRegion").build());
                row3.add(InlineKeyboardButton.builder().text("\uD83D\uDD19 Back").callbackData("goBack").build());
                rows.add(row1);
                rows.add(row2);
                rows.add(row3);
                markup.setKeyboard(rows);
                EditMessageCaption editMessageCaption = new EditMessageCaption();
                editMessageCaption.setChatId(chatId);
                editMessageCaption.setInlineMessageId(inlineMessageId);
                editMessageCaption.setCaption("Please select an option to get IPTV:");
                editMessageCaption.setReplyMarkup(markup);
                editMessageCaption.setMessageId(message.getMessageId());

                try {
                    execute(editMessageCaption);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.getCallbackQuery().getData().equals("howITWorks")) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                row3.add(InlineKeyboardButton.builder().text("\uD83D\uDD19 Back").callbackData("goBack").build());
                rows.add(row3);
                markup.setKeyboard(rows);
                EditMessageCaption editMessageCaption = new EditMessageCaption();
                editMessageCaption.setChatId(chatId);
                editMessageCaption.setInlineMessageId(inlineMessageId);
                editMessageCaption.setCaption("""
                        Our IPTV Bot works as follows:
                                                                                                
                        1. Click "Get IPTV" button.
                        2. Choose category, country, language, or region.
                        3. View channels based on your selection.
                        4. Download .m3u file or copy streaming link.
                        5. Use player button to watch channels on a website.
                        Enjoy your IPTV experience!""");
                editMessageCaption.setReplyMarkup(markup);
                editMessageCaption.setMessageId(message.getMessageId());
                try {
                    execute(editMessageCaption);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.getCallbackQuery().getData().equals("getCategory")) {
                try {
                    var IPTVList = IPTVParser.getIPTVListByCategories();
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    int count = 0;
                    KeyboardRow row = new KeyboardRow();

                    for (var iptvModel : IPTVList) {
                        if (count == 2) {
                            rows.add(row);
                            row = new KeyboardRow();
                            count = 0;
                        }
                        row.add(new KeyboardButton("Category: " + iptvModel.getName()));
                        count++;
                    }

                    // Add the last row if it's not empty
                    if (!row.isEmpty()) {
                        rows.add(row);
                    }

                    // back button
                    KeyboardRow row3 = new KeyboardRow();
                    row3.add(new KeyboardButton("\uD83D\uDD19 Back"));
                    rows.add(row3);
                    replyKeyboardMarkup.setKeyboard(rows);
                    replyKeyboardMarkup.setResizeKeyboard(true);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setReplyMarkup(replyKeyboardMarkup);
                    sendMessage.setText("Please select a category:");
                    execute(sendMessage);
                } catch (IOException | TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (update.getCallbackQuery().getData().equals("getByLanguage")) {
                try {
                    var IPTVList = IPTVParser.getIPTVListByLanguages();
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    int count = 0;
                    KeyboardRow row = new KeyboardRow();

                    for (var iptvModel : IPTVList) {
                        if (count == 2) {
                            rows.add(row);
                            row = new KeyboardRow();
                            count = 0;
                        }
                        row.add(new KeyboardButton("Language: " + iptvModel.getName()));
                        count++;
                    }

                    // Add the last row if it's not empty
                    if (!row.isEmpty()) {
                        rows.add(row);
                    }
                    // back button
                    KeyboardRow row3 = new KeyboardRow();
                    row3.add(new KeyboardButton("\uD83D\uDD19 Back"));
                    rows.add(row3);
                    replyKeyboardMarkup.setKeyboard(rows);
                    replyKeyboardMarkup.setResizeKeyboard(true);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setReplyMarkup(replyKeyboardMarkup);
                    sendMessage.setText("Please select a category:");
                    execute(sendMessage);
                } catch (IOException | TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            // TODO: I should find a way to show countries in a better way
//            else if (update.getCallbackQuery().getData().equals("getByCountry")) {
//                try {
//                    var IPTVList = IPTVParser.getIPTVListByCountries();
//                    System.out.println(IPTVList.size());
//                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//                    List<KeyboardRow> rows = new ArrayList<>();
//                    int count = 0;
//                    KeyboardRow row = new KeyboardRow();
//
//                    for (var iptvModel : IPTVList) {
//                        if (count == 3) {
//                            rows.add(row);
//                            row = new KeyboardRow();
//                            count = 0;
//                        }
//                        row.add(new KeyboardButton("C: " + iptvModel.getName()));
//                        count++;
//                    }
//
//                    // Add the last row if it's not empty
//                    if (!row.isEmpty()) {
//                        rows.add(row);
//                    }
//
//                    replyKeyboardMarkup.setKeyboard(rows);
//                    replyKeyboardMarkup.setResizeKeyboard(true);
//                    SendMessage sendMessage = new SendMessage();
//                    sendMessage.setChatId(chatId);
//                    sendMessage.setReplyMarkup(replyKeyboardMarkup);
//                    sendMessage.setText("Please select a category:");
//                    execute(sendMessage);
//                } catch (IOException | TelegramApiException e) {
//                    throw new RuntimeException(e);
//                }
//            }
            else if (update.getCallbackQuery().getData().equals("getByRegion")) {
                try {
                    var IPTVList = IPTVParser.getIPTVListByRegion();
                    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    int count = 0;
                    KeyboardRow row = new KeyboardRow();

                    for (var iptvModel : IPTVList) {
                        if (count == 2) {
                            rows.add(row);
                            row = new KeyboardRow();
                            count = 0;
                        }
                        row.add(new KeyboardButton("Language: " + iptvModel.getName()));
                        count++;
                    }

                    // Add the last row if it's not empty
                    if (!row.isEmpty()) {
                        rows.add(row);
                    }
                    // back button
                    KeyboardRow row3 = new KeyboardRow();
                    row3.add(new KeyboardButton("\uD83D\uDD19 Back"));
                    rows.add(row3);
                    replyKeyboardMarkup.setKeyboard(rows);
                    replyKeyboardMarkup.setResizeKeyboard(true);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setReplyMarkup(replyKeyboardMarkup);
                    sendMessage.setText("Please select a category:");
                    execute(sendMessage);
                } catch (IOException | TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // this method must return bot's username
    @Override
    public String getBotUsername() {
        return PropertiesReader.getInstance().getBotUsername();
    }
}
