package com.springboot.lab02.service;

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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.springboot.lab02.model.Volcano;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VolcanoService implements DataService<Volcano> {
    private final RestTemplate restTemplate;
    private List<Volcano> cachedVolcanoes = new CopyOnWriteArrayList<>();

    /**
     * 애플리케이션 시작 시 화산 데이터를 미리 로드하여 캐싱합니다.
     */
    @PostConstruct
    public void loadInitialData() {
        log.info("초기 화산 데이터 로딩 시작...");
        List<Volcano> volcanoes = fetchWorldwideVolcanoList();
        if (volcanoes != null && !volcanoes.isEmpty()) {
            cachedVolcanoes.addAll(volcanoes);
            log.info("총 {}개의 화산 데이터를 캐시에 로드했습니다.", cachedVolcanoes.size());
        } else {
            log.warn("API로부터 화산 데이터를 로드하지 못했습니다.");
        }
    }

    /**
     * 캐시된 모든 화산 데이터를 반환합니다.
     * DataService 인터페이스 구현
     * @return 화산 목록
     */
    @Override
    public List<Volcano> getAllData() {
        if (cachedVolcanoes.isEmpty()) {
             log.warn("캐시된 화산 데이터가 없어 다시 로드를 시도합니다.");
             loadInitialData();
        }
        return Collections.unmodifiableList(cachedVolcanoes);
    }

    /**
     * ID로 캐시에서 특정 화산 정보를 찾습니다.
     * DataService 인터페이스 구현
     * @param id 화산 ID
     * @return 화산 데이터 또는 null
     */
    @Override
    public Volcano getDataById(String id) {
        if (id == null) return null;
        return cachedVolcanoes.stream()
                .filter(v -> id.equals(v.getVnum()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 캐시된 화산 데이터에 대해 페이징 처리를 수행합니다.
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 화산 데이터
     */
    public Page<Volcano> getPagedVolcanoes(Pageable pageable) {
        List<Volcano> sortedVolcanoes = sortVolcanoes(cachedVolcanoes, pageable.getSort());

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<Volcano> list;

        if (sortedVolcanoes.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, sortedVolcanoes.size());
            list = sortedVolcanoes.subList(startItem, toIndex);
        }

        return new PageImpl<>(list, pageable, sortedVolcanoes.size());
    }

    // 정렬 로직 분리
    private List<Volcano> sortVolcanoes(List<Volcano> volcanoes, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new ArrayList<>(volcanoes);
        }

        List<Volcano> sortedList = new ArrayList<>(volcanoes);
        List<Sort.Order> orders = sort.stream().collect(Collectors.toList());

        Comparator<Volcano> comparator = Comparator.comparing(v -> v.getVnum() != null ? v.getVnum() : "");

        for (Sort.Order order : orders) {
            Comparator<Volcano> currentComparator;
            switch (order.getProperty()) {
                case "vName":
                    currentComparator = Comparator.comparing(v -> v.getVName() != null ? v.getVName() : "", Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                case "country":
                    currentComparator = Comparator.comparing(v -> v.getCountry() != null ? v.getCountry() : "", Comparator.nullsLast(String::compareToIgnoreCase));
                    break;
                case "elevationM":
                    currentComparator = Comparator.comparing(Volcano::getElevationM, Comparator.nullsLast(Integer::compareTo));
                    break;
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

    // 전 세계 화산 목록 API 호출 (내부 사용 메서드)
    private List<Volcano> fetchWorldwideVolcanoList() {
        try {
            log.info("화산 API 호출 시작 (캐싱용) - Map으로 수동 매핑");
            ResponseEntity<Map[]> mapResponse = restTemplate.getForEntity(
                "https://volcanoes.usgs.gov/vsc/api/volcanoApi/volcanoesGVP", 
                Map[].class
            );
                
            if (mapResponse.getBody() != null) {
                log.info("맵 형식으로 화산 데이터 {} 개 로드 성공 (캐싱용)", mapResponse.getBody().length);
                List<Volcano> volcanoList = new ArrayList<>();
                
                for (Map<String, Object> volcanoMap : mapResponse.getBody()) {
                    try {
                        Volcano volcano = new Volcano();
                        volcano.setVnum(volcanoMap.get("vnum") != null ? volcanoMap.get("vnum").toString() : null);
                        volcano.setVName(volcanoMap.get("vName") != null ? volcanoMap.get("vName").toString() : null);
                        volcano.setCountry(volcanoMap.get("country") != null ? volcanoMap.get("country").toString() : null);
                        volcano.setSubregion(volcanoMap.get("subregion") != null ? volcanoMap.get("subregion").toString() : null);
                        
                        if (volcanoMap.get("latitude") != null) {
                            volcano.setLatitude(parseDouble(volcanoMap.get("latitude")));
                        }
                        if (volcanoMap.get("longitude") != null) {
                            volcano.setLongitude(parseDouble(volcanoMap.get("longitude")));
                        }
                        if (volcanoMap.get("elevation_m") != null) {
                            volcano.setElevationM(parseInteger(volcanoMap.get("elevation_m")));
                        }
                        
                        volcano.setObsAbbr(volcanoMap.get("obsAbbr") != null ? volcanoMap.get("obsAbbr").toString() : null);
                        volcano.setWebpage(volcanoMap.get("webpage") != null ? volcanoMap.get("webpage").toString() : null);
                        
                        if (volcano.getVnum() != null) {
                             volcanoList.add(volcano);
                        }
                    } catch (Exception ex) {
                        log.error("화산 객체 변환 오류: {}, 데이터: {}", ex.getMessage(), volcanoMap);
                    }
                }
                log.info("수동 매핑으로 화산 데이터 {} 개 변환 완료 (캐싱용)", volcanoList.size());
                return volcanoList;
            }
        } catch (Exception e) {
            log.error("화산 데이터 로드 중 오류 발생 (캐싱용 - Map 방식): {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    // 숫자 파싱 헬퍼 메서드 (안정성 강화)
    private Double parseDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj != null) {
            try {
                return Double.parseDouble(obj.toString());
            } catch (NumberFormatException e) {
                log.warn("Double 파싱 오류: {}", obj);
            }
        }
        return null;
    }

    private Integer parseInteger(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj != null) {
            try {
                return Integer.parseInt(obj.toString());
            } catch (NumberFormatException e) {
                log.warn("Integer 파싱 오류: {}", obj);
            }
        }
        return null;
    }
} 