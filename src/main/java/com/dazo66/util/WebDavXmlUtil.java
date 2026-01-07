package com.dazo66.util;

import com.dazo66.model.EverythingFileItem;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WebDavXmlUtil {

    private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    @SneakyThrows
    public static String toWebDavXml(List<EverythingFileItem> items) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<D:multistatus xmlns:D=\"DAV:\">\n");

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        for (EverythingFileItem item : items) {
            if (item.getPath() == null || item.getName() == null) continue;
            xml.append("  <D:response>\n");
            
            String href = item.getPath();
            if (href == null) href = "";

            xml.append("    <D:href>").append(escapeXml(href)).append("</D:href>\n");
            
            xml.append("    <D:propstat>\n");
            xml.append("      <D:prop>\n");
            
            xml.append("        <D:displayname>").append(escapeXml(item.getName())).append("</D:displayname>\n");
            
            if (item.getModified() != null) {
                String dateStr = sdf.format(new Date(item.getModified()));
                xml.append("        <D:getlastmodified>").append(dateStr).append("</D:getlastmodified>\n");
            }
            
            if (Boolean.TRUE.equals(item.getIsFile())) {
                xml.append("        <D:resourcetype/>\n");
                if (item.getSize() != null) {
                    long bytes = (long) (item.getSize() * 1024);
                    xml.append("        <D:getcontentlength>").append(bytes).append("</D:getcontentlength>\n");
                }
            } else {
                xml.append("        <D:resourcetype><D:collection/></D:resourcetype>\n");
            }
            
            xml.append("      </D:prop>\n");
            xml.append("      <D:status>HTTP/1.1 200 OK</D:status>\n");
            xml.append("    </D:propstat>\n");
            xml.append("  </D:response>\n");
        }

        xml.append("</D:multistatus>");
        return xml.toString();
    }
    
    private static String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
