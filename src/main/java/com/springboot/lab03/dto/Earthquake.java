package com.springboot.lab03.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Earthquake {

    @JsonProperty("id")
    private int id;

    private int year;
    private int month;
    private int day;

    @JsonProperty("locationName")
    private String location;

    private double latitude;
    private double longitude;

    @JsonProperty("eqDepth")
    private double depthKm;

    @JsonProperty("eqMagnitude")
    private Double magnitude;
} 