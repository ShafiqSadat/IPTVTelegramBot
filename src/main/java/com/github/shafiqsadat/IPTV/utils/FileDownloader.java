package com.github.shafiqsadat.IPTV.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileDownloader {

    public static File downloadFile(String fileUrl, String savePath) throws IOException {
        // Create a URI object and convert to URL (non-deprecated approach)
        URI uri = URI.create(fileUrl);
        
        // Open a connection to the URL
        try (InputStream inputStream = uri.toURL().openStream()) {
            // Save the file to a temporary location
            Path tempFilePath = Files.createTempFile("iptv_temp", ".m3u");
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create a File object from the temporary file path
            File file = new File(savePath);
            
            // Move the temporary file to the desired save location
            Files.move(tempFilePath, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            return file;
        }
    }
}