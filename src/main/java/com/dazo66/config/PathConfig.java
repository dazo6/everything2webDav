package com.dazo66.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
public class PathConfig {
    
    private Map<String, String> map = new HashMap<>();

    @SneakyThrows
    @Value("${base.path.map:}")
    public void setMapFromConfig(String mapStr) {
        ObjectMapper map1 = new ObjectMapper();
        map = map1.readValue(mapStr, new TypeReference<Map<String, String>>(){});
    }

}
