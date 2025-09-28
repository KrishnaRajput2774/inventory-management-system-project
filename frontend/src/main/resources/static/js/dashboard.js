// Dashboard JavaScript with enhanced functionality
let chartInstances = {};
let salesTrendChart = null;
let currentSalesPeriod = 'week';

document.addEventListener('DOMContentLoaded', function() {
    initializeDashboard();
});

function initializeDashboard() {
    loadAnalyticsData();
    setupSalesTrendButtons();
    setupModalEventListeners();
}

function setupModalEventListeners() {
    // Profit modal events
    document.getElementById('profitWeekBtn').addEventListener('click', () => loadProfitData('week'));
    document.getElementById('profitMonthBtn').addEventListener('click', () => loadProfitData('month'));
    document.getElementById('profitYearBtn').addEventListener('click', () => loadProfitData('year'));
}

function setupSalesTrendButtons() {
    const weekBtn = document.getElementById('weekBtn');
    const monthBtn = document.getElementById('monthBtn');
    const yearBtn = document.getElementById('yearBtn');

    weekBtn.addEventListener('click', () => switchSalesPeriod('week', weekBtn));
    monthBtn.addEventListener('click', () => switchSalesPeriod('month', monthBtn));
    yearBtn.addEventListener('click', () => switchSalesPeriod('year', yearBtn));
}

function switchSalesPeriod(period, button) {
    // Update button states
    document.querySelectorAll('#weekBtn, #monthBtn, #yearBtn').forEach(btn => {
        btn.className = 'px-3 py-1 text-sm text-gray-600 rounded-lg font-medium hover:bg-gray-100';
    });
    button.className = 'px-3 py-1 text-sm bg-blue-100 text-blue-700 rounded-lg font-medium';

    currentSalesPeriod = period;
    loadAnalyticsData(); // Reload to update sales trend
}

function loadAnalyticsData() {
    showLoadingSpinners();

    fetch('/dashboard/chart-data')
        .then(response => response.json())
        .then(data => {
            hideLoadingSpinners();
            renderCharts(data);
        })
        .catch(error => {
            console.error('Error loading analytics data:', error);
            showErrorMessages();
        });
}

function showLoadingSpinners() {
    const spinners = [
        'topSellingLoading', 'profitableLoading', 'lossLoading', 'stockLoading',
        'salesTrendLoading', 'categoryProfitLoading', 'inventoryTurnoverLoading', 'ordersByChannelLoading'
    ];

    spinners.forEach(id => {
        const element = document.getElementById(id);
        if (element) element.style.display = 'flex';
    });
}

function hideLoadingSpinners() {
    const spinners = [
        'topSellingLoading', 'profitableLoading', 'lossLoading', 'stockLoading',
        'salesTrendLoading', 'categoryProfitLoading', 'inventoryTurnoverLoading', 'ordersByChannelLoading'
    ];

    spinners.forEach(id => {
        const element = document.getElementById(id);
        if (element) element.style.display = 'none';
    });
}

function showErrorMessages() {
    const containers = [
        'topSellingChart', 'profitableChart', 'lossChart', 'stockChart',
        'salesTrendChart', 'categoryProfitChart', 'inventoryTurnoverChart', 'ordersByChannelChart'
    ];

    containers.forEach(id => {
        const canvas = document.getElementById(id);
        if (canvas) {
            const container = canvas.parentElement;
            container.innerHTML = '<div class="error-message">Failed to load chart data</div>';
        }
    });
}

function renderCharts(data) {
    // Destroy existing charts
    Object.values(chartInstances).forEach(chart => {
        if (chart) chart.destroy();
    });
    chartInstances = {};

    // Render all charts
    renderTopSellingChart(data.topSelling);
    renderProfitableChart(data.profitable);
    renderLossChart(data.lossMaking);
    renderStockChart(data.stockLevels);
    renderSalesTrendChart(data.salesTrend);
    renderCategoryProfitChart(data.categoryProfit);
    renderInventoryTurnoverChart(data.inventoryTurnover);
    renderOrdersByChannelChart(data.ordersByChannel);
}

function renderTopSellingChart(data) {
    const ctx = document.getElementById('topSellingChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No sales data available');
        return;
    }

    chartInstances.topSelling = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Units Sold',
                data: data.data,
                backgroundColor: '#3b82f6',
                borderColor: '#1d4ed8',
                borderWidth: 1,
                borderRadius: 6,
                borderSkipped: false,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return `Sold: ${context.parsed.y} units`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f3f4f6' },
                    ticks: { color: '#6b7280' }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#6b7280',
                        maxRotation: 45
                    }
                }
            }
        }
    });
}

function renderProfitableChart(data) {
    const ctx = document.getElementById('profitableChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No profit data available');
        return;
    }

    chartInstances.profitable = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Profit',
                data: data.data,
                backgroundColor: '#10b981',
                borderColor: '#047857',
                borderWidth: 1,
                borderRadius: 6,
                borderSkipped: false,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return `Profit: ₹${context.parsed.y.toLocaleString()}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f3f4f6' },
                    ticks: {
                        color: '#6b7280',
                        callback: function(value) {
                            return '₹' + value.toLocaleString();
                        }
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#6b7280',
                        maxRotation: 45
                    }
                }
            }
        }
    });
}

function renderLossChart(data) {
    const ctx = document.getElementById('lossChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No loss data to display');
        return;
    }

    chartInstances.loss = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Loss',
                data: data.data,
                backgroundColor: '#ef4444',
                borderColor: '#dc2626',
                borderWidth: 1,
                borderRadius: 6,
                borderSkipped: false,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return `Loss: ₹${Math.abs(context.parsed.y).toLocaleString()}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f3f4f6' },
                    ticks: {
                        color: '#6b7280',
                        callback: function(value) {
                            return '₹' + Math.abs(value).toLocaleString();
                        }
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#6b7280',
                        maxRotation: 45
                    }
                }
            }
        }
    });
}

function renderStockChart(data) {
    const ctx = document.getElementById('stockChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No stock data available');
        return;
    }

    chartInstances.stock = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Stock Quantity',
                data: data.data,
                backgroundColor: data.colors || '#6b7280',
                borderWidth: 1,
                borderRadius: 6,
                borderSkipped: false,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return `Stock: ${context.parsed.y} units`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f3f4f6' },
                    ticks: { color: '#6b7280' }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#6b7280',
                        maxRotation: 45
                    }
                }
            }
        }
    });
}

function renderSalesTrendChart(data) {
    const ctx = document.getElementById('salesTrendChart');
    if (!ctx || !data || !data[currentSalesPeriod]) {
        showEmptyState(ctx, 'No sales trend data available');
        return;
    }

    const periodData = data[currentSalesPeriod];

    if (salesTrendChart) {
        salesTrendChart.destroy();
    }

    salesTrendChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: periodData.labels,
            datasets: [{
                label: 'Sales',
                data: periodData.data,
                borderColor: '#3b82f6',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#3b82f6',
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointRadius: 6,
                pointHoverRadius: 8,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return `Sales: ₹${context.parsed.y.toLocaleString()}`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f3f4f6' },
                    ticks: {
                        color: '#6b7280',
                        callback: function(value) {
                            return '₹' + value.toLocaleString();
                        }
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: { color: '#6b7280' }
                }
            }
        }
    });
}

function renderCategoryProfitChart(data) {
    const ctx = document.getElementById('categoryProfitChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No category profit data available');
        return;
    }

    chartInstances.categoryProfit = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: data.labels,
            datasets: [{
                data: data.data,
                backgroundColor: data.colors || [
                    '#3b82f6', '#ec4899', '#10b981', '#f59e0b', '#8b5cf6', '#6b7280'
                ],
                borderWidth: 0,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((context.parsed / total) * 100).toFixed(1);
                            return `${context.label}: ₹${context.parsed.toLocaleString()} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });

    // Update category legend
    updateCategoryLegend(data);
}

function renderInventoryTurnoverChart(data) {
    const ctx = document.getElementById('inventoryTurnoverChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No inventory turnover data available');
        return;
    }

    chartInstances.inventoryTurnover = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Turnover Ratio',
                data: data.data,
                backgroundColor: '#8b5cf6',
                borderColor: '#7c3aed',
                borderWidth: 1,
                borderRadius: 6,
                borderSkipped: false,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return `Turnover: ${context.parsed.y.toFixed(2)}x`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: { color: '#f3f4f6' },
                    ticks: {
                        color: '#6b7280',
                        callback: function(value) {
                            return value.toFixed(1) + 'x';
                        }
                    }
                },
                x: {
                    grid: { display: false },
                    ticks: {
                        color: '#6b7280',
                        maxRotation: 45
                    }
                }
            }
        }
    });
}

function renderOrdersByChannelChart(data) {
    const ctx = document.getElementById('ordersByChannelChart');
    if (!ctx || !data || !data.labels || data.labels.length === 0) {
        showEmptyState(ctx, 'No channel data available');
        return;
    }

    chartInstances.ordersByChannel = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: data.labels,
            datasets: [{
                data: data.data,
                backgroundColor: data.colors || ['#3b82f6', '#10b981', '#f59e0b', '#6b7280'],
                borderWidth: 2,
                borderColor: '#fff',
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 20,
                        usePointStyle: true,
                        color: '#6b7280'
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: '#fff',
                    bodyColor: '#fff',
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((context.parsed / total) * 100).toFixed(1);
                            return `${context.label}: ${context.parsed} orders (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

function updateCategoryLegend(data) {
    const legendContainer = document.getElementById('categoryLegend');
    if (!legendContainer || !data.labels) return;

    let legendHtml = '';
    data.labels.forEach((label, index) => {
        const color = data.colors[index];
        const value = data.data[index];
        const total = data.data.reduce((a, b) => a + b, 0);
        const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;

        legendHtml += `
            <div class="flex items-center justify-between p-1.5 rounded-lg bg-gray-50">
                <div class="flex items-center space-x-3">
                    <div class="w-2 h-2 rounded-full" style="background-color: ${color}"></div>
                    <span class="text-xs font-medium text-gray-700">${label}</span>
                </div>
                <div class="text-right">
                    <div class="text-xs font-semibold text-gray-900">₹${value.toLocaleString()}</div>
                    <div class="text-[10px] text-gray-500">${percentage}%</div>
                </div>
            </div>
        `;
    });

    legendContainer.innerHTML = legendHtml;
}

function showEmptyState(canvas, message) {
    if (!canvas) return;

    const container = canvas.parentElement;
    container.innerHTML = `
        <div class="empty-state">
            <div class="empty-state-icon">📊</div>
            <p>${message}</p>
        </div>
    `;
}

// Modal Functions
function openProfitModal() {
    document.getElementById('profitModal').style.display = 'block';
    loadProfitData('month'); // Default to month
}

function closeProfitModal() {
    document.getElementById('profitModal').style.display = 'none';
}

function openRevenueModal() {
    document.getElementById('revenueModal').style.display = 'block';
    loadRevenueData();
}

function closeRevenueModal() {
    document.getElementById('revenueModal').style.display = 'none';
}

function loadProfitData(period) {
    // Update button states
    document.querySelectorAll('#profitWeekBtn, #profitMonthBtn, #profitYearBtn').forEach(btn => {
        btn.className = 'px-2 py-1 text-xs text-gray-600 rounded-lg font-medium hover:bg-gray-100';
    });

    const buttonId = period === 'week' ? 'profitWeekBtn' :
                     period === 'year' ? 'profitYearBtn' : 'profitMonthBtn';
    document.getElementById(buttonId).className = 'px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded-lg font-medium';

    fetch(`/dashboard/profit-data?period=${period}`)
        .then(response => response.json())
        .then(data => {
            updateProfitModal(data, period);
        })
        .catch(error => {
            console.error('Error loading profit data:', error);
        });
}

function updateProfitModal(data, period) {
    // Update period label
    const labels = {
        'week': 'This Week',
        'month': 'This Month',
        'year': 'This Year'
    };

    document.getElementById('currentPeriodProfit').textContent = `₹${data.totalProfit?.toLocaleString() || 0}`;
    document.getElementById('currentPeriodProfit').nextElementSibling.textContent = labels[period];

    // Update top products
    const topProductsContainer = document.getElementById('topProfitProducts');
    if (!data.topProducts || data.topProducts.length === 0) {
        topProductsContainer.innerHTML = '<div class="text-gray-500 text-center py-1">No profit data available for this period</div>';
        return;
    }

    let productsHtml = '';
    data.topProducts.forEach((product, index) => {
        productsHtml += `
            <div class="flex items-center justify-between p-1.5 bg-gray-50 rounded-lg">
                <div class="flex items-center space-x-2">
                    <div class="w-4 h-4 bg-green-100 text-green-600 rounded-full flex items-center justify-center text-[10px] font-semibold">
                        ${index + 1}
                    </div>
                    <span class="font-medium text-xs text-gray-900">${product.name}</span>
                </div>
                <div class="text-right">
                    <div class="font-semibold text-xs text-green-600">₹${product.profit.toLocaleString()}</div>
                    <div class="text-[10px] text-gray-500">${product.percentage.toFixed(1)}% of total</div>
                </div>
            </div>
        `;
    });

    topProductsContainer.innerHTML = productsHtml;
}

function loadRevenueData() {
    fetch('/dashboard/revenue-data')
        .then(response => response.json())
        .then(data => {
            updateRevenueModal(data);
        })
        .catch(error => {
            console.error('Error loading revenue data:', error);
        });
}

function updateRevenueModal(data) {
    // Update revenue metrics
    const revenueMetrics = document.getElementById('revenueMetrics');
    revenueMetrics.innerHTML = `
        <div class="text-center p-2 bg-blue-50 rounded-lg">
            <div class="text-lg font-bold text-blue-600">₹${data.weekly?.toLocaleString() || 0}</div>
            <div class="text-xs text-gray-600">Weekly Revenue</div>
        </div>
        <div class="text-center p-2 bg-green-50 rounded-lg">
            <div class="text-lg font-bold text-green-600">₹${data.monthly?.toLocaleString() || 0}</div>
            <div class="text-xs text-gray-600">Monthly Revenue</div>
        </div>
        <div class="text-center p-2 bg-purple-50 rounded-lg">
            <div class="text-lg font-bold text-purple-600">₹${data.yearly?.toLocaleString() || 0}</div>
            <div class="text-xs text-gray-600">Yearly Revenue</div>
        </div>
    `;

    // Update order metrics
    const orderMetrics = document.getElementById('orderMetrics');
    orderMetrics.innerHTML = `
        <div class="text-center p-2 bg-orange-50 rounded-lg">
            <div class="text-lg font-bold text-orange-600">₹${data.averageOrderValue?.toLocaleString() || 0}</div>
            <div class="text-xs text-gray-600">Avg Order Value</div>
        </div>
        <div class="text-center p-2 bg-pink-50 rounded-lg">
            <div class="text-lg font-bold text-pink-600">${data.totalOrders || 0}</div>
            <div class="text-xs text-gray-600">Total Orders</div>
        </div>
        <div class="text-center p-2 bg-indigo-50 rounded-lg">
            <div class="text-lg font-bold text-indigo-600">${data.contributingCustomers || 0}</div>
            <div class="text-xs text-gray-600">Active Customers</div>
        </div>
    `;
}

// Close modals when clicking outside
window.onclick = function(event) {
    const profitModal = document.getElementById('profitModal');
    const revenueModal = document.getElementById('revenueModal');

    if (event.target === profitModal) {
        closeProfitModal();
    }
    if (event.target === revenueModal) {
        closeRevenueModal();
    }
}

// Refresh data every 5 minutes
setInterval(function() {
    loadAnalyticsData();

    // Also refresh stock overview if the function exists
    if (typeof loadStockOverview === 'function') {
        loadStockOverview();
    }
}, 300000); // 5 minutes

// Export functions for global access
window.openProfitModal = openProfitModal;
window.closeProfitModal = closeProfitModal;
window.openRevenueModal = openRevenueModal;
window.closeRevenueModal = closeRevenueModal;