package com.springboot.lab02.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.springboot.lab02.model.Earthquake;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarthquakeService implements DataService<Earthquake> {
    private final RestTemplate restTemplate;
    // 캐시된 지진 데이터
    private List<Earthquake> cachedEarthquakes = new CopyOnWriteArrayList<>();

    /**
     * 애플리케이션 시작 시 지진 데이터를 미리 로드하여 캐싱합니다.
     */
    @PostConstruct
    public void loadInitialData() {
        log.info("초기 지진 데이터 로딩 시작...");
        List<Earthquake> earthquakes = fetchAllEarthquakesFromApi(); // API 호출 메서드명 변경
        if (earthquakes != null && !earthquakes.isEmpty()) {
            cachedEarthquakes.addAll(earthquakes);
            log.info("총 {}개의 지진 데이터를 캐시에 로드했습니다.", cachedEarthquakes.size());
        } else {
            log.warn("API로부터 지진 데이터를 로드하지 못했습니다.");
        }
    }

    /**
     * 캐시된 모든 지진 데이터를 가져옵니다.
     * DataService 인터페이스 구현
     * @return 지진 목록
     */
    @Override
    public List<Earthquake> getAllData() {
        if (cachedEarthquakes.isEmpty()) {
             log.warn("캐시된 지진 데이터가 없어 다시 로드를 시도합니다.");
             loadInitialData();
        }
        return Collections.unmodifiableList(cachedEarthquakes);
    }

    /**
     * ID로 캐시에서 특정 지진 정보를 찾습니다.
     * DataService 인터페이스 구현
     * @param id 지진 ID
     * @return 지진 데이터 또는 null
     */
    @Override
    public Earthquake getDataById(String id) {
        if (id == null) return null;
        return cachedEarthquakes.stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst()
                .orElse(null);
    }

     /**
     * 캐시된 지진 데이터에 대해 페이징 처리를 수행합니다.
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 지진 데이터
     */
    public Page<Earthquake> getPagedEarthquakes(Pageable pageable) {
        List<Earthquake> sortedEarthquakes = sortEarthquakes(cachedEarthquakes, pageable.getSort());

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<Earthquake> list;

        if (sortedEarthquakes.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, sortedEarthquakes.size());
            list = sortedEarthquakes.subList(startItem, toIndex);
        }

        return new PageImpl<>(list, pageable, sortedEarthquakes.size());
    }

    // 정렬 로직 분리
    private List<Earthquake> sortEarthquakes(List<Earthquake> earthquakes, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new ArrayList<>(earthquakes); // 기본 정렬 - API 반환 순서 
        }

        List<Earthquake> sortedList = new ArrayList<>(earthquakes);
        List<Sort.Order> orders = sort.stream().collect(Collectors.toList());

        Comparator<Earthquake> comparator = Comparator.comparing(e -> e.getId() != null ? e.getId() : ""); // 기본 ID 정렬

        for (Sort.Order order : orders) {
            Comparator<Earthquake> currentComparator;
            switch (order.getProperty()) {
                case "time": // 시간 기준 정렬 (Long 타입)
                    currentComparator = Comparator.comparing(Earthquake::getTime, Comparator.nullsLast(Long::compareTo));
                    break;
                case "magnitude": // 규모 기준 정렬 (Double 타입)
                    currentComparator = Comparator.comparing(Earthquake::getMagnitude, Comparator.nullsLast(Double::compareTo));
                    break;
                case "depth": // 깊이 기준 정렬 (Double 타입)
                    currentComparator = Comparator.comparing(Earthquake::getDepth, Comparator.nullsLast(Double::compareTo));
                    break;
                 case "place":
                    currentComparator = Comparator.comparing(e -> e.getPlace() != null ? e.getPlace() : "", Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                // 다른 필드 정렬 추가 가능
                default:
                    currentComparator = comparator;
            }

            if (order.getDirection() == Sort.Direction.DESC) {
                currentComparator = currentComparator.reversed();
            }
            comparator = currentComparator;
        }

        sortedList.sort(comparator);
        return sortedList;
    }


    // 지정 기간 및 최소 규모 조건의 지진 데이터를 GeoJSON 형식으로 가져와 파싱 (내부 사용)
    private List<Earthquake> fetchEarthquakeDataFromApi(String startTime, String endTime, double minMagnitude) {
        String url = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson" +
                     "&starttime=" + startTime +
                     "&endtime=" + endTime +
                     "&minmagnitude=" + minMagnitude;

        try {
            // API 응답 타입 Map<String, Object> 또는 직접 클래스 매핑 고려
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("features")) {
                log.warn("GeoJSON 응답에서 'features' 키를 찾을 수 없습니다.");
                return new ArrayList<>();
            }

            List<?> features = (List<?>) response.get("features");
            List<Earthquake> earthquakes = new ArrayList<>();
            for (Object obj : features) {
                if (!(obj instanceof Map)) continue;
                Map<?, ?> feature = (Map<?, ?>) obj;
                Map<?, ?> properties = feature.get("properties") instanceof Map ? (Map<?, ?>) feature.get("properties") : null;
                Map<?, ?> geometry = feature.get("geometry") instanceof Map ? (Map<?, ?>) feature.get("geometry") : null;

                if (properties == null || geometry == null) continue;
                List<?> coordinates = geometry.get("coordinates") instanceof List ? (List<?>) geometry.get("coordinates") : null;
                if (coordinates == null || coordinates.size() < 3) continue;

                try {
                    Earthquake earthquake = new Earthquake();
                    earthquake.setId(feature.get("id") != null ? feature.get("id").toString() : null);
                    // 숫자 변환 시 타입 체크 및 예외 처리 강화
                    earthquake.setMagnitude(properties.get("mag") instanceof Number ? ((Number) properties.get("mag")).doubleValue() : null);
                    earthquake.setPlace(properties.get("place") != null ? properties.get("place").toString() : null);
                    earthquake.setTime(properties.get("time") instanceof Number ? ((Number) properties.get("time")).longValue() : null);
                    earthquake.setLongitude(coordinates.get(0) instanceof Number ? ((Number) coordinates.get(0)).doubleValue() : null);
                    earthquake.setLatitude(coordinates.get(1) instanceof Number ? ((Number) coordinates.get(1)).doubleValue() : null);
                    earthquake.setDepth(coordinates.get(2) instanceof Number ? ((Number) coordinates.get(2)).doubleValue() : null);

                    // 필수 값이 null이면 리스트에 추가하지 않음 (선택적)
                    if (earthquake.getId() != null && earthquake.getLatitude() != null && earthquake.getLongitude() != null) {
                        earthquakes.add(earthquake);
                    }
                } catch (Exception parseEx) {
                    log.error("지진 데이터 항목 파싱 중 오류: {}, 데이터: {}", parseEx.getMessage(), feature);
                }
            }
            log.info("API로부터 지진 데이터 {} 개 파싱 완료", earthquakes.size());
            return earthquakes;
        } catch (Exception e) {
            log.error("지진 데이터 API 호출/처리 중 오류 발생: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 캐싱을 위해 API에서 모든 지진 데이터를 가져옵니다.
     * 기본적으로 최근 30일 내 규모 4.5 이상의 지진을 검색합니다.
     * @return 지진 목록
     */
    private List<Earthquake> fetchAllEarthquakesFromApi() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(30); // 데이터 기간 조절 가능
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);
        double minMagnitude = 4.5; // 최소 규모 조절 가능

        log.info("지진 API 호출: {} ~ {}, 최소 규모 {}", startTimeStr, endTimeStr, minMagnitude);
        List<Earthquake> earthquakes = fetchEarthquakeDataFromApi(startTimeStr, endTimeStr, minMagnitude);
        log.info("캐싱을 위해 API에서 지진 데이터 {} 개 가져옴", earthquakes.size());
        return earthquakes;
    }
        
} 