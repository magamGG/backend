package com.kh.magamGG.domain.holiday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayItem {
    @JsonProperty("date")
    private String date; // "2026-01-01"
    
    @JsonProperty("name")
    private String name; // "신정"
    
    @JsonProperty("isSaturday")
    private Boolean isSaturday;
    
    @JsonProperty("isSunday")
    private Boolean isSunday;
}
