package com.dazo66;

import com.dazo66.model.EverythingFileItem;
import com.dazo66.util.EverythingHtmlParser;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.dazo66.util.EverythingHtmlParser.decodePath;

public class ParserTest {

    @Test
    public void testParse() {
        String html = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \" `http://www.w3.org/TR/html4/loose.dtd`  \"> \n" +
                " \n" +
                " <head> \n" +
                " \n" +
                " <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"> \n" +
                " \n" +
                " <meta name=\"viewport\" content=\"width=512\"> \n" +
                " \n" +
                " <meta name=\"robots\" content=\"noindex, nofollow\"> \n" +
                " \n" +
                " <title>D:\\OneDrive - Everything</title> \n" +
                " \n" +
                " <link rel=\"stylesheet\" href=\"/main.css\" type=\"text/css\"> \n" +
                " \n" +
                " <link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\"> \n" +
                " \n" +
                " </head> \n" +
                " \n" +
                " <body> \n" +
                " \n" +
                " <center> \n" +
                " \n" +
                " <br> \n" +
                " \n" +
                " <br> \n" +
                " \n" +
                " <a href=\"/\"> \n" +
                " \n" +
                " <img class=\"logo\" src=\"/Everything.gif\" alt=\"Everything\"> \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " <br> \n" +
                " \n" +
                " <br> \n" +
                " \n" +
                " <form id=\"searchform\" action=\"/\" method=\"get\"> \n" +
                " \n" +
                " <input class=\"searchbox\" style=\"width:480px\" id=\"search\" name=\"search\" type=\"text\" onfocus=\"this.select()\" title=\"搜索 Everything\" value=\"\" autofocus> \n" +
                " \n" +
                " <input type=\"hidden\" name=\"sort\" value=\"name\"/> \n" +
                " \n" +
                " <input type=\"hidden\" name=\"ascending\" value=\"0\"/> \n" +
                " \n" +
                " </form> \n" +
                " \n" +
                " <table cellspacing=\"0\" width=\"480px\"> \n" +
                " \n" +
                " <tr> \n" +
                " \n" +
                " <td colspan=\"3\"> \n" +
                " \n" +
                " <p class=\"indexof\">索引 D:\\OneDrive</p> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " </tr> \n" +
                " \n" +
                " <tr> \n" +
                " \n" +
                " <td class=\"updir\" colspan=\"3\"> \n" +
                " \n" +
                " <a href=\"/\"> \n" +
                " \n" +
                " <img class=\"icon\" src=\"/updir.gif\" alt=\"\">上一目录.. \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " </tr> \n" +
                " \n" +
                " <tr> \n" +
                " \n" +
                " <td class=\"nameheader\"> \n" +
                " \n" +
                " <a href=\"/D%3A/OneDrive?sort=name&amp;ascending=1\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " 名称<img class=\"updown\" src=\"/down.gif\" alt=\"\"> \n" +
                " \n" +
                " </nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"sizeheader\"> \n" +
                " \n" +
                " <a href=\"/D%3A/OneDrive?sort=size&amp;ascending=0\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr>大小</nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"modifiedheader\"> \n" +
                " \n" +
                " <a href=\"/D%3A/OneDrive?sort=date_modified&amp;ascending=0\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr>修改日期</nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " </tr> \n" +
                " \n" +
                " <tr> \n" +
                " \n" +
                " <td colspan=\"3\" class=\"lineshadow\" height=\"1\"></td> \n" +
                " \n" +
                " </tr> \n" +
                " \n" +
                " <tr class=\"trdata1\"> \n" +
                " \n" +
                " <td class=\"file\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " <a href=\"/D%3A/OneDrive/Personal%20Vault-DESKTOP-OES6R2E.lnk\"> \n" +
                " \n" +
                " <img class=\"icon\" src=\"/file.gif\" alt=\"\">Personal Vault-DESKTOP-OES6R2E.lnk \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"sizedata\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr>2 KB</nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"modifieddata\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr>2025/12/27 0:59</nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " </tr> \n" +
                " \n" +
                " <tr class=\"trdata2\"> \n" +
                " \n" +
                " <td class=\"folder\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " <a href=\"/D%3A/OneDrive/龙龙专用\"> \n" +
                " \n" +
                " <img class=\"icon\" src=\"/folder.gif\" alt=\"\">龙龙专用 \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"sizedata\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr></nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"modifieddata\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr>2025/12/28 6:30</nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " </tr> \n" +
                " \n" +
                " <tr class=\"trdata1\"> \n" +
                " \n" +
                " <td class=\"folder\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " <a href=\"/D%3A/OneDrive/附件\"> \n" +
                " \n" +
                " <img class=\"icon\" src=\"/folder.gif\" alt=\"\">附件 \n" +
                " \n" +
                " </a> \n" +
                " \n" +
                " </nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"sizedata\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr></nobr> \n" +
                " \n" +
                " </span> \n" +
                " \n" +
                " </td> \n" +
                " \n" +
                " <td class=\"modifieddata\"> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr> \n" +
                " \n" +
                " <span class=\"nobr\"> \n" +
                " \n" +
                " <nobr>2025/5/18 8:23</nobr> \n" +
                " \n" +
                " </span> \n" +
                " </td> \n" +
                " </tr> \n" +
                " </table> \n" +
                " </center> \n" +
                " </body>";
        
        List<EverythingFileItem> items = EverythingHtmlParser.parse(html);
        
        for (EverythingFileItem item : items) {
            System.out.println(item);
        }
    }
    
    @Test
    public void testParseSize() {
        String html = "<tr class=\"trdata1\"> \n" +
                " <td class=\"file\"> \n" +
                " <span class=\"nobr\"> \n" +
                " <nobr> \n" +
                " <a href=\"/test\"> \n" +
                " <img class=\"icon\" src=\"/file.gif\" alt=\"\">test_1MB.txt \n" +
                " </a> \n" +
                " </nobr> \n" +
                " </span> \n" +
                " </td> \n" +
                " <td class=\"sizedata\"> \n" +
                " <span class=\"nobr\"> \n" +
                " <nobr>1 MB</nobr> \n" +
                " </span> \n" +
                " </td> \n" +
                " <td class=\"modifieddata\"> </td> \n" +
                " </tr>" +
                "<tr class=\"trdata1\"> \n" +
                " <td class=\"file\"> \n" +
                " <span class=\"nobr\"> \n" +
                " <nobr> \n" +
                " <a href=\"/test\"> \n" +
                " <img class=\"icon\" src=\"/file.gif\" alt=\"\">test_1GB.txt \n" +
                " </a> \n" +
                " </nobr> \n" +
                " </span> \n" +
                " </td> \n" +
                " <td class=\"sizedata\"> \n" +
                " <span class=\"nobr\"> \n" +
                " <nobr>1 GB</nobr> \n" +
                " </span> \n" +
                " </td> \n" +
                " <td class=\"modifieddata\"> </td> \n" +
                " </tr>";
        
        List<EverythingFileItem> items = EverythingHtmlParser.parse(html);
        for (EverythingFileItem item : items) {
            System.out.println(item.getName() + " : " + item.getSize());
        }
    }

    @SneakyThrows
    @Test
    public void testDecode() {
        String decode1 = decodePath("/D%3A/OneDrive/色图/本子/(Ono Rin)/2193728-[(Ono Rin)] oyobanu koi no dōjōyaburi  无望之恋的踢馆 [Chinese]");
        System.out.println(decode1);
        System.out.println(UriUtils.encodePath(decode1, "UTF-8"));
        System.out.println(URLDecoder.decode("/D%3A/OneDrive/色图/本子/(Ono%20Rin)/2193728-%5B(Ono%20Rin)%5D%20oyobanu%20koi%20no%20dōjōyaburi%20%20无望之恋的踢馆%20%5BChinese%5D", "UTF-8"));
        String decode = URLDecoder.decode("%2FD%3A%2F%E7%95%AA%E5%89%A7%2F%5BDBD-Raws%5D%5B%E8%8B%B1%E9%9B%84%E6%95%99%E5%AE%A4%5D%5B01-12TV%E5%85%A8%E9%9B%86%2BOVA%2B%E7%89%B9%E5%85%B8%E6%98%A0%E5%83%8F%5D%5B1080P%5D%5BBDRip%5D%5BHEVC-10bit%5D%5B%E7%AE%80%E7%B9%81%E5%A4%96%E6%8C%82%5D%5BFLAC%5D%5BMKV%5D%2F%E7%89%B9%E5%85%B8%E6%98%A0%E5%83%8F%2F%5BDBD-Raws%5D%5BEiyuu+Kyoushitsu%5D%5BTokuten%5D%5B01%5D%5B1080P%5D%5BBDRip%5D%5BHEVC-10bit%5D%5BFLAC%5D.mkv",
                "UTF-8");
        System.out.println(decode);
        decode = URLDecoder.decode(decode, "UTF-8");
        System.out.println(decode);
        decode = URLDecoder.decode(decode, "UTF-8");
        System.out.println(decode);
        decode = URLDecoder.decode(decode, "UTF-8");
        System.out.println(decode);
        decode = URLDecoder.decode(decode, "UTF-8");
        System.out.println(decode);

    }

    @Test
    public void testParseDate() {
        String html = "<tr class=\"trdata1\"> \n" +
                " <td class=\"file\"> \n" +
                " <span class=\"nobr\"> \n" +
                " <nobr> \n" +
                " <a href=\"/test\"> \n" +
                " <img class=\"icon\" src=\"/file.gif\" alt=\"\">test_date.txt \n" +
                " </a> \n" +
                " </nobr> \n" +
                " </span> \n" +
                " </td> \n" +
                " <td class=\"sizedata\"> </td> \n" +
                " <td class=\"modifieddata\"> \n" +
                " <span class=\"nobr\"> \n" +
                " <nobr>2025/12/27 0:59</nobr> \n" +
                " </span> \n" +
                " </td> \n" +
                " </tr>";
        List<EverythingFileItem> items = EverythingHtmlParser.parse(html);
        for (EverythingFileItem item : items) {
            System.out.println(item.getName() + " : " + item.getModified());
        }
    }
}
