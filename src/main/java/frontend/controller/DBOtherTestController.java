package frontend.controller;

import backend.dataset.DisplayResult;
import backend.dataset.TestResult;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class DBOtherTestController {
    public DBOtherTestController() { }

    @FXML
    private LineChart<String, Number> cpuUsageLineChart;
    @FXML
    private LineChart<String, Number> memoryUsageLineChart;
    @FXML
    private LineChart<String, Number> diskSpeedLineChart;
    @FXML
    public TextArea currentStepTextArea;

    @FXML
    private TableView<DisplayResult> resultsTableView;
    @FXML
    private TableColumn<DisplayResult, String> metricsColumn;
    @FXML
    private TableColumn<DisplayResult, String> resultColumn;

    private List<List<Double>> timeData;

    private ObservableList<DisplayResult> displayResultsData = FXCollections.observableArrayList();

    public void initialize() {
        resultsTableView.widthProperty().addListener((obs, oldVal, newVal) -> {
            // 表格的内部宽度减去2，这个值可能需要根据实际情况调整，以避免出现水平滚动条
            double tableWidth = newVal.doubleValue() - 2;

            // 两列均分宽度
            metricsColumn.prefWidthProperty().bind(resultsTableView.widthProperty().divide(2));
            resultColumn.prefWidthProperty().bind(resultsTableView.widthProperty().divide(2));
        });
        // 为TableView的列设置如何从DisplayResult对象获取其值
        metricsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // 将ObservableList与TableView绑定
        resultsTableView.setItems(displayResultsData);
        // 初始化图表（可选：根据实际情况调用）
        setupCharts();
    }

    // 一个辅助方法，用于将TestResult对象的数据添加到TableView中
    public void displayTestResults(TestResult testResult) {
        // 先清除之前的数据
        displayResultsData.clear();

        // 假设names和values数组长度相同
        for (int i = 0; i < testResult.names.length; i++) {
            displayResultsData.add(new DisplayResult(testResult.names[i], testResult.values[i]));
        }
    }

    public void setTimeData(List<List<Double>> timeData) {
        this.timeData = timeData;
        updateCharts();
    }

    private void setupCharts() {
        cpuUsageLineChart.setTitle("CPU利用率");
        memoryUsageLineChart.setTitle("内存占用率");
        diskSpeedLineChart.setTitle("读写延迟");

        // 清空图表数据
        cpuUsageLineChart.getData().clear();
        memoryUsageLineChart.getData().clear();
        diskSpeedLineChart.getData().clear();
    }

    private void updateCharts() {
        // 确保图例可见
        cpuUsageLineChart.setLegendVisible(true);
        memoryUsageLineChart.setLegendVisible(true);
        diskSpeedLineChart.setLegendVisible(true);

        // 设置图表标题
        cpuUsageLineChart.setTitle("CPU利用率(%)");
        memoryUsageLineChart.setTitle("内存占用率(%)");
        diskSpeedLineChart.setTitle("磁盘读写速度(MB/s)");

        // 设置图例位置到右侧
        cpuUsageLineChart.setLegendSide(Side.RIGHT);
        memoryUsageLineChart.setLegendSide(Side.RIGHT);
        diskSpeedLineChart.setLegendSide(Side.RIGHT);

        // 清除旧数据
        cpuUsageLineChart.getData().clear();
        memoryUsageLineChart.getData().clear();
        diskSpeedLineChart.getData().clear();

        // 创建并添加IOPS数据系列
        XYChart.Series<String, Number> cpuUsageSeries = new XYChart.Series<>();
        cpuUsageSeries.setName("CPU利用率");
        for (int i = 0; i < timeData.get(0).size(); i++) {
            cpuUsageSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(0).get(i)));
        }
        cpuUsageLineChart.getData().add(cpuUsageSeries);

        // 创建并添加读写带宽数据系列
        XYChart.Series<String, Number> memoryUsageSeries = new XYChart.Series<>();
        memoryUsageSeries.setName("内存占用率");
        for (int i = 0; i < timeData.get(1).size(); i++) {
            memoryUsageSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(1).get(i)));
        }
        memoryUsageLineChart.getData().addAll(memoryUsageSeries);

        // 创建并添加读写延迟数据系列
        XYChart.Series<String, Number> readSpeedSeries = new XYChart.Series<>();
        readSpeedSeries.setName("读速度");
        XYChart.Series<String, Number> writeSpeedSeries = new XYChart.Series<>();
        writeSpeedSeries.setName("写速度");
        for (int i = 0; i < timeData.get(3).size(); i++) {
            readSpeedSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(2).get(i)));
            writeSpeedSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), timeData.get(3).get(i)));
        }
        diskSpeedLineChart.getData().addAll(readSpeedSeries, writeSpeedSeries);

        // 请求布局更新，确保数据变化反映到UI上
        cpuUsageLineChart.requestLayout();
        memoryUsageLineChart.requestLayout();
        diskSpeedLineChart.requestLayout();
    }

    public void clearAll() {
        cpuUsageLineChart.getData().clear();
        memoryUsageLineChart.getData().clear();
        diskSpeedLineChart.getData().clear();

        currentStepTextArea.clear();

        resultsTableView.getItems().clear();
    }
}
