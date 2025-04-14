package com.springboot.lab03.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.springboot.lab03.dto.PageDTO;
import com.springboot.lab03.dto.SearchCriteria;
import com.springboot.lab03.dto.Volcano;
import com.springboot.lab03.dto.api.VolcanoApiResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VolcanoServiceImpl implements VolcanoService {

    private final RestTemplate restTemplate;
    private final String noaaBaseUrl; // NOAA API Base URL
    private final String volcanoListEndpoint = "/hazard-service/api/v1/volcanoes"; // 목록 엔드포인트
    // 상세 정보 엔드포인트 추가 (경로가 다름)
    private final String volcanoDetailEndpoint = "/hazard-service/api/v1/volcanoes/{id}/info";

    @Autowired
    public VolcanoServiceImpl(RestTemplate restTemplate, @Value("${noaa.api.base-url}") String noaaBaseUrl) {
        this.restTemplate = restTemplate;
        this.noaaBaseUrl = noaaBaseUrl;
    }

    @Override
    public PageDTO<Volcano> getData(SearchCriteria criteria) {
        int minYear = extractYear(criteria.getMinDate(), 2020);
        int maxYear = extractYear(criteria.getMaxDate(), 2025);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + volcanoListEndpoint)
                .queryParam("minYear", minYear)
                .queryParam("maxYear", maxYear)
                .queryParamIfPresent("minElevation", java.util.Optional.ofNullable(criteria.getMinElevation()))
                .queryParamIfPresent("minVei", java.util.Optional.ofNullable(criteria.getMinVei()))
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
        System.out.println("Requesting URL: " + url);

        VolcanoApiResponse apiResponse = restTemplate.getForObject(url, VolcanoApiResponse.class);

        if (apiResponse == null || apiResponse.getItems() == null) {
            System.err.println("API 응답 없음 또는 items 필드 없음. 빈 PageDTO 반환");
            return new PageDTO<>();
        }

        // --- 클라이언트 측 날짜 필터링 시작 (Volcano 용) ---
        List<Volcano> items = apiResponse.getItems();
        List<Volcano> filteredItems = items;

        LocalDate minCriteriaDate = parseDate(criteria.getMinDate());
        LocalDate maxCriteriaDate = parseDate(criteria.getMaxDate());

        if (minCriteriaDate != null || maxCriteriaDate != null) {
            LocalDate finalMinCriteriaDate = minCriteriaDate;
            LocalDate finalMaxCriteriaDate = maxCriteriaDate;

            filteredItems = items.stream()
                    .filter(item -> {
                        LocalDate itemDate = getItemDate(item); // Volcano 객체에서 날짜 가져오기
                        if (itemDate == null) {
                            return false; // 날짜 정보 없으면 제외
                        }
                        boolean afterMin = (finalMinCriteriaDate == null) || !itemDate.isBefore(finalMinCriteriaDate);
                        boolean beforeMax = (finalMaxCriteriaDate == null) || !itemDate.isAfter(finalMaxCriteriaDate);
                        return afterMin && beforeMax;
                    })
                    .collect(Collectors.toList());
        }
        // --- 클라이언트 측 날짜 필터링 종료 ---

        // --- 클라이언트 측 고도 필터링 시작 (minElevation) ---
        Integer minElevationCriteria = criteria.getMinElevation();
        if (minElevationCriteria != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> item.getElevation() >= minElevationCriteria)
                    .collect(Collectors.toList());
        }
        // --- 클라이언트 측 고도 필터링 종료 ---

        // --- 클라이언트 측 위도/경도 필터링 시작 ---
        Double minLat = criteria.getMinLatitude();
        Double maxLat = criteria.getMaxLatitude();
        Double minLon = criteria.getMinLongitude();
        Double maxLon = criteria.getMaxLongitude();

        if (minLat != null || maxLat != null || minLon != null || maxLon != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> {
                        Double itemLat = item.getLatitude(); // Wrapper type, can be null
                        Double itemLon = item.getLongitude();
                        if (itemLat == null || itemLon == null) return false; // 위도 또는 경도 없으면 제외

                        boolean latOk = (minLat == null || itemLat >= minLat) && (maxLat == null || itemLat <= maxLat);
                        boolean lonOk = (minLon == null || itemLon >= minLon) && (maxLon == null || itemLon <= maxLon);
                        return latOk && lonOk;
                    })
                    .collect(Collectors.toList());
        }
        // --- 클라이언트 측 위도/경도 필터링 종료 ---

        return new PageDTO<>(
                filteredItems, // 최종 필터링된 리스트 사용
                apiResponse.getItemsPerPage(),
                apiResponse.getPage(),
                apiResponse.getTotalItems(),
                apiResponse.getTotalPages()
        );
    }

    @Override
    public Volcano getDataById(int id) {
        String url = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + volcanoDetailEndpoint)
                .buildAndExpand(id)
                .toUriString();

        System.out.println("Requesting Detail URL: " + url);

        try {
            Volcano volcano = restTemplate.getForObject(url, Volcano.class);
            return volcano;
        } catch (HttpClientErrorException.NotFound nf) {
             System.err.println("Volcano not found for ID: " + id);
             return null;
        } catch (RestClientException e) {
            System.err.println("Error fetching volcano details for ID " + id + ": " + e.getMessage());
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
                return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
            } catch (Exception e) {
                System.err.println("날짜 파싱 오류: " + dateString);
            }
        }
        return null;
    }

    private LocalDate getItemDate(Volcano item) {
        if (item == null || item.getYear() == 0 || item.getMonth() == 0 || item.getDay() == 0) {
            return null;
        }
        try {
            return LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
        } catch (Exception e) {
            System.err.println("화산 데이터 날짜 생성 오류 (ID: " + item.getId() + "): " + e.getMessage());
            return null;
        }
    }
} 