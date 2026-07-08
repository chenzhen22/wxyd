package com.chenzhen.test;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class TuominField {
    private String name;
    private String desc;

    public TuominField(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
