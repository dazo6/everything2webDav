package com.dazo66.util;

import com.dazo66.model.EverythingFileItem;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriUtils;
import org.yaml.snakeyaml.util.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EverythingHtmlParser {

    @SneakyThrows
    public static String decodePath(String path) {
        String path1 = uriDecode(path, StandardCharsets.UTF_8);
        if (path1.equals(path)) return path1;
        path = path1;
        if (path.contains("%")) return decodePath(path);
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
        return UriUtils.encodePath(content, "UTF-8");
    }


    @SneakyThrows
    public static String uriDecode(String source, Charset charset) {
        int length = source.length();
        if (length == 0) {
            return source;
        }
        Assert.notNull(charset, "Charset must not be null");

        ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
        boolean changed = false;
        List<Character> characters = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int ch = source.charAt(i);
            if (ch == '%') {
                if (i + 2 < length) {
                    char hex1 = source.charAt(i + 1);
                    char hex2 = source.charAt(i + 2);
                    int u = Character.digit(hex1, 16);
                    int l = Character.digit(hex2, 16);
                    if (u == -1 || l == -1) {
                        characters.add((char)ch);
                        continue;
                    }
                    baos.write(((u << 4) + l));
                    i += 2;
                    changed = true;
                } else {
                    characters.add((char)ch);
                }
            } else {
                if (baos.size() > 0) {
                    String string = StreamUtils.copyToString(baos, charset);
                    for (int i1 = 0; i1 < string.toCharArray().length; i1++) {
                        characters.add(string.charAt(i1));
                    }
                    baos = new ByteArrayOutputStream();
                }
                characters.add((char)ch);
            }
        }
        if (baos.size() > 0) {
            String string = StreamUtils.copyToString(baos, charset);
            for (int i1 = 0; i1 < string.toCharArray().length; i1++) {
                characters.add(string.charAt(i1));
            }
        }
        char[] chars = new char[characters.size()];
        for (int i = 0; i < characters.size(); i++) {
            chars[i] = characters.get(i);
        }
        return (changed ? new String(chars) : source);
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
                    item.setPath(decodePath(nameLink.attr("href")));
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

        long i = System.currentTimeMillis();
        List<EverythingFileItem> collect = items.stream().distinct().collect(Collectors.toList());
        return collect;
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

    public static void main(String[] args) {
        System.out.println(uriDecode("\uD83E\uDD5A", Charset.defaultCharset()));
        System.out.println(uriDecode("/D%3A/OneDrive/%E8%89%B2%E5%9B%BE/test/\uD83E\uDD5A/", Charset.defaultCharset()));
        System.out.println("\uD83E\uDD5A".charAt(0));
        System.out.println(String.valueOf("\uD83E\uDD5A".charAt(0)));
        System.out.println("\uD83E\uDD5A".charAt(1));
    }
}
