/**
 * Chart.js integration for data visualization
 */

class ChartManager {
    constructor() {
        this.charts = {};
        this.initializeCharts();
    }

    initializeCharts() {
        // Revenue Over Time Chart
        const revenueCtx = document.getElementById('revenueChart');
        if (revenueCtx) {
            this.charts.revenue = new Chart(revenueCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Revenue',
                        data: [],
                        borderColor: 'rgb(59, 130, 246)',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        tension: 0.4,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return '$' + value.toFixed(2);
                                }
                            }
                        }
                    }
                }
            });
        }

        // Events by Type Chart
        const eventsCtx = document.getElementById('eventsChart');
        if (eventsCtx) {
            this.charts.events = new Chart(eventsCtx, {
                type: 'doughnut',
                data: {
                    labels: [],
                    datasets: [{
                        data: [],
                        backgroundColor: [
                            'rgba(59, 130, 246, 0.8)',
                            'rgba(16, 185, 129, 0.8)',
                            'rgba(239, 68, 68, 0.8)',
                            'rgba(245, 158, 11, 0.8)',
                            'rgba(139, 92, 246, 0.8)',
                            'rgba(236, 72, 153, 0.8)'
                        ]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        }

        // Categories Chart
        const categoriesCtx = document.getElementById('categoriesChart');
        if (categoriesCtx) {
            this.charts.categories = new Chart(categoriesCtx, {
                type: 'bar',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Sales',
                        data: [],
                        backgroundColor: 'rgba(16, 185, 129, 0.8)'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return '$' + value.toFixed(2);
                                }
                            }
                        }
                    }
                }
            });
        }

        // Events Per Minute Chart
        const eventsPerMinuteCtx = document.getElementById('eventsPerMinuteChart');
        if (eventsPerMinuteCtx) {
            this.charts.eventsPerMinute = new Chart(eventsPerMinuteCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Events',
                        data: [],
                        borderColor: 'rgb(245, 158, 11)',
                        backgroundColor: 'rgba(245, 158, 11, 0.1)',
                        tension: 0.4,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
        }
    }

    updateRevenueChart(revenueData) {
        if (!this.charts.revenue || !revenueData) return;

        const labels = revenueData.map(item => {
            const date = new Date(item.timestamp);
            return date.toLocaleTimeString();
        });
        const data = revenueData.map(item => item.revenue);

        this.charts.revenue.data.labels = labels.slice(-20); // Keep last 20 points
        this.charts.revenue.data.datasets[0].data = data.slice(-20);
        this.charts.revenue.update('none');
    }

    updateEventsChart(eventsByType) {
        if (!this.charts.events || !eventsByType) return;

        const labels = Object.keys(eventsByType);
        const data = Object.values(eventsByType);

        this.charts.events.data.labels = labels;
        this.charts.events.data.datasets[0].data = data;
        this.charts.events.update('none');
    }

    updateCategoriesChart(categorySales) {
        if (!this.charts.categories || !categorySales) return;

        // Sort by sales and take top 5
        const sorted = Object.entries(categorySales)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 5);

        const labels = sorted.map(([category]) => category);
        const data = sorted.map(([, sales]) => sales);

        this.charts.categories.data.labels = labels;
        this.charts.categories.data.datasets[0].data = data;
        this.charts.categories.update('none');
    }

    updateEventsPerMinuteChart(eventsPerMinute) {
        if (!this.charts.eventsPerMinute || !eventsPerMinute) return;

        const labels = eventsPerMinute.map(item => {
            const [date, time] = item.timestamp.split(' ');
            return time;
        });
        const data = eventsPerMinute.map(item => item.count);

        this.charts.eventsPerMinute.data.labels = labels.slice(-20); // Keep last 20 points
        this.charts.eventsPerMinute.data.datasets[0].data = data.slice(-20);
        this.charts.eventsPerMinute.update('none');
    }
}

// Create global chart manager instance
const chartManager = new ChartManager();
