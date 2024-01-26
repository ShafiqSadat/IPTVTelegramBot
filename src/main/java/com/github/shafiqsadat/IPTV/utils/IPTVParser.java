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
        return getIPTVList(2);
    }

    public static List<IPTVModel> getIPTVListByRegion() throws IOException {
        return getIPTVList(3);
    }

    public static List<IPTVModel> getIPTVList(int index) throws IOException {
        Document doc = Jsoup.connect(Constants.IPTV_GITHUB_RAW_LINK).get();
        Element tableElement = doc.select("details table").get(index);
        Elements tableRows = tableElement.select("tr");
        List<IPTVModel> iptvModelList = new ArrayList<>();
        for (Element row : tableRows) {
            Elements cells = row.select("td");
            if (cells.size() == 3) {
                if(cells.get(0).text().equals("XXX")){
                    continue;
                }
                String name = cells.get(0).text();
                String count = cells.get(1).text();
                String streamLink = cells.get(2).text();
                IPTVModel iptvModel = new IPTVModel(name, count, streamLink);
                iptvModelList.add(iptvModel);
            }
        }
        return iptvModelList;
    }

}
