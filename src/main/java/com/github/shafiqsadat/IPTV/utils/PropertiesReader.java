package com.github.shafiqsadat.IPTV.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesReader {
    private static final String FILE_NAME = "local.properties";
    private static PropertiesReader instance;

    private final Properties properties = new Properties();

    private PropertiesReader() {
        try (InputStream inputStream = PropertiesReader.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (inputStream == null) {
                throw new IllegalStateException(FILE_NAME + " not found on the classpath. "
                        + "Copy src/main/resources/example_local.properties to src/main/resources/" + FILE_NAME
                        + " and fill in your bot token.");
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + FILE_NAME, e);
        }
    }

    public static synchronized PropertiesReader getInstance() {
        if (instance == null) {
            instance = new PropertiesReader();
        }
        return instance;
    }

    public String getBotToken() {
        return require("botToken");
    }

    public String getBotUsername() {
        return require("botUsername");
    }

    public String getRedisHost() {
        return properties.getProperty("redisHost", "localhost").trim();
    }

    public int getRedisPort() {
        String port = properties.getProperty("redisPort", "6379").trim();
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid redisPort '" + port + "' in " + FILE_NAME);
        }
    }

    /** Returns null when no password is configured. */
    public String getRedisPassword() {
        String password = properties.getProperty("redisPassword", "").trim();
        return password.isEmpty() ? null : password;
    }

    private String require(String key) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Missing required property '" + key + "' in " + FILE_NAME);
        }
        return value;
    }
}
