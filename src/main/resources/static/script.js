// 전역 변수 선언
let map;
let chart;
let currentTab = 'volcano';

// 초기화 함수
async function init() {
    // 지도 초기화
    initMap();

    // 데이터 로드 및 시각화
    await loadData(currentTab);

    // 탭 이벤트 리스너 설정
    setupTabListeners();
}

// 지도 초기화
function initMap() {
    map = L.map('map', {
        minZoom: 1.2,
        maxZoom: 18,
        maxBounds: [
            [-90, -180], // 남서쪽 경계
            [90, 180] // 북동쪽 경계
        ],
        maxBoundsViscosity: 1.0 // 경계에서 튕기는 강도 (0~1)
    }).setView([10, 0], 2.5); // 위도를 10도로 낮추고, 줌 레벨을 2.5로 설정

    // Stadia Maps의 Alidade Smooth Dark 스타일 적용
    L.tileLayer('https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}{r}.png', {
        noWrap: true, // 지도가 반복되지 않도록 설정
        bounds: [
            [-90, -180],
            [90, 180]
        ]
    }).addTo(map);

    // 지도 컨트롤 추가
    L.control.scale().addTo(map);

    // 지각판 경계 추가
    fetch('https://raw.githubusercontent.com/fraxen/tectonicplates/master/GeoJSON/PB2002_plates.json')
        .then(response => response.json())
        .then(data => {
            L.geoJSON(data, {
                style: {
                    color: '#FF4081',
                    weight: 1.5, // 선 두께를 약간 줄임
                    opacity: 0.8, // 선 투명도 조정
                    fillOpacity: 0, // 내부 채우기를 완전 투명하게
                    dashArray: '5, 5'
                },
                onEachFeature: (feature, layer) => {
                    if (feature.properties && feature.properties.PlateName) {
                        layer.bindTooltip(feature.properties.PlateName, {
                            permanent: true,
                            direction: 'center',
                            className: 'plate-label'
                        });
                    }
                }
            }).addTo(map);
        })
        .catch(error => console.error('지각판 데이터 로드 실패:', error));
}

// 데이터 로드 함수
async function loadData(dataType) {
    try {
        const response = await fetch(`data/${dataType}_data.csv`);
        const csvData = await response.text();

        Papa.parse(csvData, {
            header: true,
            dynamicTyping: true, // 자동으로 타입 변환 시도
            skipEmptyLines: true, // 빈 줄 건너뛰기
            complete: (results) => {
                let filteredData;
                if (dataType === 'earthquake') {
                    // 지진 데이터 최적화
                    const yearGroups = {};

                    // 연도별로 데이터 그룹화
                    results.data.forEach(item => {
                        let year;
                        try {
                            if (item.date_time) {
                                year = parseInt(item.date_time.split('-')[2].split(' ')[0]);
                            } else if (item.time) {
                                year = new Date(item.time).getFullYear();
                            } else if (item.date) {
                                year = new Date(item.date).getFullYear();
                            } else {
                                year = parseInt(item.year);
                            }
                        } catch (error) {
                            console.warn('날짜 파싱 실패:', item);
                            return;
                        }

                        // 유효한 연도인지 확인 (범위 확장: 1950년 ~ 2023년)
                        if (!isNaN(year) && year >= 1950 && year <= 2023) {
                            if (!yearGroups[year]) {
                                yearGroups[year] = [];
                            }
                            
                            // 위도/경도 값을 명시적으로 숫자로 변환하고 유효성 검사
                            const lat = parseFloat(item.latitude);
                            const lng = parseFloat(item.longitude);
                            
                            yearGroups[year].push({
                                year: year,
                                magnitude: parseFloat(item.magnitude),
                                depth: parseFloat(item.depth),
                                latitude: !isNaN(lat) ? lat : null,
                                longitude: !isNaN(lng) ? lng : null,
                                location: item.location,
                                country: item.country,
                                tsunami: item.tsunami,
                                alert: item.alert
                            });
                        }
                    });

                    // 각 연도의 모든 데이터 포함
                    filteredData = Object.entries(yearGroups).map(([year, yearData]) => {
                        // 규모순으로 정렬하고 모든 데이터 반환
                        return yearData
                            .sort((a, b) => (parseFloat(b.magnitude) || 0) - (parseFloat(a.magnitude) || 0));
                    }).flat(); // 2차원 배열을 1차원으로 평탄화

                    // 지진 데이터는 1995년부터만 필터링
                    filteredData = filteredData.filter(item => parseInt(item.year) >= 1995);

                    // 오름차순 정렬 (1995 -> 2023)
                    filteredData.sort((a, b) => parseInt(a.year) - parseInt(b.year));
                    
                    // 위치 데이터 분석 로깅
                    const totalItems = filteredData.length;
                    const itemsWithLocation = filteredData.filter(item => item.latitude !== null && item.longitude !== null).length;
                    console.log(`지진 데이터: 총 ${totalItems}개 중 ${itemsWithLocation}개에 위치 정보가 있습니다 (${Math.round(itemsWithLocation/totalItems*100)}%)`);

                } else if (dataType === 'volcano') {
                    filteredData = results.data
                        .filter(item => {
                            const year = parseInt(item.Year);
                            return year >= 1950 && year <= 2023 && !isNaN(year); // 범위 확장
                        })
                        .map(item => {
                            // 위도/경도 값을 명시적으로 숫자로 변환하고 유효성 검사
                            const lat = parseFloat(item.Latitude);
                            const lng = parseFloat(item.Longitude);
                            
                            return {
                                year: item.Year,
                                month: item.Month,
                                day: item.Day,
                                name: item.Name,
                                location: item.Location,
                                country: item.Country,
                                latitude: !isNaN(lat) ? lat : null,
                                longitude: !isNaN(lng) ? lng : null,
                                type: item.Type,
                                vei: item.VEI
                            };
                        })
                        .sort((a, b) => parseInt(a.year) - parseInt(b.year));
                        
                    // 위치 데이터 분석 로깅
                    const totalItems = filteredData.length;
                    const itemsWithLocation = filteredData.filter(item => item.latitude !== null && item.longitude !== null).length;
                    console.log(`화산 데이터: 총 ${totalItems}개 중 ${itemsWithLocation}개에 위치 정보가 있습니다 (${Math.round(itemsWithLocation/totalItems*100)}%)`);
                } else if (dataType === 'tsunami') {
                    filteredData = results.data
                        .filter(item => {
                            const year = parseInt(item.YEAR);
                            return year >= 1950 && year <= 2023 && !isNaN(year); // 범위 확장
                        })
                        .map(item => {
                            // 위도/경도 값을 명시적으로 숫자로 변환하고 유효성 검사
                            const lat = parseFloat(item.LATITUDE);
                            const lng = parseFloat(item.LONGITUDE);
                            
                            return {
                                year: item.YEAR,
                                latitude: !isNaN(lat) ? lat : null,
                                longitude: !isNaN(lng) ? lng : null,
                                location: item.LOCATION_NAME,
                                country: item.COUNTRY,
                                region: item.REGION,
                                cause: item.CAUSE,
                                magnitude: item.EQ_MAGNITUDE,
                                depth: item.EQ_DEPTH,
                                intensity: item.TS_INTENSITY,
                                damage: item.DAMAGE_TOTAL_DESCRIPTION,
                                deaths: item.DEATHS_TOTAL_DESCRIPTION
                            };
                        })
                        .sort((a, b) => parseInt(a.year) - parseInt(b.year));
                        
                    // 위치 데이터 분석 로깅
                    const totalItems = filteredData.length;
                    const itemsWithLocation = filteredData.filter(item => item.latitude !== null && item.longitude !== null).length;
                    console.log(`쓰나미 데이터: 총 ${totalItems}개 중 ${itemsWithLocation}개에 위치 정보가 있습니다 (${Math.round(itemsWithLocation/totalItems*100)}%)`);
                }

                updateVisualization(filteredData, dataType);
            }
        });
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    }
}

// 시각화 업데이트
function updateVisualization(data, type) {
    clearMap();
    updateChart(data, type);

    // 마커 스타일 정의
    const getMarkerStyle = (type, item) => {
        switch (type) {
            case 'volcano':
                return 'background-color: #ff4444; border-radius: 50%; width: 10px; height: 10px;';
            case 'tsunami':
                return 'background-color: #2196f3; border-radius: 50%; width: 10px; height: 10px;';
            case 'earthquake':
                return 'background-color: #4caf50; border-radius: 50%; width: 10px; height: 10px;';
        }
    };

    // 지도에 마커 추가
    let markersAdded = 0;
    data.forEach(item => {
        // 위도/경도가 유효한 숫자인지 확인
        if (item.latitude !== null && item.longitude !== null) {
            const icon = L.divIcon({
                className: 'custom-div-icon',
                html: `<div style="${getMarkerStyle(type, item)}"></div>`,
                iconSize: [10, 10]
            });

            try {
                L.marker([item.latitude, item.longitude], { icon: icon })
                    .addTo(map)
                    .bindPopup(createPopupContent(item, type));
                markersAdded++;
            } catch (error) {
                console.warn(`마커 추가 실패: ${error.message}`, item);
            }
        }
    });
    
    console.log(`${type} 데이터: 총 ${data.length}개 중 ${markersAdded}개 마커가 지도에 추가되었습니다.`);
}

// 팝업 내용 생성
function createPopupContent(item, type) {
    let content = '';
    if (type === 'earthquake') {
        content = `
            <div style="min-width: 200px;">
                <h3>지진 정보 (${item.year}년)</h3>
                <p>규모: ${item.magnitude || '미상'}</p>
                <p>깊이: ${item.depth || '미상'} km</p>
                <p>위치: ${item.location || '미상'}</p>
                <p>국가: ${item.country || '미상'}</p>
                <p>쓰나미 발생: ${item.tsunami === '1' ? '예' : '아니오'}</p>
                ${item.alert ? `<p>경보 수준: ${item.alert}</p>` : ''}
            </div>
        `;
    } else if (type === 'volcano') {
        content = `
            <h3>${item.name || '미상'}</h3>
            <p>발생년도: ${item.year}</p>
            <p>VEI: ${item.vei || '미상'}</p>
        `;
    } else if (type === 'tsunami') {
        content = `
            <h3>${item.location || '미상'}</h3>
            <p>발생년도: ${item.year}</p>
            <p>원인: ${item.cause || '미상'}</p>
            <p>쓰나미 강도: ${item.intensity || '미상'}</p>
            <p>지진 규모: ${item.magnitude || '미상'}</p>
        `;
    }
    return content;
}

// 차트 업데이트
function updateChart(data, type) {
    const chartContainer = document.getElementById('chartContainer');
    chartContainer.innerHTML = '';

    const tableContainer = document.createElement('div');
    tableContainer.className = 'table-responsive';

    const table = document.createElement('table');
    table.className = 'table table-striped table-hover';

    const thead = document.createElement('thead');
    thead.className = 'sticky-top bg-white';
    const headerRow = document.createElement('tr');

    if (type === 'earthquake') {
        ['연도', '규모', '위치', '국가', '깊이', '쓰나미 발생'].forEach(column => {
            const th = document.createElement('th');
            th.textContent = column;
            th.style.backgroundColor = '#f8f9fa';
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        data.forEach((item) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td style="color: ${getYearColor(item.year)}">${item.year}</td>
                <td style="color: ${getMagnitudeColor(item.magnitude)}">${item.magnitude || '-'}</td>
                <td>${item.location || '-'}</td>
                <td>${item.country || '-'}</td>
                <td>${item.depth || '-'} km</td>
                <td>${item.tsunami === '1' ? '예' : '아니오'}</td>
            `;
            tbody.appendChild(row);
        });
        table.appendChild(tbody);
    } else {
        let columns;
        if (type === 'volcano') {
            columns = ['연도', 'VEI', '화산명', '위치', '국가', '화산 유형'];
        } else if (type === 'tsunami') {
            columns = ['연도', '원인', '지역', '국가', '피해정도', '사망자'];
        }

        columns.forEach(column => {
            const th = document.createElement('th');
            th.textContent = column;
            th.style.backgroundColor = '#f8f9fa';
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        data.forEach(item => {
            const row = document.createElement('tr');
            if (type === 'volcano') {
                row.innerHTML = `
                    <td style="color: ${getYearColor(item.year)}">${item.year || '-'}</td>
                    <td style="color: ${getVEIColor(item.vei)}">${item.vei || '-'}</td>
                    <td>${item.name || '-'}</td>
                    <td>${item.location || '-'}</td>
                    <td>${item.country || '-'}</td>
                    <td>${item.type || '-'}</td>
                `;
            } else if (type === 'tsunami') {
                row.innerHTML = `
                    <td style="color: ${getYearColor(item.year)}">${item.year || '-'}</td>
                    <td style="color: ${getTsunamiCauseColor(item.cause)}">${item.cause || '-'}</td>
                    <td>${item.region || '-'}</td>
                    <td>${item.country || '-'}</td>
                    <td>${item.damage || '-'}</td>
                    <td>${item.deaths || '-'}</td>
                `;
            }
            tbody.appendChild(row);
        });
        table.appendChild(tbody);
    }

    tableContainer.appendChild(table);
    chartContainer.appendChild(tableContainer);
    chartContainer.appendChild(createLegend());
}

// 지도 초기화
function clearMap() {
    map.eachLayer((layer) => {
        if (layer instanceof L.Marker) {
            map.removeLayer(layer);
        }
    });
}

// 탭 이벤트 설정
function setupTabListeners() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', async(e) => {
            // 활성 탭 변경
            document.querySelector('.tab-btn.active').classList.remove('active');
            e.target.classList.add('active');

            // 데이터 로드
            currentTab = e.target.dataset.tab;

            // 제목 업데이트
            const title = document.querySelector('h1.text-center');
            // 첫 글자를 대문자로 변환
            title.textContent = currentTab.charAt(0).toUpperCase() + currentTab.slice(1);

            await loadData(currentTab);
        });
    });
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', init);

// 연도별 색상 함수
function getYearColor(year) {
    const year_num = parseInt(year);
    
    if (currentTab === 'earthquake') {
        // 지진 데이터는 1995-2023 범위에 맞게 조정
        if (year_num < 2000) return '#2196F3';     // 파랑 (1995-1999)
        else if (year_num < 2010) return '#4CAF50'; // 초록 (2000-2009)
        else if (year_num < 2020) return '#FFC107'; // 노랑 (2010-2019)
        else return '#F44336';                      // 빨강 (2020-2023)
    } else {
        // 화산, 쓰나미 데이터는 기존 범위 유지
        if (year_num < 1970) return '#2196F3';      // 파랑 (1950-1969)
        else if (year_num < 1990) return '#4CAF50'; // 초록 (1970-1989)
        else if (year_num < 2010) return '#FFC107'; // 노랑 (1990-2009)
        else return '#F44336';                      // 빨강 (2010-2023)
    }
}

// VEI 색상 함수
function getVEIColor(vei) {
    const vei_num = parseInt(vei);
    if (isNaN(vei_num)) return '#808080';  // 회색 (데이터 없음)
    if (vei_num <= 2) return '#FFD700';    // 금색 (약함)
    if (vei_num <= 4) return '#FF4500';    // 주황색 (중간)
    return '#8B0000';                      // 진한 빨강 (강함)
}

// 쓰나미 원인별 색상
function getTsunamiCauseColor(cause) {
    if (!cause) return '#808080';  // 회색 (데이터 없음)
    if (cause.includes('Earthquake')) return '#E91E63';  // 분홍 (지진)
    if (cause.includes('Volcano')) return '#9C27B0';     // 보라 (화산)
    if (cause.includes('Landslide')) return '#795548';   // 갈색 (산사태)
    return '#607D8B';  // 회색빛 파랑 (기타)
}

// 지진 규모별 색상
function getMagnitudeColor(magnitude) {
    const mag = parseFloat(magnitude);
    if (isNaN(mag)) return '#808080';      // 회색 (데이터 없음)
    if (mag < 6.0) return '#9370DB';       // 보라색 (약한 지진)
    if (mag < 7.0) return '#4B0082';       // 진한 보라 (중간 지진)
    if (mag < 8.0) return '#00CED1';       // 청록색 (강한 지진)
    return '#000080';                      // 진한 남색 (매우 강한 지진)
}

// 범례 생성 함수
function createLegend() {
    const legend = document.createElement('div');
    legend.className = 'legend collapsed';  // 초기 상태를 collapsed로 설정
    
    // 헤더 추가
    const header = document.createElement('div');
    header.className = 'legend-header';
    
    const title = document.createElement('h5');
    title.textContent = '범례';
    
    const toggleBtn = document.createElement('button');
    toggleBtn.className = 'legend-toggle';
    toggleBtn.innerHTML = '▶';  // 닫힌 상태의 아이콘으로 변경
    toggleBtn.addEventListener('click', (e) => {
        legend.classList.toggle('collapsed');
    });
    
    header.appendChild(title);
    header.appendChild(toggleBtn);
    legend.appendChild(header);
    
    // 연도별 범례
    const yearLegend = document.createElement('div');
    yearLegend.style.marginBottom = '30px';
    
    const yearTitle = document.createElement('h5');
    yearTitle.textContent = '연도별 구분';
    yearTitle.style.marginBottom = '15px';
    yearLegend.appendChild(yearTitle);

    // 현재 탭에 따라 다른 연도 범위 표시
    let yearItems;
    if (currentTab === 'earthquake') {
        yearItems = [
            { color: '#2196F3', label: '1995-1999' },
            { color: '#4CAF50', label: '2000-2009' },
            { color: '#FFC107', label: '2010-2019' },
            { color: '#F44336', label: '2020-2023' }
        ];
    } else {
        yearItems = [
            { color: '#2196F3', label: '1950-1969' },
            { color: '#4CAF50', label: '1970-1989' },
            { color: '#FFC107', label: '1990-2009' },
            { color: '#F44336', label: '2010-2023' }
        ];
    }

    yearItems.forEach(item => {
        const legendItem = document.createElement('div');
        
        const colorBox = document.createElement('div');
        colorBox.className = 'color-box';
        colorBox.style.backgroundColor = item.color;
        
        const label = document.createElement('span');
        label.textContent = item.label;
        
        legendItem.appendChild(colorBox);
        legendItem.appendChild(label);
        yearLegend.appendChild(legendItem);
    });

    legend.appendChild(yearLegend);

    // 카테고리별 범례
    const categoryLegend = document.createElement('div');
    let categoryItems = [];
    let categoryTitle = '';

    if (currentTab === 'volcano') {
        categoryTitle = 'VEI 구분';
        categoryItems = [
            { color: '#FFD700', label: 'VEI 0-2 (약함)' },
            { color: '#FF4500', label: 'VEI 3-4 (중간)' },
            { color: '#8B0000', label: 'VEI 5+ (강함)' },
            { color: '#808080', label: '데이터 없음' }
        ];
    } else if (currentTab === 'tsunami') {
        categoryTitle = '원인별 구분';
        categoryItems = [
            { color: '#E91E63', label: '지진' },
            { color: '#9C27B0', label: '화산' },
            { color: '#795548', label: '산사태' },
            { color: '#607D8B', label: '기타' },
            { color: '#808080', label: '데이터 없음' }
        ];
    } else if (currentTab === 'earthquake') {
        categoryTitle = '규모별 구분';
        categoryItems = [
            { color: '#9370DB', label: '6.0 미만' },
            { color: '#4B0082', label: '6.0-6.9' },
            { color: '#00CED1', label: '7.0-7.9' },
            { color: '#000080', label: '8.0 이상' },
            { color: '#808080', label: '데이터 없음' }
        ];
    }

    const catTitle = document.createElement('h5');
    catTitle.textContent = categoryTitle;
    catTitle.style.marginBottom = '10px';
    categoryLegend.appendChild(catTitle);

    categoryItems.forEach(item => {
        const legendItem = document.createElement('div');
        
        const colorBox = document.createElement('div');
        colorBox.className = 'color-box';
        colorBox.style.backgroundColor = item.color;
        
        const label = document.createElement('span');
        label.textContent = item.label;
        
        legendItem.appendChild(colorBox);
        legendItem.appendChild(label);
        categoryLegend.appendChild(legendItem);
    });

    legend.appendChild(categoryLegend);

    // 데이터 수집 기간 표시 추가
    const dataRangeInfo = document.createElement('div');
    dataRangeInfo.className = 'data-range-info';
    dataRangeInfo.style.marginTop = '20px';
    dataRangeInfo.style.borderTop = '1px solid rgba(0,0,0,0.1)';
    dataRangeInfo.style.paddingTop = '10px';
    dataRangeInfo.style.fontSize = '12px';
    dataRangeInfo.style.color = '#666';
    dataRangeInfo.style.textAlign = 'center';
    
    let dataRangeText = '';
    if (currentTab === 'earthquake') {
        dataRangeText = '데이터 수집 기간: 1995-2023';
    } else if(currentTab === 'volcano'){
        dataRangeText = '데이터 수집 기간: 1950-2023';
    } else if(currentTab === 'tsunami'){
        dataRangeText = '데이터 수집 기간: 1950-2020';
    }
    
    dataRangeInfo.textContent = dataRangeText;
    legend.appendChild(dataRangeInfo);

    return legend;
}