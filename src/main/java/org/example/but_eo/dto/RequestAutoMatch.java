package org.example.but_eo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestAutoMatch {
    private String teamId;
    private String sportType;
    private String region;
    private int rating;
}
