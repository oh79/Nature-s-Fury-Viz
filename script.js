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
    }).setView([10, 0], 1.2); // 위도를 10도로 낮추고, 줌 레벨을 1.2로 설정

    // Stadia Maps의 Alidade Smooth Dark 스타일 적용
    L.tileLayer('https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a> &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
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

                        // 유효한 연도인지 확인
                        if (!isNaN(year) && year >= 1950 && year <= 2020) {
                            if (!yearGroups[year]) {
                                yearGroups[year] = [];
                            }
                            yearGroups[year].push({
                                year: year,
                                magnitude: parseFloat(item.magnitude),
                                depth: parseFloat(item.depth),
                                latitude: parseFloat(item.latitude),
                                longitude: parseFloat(item.longitude),
                                location: item.location,
                                country: item.country,
                                tsunami: item.tsunami,
                                alert: item.alert
                            });
                        }
                    });

                    // 각 연도별로 상위 5개 선택
                    filteredData = Object.entries(yearGroups).map(([year, yearData]) => {
                        // 규모순으로 정렬하고 상위 5개 선택
                        return yearData
                            .sort((a, b) => (parseFloat(b.magnitude) || 0) - (parseFloat(a.magnitude) || 0))
                            .slice(0, 5);
                    }).flat(); // 2차원 배열을 1차원으로 평탄화

                    // 오름차순 정렬 (1950 -> 2020)
                    filteredData.sort((a, b) => parseInt(a.year) - parseInt(b.year));

                } else if (dataType === 'volcano') {
                    filteredData = results.data
                        .filter(item => {
                            const year = parseInt(item.Year);
                            return year >= 1950 && year <= 2020 && !isNaN(year);
                        })
                        .map(item => ({
                            year: item.Year,
                            month: item.Month,
                            day: item.Day,
                            name: item.Name,
                            location: item.Location,
                            country: item.Country,
                            latitude: item.Latitude,
                            longitude: item.Longitude,
                            type: item.Type,
                            vei: item.VEI
                        }))
                        .sort((a, b) => parseInt(a.year) - parseInt(b.year));
                } else if (dataType === 'tsunami') {
                    filteredData = results.data
                        .filter(item => {
                            const year = parseInt(item.YEAR);
                            return year >= 1950 && year <= 2020 && !isNaN(year);
                        })
                        .map(item => ({
                            year: item.YEAR,
                            latitude: item.LATITUDE,
                            longitude: item.LONGITUDE,
                            location: item.LOCATION_NAME,
                            country: item.COUNTRY,
                            region: item.REGION,
                            cause: item.CAUSE,
                            magnitude: item.EQ_MAGNITUDE,
                            depth: item.EQ_DEPTH,
                            intensity: item.TS_INTENSITY,
                            damage: item.DAMAGE_TOTAL_DESCRIPTION,
                            deaths: item.DEATHS_TOTAL_DESCRIPTION
                        }))
                        .sort((a, b) => parseInt(a.year) - parseInt(b.year));
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
                // 모든 지진 마커를 초록색으로 통일
                return 'background-color: #4caf50; border-radius: 50%; width: 10px; height: 10px;';
            default:
                return 'background-color: #9c27b0; border-radius: 50%; width: 10px; height: 10px;';
        }
    };

    // 지도에 마커 추가
    data.forEach(item => {
        if (item.latitude && item.longitude) {
            const icon = L.divIcon({
                className: 'custom-div-icon',
                html: `<div style="${getMarkerStyle(type, item)}"></div>`,
                iconSize: [10, 10]
            });

            L.marker([item.latitude, item.longitude], { icon: icon })
                .addTo(map)
                .bindPopup(createPopupContent(item, type));
        }
    });
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

    if (type === 'earthquake') {
        // 연도별 그룹화
        const yearGroups = {};
        data.forEach(item => {
            if (!yearGroups[item.year]) {
                yearGroups[item.year] = [];
            }
            yearGroups[item.year].push(item);
        });

        const tableContainer = document.createElement('div');
        tableContainer.className = 'table-responsive';
        tableContainer.style.height = '500px';

        const table = document.createElement('table');
        table.className = 'table table-striped table-hover';

        // 테이블 헤더
        const thead = document.createElement('thead');
        thead.className = 'sticky-top bg-white';
        const headerRow = document.createElement('tr');
        ['연도', '순위', '규모', '깊이', '위치', '국가', '쓰나미 발생'].forEach(column => {
            const th = document.createElement('th');
            th.textContent = column;
            th.style.backgroundColor = '#f8f9fa';
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        table.appendChild(thead);

        // 테이블 본문
        const tbody = document.createElement('tbody');
        Object.entries(yearGroups).forEach(([year, earthquakes]) => {
            earthquakes.forEach((item, index) => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${item.year}</td>
                    <td>${index + 1}위</td>
                    <td>${item.magnitude || '-'}</td>
                    <td>${item.depth || '-'}</td>
                    <td>${item.location || '-'}</td>
                    <td>${item.country || '-'}</td>
                    <td>${item.tsunami === '1' ? '예' : '아니오'}</td>
                `;
                tbody.appendChild(row);
            });
        });
        table.appendChild(tbody);

        tableContainer.appendChild(table);
        chartContainer.appendChild(tableContainer);
    } else {
        const tableContainer = document.createElement('div');
        tableContainer.className = 'table-responsive';
        tableContainer.style.height = '500px';

        const table = document.createElement('table');
        table.className = 'table table-striped table-hover';

        const thead = document.createElement('thead');
        thead.className = 'sticky-top bg-white';
        const headerRow = document.createElement('tr');

        // 데이터 타입에 따른 컬럼 설정
        let columns;
        if (type === 'volcano') {
            columns = ['연도', '월', '일', '화산명', '위치', '국가', '화산 유형', 'VEI'];
        } else if (type === 'tsunami') {
            columns = ['연도', '위치', '국가', '지역', '원인', '지진규모', '지진깊이', '쓰나미강도', '피해정도', '사망자'];
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
                    <td>${item.year || '-'}</td>
                    <td>${item.month || '-'}</td>
                    <td>${item.day || '-'}</td>
                    <td>${item.name || '-'}</td>
                    <td>${item.location || '-'}</td>
                    <td>${item.country || '-'}</td>
                    <td>${item.type || '-'}</td>
                    <td>${item.vei || '-'}</td>
                `;
            } else if (type === 'tsunami') {
                row.innerHTML = `
                    <td>${item.year || '-'}</td>
                    <td>${item.location || '-'}</td>
                    <td>${item.country || '-'}</td>
                    <td>${item.region || '-'}</td>
                    <td>${item.cause || '-'}</td>
                    <td>${item.magnitude || '-'}</td>
                    <td>${item.depth || '-'}</td>
                    <td>${item.intensity || '-'}</td>
                    <td>${item.damage || '-'}</td>
                    <td>${item.deaths || '-'}</td>
                `;
            }
            tbody.appendChild(row);
        });
        table.appendChild(tbody);

        tableContainer.appendChild(table);
        chartContainer.appendChild(tableContainer);
    }
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