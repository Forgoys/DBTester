package frontend.controller;

import backend.dataset.DisplayResult;
import backend.dataset.TestResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;

import java.util.Collections;
import java.util.List;

public class FSReadWriteTestController {
    @FXML
    public Label cpuMaxLabel;
    @FXML
    public Label cpuMinLabel;
    @FXML
    public Label cpuAvgLabel;
    @FXML
    public Label memoryMaxLabel;
    @FXML
    public Label memoryMinLabel;
    @FXML
    public Label memoryAvgLabel;

    @FXML
    private TableView<DisplayResult> fioResultsTableView;
    @FXML
    private TableColumn<DisplayResult, String> metricsColumn;
    @FXML
    private TableColumn<DisplayResult, String> resultColumn;


    @FXML
    private LineChart<String, Number> cpuUsageLineChart;
    @FXML
    private LineChart<String, Number> memoryUsageLineChart;

    @FXML
    public TextArea currentStepTextArea;

    private List<List<Double>> timeData;

    private ObservableList<DisplayResult> displayResultsData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        fioResultsTableView.widthProperty().addListener((obs, oldVal, newVal) -> {
            // 表格的内部宽度减去2，这个值可能需要根据实际情况调整，以避免出现水平滚动条
            double tableWidth = newVal.doubleValue() - 2;

            // 两列均分宽度
            metricsColumn.prefWidthProperty().bind(fioResultsTableView.widthProperty().divide(2));
            resultColumn.prefWidthProperty().bind(fioResultsTableView.widthProperty().divide(2));
        });
        // 为TableView的列设置如何从DisplayResult对象获取其值
        metricsColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        resultColumn.setCellValueFactory(new PropertyValueFactory<>("value"));

        // 将ObservableList与TableView绑定
        fioResultsTableView.setItems(displayResultsData);

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

        // 清空图表数据
        cpuUsageLineChart.getData().clear();
        memoryUsageLineChart.getData().clear();
    }

    void updateCharts() {
        // 确保图例可见
        cpuUsageLineChart.setLegendVisible(true);
        memoryUsageLineChart.setLegendVisible(true);

        // 设置图表标题
        cpuUsageLineChart.setTitle("CPU利用率");
        memoryUsageLineChart.setTitle("内存占用率");

        // 设置图例位置到右侧
        cpuUsageLineChart.setLegendSide(Side.RIGHT);
        memoryUsageLineChart.setLegendSide(Side.RIGHT);

        // 清除旧数据
        cpuUsageLineChart.getData().clear();
        memoryUsageLineChart.getData().clear();

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


        Util.customizeChartSeriesStyle(cpuUsageLineChart);
        Util.customizeChartSeriesStyle(memoryUsageLineChart);

        // 更新CPU使用率的统计信息标签
        updateStatisticsLabels(timeData.get(0), cpuMaxLabel, cpuMinLabel, cpuAvgLabel);

        // 更新内存使用率的统计信息标签
        updateStatisticsLabels(timeData.get(1), memoryMaxLabel, memoryMinLabel, memoryAvgLabel);

        // 请求布局更新，确保数据变化反映到UI上
        cpuUsageLineChart.requestLayout();
        memoryUsageLineChart.requestLayout();
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
        cpuUsageLineChart.getData().clear();
        memoryUsageLineChart.getData().clear();

        currentStepTextArea.clear();

        fioResultsTableView.getItems().clear();
    }
}
