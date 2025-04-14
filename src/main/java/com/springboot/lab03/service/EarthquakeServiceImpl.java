package com.springboot.lab03.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.springboot.lab03.dto.Earthquake;
import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;
import com.springboot.lab03.dto.api.EarthquakeApiResponse;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EarthquakeServiceImpl implements EarthquakeService {

    private final RestTemplate restTemplate;
    private final String noaaBaseUrl;
    private final String earthquakeListEndpoint = "/hazard-service/api/v1/earthquakes"; // 목록 엔드포인트
    // 상세 정보 엔드포인트 추가
    private final String earthquakeDetailEndpoint = "/hazard-service/api/v1/earthquakes/{id}/info";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE; // "yyyy-MM-dd" 형식

    @Autowired
    public EarthquakeServiceImpl(RestTemplate restTemplate, @Value("${noaa.api.base-url}") String noaaBaseUrl) {
        this.restTemplate = restTemplate;
        this.noaaBaseUrl = noaaBaseUrl;
    }

    @Override
    public PageDTO<Earthquake> getData(SearchCriteria criteria) {
        int minYear = extractYear(criteria.getMinDate(), 2020);
        int maxYear = extractYear(criteria.getMaxDate(), 2025);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + earthquakeListEndpoint)
                .queryParam("minYear", minYear)
                .queryParam("maxYear", maxYear)
                // 규모 파라미터 추가 복원 (null 체크 포함)
                .queryParam("minEqMagnitude", criteria.getMinMagnitude() != null ? criteria.getMinMagnitude() : 6.0)
                .queryParam("maxEqMagnitude", criteria.getMaxMagnitude() != null ? criteria.getMaxMagnitude() : 9.9)
                // 페이지네이션 파라미터 복원
                .queryParam("page", criteria.getPage())
                .queryParam("itemsPerPage", criteria.getSize());

        // 키워드 파라미터 추가 로직 제거
        /*
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            builder.queryParam("keyword", criteria.getKeyword().trim());
        }
        */
        // TODO: 위/경도 등 다른 필터 파라미터 추가 로직...

        String url = builder.toUriString();
        System.out.println("Requesting URL: " + url); // 디버깅용 로그 복원

        EarthquakeApiResponse apiResponse = restTemplate.getForObject(url, EarthquakeApiResponse.class);

        if (apiResponse == null || apiResponse.getItems() == null) {
            System.err.println("API 응답 없음 또는 items 필드 없음. 빈 PageDTO 반환");
            return new PageDTO<>(); // 빈 PageDTO 반환 복원
        }

        // 클라이언트 측 날짜 필터링 로직은 유지 (API가 연도만 지원하므로)
        List<Earthquake> items = apiResponse.getItems();
        List<Earthquake> filteredItems = items;
        LocalDate minCriteriaDate = parseDate(criteria.getMinDate());
        LocalDate maxCriteriaDate = parseDate(criteria.getMaxDate());
        if (minCriteriaDate != null || maxCriteriaDate != null) {
            filteredItems = items.stream().filter(item -> {
                LocalDate itemDate = getItemDate(item);
                if (itemDate == null) return false;
                boolean afterMin = (minCriteriaDate == null) || !itemDate.isBefore(minCriteriaDate);
                boolean beforeMax = (maxCriteriaDate == null) || !itemDate.isAfter(maxCriteriaDate);
                return afterMin && beforeMax;
            }).collect(Collectors.toList());
        }

        // --- 클라이언트 측 위도/경도 필터링 시작 ---
        Double minLat = criteria.getMinLatitude();
        Double maxLat = criteria.getMaxLatitude();
        Double minLon = criteria.getMinLongitude();
        Double maxLon = criteria.getMaxLongitude();

        if (minLat != null || maxLat != null || minLon != null || maxLon != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> {
                        Double itemLat = item.getLatitude(); // Earthquake DTO의 latitude 필드
                        Double itemLon = item.getLongitude(); // Earthquake DTO의 longitude 필드
                        if (itemLat == null || itemLon == null) return false; // null 값 처리

                        boolean latOk = (minLat == null || itemLat >= minLat) && (maxLat == null || itemLat <= maxLat);
                        boolean lonOk = (minLon == null || itemLon >= minLon) && (maxLon == null || itemLon <= maxLon);
                        return latOk && lonOk;
                    })
                    .collect(Collectors.toList());
        }
        // --- 클라이언트 측 위도/경도 필터링 종료 ---

        // PageDTO 생성 로직 복원 (필터링된 리스트 사용)
        return new PageDTO<>(
                filteredItems,
                apiResponse.getItemsPerPage(),
                apiResponse.getPage(),
                apiResponse.getTotalItems(),
                apiResponse.getTotalPages()
        );
    }

    @Override
    public Earthquake getDataById(int id) {
        String url = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + earthquakeDetailEndpoint)
                .buildAndExpand(id)
                .toUriString();

        System.out.println("Requesting Detail URL: " + url);

        try {
            Earthquake earthquake = restTemplate.getForObject(url, Earthquake.class);
            return earthquake;
        } catch (HttpClientErrorException.NotFound nf) {
             System.err.println("Earthquake not found for ID: " + id);
             return null;
        } catch (RestClientException e) {
            System.err.println("Error fetching earthquake details for ID " + id + ": " + e.getMessage());
            return null;
        }
    }

    private int extractYear(String dateString, int defaultYear) {
        LocalDate date = parseDate(dateString);
        return (date != null) ? date.getYear() : defaultYear;
    }

    private LocalDate parseDate(String dateString) {
        if (dateString != null && !dateString.isEmpty()) {
            try {
                return LocalDate.parse(dateString, DATE_FORMATTER);
            } catch (Exception e) {
                System.err.println("날짜 파싱 오류: " + dateString);
            }
        }
        return null;
    }

    private LocalDate getItemDate(Earthquake item) {
        if (item == null || item.getYear() == 0 || item.getMonth() == 0 || item.getDay() == 0) {
            return null;
        }
        try {
            return LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
        } catch (Exception e) {
            System.err.println("지진 데이터 날짜 생성 오류 (ID: " + item.getId() + "): " + e.getMessage());
            return null;
        }
    }
} 