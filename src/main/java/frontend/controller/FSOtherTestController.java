package frontend.controller;

import backend.dataset.DisplayResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import backend.dataset.TestResult;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class FSOtherTestController {
    @FXML
    private TableView<DisplayResult> fioResultsTableView;
    @FXML
    private TableColumn<DisplayResult, String> metricsColumn;
    @FXML
    private TableColumn<DisplayResult, String> resultColumn;
    @FXML
    public TextArea currentStepTextArea;

    private ObservableList<DisplayResult> displayResultsData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
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


