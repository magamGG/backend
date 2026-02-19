package com.kh.magamGG.domain.holiday.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.magamGG.domain.holiday.dto.HolidayItem;
import com.kh.magamGG.domain.holiday.dto.response.HolidayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {
    
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${file.upload-dir:uploads}")
    private String uploadDir; // 기존 uploads 경로 사용
    
    @Value("${public-data.holiday.api-key}")
    private String apiKey;
    
    @Value("${public-data.holiday.base-url}")
    private String baseUrl;
    
    @Value("${public-data.holiday.data-dir:holidays}")
    private String holidayDataDir; // holidays 서브디렉토리
    
    /**
     * 연도별 공휴일 조회 (파일에서 읽기, 없으면 API 호출 후 저장)
     */
    public HolidayResponse getHolidaysByYear(int year) {
        try {
            // 1. 파일에서 읽기 시도
            List<HolidayItem> holidays = readFromFile(year);
            
            if (holidays != null && !holidays.isEmpty()) {
                log.info("파일에서 공휴일 데이터 로드: year={}, count={}", year, holidays.size());
                return HolidayResponse.builder()
                    .year(year)
                    .holidays(holidays)
                    .build();
            }
            
            // 2. 파일이 없으면 API 호출
            log.info("파일이 없어 API 호출: year={}", year);
            holidays = fetchHolidaysFromApi(year);
            
            // 3. 파일로 저장
            if (!holidays.isEmpty()) {
                saveToFile(year, holidays);
            }
            
            return HolidayResponse.builder()
                .year(year)
                .holidays(holidays)
                .build();
            
        } catch (Exception e) {
            log.error("공휴일 조회 실패: year={}", year, e);
            return HolidayResponse.builder()
                .year(year)
                .holidays(new ArrayList<>())
                .build();
        }
    }
    
    /**
     * 파일에서 읽기
     */
    private List<HolidayItem> readFromFile(int year) {
        try {
            Path filePath = getFilePath(year);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return null;
            }
            
            String jsonContent = Files.readString(filePath);
            List<HolidayItem> items = objectMapper.readValue(
                jsonContent,
                new TypeReference<List<HolidayItem>>() {}
            );
            
            log.debug("파일에서 공휴일 읽기 성공: year={}, count={}", year, items.size());
            return items;
            
        } catch (IOException e) {
            log.warn("파일 읽기 실패: year={}, error={}", year, e.getMessage());
            return null;
        }
    }
    
    /**
     * 파일로 저장
     */
    private void saveToFile(int year, List<HolidayItem> holidays) {
        try {
            Path filePath = getFilePath(year);
            Path dirPath = filePath.getParent();
            
            // 디렉토리 생성 (기존 FileStorageService 패턴과 동일)
            if (dirPath != null && !Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            // JSON 변환 후 저장
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(holidays);
            
            Files.writeString(filePath, jsonContent);
            log.info("공휴일 데이터 파일 저장 완료: year={}, path={}, count={}", year, filePath, holidays.size());
            
        } catch (IOException e) {
            log.error("파일 저장 실패: year={}", year, e);
        }
    }
    
    /**
     * 파일 경로 생성 (기존 FileStorageService 패턴과 동일)
     */
    private Path getFilePath(int year) {
        // uploads/holidays/{year}.json
        Path uploadPath = Paths.get(uploadDir, holidayDataDir);
        return uploadPath.resolve(year + ".json");
    }
    
    /**
     * 공공데이터 API 호출
     */
    private List<HolidayItem> fetchHolidaysFromApi(int year) {
        try {
            // 이미 인코딩된 키를 직접 URL에 포함
            String url = String.format(
                "%s/getRestDeInfo?serviceKey=%s&solYear=%d&numOfRows=100&_type=json",
                baseUrl, apiKey, year
            );
            
            // URI로 변환 (추가 인코딩 없음)
            URI uri = new URI(url);
            
            log.info("공공데이터 API 호출: year={}, uri={}", year, uri);
            String response = restTemplate.getForObject(uri, String.class);
            
            if (response == null || response.isEmpty()) {
                log.warn("API 응답이 비어있음: year={}", year);
                return new ArrayList<>();
            }
            
            // API 응답 파싱
            List<HolidayItem> items = parseApiResponse(response, year);
            
            log.info("공휴일 API 호출 성공: year={}, count={}", year, items.size());
            return items;
            
        } catch (Exception e) {
            log.error("공휴일 API 호출 실패: year={}, error={}", year, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * API 응답 파싱 (공공데이터 API 실제 응답 구조)
     */
    private List<HolidayItem> parseApiResponse(String response, int year) {
        try {
            JsonNode root = objectMapper.readTree(response);
            
            // 응답 구조: response.body.items.item (배열 또는 단일 객체)
            JsonNode items = root
                .path("response")
                .path("body")
                .path("items")
                .path("item");
            
            List<HolidayItem> holidayList = new ArrayList<>();
            
            // item이 배열인 경우
            if (items.isArray()) {
                for (JsonNode item : items) {
                    HolidayItem holidayItem = parseHolidayItem(item);
                    if (holidayItem != null) {
                        holidayList.add(holidayItem);
                    }
                }
            } 
            // item이 단일 객체인 경우 (공휴일이 1개만 있을 때)
            else if (items.isObject() && !items.isEmpty()) {
                HolidayItem holidayItem = parseHolidayItem(items);
                if (holidayItem != null) {
                    holidayList.add(holidayItem);
                }
            }
            
            log.info("공휴일 파싱 완료: year={}, count={}", year, holidayList.size());
            return holidayList;
            
        } catch (Exception e) {
            log.error("API 응답 파싱 실패: year={}, response={}", year, response, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 개별 공휴일 아이템 파싱
     */
    private HolidayItem parseHolidayItem(JsonNode item) {
        try {
            // locdate는 숫자 또는 문자열일 수 있음
            String locdateStr;
            if (item.path("locdate").isNumber()) {
                locdateStr = String.valueOf(item.path("locdate").asLong());
            } else {
                locdateStr = item.path("locdate").asText();
            }
            
            if (locdateStr == null || locdateStr.isEmpty()) {
                return null;
            }
            
            String dateName = item.path("dateName").asText();
            
            // 날짜 형식 변환: "20260101" -> "2026-01-01"
            String formattedDate = formatDate(locdateStr);
            
            // 토요일/일요일 여부 계산
            boolean isSat = isSaturday(formattedDate);
            boolean isSun = isSunday(formattedDate);
            
            return HolidayItem.builder()
                .date(formattedDate)
                .name(dateName)
                .isSaturday(isSat)
                .isSunday(isSun)
                .build();
                
        } catch (Exception e) {
            log.warn("공휴일 아이템 파싱 실패: item={}, error={}", item, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 날짜 형식 변환: "20260101" -> "2026-01-01"
     */
    private String formatDate(String locdate) {
        if (locdate == null || locdate.length() != 8) {
            return locdate;
        }
        return locdate.substring(0, 4) + "-" + 
               locdate.substring(4, 6) + "-" + 
               locdate.substring(6, 8);
    }
    
    /**
     * 토요일/일요일 여부 계산
     */
    private boolean isSaturday(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.getDayOfWeek() == DayOfWeek.SATURDAY;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isSunday(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.getDayOfWeek() == DayOfWeek.SUNDAY;
        } catch (Exception e) {
            return false;
        }
    }
}
