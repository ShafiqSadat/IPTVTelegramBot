package com.github.shafiqsadat.IPTV.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IPTVParser {
    private static final Logger logger = LoggerFactory.getLogger(IPTVParser.class);
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int FETCH_TIMEOUT_MS = 30_000;
    private static final Pattern LIST_ITEM = Pattern.compile("^- (.+?)\\s*<code>(.+?)</code>");

    private record Playlists(List<IPTVModel> categories, List<IPTVModel> languages,
                             List<IPTVModel> countries, List<IPTVModel> regions) {
    }

    private static Playlists cached;
    private static Instant cachedAt = Instant.MIN;

    private IPTVParser() {
    }

    public static List<IPTVModel> getIPTVListByCategories() throws IOException {
        return playlists().categories();
    }

    public static List<IPTVModel> getIPTVListByLanguages() throws IOException {
        return playlists().languages();
    }

    public static List<IPTVModel> getIPTVListByCountries() throws IOException {
        return playlists().countries();
    }

    public static List<IPTVModel> getIPTVListByRegion() throws IOException {
        return playlists().regions();
    }

    private static synchronized Playlists playlists() throws IOException {
        if (cached != null && Instant.now().isBefore(cachedAt.plus(CACHE_TTL))) {
            return cached;
        }

        logger.info("Fetching playlist index from {}", Constants.IPTV_GITHUB_RAW_LINK);
        String content = Jsoup.connect(Constants.IPTV_GITHUB_RAW_LINK)
                .ignoreContentType(true)
                .timeout(FETCH_TIMEOUT_MS)
                .maxBodySize(0)
                .execute()
                .body();

        Document doc = Jsoup.parse(content);
        Playlists playlists = new Playlists(
                parseTableSection(doc, 0),
                parseTableSection(doc, 1),
                parseListSection(content, "#### Countries"),
                parseListSection(content, "#### Regions"));
        logger.info("Parsed {} categories, {} languages, {} countries, {} regions",
                playlists.categories().size(), playlists.languages().size(),
                playlists.countries().size(), playlists.regions().size());

        cached = playlists;
        cachedAt = Instant.now();
        return playlists;
    }

    private static List<IPTVModel> parseTableSection(Document doc, int index) throws IOException {
        Elements detailsSections = doc.select("details");
        if (detailsSections.size() <= index) {
            throw new IOException("Playlist index has only " + detailsSections.size()
                    + " sections, expected at least " + (index + 1));
        }
        Element table = detailsSections.get(index).selectFirst("table");
        if (table == null) {
            throw new IOException("No table found in playlist index section " + index);
        }

        List<IPTVModel> items = new ArrayList<>();
        for (Element row : table.select("tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 3) {
                continue;
            }
            String name = cells.get(0).text();
            if (name.isEmpty() || name.equals("XXX")) {
                continue;
            }
            items.add(new IPTVModel(name, cells.get(1).text(), cells.get(2).text()));
        }
        if (items.isEmpty()) {
            throw new IOException("No playlists found in playlist index section " + index);
        }
        return List.copyOf(items);
    }

    private static List<IPTVModel> parseListSection(String content, String heading) throws IOException {
        List<IPTVModel> items = new ArrayList<>();
        boolean inSection = false;
        for (String line : content.split("\n")) {
            if (line.startsWith("#")) {
                if (inSection) {
                    break;
                }
                inSection = line.trim().equals(heading);
                continue;
            }
            if (!inSection) {
                continue;
            }
            if (line.startsWith("</details>")) {
                break;
            }
            // Sub-items (subdivisions, cities) are indented, so anchoring at column 0 skips them
            Matcher matcher = LIST_ITEM.matcher(line);
            if (matcher.find()) {
                items.add(new IPTVModel(matcher.group(1).trim(), "", matcher.group(2).trim()));
            }
        }
        if (items.isEmpty()) {
            throw new IOException("Section '" + heading + "' not found in playlist index");
        }
        return List.copyOf(items);
    }
}
