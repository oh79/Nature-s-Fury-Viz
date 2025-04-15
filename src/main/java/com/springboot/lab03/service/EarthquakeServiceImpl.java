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

import com.springboot.lab03.dto.Earthquake;
import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;
import com.springboot.lab03.dto.api.EarthquakeApiResponse;

@Service
public class EarthquakeServiceImpl implements EarthquakeService {

    private final RestTemplate restTemplate;
    private final String noaaBaseUrl;
    private final String earthquakeListEndpoint = "/hazard-service/api/v1/earthquakes"; // 목록 API 엔드포인트
    private final String earthquakeDetailEndpoint = "/hazard-service/api/v1/earthquakes/{id}/info"; // 상세 API 엔드포인트
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE; // 날짜 포맷터

    @Autowired
    public EarthquakeServiceImpl(RestTemplate restTemplate, @Value("${noaa.api.base-url}") String noaaBaseUrl) {
        this.restTemplate = restTemplate;
        this.noaaBaseUrl = noaaBaseUrl;
    }

    /**
     * 지진 데이터 목록 조회 (페이징 및 검색 조건 적용).
     * API 호출 후 클라이언트 측 추가 필터링(날짜, 위/경도) 수행.
     */
    @Override
    public PageDTO<Earthquake> getData(SearchCriteria criteria) {
        // API 요청 년도 추출 (기본값: 2020-2025)
        int minYear = extractYear(criteria.getMinDate(), 2020);
        int maxYear = extractYear(criteria.getMaxDate(), 2025);

        // API URL 빌더
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + earthquakeListEndpoint)
                .queryParam("minYear", minYear)
                .queryParam("maxYear", maxYear)
                // 지진 규모 파라미터 (기본값 6.0-9.9)
                .queryParam("minEqMagnitude", criteria.getMinMagnitude() != null ? criteria.getMinMagnitude() : 6.0)
                .queryParam("maxEqMagnitude", criteria.getMaxMagnitude() != null ? criteria.getMaxMagnitude() : 9.9)
                // 페이징 파라미터
                .queryParam("page", criteria.getPage())
                .queryParam("itemsPerPage", criteria.getSize());

        String url = builder.toUriString();
        // System.out.println("Requesting URL: " + url); // 디버깅 시 활성화

        // API 호출
        EarthquakeApiResponse apiResponse = restTemplate.getForObject(url, EarthquakeApiResponse.class);

        // 응답 검증
        if (apiResponse == null || apiResponse.getItems() == null) {
            System.err.println("API 응답 없음 - Earthquake");
            return new PageDTO<>(); // 빈 결과 반환
        }

        // 클라이언트 측 필터링
        List<Earthquake> items = apiResponse.getItems();
        List<Earthquake> filteredItems = filterByDate(items, criteria.getMinDate(), criteria.getMaxDate());
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
     * ID로 지진 상세 정보 조회.
     */
    @Override
    public Earthquake getDataById(int id) {
        String url = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + earthquakeDetailEndpoint)
                .buildAndExpand(id)
                .toUriString();
        // System.out.println("Requesting Detail URL: " + url);

        try {
            return restTemplate.getForObject(url, Earthquake.class);
        } catch (HttpClientErrorException.NotFound nf) {
             System.err.println("Earthquake not found: " + id);
             return null;
        } catch (RestClientException e) {
            System.err.println("Error fetching earthquake detail (ID: " + id + "): " + e.getMessage());
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
                // System.err.println("Date parsing error: " + dateString); // 필요 시 로그 추가
            }
        }
        return null;
    }

    /** Earthquake 객체에서 LocalDate 생성 */
    private LocalDate getItemDate(Earthquake item) {
        if (item == null || item.getYear() == 0 || item.getMonth() == 0 || item.getDay() == 0) return null;
        try {
            return LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
        } catch (Exception e) {
            // System.err.println("Error creating date for Earthquake ID: " + item.getId());
            return null;
        }
    }

    /** 날짜 범위로 목록 필터링 */
    private List<Earthquake> filterByDate(List<Earthquake> items, String minDateStr, String maxDateStr) {
        LocalDate minCriteriaDate = parseDate(minDateStr);
        LocalDate maxCriteriaDate = parseDate(maxDateStr);
        if (minCriteriaDate == null && maxCriteriaDate == null) {
            return items; // 필터링 필요 없음
        }
        return items.stream().filter(item -> {
            LocalDate itemDate = getItemDate(item);
            if (itemDate == null) return false; // 날짜 없으면 제외
            boolean afterMin = (minCriteriaDate == null) || !itemDate.isBefore(minCriteriaDate);
            boolean beforeMax = (maxCriteriaDate == null) || !itemDate.isAfter(maxCriteriaDate);
            return afterMin && beforeMax;
        }).collect(Collectors.toList());
    }

    /** 위/경도 범위로 목록 필터링 */
    private List<Earthquake> filterByLocation(List<Earthquake> items, SearchCriteria criteria) {
        Double minLat = criteria.getMinLatitude();
        Double maxLat = criteria.getMaxLatitude();
        Double minLon = criteria.getMinLongitude();
        Double maxLon = criteria.getMaxLongitude();

        if (minLat == null && maxLat == null && minLon == null && maxLon == null) {
            return items; // 필터링 필요 없음
        }

        return items.stream()
                .filter(item -> {
                    Double itemLat = item.getLatitude();
                    Double itemLon = item.getLongitude();
                    if (itemLat == null || itemLon == null) return false; // 위치 정보 없으면 제외

                    boolean latOk = (minLat == null || itemLat >= minLat) && (maxLat == null || itemLat <= maxLat);
                    boolean lonOk = (minLon == null || itemLon >= minLon) && (maxLon == null || itemLon <= maxLon);
                    return latOk && lonOk;
                })
                .collect(Collectors.toList());
    }
} 