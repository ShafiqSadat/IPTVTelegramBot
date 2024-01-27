package com.github.shafiqsadat.IPTV.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileDownloader {

    public static File downloadFile(String fileUrl, String savePath) throws IOException {
        // Create a URL object
        URL url = new URL(fileUrl);

        // Open a connection to the URL
        InputStream inputStream = url.openStream();

        // Save the file to a temporary location
        Path tempFilePath = Files.createTempFile("temp", null);
        Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Create a File object from the temporary file path
        File file = new File(savePath);

        // Move the temporary file to the desired save location
        Files.move(tempFilePath, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return file;
    }
}