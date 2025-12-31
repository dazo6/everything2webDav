package com.dazo66.model;

import lombok.Data;

@Data
public class EverythingFileItem {
    private String name;
    private String path;
    private Boolean isFile;
    private Double size;
    private Long modified;
}
