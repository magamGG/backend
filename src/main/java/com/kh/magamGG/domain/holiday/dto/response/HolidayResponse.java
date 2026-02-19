package com.kh.magamGG.domain.holiday.dto.response;

import com.kh.magamGG.domain.holiday.dto.HolidayItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {
    private Integer year;
    private List<HolidayItem> holidays;
}

