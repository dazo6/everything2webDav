package com.dazo66;

import com.dazo66.model.EverythingFileItem;
import com.dazo66.util.WebDavXmlUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class WebDavUtilTest {

    @Test
    public void testXmlGeneration() {
        List<EverythingFileItem> items = new ArrayList<>();
        EverythingFileItem item1 = new EverythingFileItem();
        item1.setName("test.txt");
        item1.setPath("/C%3A/test.txt");
        item1.setIsFile(true);
        item1.setSize(1.5); // 1.5 KB
        item1.setModified(1703635200000L); // 2023-12-27 ...
        items.add(item1);
        
        EverythingFileItem item2 = new EverythingFileItem();
        item2.setName("folder");
        item2.setPath("/C%3A/folder");
        item2.setIsFile(false);
        item2.setModified(1703635200000L);
        items.add(item2);

        String xml = WebDavXmlUtil.toWebDavXml(items);
        System.out.println(xml);
    }
}
