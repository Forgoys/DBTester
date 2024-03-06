package frontend.controller;

import backend.tester.fileSystem.FioReadWriteTest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Util {
    public enum status {
        SSH_UNCONNECT;
    }

    public static void popUpInfo(String information, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, information, ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    public static Optional<ButtonType> popUpChoose(String information, String title) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, information, ButtonType.OK, ButtonType.NO);
        alert.setHeaderText(title);
        return alert.showAndWait();
    }

    /**
     * 清除gridPane第一行之后的所有行，第一行是测试对象或项目的下拉列表，后面是相关的参数配置
     * @param gridPane 装了测试对象或项目的gridPane
     */
    public static void clearGridPaneRowsAfterFirst(GridPane gridPane) {
        // 创建一个列表来收集所有第一行之后的节点
        List<Node> nodesToRemove = new ArrayList<>();

        // 遍历GridPane中的所有节点
        for (Node child : gridPane.getChildren()) {
            // GridPane.getRowIndex(node)可能返回null，所以使用默认值0
            Integer rowIndex = GridPane.getRowIndex(child);
            if (rowIndex == null) {
                rowIndex = 0;
            }

            // 如果节点的行索引大于0，则将其添加到待移除列表中
            if (rowIndex > 0) {
                nodesToRemove.add(child);
            }
        }

        // 移除所有第一行之后的节点
        gridPane.getChildren().removeAll(nodesToRemove);

        // 可选：清除所有除第一行之外的行约束
        if (gridPane.getRowConstraints().size() > 1) {
            gridPane.getRowConstraints().subList(1, gridPane.getRowConstraints().size()).clear();
        }
    }

//    public Task<Void> getTask(Object contorller, String testProject) {
//        return new Task<>() {
//            @Override
//            protected Void call() throws Exception {
//                TextArea currentStepTextArea;
//                if (testProject.equals("读写速度测试")) {
//                    currentStepTextArea = (FSReadWriteTestController)contorller.currentStepTextArea;
//                }
//                updateMessage(message2Update.append("开始fio读写性能测试\n").toString());
//                updateMessage(message2Update.append("测试中....\n").toString());
//                testItem = new FioReadWriteTest(testArguments.values.get(0), testArguments.values.get(1), testArguments.values.get(2), testArguments.values.get(3));
//                System.out.println("开始fio读写性能测试1\n");
//                testItem.startTest();
//                Platform.runLater(() -> currentStepTextArea.appendText("测试完成\n"));
//                System.out.println("开始fio读写性能测试2\n");
//                Platform.runLater(() -> {
//                    currentStepTextArea.appendText("开始生成测试结果\n");
//                    testResult = testItem.getTestResults();
////                            System.out.println(testResult.values);
//                    fsReadWriteTestController.displayTestResults(testResult);
//                    currentStepTextArea.appendText("生成完毕\n");
//                });
//                return null;
//            }
//        };
//    }

//    private void configureTestProjectParmUI_backup() {
//        Util.clearGridPaneRowsAfterFirst(testProjectConfigPane);
//        String testProject = testProjectSelectBox.getValue();
//        int rowIndex = 1;
//        switch (testProject) {
//            case "TPC-C":
//                ComboBox<String> tpccWarehousesComboBox = new ComboBox<>();
//                tpccWarehousesComboBox.setId("tpccWarehousesComboBox");
//                tpccWarehousesComboBox.getItems().addAll("20", "50", "100", "500", "1000");
//                testProjectConfigPane.add(new Label("数据规模"), 0, rowIndex);
//                testProjectConfigPane.add(tpccWarehousesComboBox, 1, rowIndex++);
//                break;
//            case "TPC-H":
//                ComboBox<String> tpchDataScaleComboBox = new ComboBox<>();
//                tpchDataScaleComboBox.setId("tpccWarehousesComboBox");
//                tpchDataScaleComboBox.getItems().addAll("5", "10", "20");
//                testProjectConfigPane.add(new Label("数据规模"), 0, rowIndex);
//                testProjectConfigPane.add(tpchDataScaleComboBox, 1, rowIndex++);
//                break;
//            case "可靠性测试":
//                String testObject = testObjectSelectBox.getValue();
//                switch (testObject) {
//                    case "PolarDB":
//                    case "神通数据库":
//                    case "OpenGauss":
//                        testProjectConfigPane.add(new Label("CSV数据文件设置："), 0, rowIndex++);
//                        // 在这里补充代码
//                        // CSV数据文件设置部分
//                        Label fileNameLabel = new Label("未选择文件");
//                        testProjectConfigPane.add(new Label("CSV数据文件"), 0, rowIndex);
//                        testProjectConfigPane.add(fileNameLabel, 1, rowIndex++);
//                        Button fileChooserButton = new Button("选择文件");
//                        fileChooserButton.setOnAction(e -> {
//                            FileChooser fileChooser = new FileChooser();
//                            fileChooser.setTitle("选择CSV数据文件");
//                            // 设置初始目录为程序的当前工作目录
//                            fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.dir")));
//                            // 设置文件过滤器，只允许CSV文件
//                            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件 (*.csv)", "*.csv"));
//
//                            // 显示文件选择器，并获取选择的文件
//                            Stage stage = (Stage) testObjectSelectBox.getScene().getWindow(); // 获取当前窗口作为父窗口
//                            java.io.File selectedFile = fileChooser.showOpenDialog(stage);
//                            if (selectedFile != null) {
//                                fileNameLabel.setText(selectedFile.getAbsolutePath());
//                            }
//                        });
//                        testProjectConfigPane.add(fileChooserButton, 1, rowIndex++);
//
//                        // 文件编码设置
//                        ComboBox<String> encodingComboBox = new ComboBox<>();
//                        encodingComboBox.getItems().addAll("UTF-8", "GBK", "ISO-8859-1", "UTF-16");
//                        encodingComboBox.setValue("UTF-8"); // 默认选择UTF-8
//                        testProjectConfigPane.add(new Label("文件编码"), 0, rowIndex);
//                        testProjectConfigPane.add(encodingComboBox, 1, rowIndex++);
//
//                        // 变量名称设置
//                        TextField variableNameTextField = new TextField("query");
//                        testProjectConfigPane.add(new Label("变量名称"), 0, rowIndex);
//                        testProjectConfigPane.add(variableNameTextField, 1, rowIndex++);
//
//                        // 分隔符设置
//                        TextField delimiterTextField = new TextField("!");
//                        testProjectConfigPane.add(new Label("分隔符"), 0, rowIndex);
//                        testProjectConfigPane.add(delimiterTextField, 1, rowIndex++);
//
//                        // 是否允许带引号设置
//                        CheckBox allowQuotesCheckBox = new CheckBox();
//                        allowQuotesCheckBox.setSelected(true); // 默认允许
//                        testProjectConfigPane.add(new Label("是否允许带引号"), 0, rowIndex);
//                        testProjectConfigPane.add(allowQuotesCheckBox, 1, rowIndex++);
//
//                        break;
//                    case "涛思数据库":
//                    case "Lindorm":
//
//                        break;
//                    case "GlusterFS":
//                    case "OceanFS":
//
//                        break;
//                }
//            case "适配性测试":
//                break;
//            case "读写速度测试":
//            case "并发度测试":
//
//        }
//        // 添加确认按钮
//        Button testProjectParmConfirmButton = new Button("确认");
//        testProjectParmConfirmButton.setOnAction(event -> onTestProjectParmConfirmButtonClicked());
//        testProjectConfigPane.add(testProjectParmConfirmButton, 1, rowIndex, 1, 2);
//    }
}
