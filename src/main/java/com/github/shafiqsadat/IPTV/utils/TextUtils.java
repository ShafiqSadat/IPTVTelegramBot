package com.github.shafiqsadat.IPTV.utils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for text formatting and validation
 */
public class TextUtils {
    
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
        "[\\u{1F300}-\\u{1F9FF}]|[\\u{2600}-\\u{26FF}]|[\\u{2700}-\\u{27BF}]",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    /**
     * Escape special characters for Telegram MarkdownV2
     */
    public static String escapeMarkdownV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
    
    /**
     * Truncate text to specified length with ellipsis
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Check if string contains emoji
     */
    public static boolean containsEmoji(String text) {
        if (text == null) return false;
        return EMOJI_PATTERN.matcher(text).find();
    }
    
    /**
     * Validate if string is not null or empty
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
    
    /**
     * Safely get user's full name
     */
    public static String getFullName(String firstName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (isNotEmpty(firstName)) {
            name.append(firstName);
        }
        if (isNotEmpty(lastName)) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(lastName);
        }
        return name.length() > 0 ? name.toString() : "User";
    }
    
    /**
     * Format number with commas
     */
    public static String formatNumber(String number) {
        try {
            long num = Long.parseLong(number);
            return String.format("%,d", num);
        } catch (NumberFormatException e) {
            return number;
        }
    }
    
    /**
     * Split list into chunks for pagination
     */
    public static <T> List<List<T>> chunk(List<T> list, int chunkSize) {
        List<List<T>> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }
    
    /**
     * Normalize country name for search
     */
    public static String normalizeCountryName(String name) {
        if (name == null) return "";
        return name.trim()
                   .toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", "")
                   .replaceAll("\\s+", " ");
    }
    
    /**
     * Calculate similarity between two strings (for fuzzy search)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        
        String longer = s1.length() > s2.length() ? s1 : s2;
        String shorter = s1.length() > s2.length() ? s2 : s1;
        
        int longerLength = longer.length();
        if (longerLength == 0) return 1.0;
        
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }
    
    /**
     * Calculate edit distance between two strings
     */
    private static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}
