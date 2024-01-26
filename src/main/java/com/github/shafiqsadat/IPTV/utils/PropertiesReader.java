package com.github.shafiqsadat.IPTV.utils;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesReader {

    // Singleton Design Pattern
    private static PropertiesReader instance;

    // Properties class is used to read properties file
    private Properties properties;

    // Constructor is private to prevent creating object from outside the class
    private PropertiesReader() {
        try {
            properties = new Properties();
            InputStream inputStream = PropertiesReader.class.getClassLoader().getResourceAsStream("local.properties");
            properties.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This method is used to get the instance of the class
    public static PropertiesReader getInstance() {
        if (instance == null) {
            instance = new PropertiesReader();
        }
        return instance;
    }

    // This method is used to get the value of the key from properties file
    public String getBotToken() {
        return properties.getProperty("botToken");
    }

    // This method is used to get the value of the key from properties file
    public String getBotUsername() {
        return properties.getProperty("botUsername");
    }
}