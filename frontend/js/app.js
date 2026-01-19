/**
 * Main application logic
 */

class App {
    constructor() {
        this.streamRunning = false;
        this.eventHistory = [];
        this.maxEventHistory = 100;
        this.initializeEventListeners();
        this.setupWebSocketListeners();
        this.loadInitialStats();
    }

    initializeEventListeners() {
        const startBtn = document.getElementById('startBtn');
        const stopBtn = document.getElementById('stopBtn');

        startBtn.addEventListener('click', () => this.startStream());
        stopBtn.addEventListener('click', () => this.stopStream());
    }

    setupWebSocketListeners() {
        // Listen for statistics updates
        wsClient.on('stats', (stats) => {
            this.updateStatistics(stats);
            this.updateCharts(stats);
        });

        // Listen for new events
        wsClient.on('events', (events) => {
            events.forEach(event => this.addEvent(event));
        });

        // Handle connection events
        wsClient.on('connect', () => {
            console.log('Connected to WebSocket');
        });

        wsClient.on('disconnect', () => {
            console.log('Disconnected from WebSocket');
        });

        wsClient.on('error', (error) => {
            console.error('WebSocket error:', error);
        });

        // Connect WebSocket
        wsClient.connect();
    }

    async loadInitialStats() {
        try {
            const response = await fetch('/api/stats');
            if (response.ok) {
                const stats = await response.json();
                this.updateStatistics(stats);
                this.updateCharts(stats);
            }
        } catch (error) {
            console.error('Error loading initial stats:', error);
        }
    }

    async startStream() {
        try {
            const response = await fetch('/api/stream/start', {
                method: 'POST'
            });

            if (response.ok) {
                const result = await response.json();
                console.log('Stream started:', result);
                this.streamRunning = true;
                this.updateUI();
            } else {
                const error = await response.json();
                alert(`Failed to start stream: ${error.detail || 'Unknown error'}`);
            }
        } catch (error) {
            console.error('Error starting stream:', error);
            alert('Failed to start stream. Make sure Kafka is running.');
        }
    }

    async stopStream() {
        try {
            const response = await fetch('/api/stream/stop', {
                method: 'POST'
            });

            if (response.ok) {
                console.log('Stream stopped');
                this.streamRunning = false;
                this.updateUI();
            } else {
                const error = await response.json();
                alert(`Failed to stop stream: ${error.detail || 'Unknown error'}`);
            }
        } catch (error) {
            console.error('Error stopping stream:', error);
        }
    }

    updateUI() {
        const startBtn = document.getElementById('startBtn');
        const stopBtn = document.getElementById('stopBtn');
        const statusText = document.getElementById('statusText');
        const statusDot = document.getElementById('statusDot');

        if (this.streamRunning) {
            startBtn.disabled = true;
            stopBtn.disabled = false;
            statusText.textContent = 'Running';
            statusDot.className = 'status-dot running';
        } else {
            startBtn.disabled = false;
            stopBtn.disabled = true;
            statusText.textContent = 'Stopped';
            statusDot.className = 'status-dot stopped';
        }
    }

    updateStatistics(stats) {
        // Update statistics cards
        document.getElementById('totalEvents').textContent = stats.total_events || 0;
        document.getElementById('totalRevenue').textContent = 
            `$${(stats.total_revenue || 0).toFixed(2)}`;
        document.getElementById('totalOrders').textContent = stats.total_orders || 0;
        document.getElementById('uniqueUsers').textContent = stats.unique_users || 0;
    }

    updateCharts(stats) {
        // Update revenue chart
        if (stats.revenue_over_time && stats.revenue_over_time.length > 0) {
            chartManager.updateRevenueChart(stats.revenue_over_time);
        }

        // Update events by type chart
        if (stats.events_by_type) {
            chartManager.updateEventsChart(stats.events_by_type);
        }

        // Update categories chart
        if (stats.category_sales) {
            chartManager.updateCategoriesChart(stats.category_sales);
        }

        // Update events per minute chart
        if (stats.events_per_minute && stats.events_per_minute.length > 0) {
            chartManager.updateEventsPerMinuteChart(stats.events_per_minute);
        }
    }

    addEvent(event) {
        // Add to history
        this.eventHistory.unshift(event);
        if (this.eventHistory.length > this.maxEventHistory) {
            this.eventHistory.pop();
        }

        // Render event
        this.renderEvent(event);
    }

    renderEvent(event) {
        const eventList = document.getElementById('eventList');
        const eventItem = document.createElement('div');
        eventItem.className = 'event-item';

        const eventType = event.event_type || 'unknown';
        const timestamp = event.timestamp ? new Date(event.timestamp).toLocaleTimeString() : 'Unknown';
        
        let details = '';
        if (event.product) {
            details += `<span class="event-product">${event.product.product_name}</span>`;
        }
        if (event.total_amount) {
            details += ` - <span class="event-amount">$${event.total_amount.toFixed(2)}</span>`;
        }
        if (event.user) {
            details += ` - ${event.user.name}`;
        }

        eventItem.innerHTML = `
            <div class="event-header">
                <span class="event-type">${eventType.replace('_', ' ')}</span>
                <span class="event-time">${timestamp}</span>
            </div>
            <div class="event-details">${details || 'No details available'}</div>
        `;

        eventList.insertBefore(eventItem, eventList.firstChild);

        // Keep only last 50 events visible
        while (eventList.children.length > 50) {
            eventList.removeChild(eventList.lastChild);
        }
    }

    async checkStreamStatus() {
        try {
            const response = await fetch('/api/stream/status');
            if (response.ok) {
                const status = await response.json();
                this.streamRunning = status.producer_running && status.consumer_running;
                this.updateUI();
            }
        } catch (error) {
            console.error('Error checking stream status:', error);
        }
    }
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    const app = new App();
    
    // Check stream status periodically
    setInterval(() => {
        app.checkStreamStatus();
    }, 5000);
});
