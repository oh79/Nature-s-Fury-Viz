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
    map = L.map('map').setView([0, 0], 2);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
}

// 데이터 로드 함수
async function loadData(dataType) {
    try {
        const response = await fetch(`data/${dataType}_data.csv`);
        const csvData = await response.text();

        Papa.parse(csvData, {
            header: true,
            complete: (results) => {
                const data = filterData(results.data);
                updateVisualization(data, dataType);
            }
        });
    } catch (error) {
        console.error('데이터 로드 실패:', error);
    }
}

// 데이터 필터링 (1950-2020년)
function filterData(data) {
    return data.filter(item => {
        const year = parseInt(item.year);
        return year >= 1950 && year <= 2020;
    });
}

// 시각화 업데이트
function updateVisualization(data, type) {
    clearMap();
    updateChart(data, type);

    // 지도에 마커 추가
    data.forEach(item => {
        if (item.latitude && item.longitude) {
            const marker = L.marker([item.latitude, item.longitude])
                .addTo(map)
                .bindPopup(createPopupContent(item, type));
        }
    });
}

// 팝업 내용 생성
function createPopupContent(item, type) {
    let content = `<h3>${item.name || '미상'}</h3>`;
    content += `<p>발생년도: ${item.year}</p>`;

    if (type === 'volcano') {
        content += `<p>VEI: ${item.vei || '미상'}</p>`;
    }

    return content;
}

// 차트 업데이트
function updateChart(data, type) {
    const ctx = document.getElementById('chart').getContext('2d');

    if (chart) {
        chart.destroy();
    }

    // 연도별 데이터 집계
    const yearCounts = {};
    data.forEach(item => {
        const year = item.year;
        yearCounts[year] = (yearCounts[year] || 0) + 1;
    });

    chart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Object.keys(yearCounts),
            datasets: [{
                label: `${type} 발생 횟수`,
                data: Object.values(yearCounts),
                backgroundColor: 'rgba(54, 162, 235, 0.5)'
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
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
            await loadData(currentTab);
        });
    });
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', init);