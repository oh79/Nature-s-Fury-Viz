package com.springboot.lab03.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;
import com.springboot.lab03.dto.Tsunami;
import com.springboot.lab03.dto.api.TsunamiApiResponse;

@Service
public class TsunamiServiceImpl implements TsunamiService {

    private final RestTemplate restTemplate;
    private final String noaaBaseUrl; // NOAA API 기본 URL
    private final String tsunamiListEndpoint = "/hazard-service/api/v1/tsunamis/events"; // 목록 API 엔드포인트
    private final String tsunamiDetailEndpoint = "/hazard-service/api/v1/tsunamis/events/{id}/info"; // 상세 API 엔드포인트
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE; // 날짜 포맷터

    @Autowired
    public TsunamiServiceImpl(RestTemplate restTemplate, @Value("${noaa.api.base-url}") String noaaBaseUrl) {
        this.restTemplate = restTemplate;
        this.noaaBaseUrl = noaaBaseUrl;
    }

    /**
     * 쓰나미 데이터 목록 조회 (페이징 및 검색 조건 적용).
     * API 호출 후 클라이언트 측 추가 필터링(날짜, 수위, Runups, 위/경도) 수행.
     */
    @Override
    public PageDTO<Tsunami> getData(SearchCriteria criteria) {
        // API 요청 년도 추출 (기본값: 2020-2025)
        int minYear = extractYear(criteria.getMinDate(), 2020);
        int maxYear = extractYear(criteria.getMaxDate(), 2025);

        // API URL 빌더
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + tsunamiListEndpoint)
                .queryParam("minYear", minYear)
                .queryParam("maxYear", maxYear)
                // 수위, Runups 파라미터 (값이 있을 때만 포함)
                .queryParamIfPresent("minMaximumWaterHeight", java.util.Optional.ofNullable(criteria.getMinMaximumWaterHeight()))
                .queryParamIfPresent("minNumberOfRunup", java.util.Optional.ofNullable(criteria.getMinNumberOfRunup()))
                // 페이징 파라미터
                .queryParam("page", criteria.getPage())
                .queryParam("itemsPerPage", criteria.getSize());

        String url = builder.toUriString();
        // System.out.println("Requesting URL: " + url);

        // API 호출
        TsunamiApiResponse apiResponse = restTemplate.getForObject(url, TsunamiApiResponse.class);

        // 응답 검증
        if (apiResponse == null || apiResponse.getItems() == null) {
            System.err.println("API 응답 없음 - Tsunami");
            return new PageDTO<>(); // 빈 결과 반환
        }

        // 클라이언트 측 필터링
        List<Tsunami> items = apiResponse.getItems();
        List<Tsunami> filteredItems = filterByDate(items, criteria.getMinDate(), criteria.getMaxDate());
        filteredItems = filterTsunamiSpecific(filteredItems, criteria);
        filteredItems = filterByLocation(filteredItems, criteria);

        // 최종 PageDTO 반환
        return new PageDTO<>(
                filteredItems,
                apiResponse.getItemsPerPage(),
                apiResponse.getPage(),
                apiResponse.getTotalItems(),
                apiResponse.getTotalPages()
        );
    }

    /**
     * ID로 쓰나미 상세 정보 조회.
     */
    @Override
    public Tsunami getDataById(int id) {
        String url = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + tsunamiDetailEndpoint)
                .buildAndExpand(id)
                .toUriString();
        // System.out.println("Requesting Detail URL: " + url);

        try {
            return restTemplate.getForObject(url, Tsunami.class);
        } catch (HttpClientErrorException.NotFound nf) {
             System.err.println("Tsunami not found: " + id);
             return null;
        } catch (RestClientException e) {
            System.err.println("Error fetching tsunami detail (ID: " + id + "): " + e.getMessage());
            return null;
        }
    }

    // --- Helper Methods ---

    /** 날짜 문자열에서 연도 추출 (기본값 반환 가능) */
    private int extractYear(String dateString, int defaultYear) {
        LocalDate date = parseDate(dateString);
        return (date != null) ? date.getYear() : defaultYear;
    }

    /** YYYY-MM-DD 형식 문자열을 LocalDate로 파싱 */
    private LocalDate parseDate(String dateString) {
        if (dateString != null && !dateString.isEmpty()) {
            try {
                return LocalDate.parse(dateString, DATE_FORMATTER);
            } catch (Exception e) {
                // System.err.println("Date parsing error: " + dateString);
            }
        }
        return null;
    }

    /** Tsunami 객체에서 LocalDate 생성 */
    private LocalDate getItemDate(Tsunami item) {
        if (item == null || item.getYear() == 0 || item.getMonth() == 0 || item.getDay() == 0) return null;
        try {
            return LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
        } catch (Exception e) {
            // System.err.println("Error creating date for Tsunami ID: " + item.getId());
            return null;
        }
    }

    /** 날짜 범위로 목록 필터링 */
    private List<Tsunami> filterByDate(List<Tsunami> items, String minDateStr, String maxDateStr) {
        LocalDate minCriteriaDate = parseDate(minDateStr);
        LocalDate maxCriteriaDate = parseDate(maxDateStr);
        if (minCriteriaDate == null && maxCriteriaDate == null) return items;
        return items.stream().filter(item -> {
            LocalDate itemDate = getItemDate(item);
            if (itemDate == null) return false;
            boolean afterMin = (minCriteriaDate == null) || !itemDate.isBefore(minCriteriaDate);
            boolean beforeMax = (maxCriteriaDate == null) || !itemDate.isAfter(maxCriteriaDate);
            return afterMin && beforeMax;
        }).collect(Collectors.toList());
    }

    /** 쓰나미 관련 조건(수위, Runups)으로 목록 필터링 */
    private List<Tsunami> filterTsunamiSpecific(List<Tsunami> items, SearchCriteria criteria) {
        Double minWaterHeightCriteria = criteria.getMinMaximumWaterHeight();
        Integer minRunupsCriteria = criteria.getMinNumberOfRunup();
        if (minWaterHeightCriteria == null && minRunupsCriteria == null) return items;

        return items.stream()
                .filter(item -> {
                    boolean waterHeightOk = (minWaterHeightCriteria == null) || (item.getMaximumWaterHeight() != null && item.getMaximumWaterHeight() >= minWaterHeightCriteria);
                    boolean runupsOk = (minRunupsCriteria == null) || (item.getNumberOfRunup() != null && item.getNumberOfRunup() >= minRunupsCriteria);
                    return waterHeightOk && runupsOk;
                })
                .collect(Collectors.toList());
    }

    /** 위/경도 범위로 목록 필터링 */
    private List<Tsunami> filterByLocation(List<Tsunami> items, SearchCriteria criteria) {
        Double minLat = criteria.getMinLatitude();
        Double maxLat = criteria.getMaxLatitude();
        Double minLon = criteria.getMinLongitude();
        Double maxLon = criteria.getMaxLongitude();

        if (minLat == null && maxLat == null && minLon == null && maxLon == null) return items;

        return items.stream()
                .filter(item -> {
                    Double itemLat = item.getLatitude();
                    Double itemLon = item.getLongitude();
                    if (itemLat == null || itemLon == null) return false;

                    boolean latOk = (minLat == null || itemLat >= minLat) && (maxLat == null || itemLat <= maxLat);
                    boolean lonOk = (minLon == null || itemLon >= minLon) && (maxLon == null || itemLon <= maxLon);
                    return latOk && lonOk;
                })
                .collect(Collectors.toList());
    }
} 