package com.dazo66.util;

import com.dazo66.model.EverythingFileItem;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EverythingHtmlParser {

    @SneakyThrows
    public static String decodePath(String path) {
        path = URLDecoder.decode(path, "UTF-8");
        if (path.contains("%"))
            return decodePath(path);
        return path;
    }

    /**
     * RFC 3986规范URL编码（空格转为%20，而非+）
     * @param content 待编码的内容
     * @return 符合RFC 3986规范的编码结果
     */
    @SneakyThrows
    public static String encodePath(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // 第一步：使用URLEncoder完成基础UTF-8编码
        String encoded = URLEncoder.encode(content, "UTF-8");
        // 第二步：将+替换为%20，适配RFC 3986规范
        return encoded.replace("+", "%20");
    }


    public static List<EverythingFileItem> parse(String html) {
        List<EverythingFileItem> items = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        
        // Select rows with class trdata1 or trdata2
        Elements rows = doc.select("tr.trdata1, tr.trdata2");
        
        for (Element row : rows) {
            EverythingFileItem item = new EverythingFileItem();
            
            // Name and Path
            // Determine if it is a file or folder based on the td class
            Element fileTd = row.selectFirst("td.file");
            Element folderTd = row.selectFirst("td.folder");
            Element nameTd = fileTd != null ? fileTd : folderTd;
            
            if (nameTd != null) {
                item.setIsFile(fileTd != null);
                Element nameLink = nameTd.selectFirst("span > nobr > a");
                if (nameLink != null) {
                    item.setName(nameLink.ownText());
                    item.setPath((nameLink.attr("href")));
                }
            } else {
                 continue;
            }

            // Size
            Element sizeTd = row.selectFirst("td.sizedata");
            if (sizeTd != null) {
                item.setSize(parseSizeToKb(sizeTd.text()));
            }
            
            // Modified
            Element modifiedTd = row.selectFirst("td.modifieddata");
            if (modifiedTd != null) {
                item.setModified(parseDateToMillis(modifiedTd.text()));
            }
            
            items.add(item);
        }
        
        return items;
    }

    private static Double parseSizeToKb(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return null;
        }
        sizeStr = sizeStr.trim();
        String[] parts = sizeStr.split("\\s+");
        if (parts.length < 2) {
            // Assume bytes if no unit provided, or handle as error?
            // If just a number, let's assume bytes and convert to KB
            try {
                double val = Double.parseDouble(sizeStr.replace(",", ""));
                return val / 1024.0;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        try {
            double val = Double.parseDouble(parts[0].replace(",", ""));
            String unit = parts[1].toUpperCase();

            switch (unit) {
                case "B": return val / 1024.0;
                case "KB": return val;
                case "MB": return val * 1024.0;
                case "GB": return val * 1024.0 * 1024.0;
                case "TB": return val * 1024.0 * 1024.0 * 1024.0;
                default: return val; // Unknown unit, return as is (assuming KB) or null? Let's return as is.
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static Long parseDateToMillis(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        // Format: 2025/12/27 0:59 or 2025/12/28 6:30
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd H:mm");
        try {
            Date date = sdf.parse(dateStr.trim());
            return date.getTime();
        } catch (ParseException e) {
            return null;
        }
    }
}
