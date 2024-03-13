package frontend.controller;

import backend.dataset.DisplayResult;
import backend.dataset.TestResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

public class FSOtherTestController {
    @FXML
    public TextArea currentStepTextArea;
    @FXML
    private TableView<DisplayResult> fioResultsTableView;
    @FXML
    private TableColumn<DisplayResult, String> metricsColumn;
    @FXML
    private TableColumn<DisplayResult, String> resultColumn;
    private final ObservableList<DisplayResult> displayResultsData = FXCollections.observableArrayList();

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

    public void clearAll() {
        currentStepTextArea.clear();

        fioResultsTableView.getItems().clear();
    }
}


