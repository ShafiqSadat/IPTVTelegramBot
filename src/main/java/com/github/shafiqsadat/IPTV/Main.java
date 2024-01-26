package com.github.shafiqsadat.IPTV;

import com.github.shafiqsadat.IPTV.utils.IPTVModel;
import com.github.shafiqsadat.IPTV.utils.IPTVParser;
import com.github.shafiqsadat.IPTV.utils.PropertiesReader;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;


public class Main {
    public static void main(String[] args) throws IOException {
//        List<IPTVModel> iptvModelList = IPTVParser.getIPTVListByRegion();
//        for (IPTVModel iptvModel : iptvModelList) {
//            System.out.println(iptvModel.getName() + " " + iptvModel.getCount() + " " + iptvModel.getStreamLink());
//        }
//        System.out.println(PropertiesReader.getInstance().getBotToken());

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new IPTVBot(PropertiesReader.getInstance().getBotToken()));
            System.out.println("Bot is running...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}