package frontend.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.Collections;
import java.util.List;
import javafx.geometry.Side;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class FSReliabilityTestController {
    @FXML
    private Label iopsReadMaxLabel;
    @FXML
    private Label iopsReadMinLabel;
    @FXML
    private Label iopsReadAvgLabel;
    @FXML
    private Label iopsWriteMaxLabel;
    @FXML
    private Label iopsWriteMinLabel;
    @FXML
    private Label iopsWriteAvgLabel;
    @FXML
    private Label bdReadMaxLabel;
    @FXML
    private Label bdReadMinLabel;
    @FXML
    private Label bdReadAvgLabel;
    @FXML
    private Label bdWriteMaxLabel;
    @FXML
    private Label bdWriteMinLabel;
    @FXML
    private Label bdWriteAvgLabel;
    @FXML
    private Label latencyReadMaxLabel;
    @FXML
    private Label latencyReadMinLabel;
    @FXML
    private Label latencyReadAvgLabel;
    @FXML
    private Label latencyWriteMaxLabel;
    @FXML
    private Label latencyWriteMinLabel;
    @FXML
    private Label latencyWriteAvgLabel;

    @FXML
    private LineChart<String, Number> iopsLineChart;
    @FXML
    private LineChart<String, Number> bandwidthLineChart;
    @FXML
    private LineChart<String, Number> latencyLineChart;
    @FXML
    public TextArea currentStepTextArea;

    // 假设这是从某处获取的timeData数据
    private List<List<Double>> timeData;

    public void initialize() {
        // 初始化图表（可选：根据实际情况调用）
        setupCharts();
    }

    public void setTimeData(List<List<Double>> timeData) {
        this.timeData = timeData;
        updateCharts();
    }

    private void setupCharts() {
        iopsLineChart.setTitle("IOPS");
        bandwidthLineChart.setTitle("读写带宽");
        latencyLineChart.setTitle("读写延迟");

        // 清空图表数据
        iopsLineChart.getData().clear();
        bandwidthLineChart.getData().clear();
        latencyLineChart.getData().clear();
    }

    private void updateCharts() {
        // 确保图例可见
        iopsLineChart.setLegendVisible(true);
        bandwidthLineChart.setLegendVisible(true);
        latencyLineChart.setLegendVisible(true);

        // 设置图表标题
        iopsLineChart.setTitle("IOPS性能");
        bandwidthLineChart.setTitle("读写带宽性能");
        latencyLineChart.setTitle("读写延迟性能");

        // 设置图例位置到右侧
        iopsLineChart.setLegendSide(Side.RIGHT);
        bandwidthLineChart.setLegendSide(Side.RIGHT);
        latencyLineChart.setLegendSide(Side.RIGHT);

        // 清除旧数据
        iopsLineChart.getData().clear();
        bandwidthLineChart.getData().clear();
        latencyLineChart.getData().clear();

        // 创建并添加IOPS数据系列
        XYChart.Series<String, Number> readIopsSeries = new XYChart.Series<>();
        readIopsSeries.setName("读IOPS");
        XYChart.Series<String, Number> writeIopsSeries = new XYChart.Series<>();
        writeIopsSeries.setName("写OPS");
        for (int i = 0; i < timeData.get(0).size(); i++) {
            readIopsSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(0).get(i)));
            writeIopsSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(1).get(i)));
        }
        iopsLineChart.getData().addAll(readIopsSeries, writeIopsSeries);

        // 创建并添加读写带宽数据系列
        XYChart.Series<String, Number> readBandwidthSeries = new XYChart.Series<>();
        readBandwidthSeries.setName("读带宽");
        XYChart.Series<String, Number> writeBandwidthSeries = new XYChart.Series<>();
        writeBandwidthSeries.setName("写带宽");
        for (int i = 0; i < timeData.get(1).size(); i++) {
            readBandwidthSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(2).get(i)));
            writeBandwidthSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(3).get(i)));
        }
        bandwidthLineChart.getData().addAll(readBandwidthSeries, writeBandwidthSeries);

        // 创建并添加读写延迟数据系列
        XYChart.Series<String, Number> readLatencySeries = new XYChart.Series<>();
        readLatencySeries.setName("读延迟");
        XYChart.Series<String, Number> writeLatencySeries = new XYChart.Series<>();
        writeLatencySeries.setName("写延迟");
        for (int i = 0; i < timeData.get(3).size(); i++) {
            readLatencySeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(4).get(i)));
            writeLatencySeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(5).get(i)));
        }
        latencyLineChart.getData().addAll(readLatencySeries, writeLatencySeries);

        // 调整每个图表中系列的样式
        Util.customizeChartSeriesStyle(iopsLineChart);
        Util.customizeChartSeriesStyle(bandwidthLineChart);
        Util.customizeChartSeriesStyle(latencyLineChart);

        // 更新IOPS统计标签
        updateStatisticsLabels(timeData.get(0), iopsReadMaxLabel, iopsReadMinLabel, iopsReadAvgLabel); // 读IOPS
        updateStatisticsLabels(timeData.get(1), iopsWriteMaxLabel, iopsWriteMinLabel, iopsWriteAvgLabel); // 写IOPS

        // 更新带宽统计标签
        updateStatisticsLabels(timeData.get(2), bdReadMaxLabel, bdReadMinLabel, bdReadAvgLabel); // 读带宽
        updateStatisticsLabels(timeData.get(3), bdWriteMaxLabel, bdWriteMinLabel, bdWriteAvgLabel); // 写带宽

        // 更新延迟统计标签
        updateStatisticsLabels(timeData.get(4), latencyReadMaxLabel, latencyReadMinLabel, latencyReadAvgLabel); // 读延迟
        updateStatisticsLabels(timeData.get(5), latencyWriteMaxLabel, latencyWriteMinLabel, latencyWriteAvgLabel); // 写延迟

        // 请求布局更新，确保数据变化反映到UI上
        iopsLineChart.requestLayout();
        bandwidthLineChart.requestLayout();
        latencyLineChart.requestLayout();
    }

    private void updateStatisticsLabels(List<Double> data, Label maxLabel, Label minLabel, Label avgLabel) {
        if (data.isEmpty()) {
            maxLabel.setText("最大值: N/A");
            minLabel.setText("最小值: N/A");
            avgLabel.setText("平均值: N/A");
            return;
        }

        double max = Collections.max(data);
        double min = Collections.min(data);
        double avg = data.stream().mapToDouble(a -> a).average().orElse(0);

        maxLabel.setText(String.format("最大值: %.2f", max));
        minLabel.setText(String.format("最小值: %.2f", min));
        avgLabel.setText(String.format("平均值: %.2f", avg));
    }

    public void clearAll() {
        iopsLineChart.getData().clear();
        bandwidthLineChart.getData().clear();
        latencyLineChart.getData().clear();

        currentStepTextArea.clear();
    }

}
