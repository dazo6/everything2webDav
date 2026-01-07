package com.dazo66.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class EverythingFileItem {
    private String name;
    private String path;
    private Boolean isFile;
    private Double size;
    private Long modified;

    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EverythingFileItem) {
            return ((EverythingFileItem) obj).path.hashCode() == path.hashCode() && ((EverythingFileItem) obj).path.equals(path);
        } else {
            return false;
        }
    }
}
