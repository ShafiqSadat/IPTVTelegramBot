package com.github.shafiqsadat.IPTV.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileDownloader {
    private static final int TIMEOUT_MS = 30_000;

    private FileDownloader() {
    }

    /** Downloads to a unique temp file; the caller is responsible for deleting it. */
    public static Path downloadToTempFile(String fileUrl) throws IOException {
        URLConnection connection = URI.create(fileUrl).toURL().openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);

        Path tempFile = Files.createTempFile("iptv-", ".m3u");
        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }
}
