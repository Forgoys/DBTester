package frontend.controller;

import backend.dataset.TestArguments;
import backend.tester.fileSystem.FioReadWriteTest;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Util {
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

//    public static void popUpInfo(String information, String title) {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION, information, ButtonType.OK);
//        alert.setHeaderText(title);
//
//        // ???????????
//        DialogPane dialogPane = alert.getDialogPane();
//        dialogPane.setPrefWidth(400); // ???????????????????
//        dialogPane.setContentText(information);
//
//        // ??CSS??????????
//        dialogPane.getStylesheets().add(YourClassName.class.getResource("myStyleSheet.css").toExternalForm());
//        dialogPane.getStyleClass().add("dialog-pane");
//
//        alert.showAndWait();
//    }
//
//    public static Optional<ButtonType> popUpChoose(String information, String title) {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, information, ButtonType.OK, ButtonType.NO);
//        alert.setHeaderText(title);
//
//        // ???????????
//        DialogPane dialogPane = alert.getDialogPane();
//        dialogPane.setPrefWidth(400); // ???????????????????
//        dialogPane.setContentText(information);
//
//        // ??CSS??????????
//        dialogPane.getStylesheets().add(YourClassName.class.getResource("myStyleSheet.css").toExternalForm());
//        dialogPane.getStyleClass().add("dialog-pane");
//
//        return alert.showAndWait();
//    }


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

    public static TestArguments getTestArgFromGridPane(GridPane gridPane, int rowIndex) {
        TestArguments testArguments = new TestArguments();

        for (Node node : gridPane.getChildren()) {
            Integer nodeRowIndex = GridPane.getRowIndex(node);
            // 修正可能的null值
            int actualRowIndex = (nodeRowIndex == null) ? 0 : nodeRowIndex;

            // 只处理位于指定行及之后的行的控件
            if (actualRowIndex >= rowIndex) {
                // 获取当前控件所在列的索引
                Integer nodeColIndex = GridPane.getColumnIndex(node);
                int actualColIndex = (nodeColIndex == null) ? 0 : nodeColIndex;

                // 检查当前节点是否是TextField或ComboBox
                if (node instanceof TextField || node instanceof ComboBox) {
                    // 尝试获取左侧的Label控件
                    Label label = null;
                    for (Node possibleLabelNode : gridPane.getChildren()) {
                        Integer labelRow = GridPane.getRowIndex(possibleLabelNode);
                        Integer labelCol = GridPane.getColumnIndex(possibleLabelNode);
                        int actualLabelRow = (labelRow == null) ? 0 : labelRow;
                        int actualLabelCol = (labelCol == null) ? 0 : labelCol;

                        // 检查是否为当前TextField/ComboBox左侧的Label
                        if (actualLabelRow == actualRowIndex && actualLabelCol == actualColIndex - 1) {
                            if (possibleLabelNode instanceof Label) {
                                label = (Label) possibleLabelNode;
                                break;
                            }
                        }
                    }

                    // 如果找到了Label并且Label的内容是特定的文本，则进行特殊处理
                    if (label != null && "本机sudo密码".equals(label.getText())) {
                        // 这里处理TextField中的内容
                        if (node instanceof TextField textField) {
                            String textContent = textField.getText();
                            // 在这里添加你的特定处理逻辑
                            if (!Util.checkSudoPassword(textContent)) {
                                Util.popUpInfo("本机超级权限密码错误，请重新输入", "错误");
                                return null;
                            } else {
                                testArguments.values.add(textContent);
                            }
                        }
                    } else {
                        // 对于非特定Label或其它情况的通用处理
                        if (node instanceof TextField textField) {
                            testArguments.values.add(textField.getText());
                        } else if (node instanceof ComboBox comboBox) {
                            String selected = (String) comboBox.getSelectionModel().getSelectedItem();
                            testArguments.values.add(selected != null ? selected : "");
                        }
                    }
                }
            }
        }

        return testArguments;
    }

    public static boolean checkAllFilled(TestArguments testArguments) {
        if (testArguments == null) {
            return false;
        }
        for (String s : testArguments.values) {
            if (s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAllFilled(String... args) {
        for (String s : args) {
            if (s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

//    public static TestArguments getTestArgFromGridPane(GridPane gridPane, int rowIndex) {
//        TestArguments testArguments = new TestArguments();
//
//        for (Node node : gridPane.getChildren()) {
//            // ????????
//            Integer nodeRowIndex = GridPane.getRowIndex(node);
//            // ????????????????? 0
//            int actualRowIndex = (nodeRowIndex == null) ? 0 : nodeRowIndex;
//
//            // ????? rowIndex ???????
//            if (actualRowIndex >= rowIndex) {
//                if (node instanceof TextField textField) {
//                    testArguments.values.add(textField.getText()); // ??TextField??
//
//                } else if (node instanceof ComboBox) {
//                    @SuppressWarnings("unchecked")
//                    ComboBox<String> comboBox = (ComboBox<String>) node;
//                    String selected = comboBox.getSelectionModel().getSelectedItem();
//                    if (selected != null) {
//                        testArguments.values.add(selected); // ??ComboBox????
//                    } else {
//                        testArguments.values.add(""); // ??????????
//                    }
//                }
//            }
//        }
//
//        return testArguments;
//    }

    public static boolean checkSudoPassword(String password) {
        String command = "sudo -S ls /root"; // 示例命令，需要sudo权限
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"sh", "-c", command});
            OutputStream os = process.getOutputStream();
            os.write((password + "\n").getBytes()); // 向进程输出流写入密码
            os.flush();
            os.close();

            // 等待命令执行完成
            int exitValue = process.waitFor();

            // 根据退出值判断命令是否成功执行
            return exitValue == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        // 示例密码，实际使用中应从用户输入或其他安全方式获取
        String sudoPassword = "lhjlhj";
        boolean isCorrect = checkSudoPassword(sudoPassword);
        System.out.println("Is sudo password correct? " + isCorrect);
    }

    static void customizeChartSeriesStyle(LineChart<String, Number> chart) {
        for (XYChart.Series<String, Number> series : chart.getData()) {
            // 调整线条粗细
            series.getNode().setStyle("-fx-stroke-width: 1px;");

            // 调整数据点样式
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node dataNode = data.getNode();
                if (dataNode != null) {
                    dataNode.setStyle("-fx-padding: 0;"); // 隐藏数据点的圆圈
                }
            }
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
