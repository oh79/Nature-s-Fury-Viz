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
import com.springboot.lab03.dto.Tsunami;
import com.springboot.lab03.dto.api.TsunamiApiResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TsunamiServiceImpl implements TsunamiService {

    private final RestTemplate restTemplate;
    private final String noaaBaseUrl;
    private final String tsunamiListEndpoint = "/hazard-service/api/v1/tsunamis/events"; // 목록 엔드포인트
    private final String tsunamiDetailEndpoint = "/hazard-service/api/v1/tsunamis/events/{id}/info";

    @Autowired
    public TsunamiServiceImpl(RestTemplate restTemplate, @Value("${noaa.api.base-url}") String noaaBaseUrl) {
        this.restTemplate = restTemplate;
        this.noaaBaseUrl = noaaBaseUrl;
    }

    @Override
    public PageDTO<Tsunami> getData(SearchCriteria criteria) {
        int minYear = extractYear(criteria.getMinDate(), 2020);
        int maxYear = extractYear(criteria.getMaxDate(), 2025);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + tsunamiListEndpoint)
                .queryParam("minYear", minYear)
                .queryParam("maxYear", maxYear)
                .queryParamIfPresent("minMaximumWaterHeight", java.util.Optional.ofNullable(criteria.getMinMaximumWaterHeight()))
                .queryParamIfPresent("minNumberOfRunup", java.util.Optional.ofNullable(criteria.getMinNumberOfRunup()))
                .queryParam("page", criteria.getPage())
                .queryParam("itemsPerPage", criteria.getSize());

        String url = builder.toUriString();
        System.out.println("Requesting URL: " + url);

        TsunamiApiResponse apiResponse = restTemplate.getForObject(url, TsunamiApiResponse.class);

        if (apiResponse == null || apiResponse.getItems() == null) {
            System.err.println("API 응답 없음 또는 items 필드 없음. 빈 PageDTO 반환");
            return new PageDTO<>();
        }

        List<Tsunami> items = apiResponse.getItems();
        List<Tsunami> filteredItems = items;

        LocalDate minCriteriaDate = parseDate(criteria.getMinDate());
        LocalDate maxCriteriaDate = parseDate(criteria.getMaxDate());

        if (minCriteriaDate != null || maxCriteriaDate != null) {
            LocalDate finalMinCriteriaDate = minCriteriaDate;
            LocalDate finalMaxCriteriaDate = maxCriteriaDate;

            filteredItems = items.stream()
                    .filter(item -> {
                        LocalDate itemDate = getItemDate(item);
                        if (itemDate == null) {
                            return false;
                        }
                        boolean afterMin = (finalMinCriteriaDate == null) || !itemDate.isBefore(finalMinCriteriaDate);
                        boolean beforeMax = (finalMaxCriteriaDate == null) || !itemDate.isAfter(finalMaxCriteriaDate);
                        return afterMin && beforeMax;
                    })
                    .collect(Collectors.toList());
        }

        // --- 클라이언트 측 추가 필터링 시작 (수위, Runups) ---
        Double minWaterHeightCriteria = criteria.getMinMaximumWaterHeight();
        Integer minRunupsCriteria = criteria.getMinNumberOfRunup();

        if (minWaterHeightCriteria != null || minRunupsCriteria != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> {
                        boolean waterHeightOk = (minWaterHeightCriteria == null) || (item.getMaximumWaterHeight() != null && item.getMaximumWaterHeight() >= minWaterHeightCriteria);
                        boolean runupsOk = (minRunupsCriteria == null) || (item.getNumberOfRunup() != null && item.getNumberOfRunup() >= minRunupsCriteria);
                        return waterHeightOk && runupsOk;
                    })
                    .collect(Collectors.toList());
        }
        // --- 클라이언트 측 추가 필터링 종료 ---

        // --- 클라이언트 측 위도/경도 필터링 시작 ---
        Double minLat = criteria.getMinLatitude();
        Double maxLat = criteria.getMaxLatitude();
        Double minLon = criteria.getMinLongitude();
        Double maxLon = criteria.getMaxLongitude();

        if (minLat != null || maxLat != null || minLon != null || maxLon != null) {
            filteredItems = filteredItems.stream()
                    .filter(item -> {
                        Double itemLat = item.getLatitude(); // Tsunami DTO의 latitude
                        Double itemLon = item.getLongitude(); // Tsunami DTO의 longitude
                        if (itemLat == null || itemLon == null) return false;

                        boolean latOk = (minLat == null || itemLat >= minLat) && (maxLat == null || itemLat <= maxLat);
                        boolean lonOk = (minLon == null || itemLon >= minLon) && (maxLon == null || itemLon <= maxLon);
                        return latOk && lonOk;
                    })
                    .collect(Collectors.toList());
        }
        // --- 클라이언트 측 위도/경도 필터링 종료 ---

        return new PageDTO<>(
                filteredItems,
                apiResponse.getItemsPerPage(),
                apiResponse.getPage(),
                apiResponse.getTotalItems(),
                apiResponse.getTotalPages()
        );
    }

    @Override
    public Tsunami getDataById(int id) {
        String url = UriComponentsBuilder.fromHttpUrl(noaaBaseUrl + tsunamiDetailEndpoint)
                .buildAndExpand(id)
                .toUriString();

        System.out.println("Requesting Detail URL: " + url);

        try {
            Tsunami tsunami = restTemplate.getForObject(url, Tsunami.class);
            return tsunami;
        } catch (HttpClientErrorException.NotFound nf) {
             System.err.println("Tsunami not found for ID: " + id);
             return null;
        } catch (RestClientException e) {
            System.err.println("Error fetching tsunami details for ID " + id + ": " + e.getMessage());
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

    private LocalDate getItemDate(Tsunami item) {
        if (item == null || item.getYear() == 0 || item.getMonth() == 0 || item.getDay() == 0) {
            return null;
        }
        try {
            return LocalDate.of(item.getYear(), item.getMonth(), item.getDay());
        } catch (Exception e) {
            System.err.println("쓰나미 데이터 날짜 생성 오류 (ID: " + item.getId() + "): " + e.getMessage());
            return null;
        }
    }
} 