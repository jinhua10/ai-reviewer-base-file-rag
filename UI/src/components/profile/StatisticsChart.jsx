import React from 'react';

const StatisticsChart = ({ data }) => {
  // 简化版图表组件，实际项目中可以使用 ECharts 或 Chart.js
  return (
    <div className="statistics-chart">
      <div className="statistics-chart__placeholder">
        📊 {data ? `显示${data.length}条数据的图表` : '暂无数据'}
      </div>
    </div>
  );
};

export default StatisticsChart;

