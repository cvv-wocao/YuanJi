<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>数据看板 · 简约白</title>
    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #ffffff;
            color: #2c3e4e;
        }

        /* 滚动条极简 */
        ::-webkit-scrollbar {
            width: 5px;
            height: 5px;
        }
        ::-webkit-scrollbar-track {
            background: #f0f2f5;
        }
        ::-webkit-scrollbar-thumb {
            background: #b9cdd9;
            border-radius: 4px;
        }

        .dashboard-container {
            max-width: 1440px;
            margin: 0 auto;
            padding: 32px 40px;
        }

        /* 头部 */
        .header {
            margin-bottom: 32px;
        }
        .header h1 {
            font-size: 28px;
            font-weight: 500;
            letter-spacing: -0.2px;
            color: #1f2e3a;
            margin-bottom: 6px;
        }
        .header .subtitle {
            color: #7a8e9e;
            font-size: 14px;
            font-weight: 400;
        }

        /* 状态栏 */
        .status-bar {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 24px;
            margin-bottom: 32px;
            padding-bottom: 16px;
            border-bottom: 1px solid #eef2f6;
        }
        .status-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: #5c6f82;
        }
        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #cbdae3;
        }
        .status-dot.connected {
            background: #6bb5b0;
        }
        .status-dot.disconnected {
            background: #e38b7c;
        }
        .refresh-btn {
            background: #ffffff;
            border: 1px solid #dce5ec;
            color: #5c6f82;
            padding: 5px 16px;
            border-radius: 24px;
            font-size: 12px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
            margin-left: auto;
        }
        .refresh-btn:hover {
            border-color: #6bb5b0;
            color: #6bb5b0;
            background: #fafdfd;
        }

        /* 统计卡片网格 */
        .stats-row {
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            gap: 20px;
            margin-bottom: 28px;
        }
        .stat-card {
            background: #ffffff;
            border-radius: 20px;
            padding: 20px 16px;
            text-align: center;
            border: 1px solid #edf2f6;
            transition: all 0.2s;
        }
        .stat-card:hover {
            border-color: #d4e2ec;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
        }
        .stat-value {
            font-size: 34px;
            font-weight: 500;
            color: #1f2e3a;
            letter-spacing: -0.5px;
            line-height: 1.2;
        }
        .stat-change {
            font-size: 12px;
            margin-top: 6px;
            height: 18px;
        }
        .stat-change.up {
            color: #6bb5b0;
        }
        .stat-change.down {
            color: #e38b7c;
        }
        .stat-label {
            color: #8ba0ae;
            font-size: 12px;
            margin-top: 8px;
            font-weight: 400;
        }
        .stat-card.flash {
            background: #f7fcfc;
            transition: background 0.1s;
        }

        /* 图表卡片 */
        .chart-card {
            background: #ffffff;
            border-radius: 20px;
            padding: 18px 20px;
            border: 1px solid #edf2f6;
        }
        .chart-card h3 {
            font-size: 15px;
            font-weight: 500;
            color: #2c3e4e;
            margin-bottom: 16px;
            padding-bottom: 8px;
            border-bottom: 1px solid #eff3f8;
            letter-spacing: -0.2px;
        }
        .chart-container {
            height: 240px;
            width: 100%;
        }
        .small-chart {
            height: 200px;
        }

        /* 布局 */
        .charts-row {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin-bottom: 28px;
        }
        .charts-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 20px;
            margin-bottom: 28px;
        }
        .full-width {
            grid-column: span 2;
        }

        /* 事件流 */
        .event-feed {
            max-height: 260px;
            overflow-y: auto;
        }
        .event-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: #fafdff;
            padding: 10px 14px;
            border-radius: 16px;
            margin-bottom: 8px;
            font-size: 12px;
            border-left: 3px solid;
            border-left-color: #cbdae3;
        }
        .event-item.user { border-left-color: #6bb5b0; }
        .event-item.post { border-left-color: #c9d97c; }
        .event-item.product { border-left-color: #b0c9ce; }
        .event-item.order { border-left-color: #e8bfae; }
        .event-item.comment { border-left-color: #a1c9c4; }
        .event-badge {
            font-size: 10px;
            font-weight: 500;
            padding: 2px 10px;
            border-radius: 20px;
            background: #f0f4f9;
            color: #5c7a8a;
        }
        .event-time {
            color: #b2c3d0;
            font-size: 10px;
        }
        .order-info {
            display: flex;
            justify-content: space-around;
            text-align: center;
            padding: 16px 0 8px;
        }
        .order-info .value {
            font-size: 28px;
            font-weight: 500;
            color: #1f2e3a;
        }
        .order-info .label {
            color: #8ba0ae;
            font-size: 12px;
            margin-top: 4px;
        }

        @media (max-width: 1200px) {
            .charts-row { grid-template-columns: repeat(2, 1fr); }
            .stats-row { grid-template-columns: repeat(3, 1fr); }
            .charts-grid { grid-template-columns: repeat(2, 1fr); }
            .full-width { grid-column: span 2; }
        }
        @media (max-width: 800px) {
            .dashboard-container { padding: 20px; }
            .stats-row { grid-template-columns: repeat(2, 1fr); }
            .charts-row { grid-template-columns: 1fr; }
            .charts-grid { grid-template-columns: 1fr; }
            .full-width { grid-column: span 1; }
            .refresh-btn { margin-left: 0; margin-top: 8px; }
        }
    </style>
</head>
<body>
<div class="dashboard-container">
    <div class="header">
        <h1>数据看板</h1>
        <p class="subtitle">实时统计 · 消息驱动</p>
    </div>

    <div class="status-bar">
        <div class="status-item">
            <span class="status-dot" id="apiStatus"></span>
            <span>数据状态: <span id="apiText">加载中...</span></span>
        </div>
        <div class="status-item">
            <span class="status-dot" id="wsStatus"></span>
            <span>WebSocket: <span id="wsText">连接中...</span></span>
        </div>
        <div class="status-item">
            <span>最后更新: <span id="lastUpdate">-</span></span>
        </div>
        <button class="refresh-btn" onclick="loadAllData()">刷新数据</button>
    </div>

    <!-- 统计卡片（无emoji） -->
    <div class="stats-row">
        <div class="stat-card" id="userCard">
            <div class="stat-value" id="userCount">0</div>
            <div class="stat-change" id="userChange"></div>
            <div class="stat-label">用户总数</div>
        </div>
        <div class="stat-card" id="postCard">
            <div class="stat-value" id="postCount">0</div>
            <div class="stat-change" id="postChange"></div>
            <div class="stat-label">帖子总数</div>
        </div>
        <div class="stat-card" id="commentCard">
            <div class="stat-value" id="commentCount">0</div>
            <div class="stat-change" id="commentChange"></div>
            <div class="stat-label">评论总数</div>
        </div>
        <div class="stat-card" id="productCard">
            <div class="stat-value" id="productCount">0</div>
            <div class="stat-change" id="productChange"></div>
            <div class="stat-label">商品总数</div>
        </div>
        <div class="stat-card" id="orderCard">
            <div class="stat-value" id="orderCount">0</div>
            <div class="stat-change" id="orderChange"></div>
            <div class="stat-label">订单总数</div>
        </div>
    </div>

    <div class="charts-row">
        <div class="chart-card">
            <h3>数据分布</h3>
            <div class="chart-container" id="pieChart"></div>
        </div>
        <div class="chart-card">
            <h3>数据对比</h3>
            <div class="chart-container" id="barChart"></div>
        </div>
        <div class="chart-card">
            <h3>订单状态</h3>
            <div class="chart-container" id="orderStatusChart"></div>
        </div>
        <div class="chart-card">
            <h3>库存健康</h3>
            <div class="chart-container" id="stockChart"></div>
        </div>
    </div>

    <div class="charts-grid">
        <div class="chart-card">
            <h3>热门商品 TOP 5</h3>
            <div class="chart-container small-chart" id="hotProductsChart"></div>
        </div>
        <div class="chart-card">
            <h3>新品上架 TOP 5</h3>
            <div class="chart-container small-chart" id="newProductsChart"></div>
        </div>
        <div class="chart-card full-width">
            <h3>综合指标</h3>
            <div class="chart-container" id="radarChart"></div>
        </div>
    </div>

    <div class="charts-grid">
        <div class="chart-card">
            <h3>实时事件流</h3>
            <div class="event-feed" id="eventFeed">
                <div style="color: #b2c3d0; text-align: center; padding: 20px;">等待消息...</div>
            </div>
        </div>
        <div class="chart-card full-width">
            <h3>订单总览</h3>
            <div class="order-info">
                <div><div class="value" id="totalOrders">0</div><div class="label">总订单数</div></div>
                <div><div class="value" id="totalAmount">¥0</div><div class="label">总销售额</div></div>
                <div><div class="value" id="orderStatusTotal">0</div><div class="label">活跃订单</div></div>
            </div>
        </div>
    </div>
</div>

<script>
    // 初始化图表
    const pieChart = echarts.init(document.getElementById('pieChart'));
    const barChart = echarts.init(document.getElementById('barChart'));
    const orderStatusChart = echarts.init(document.getElementById('orderStatusChart'));
    const stockChart = echarts.init(document.getElementById('stockChart'));
    const hotProductsChart = echarts.init(document.getElementById('hotProductsChart'));
    const newProductsChart = echarts.init(document.getElementById('newProductsChart'));
    const radarChart = echarts.init(document.getElementById('radarChart'));

    window.addEventListener('resize', () => {
        [pieChart, barChart, orderStatusChart, stockChart, hotProductsChart, newProductsChart, radarChart].forEach(ch => ch.resize());
    });

    let statsData = { userCount: 0, postCount: 0, commentCount: 0, productCount: 0, orderCount: 0, totalOrders: 0, totalAmount: 0 };
    let ws = null;
    const eventFeed = document.getElementById('eventFeed');

    // 淡色主题配色：湖蓝、浅绿、浅红、淡橙、淡紫灰
    const themeColors = ['#6bb5b0', '#b8d9aa', '#e8bfae', '#a1c9c4', '#cbdae3'];

    function updatePieChart() {
        const option = {
            tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
            legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#5c7a8a', fontSize: 11 }, itemWidth: 10, itemHeight: 8 },
            color: themeColors,
            series: [{
                type: 'pie', radius: ['45%', '70%'], avoidLabelOverlap: false,
                itemStyle: { borderRadius: 6, borderColor: '#ffffff', borderWidth: 2 },
                label: { show: true, color: '#5c7a8a', fontSize: 11, formatter: '{b}\n{d}%' },
                data: [
                    { value: statsData.userCount, name: '用户' },
                    { value: statsData.postCount, name: '帖子' },
                    { value: statsData.commentCount, name: '评论' },
                    { value: statsData.productCount, name: '商品' },
                    { value: statsData.orderCount, name: '订单' }
                ]
            }]
        };
        pieChart.setOption(option);
    }

    function updateBarChart() {
        const option = {
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            grid: { left: '8%', right: '5%', bottom: '5%', containLabel: true },
            xAxis: { type: 'category', data: ['用户', '帖子', '评论', '商品', '订单'], axisLabel: { color: '#5c7a8a', fontSize: 11 }, axisLine: { lineStyle: { color: '#e0e8ef' } } },
            yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eff3f8' } }, axisLabel: { color: '#5c7a8a' }, axisLine: { show: false } },
            series: [{ type: 'bar', data: [statsData.userCount, statsData.postCount, statsData.commentCount, statsData.productCount, statsData.orderCount], itemStyle: { borderRadius: [4,4,0,0], color: '#6bb5b0' }, barWidth: '50%' }]
        };
        barChart.setOption(option);
    }

    function updateOrderStatusChart(data) {
        const statusData = data.statusData || [];
        const option = {
            tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
            legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#5c7a8a', fontSize: 10 }, itemWidth: 8 },
            color: ['#6bb5b0', '#b8d9aa', '#e8bfae', '#a1c9c4', '#cbdae3', '#d4c5b0'],
            series: [{ type: 'pie', radius: ['45%', '70%'], data: statusData, label: { show: true, color: '#5c7a8a', fontSize: 10 }, itemStyle: { borderRadius: 6, borderColor: '#ffffff' } }]
        };
        orderStatusChart.setOption(option);
        document.getElementById('orderStatusTotal').textContent = data.total || 0;
    }

    function updateStockChart(data) {
        const stockData = data.stockData || [];
        const option = {
            tooltip: { trigger: 'item' },
            legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#5c7a8a' } },
            color: ['#6bb5b0', '#b8d9aa', '#e8bfae'],
            series: [{ type: 'pie', radius: ['45%', '70%'], data: stockData, label: { show: true, color: '#5c7a8a' }, itemStyle: { borderRadius: 6, borderColor: '#ffffff' } }]
        };
        stockChart.setOption(option);
    }

    function updateHotProductsChart(data) {
        const products = data.products || [];
        const names = products.map(p => p.name.length > 10 ? p.name.substring(0,10)+'…' : p.name);
        const sales = products.map(p => p.sales || 0);
        const option = {
            tooltip: { trigger: 'axis' },
            grid: { left: '12%', right: '3%', containLabel: true },
            xAxis: { type: 'value', splitLine: { lineStyle: { color: '#eff3f8' } }, axisLabel: { color: '#5c7a8a' } },
            yAxis: { type: 'category', data: names.reverse(), axisLabel: { color: '#5c7a8a', fontSize: 11 } },
            series: [{ type: 'bar', data: sales.reverse(), itemStyle: { borderRadius: [0,4,4,0], color: '#6bb5b0' }, barWidth: '50%' }]
        };
        hotProductsChart.setOption(option);
    }

    function updateNewProductsChart(data) {
        const products = data.products || [];
        const names = products.map(p => p.name.length > 10 ? p.name.substring(0,10)+'…' : p.name);
        const stocks = products.map(p => p.stock || 0);
        const option = {
            tooltip: { trigger: 'axis' },
            grid: { left: '12%', right: '3%', containLabel: true },
            xAxis: { type: 'value', splitLine: { lineStyle: { color: '#eff3f8' } }, axisLabel: { color: '#5c7a8a' } },
            yAxis: { type: 'category', data: names.reverse(), axisLabel: { color: '#5c7a8a' } },
            series: [{ type: 'bar', data: stocks.reverse(), itemStyle: { borderRadius: [0,4,4,0], color: '#a1c9c4' }, barWidth: '50%' }]
        };
        newProductsChart.setOption(option);
    }

    function updateRadarChart() {
        const maxVal = Math.max(statsData.userCount, statsData.postCount, statsData.commentCount, statsData.productCount, statsData.orderCount, 100);
        const option = {
            radar: {
                indicator: [
                    { name: '用户', max: maxVal }, { name: '帖子', max: maxVal }, { name: '评论', max: maxVal },
                    { name: '商品', max: maxVal }, { name: '订单', max: maxVal }
                ],
                axisName: { color: '#5c7a8a', fontSize: 11 },
                splitArea: { areaStyle: { color: ['rgba(107,181,176,0.04)', 'rgba(107,181,176,0.02)'] } },
                axisLine: { lineStyle: { color: '#dce5ec' } }
            },
            series: [{
                type: 'radar', data: [{ value: [statsData.userCount, statsData.postCount, statsData.commentCount, statsData.productCount, statsData.orderCount], name: '当前数据' }],
                areaStyle: { color: 'rgba(107,181,176,0.12)' }, lineStyle: { color: '#6bb5b0', width: 2 }, itemStyle: { color: '#6bb5b0' }
            }]
        };
        radarChart.setOption(option);
    }

    function updateAllCharts() {
        updatePieChart();
        updateBarChart();
        updateRadarChart();
    }

    function flashCard(cardId) {
        const card = document.getElementById(cardId);
        card.style.backgroundColor = '#f9fdfd';
        setTimeout(() => card.style.backgroundColor = '', 200);
    }

    function updateStats(data) {
        statsData = { ...statsData, ...data };
        if (data.userCount !== undefined) { document.getElementById('userCount').textContent = data.userCount; statsData.userCount = data.userCount; }
        if (data.postCount !== undefined) { document.getElementById('postCount').textContent = data.postCount; statsData.postCount = data.postCount; }
        if (data.commentCount !== undefined) { document.getElementById('commentCount').textContent = data.commentCount; statsData.commentCount = data.commentCount; }
        if (data.productCount !== undefined) { document.getElementById('productCount').textContent = data.productCount; statsData.productCount = data.productCount; }
        if (data.orderCount !== undefined) { document.getElementById('orderCount').textContent = data.orderCount; statsData.orderCount = data.orderCount; }
        if (data.totalOrders !== undefined) { document.getElementById('totalOrders').textContent = data.totalOrders; statsData.totalOrders = data.totalOrders; }
        if (data.totalAmount !== undefined) { document.getElementById('totalAmount').textContent = '¥' + data.totalAmount; statsData.totalAmount = data.totalAmount; }
        updateAllCharts();
    }

    function addEventItem(eventType, message) {
        if (eventFeed.querySelector('.empty-state')) eventFeed.innerHTML = '';
        const div = document.createElement('div');
        div.className = `event-item ${eventType}`;
        const badge = document.createElement('span');
        badge.className = `event-badge ${eventType}`;
        badge.textContent = eventType.toUpperCase();
        const msgSpan = document.createElement('span');
        msgSpan.style.flex = '1';
        msgSpan.style.margin = '0 12px';
        msgSpan.style.color = '#5c7a8a';
        msgSpan.textContent = message;
        const timeSpan = document.createElement('span');
        timeSpan.className = 'event-time';
        timeSpan.textContent = new Date().toLocaleTimeString();
        div.appendChild(badge);
        div.appendChild(msgSpan);
        div.appendChild(timeSpan);
        eventFeed.insertBefore(div, eventFeed.firstChild);
        while (eventFeed.children.length > 25) eventFeed.removeChild(eventFeed.lastChild);
    }

    async function loadOverviewData() {
        const res = await fetch('/api/statistics/overview').catch(() => null);
        if (res && res.ok) { const data = await res.json(); updateStats(data); }
    }
    async function loadOrdersData() {
        const res = await fetch('/api/statistics/orders').catch(() => null);
        if (res && res.ok) { const data = await res.json(); updateStats(data); }
    }
    async function loadOrderStatusData() {
        const res = await fetch('/api/statistics/orderStatus').catch(() => null);
        if (res && res.ok) { const data = await res.json(); updateOrderStatusChart(data); }
    }
    async function loadStockData() {
        const res = await fetch('/api/statistics/productStock').catch(() => null);
        if (res && res.ok) { const data = await res.json(); updateStockChart(data); }
    }
    async function loadHotProductsData() {
        const res = await fetch('/api/statistics/hotProducts').catch(() => null);
        if (res && res.ok) { const data = await res.json(); updateHotProductsChart(data); }
    }
    async function loadNewProductsData() {
        const res = await fetch('/api/statistics/newProducts').catch(() => null);
        if (res && res.ok) { const data = await res.json(); updateNewProductsChart(data); }
    }

    async function loadAllData() {
        document.getElementById('apiText').textContent = '加载中...';
        document.getElementById('apiStatus').className = 'status-dot';
        try {
            await Promise.all([loadOverviewData(), loadOrdersData(), loadOrderStatusData(), loadStockData(), loadHotProductsData(), loadNewProductsData()]);
            document.getElementById('lastUpdate').textContent = new Date().toLocaleTimeString();
            document.getElementById('apiStatus').className = 'status-dot connected';
            document.getElementById('apiText').textContent = '已连接';
        } catch(e) {
            document.getElementById('apiStatus').className = 'status-dot disconnected';
            document.getElementById('apiText').textContent = '加载失败';
        }
    }

    function connectWebSocket() {
        const wsUrl = 'ws://' + window.location.host + '/ws/dashboard';
        ws = new WebSocket(wsUrl);
        ws.onopen = () => {
            document.getElementById('wsStatus').className = 'status-dot connected';
            document.getElementById('wsText').textContent = '已连接';
        };
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                const type = (data.eventType || '').toUpperCase();
                let changed = false;
                if (type.includes('USER') || type === 'REGISTER') { statsData.userCount++; changed = true; flashCard('userCard'); addEventItem('user', '新用户注册'); }
                else if (type.includes('POST')) { statsData.postCount++; changed = true; flashCard('postCard'); addEventItem('post', '新帖子发布'); }
                else if (type.includes('COMMENT')) { statsData.commentCount++; changed = true; flashCard('commentCard'); addEventItem('comment', '新评论'); }
                else if (type.includes('PRODUCT')) { statsData.productCount++; changed = true; flashCard('productCard'); addEventItem('product', '新商品上架'); }
                else if (type.includes('ORDER')) { statsData.orderCount++; statsData.totalOrders++; changed = true; flashCard('orderCard'); addEventItem('order', '新订单'); }
                if (changed) {
                    document.getElementById('userCount').textContent = statsData.userCount;
                    document.getElementById('postCount').textContent = statsData.postCount;
                    document.getElementById('commentCount').textContent = statsData.commentCount;
                    document.getElementById('productCount').textContent = statsData.productCount;
                    document.getElementById('orderCount').textContent = statsData.orderCount;
                    document.getElementById('totalOrders').textContent = statsData.totalOrders;
                    document.getElementById('lastUpdate').textContent = new Date().toLocaleTimeString();
                    updateAllCharts();
                    setTimeout(() => {
                        const empty = ['userChange','postChange','commentChange','productChange','orderChange'];
                        empty.forEach(id => { const el = document.getElementById(id); if(el) el.textContent = ''; });
                    }, 1500);
                }
            } catch(e) {}
        };
        ws.onclose = () => {
            document.getElementById('wsStatus').className = 'status-dot disconnected';
            document.getElementById('wsText').textContent = '断开,重连...';
            setTimeout(connectWebSocket, 3000);
        };
    }

    loadAllData();
    connectWebSocket();
    setInterval(loadAllData, 60000);
</script>
</body>
</html>
