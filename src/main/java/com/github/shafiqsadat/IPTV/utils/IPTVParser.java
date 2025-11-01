package com.github.shafiqsadat.IPTV.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IPTVParser {

    public static List<IPTVModel> getIPTVListByCategories() throws IOException {
        return getIPTVList(0);
    }

    public static List<IPTVModel> getIPTVListByLanguages() throws IOException {
        return getIPTVList(1);
    }

    public static List<IPTVModel> getIPTVListByCountries() throws IOException {
        // Countries are now in a list format, not a table
        try {
            System.out.println("🔍 Fetching country data...");
            
            // Fetch the raw markdown content
            String content = Jsoup.connect(Constants.IPTV_GITHUB_RAW_LINK)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            
            List<IPTVModel> iptvModelList = new ArrayList<>();
            
            // Find the Countries section
            int countriesIndex = content.indexOf("#### Countries");
            if (countriesIndex == -1) {
                throw new IOException("Countries section not found");
            }
            
            // Find the end of the Countries section (next #### or ###)
            int endIndex = content.indexOf("####", countriesIndex + 14);
            if (endIndex == -1) {
                endIndex = content.indexOf("###", countriesIndex + 14);
            }
            if (endIndex == -1) {
                endIndex = content.length();
            }
            
            String countriesSection = content.substring(countriesIndex, endIndex);
            
            // Parse each country line (format: - 🇦🇫 Afghanistan <code>https://...</code>)
            String[] lines = countriesSection.split("\n");
            for (String line : lines) {
                line = line.trim();
                
                // Skip lines without country data or with subdivisions/cities
                if (!line.startsWith("-") || !line.contains("<code>") || 
                    line.contains("subdivisions") || line.contains("cities")) {
                    continue;
                }
                
                // Extract country name (between emoji and <code>)
                int emojiEnd = 2; // Most emojis are 2 chars
                int codeStart = line.indexOf("<code>");
                
                if (codeStart == -1) continue;
                
                String name = line.substring(emojiEnd, codeStart).trim();
                
                // Extract URL from <code>...</code>
                int urlStart = line.indexOf("<code>") + 6;
                int urlEnd = line.indexOf("</code>");
                
                if (urlStart == -1 || urlEnd == -1) continue;
                
                String streamLink = line.substring(urlStart, urlEnd);
                
                if (!name.isEmpty() && !streamLink.isEmpty()) {
                    IPTVModel iptvModel = new IPTVModel(name, "N/A", streamLink);
                    iptvModelList.add(iptvModel);
                }
            }
            
            System.out.println("✅ Parsed " + iptvModelList.size() + " countries");
            return iptvModelList;
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing countries: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to parse countries", e);
        }
    }

    public static List<IPTVModel> getIPTVListByRegion() throws IOException {
        try {
            System.out.println("🔍 Fetching regions data from GitHub...");
            
            // Fetch the raw markdown content
            String content = Jsoup.connect(Constants.IPTV_GITHUB_RAW_LINK)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            
            List<IPTVModel> iptvModelList = new ArrayList<>();
            
            // Find the "#### Regions" section
            String[] lines = content.split("\n");
            boolean inRegionsSection = false;
            
            for (String line : lines) {
                // Start parsing when we find "#### Regions"
                if (line.trim().equals("#### Regions")) {
                    inRegionsSection = true;
                    continue;
                }
                
                // Stop when we reach the next section (#### Countries)
                if (inRegionsSection && line.trim().startsWith("####")) {
                    break;
                }
                
                // Parse region lines like: - Africa <code>https://iptv-org.github.io/iptv/regions/afr.m3u</code>
                if (inRegionsSection && line.trim().startsWith("- ")) {
                    try {
                        // Extract region name and URL
                        String lineContent = line.substring(line.indexOf("- ") + 2).trim();
                        
                        // Split by <code> tag
                        if (lineContent.contains("<code>") && lineContent.contains("</code>")) {
                            int codeStart = lineContent.indexOf("<code>") + 6;
                            int codeEnd = lineContent.indexOf("</code>");
                            
                            String url = lineContent.substring(codeStart, codeEnd).trim();
                            String name = lineContent.substring(0, lineContent.indexOf("<code>")).trim();
                            
                            // Create IPTVModel with all required parameters
                            IPTVModel model = new IPTVModel(name, "Unknown", url);
                            
                            iptvModelList.add(model);
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error parsing region line: " + line);
                        e.printStackTrace();
                    }
                }
            }
            
            System.out.println("✅ Parsed " + iptvModelList.size() + " regions");
            return iptvModelList;
            
        } catch (Exception e) {
            System.err.println("❌ Error parsing regions: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to parse regions", e);
        }
    }

    public static List<IPTVModel> getIPTVList(int index) throws IOException {
        try {
            System.out.println("🔍 Fetching IPTV data from: " + Constants.IPTV_GITHUB_RAW_LINK);
            Document doc = Jsoup.connect(Constants.IPTV_GITHUB_RAW_LINK).get();
            
            // Select all tables within details sections
            Elements detailsSections = doc.select("details");
            System.out.println("📊 Found " + detailsSections.size() + " details sections");
            
            if (detailsSections.size() <= index) {
                System.err.println("❌ Not enough details sections. Expected at least " + (index + 1) + " but found " + detailsSections.size());
                throw new IOException("Invalid section index: " + index + ". Only " + detailsSections.size() + " sections found.");
            }
            
            Element detailsSection = detailsSections.get(index);
            Element tableElement = detailsSection.select("table").first();
            
            if (tableElement == null) {
                System.err.println("❌ No table found in details section " + index);
                throw new IOException("No table found in section " + index);
            }
            
            Elements tableRows = tableElement.select("tr");
            List<IPTVModel> iptvModelList = new ArrayList<>();
            
            for (Element row : tableRows) {
                Elements cells = row.select("td");
                if (cells.size() >= 3) {
                    String name = cells.get(0).text();
                    if(name.equals("XXX") || name.isEmpty()){
                        continue;
                    }
                    String count = cells.get(1).text();
                    String streamLink = cells.get(2).text();
                    IPTVModel iptvModel = new IPTVModel(name, count, streamLink);
                    iptvModelList.add(iptvModel);
                }
            }
            
            System.out.println("✅ Parsed " + iptvModelList.size() + " items from section " + index);
            return iptvModelList;
        } catch (Exception e) {
            System.err.println("❌ Error parsing IPTV list for index " + index + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

}
