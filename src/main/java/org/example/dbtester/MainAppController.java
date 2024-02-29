package org.example.dbtester;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.*;

public class MainAppController {
    // ssh 连接参数
    @FXML
    private TextField sshIPInput;
    @FXML
    private TextField sshPortInput;
    @FXML
    private TextField sshUserNameInput;
    @FXML
    private TextField sshPasswordInput;

    // 测试对象选择
    @FXML
    private ComboBox<String> testObjectSelectBox;  // 测试对象选择下拉列表
    @FXML
    private GridPane testObjectConfigPane;  // 测试对象配置输入表格

    // 测试项目选择
    @FXML
    private ComboBox<String> testProjectSelectBox;  // 测试项目选择下拉列表
    @FXML
    private GridPane testProjectConfigPane;  // 测试项目配置输入表格

    private Map<String, List<TestProject>> testProjectsMap = new HashMap<>();

    private SSHConnection currentSSHConnection;

    @FXML
    private void initialize() {

        testObjectSelectBox.getItems().addAll("PolarDB", "神通数据库", "OpenGauss", "TDengine", "InfluxDB", "Lindorm", "GlusterFS", "OceanFS");
//        testObjectSelectBox.setOnAction(event -> onTestObjectSelect());
    }

    @FXML
    private void sshConnectButtonClick() {
        String ip = sshIPInput.getText().isEmpty() ? "127.0.0.1" : sshIPInput.getText();
        int port = sshPortInput.getText().isEmpty() ? 22 : Integer.parseInt(sshPortInput.getText());
        String userName = sshUserNameInput.getText();
        String password = sshPasswordInput.getText();

        // 如果当前有连接，检查连接信息是否相同
        if (currentSSHConnection != null && currentSSHConnection.getStatus()) {
            // 检查IP、端口、用户名是否相同
            if (currentSSHConnection.getIp().equals(ip) && currentSSHConnection.getPort() == port && currentSSHConnection.getUserName().equals(userName)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "当前已连接到相同的服务器，无需重新连接。", ButtonType.OK);
                alert.setHeaderText("SSH连接信息");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前已有其他SSH连接，是否断开并重新连接到新的服务器？", ButtonType.YES, ButtonType.NO);
                alert.setHeaderText("确认新的SSH连接");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    currentSSHConnection.sshDisconnect(); // 断开当前连接
                    connectToNewSSH(ip, port, userName, password); // 连接到新的服务器
                }
            }
        } else {
            // 如果没有当前连接，直接连接
            connectToNewSSH(ip, port, userName, password);
        }
    }

    private void connectToNewSSH(String ip, int port, String userName, String password) {
        currentSSHConnection = new SSHConnection(ip, port, userName, password);
        boolean isConnected = currentSSHConnection.sshConnect();
        Alert alert;
        if (isConnected) {
            alert = new Alert(Alert.AlertType.INFORMATION, "成功连接到 " + ip, ButtonType.OK);
            alert.setHeaderText("SSH连接成功");
        } else {
            alert = new Alert(Alert.AlertType.ERROR, "无法连接到 " + ip + "。请检查您的连接信息后再试。", ButtonType.OK);
            alert.setHeaderText("SSH连接失败");
        }
        alert.showAndWait();
    }

    @FXML
    // 选择测试对象，显示对应的配置
    private void onTestObjectSelect() {
        String selectedTestObject = testObjectSelectBox.getValue();
        clearGridPaneRowsAfterFirst(testObjectConfigPane);
        testProjectSelectBox.getItems().clear();

        int rowIndex = 1;
        switch (selectedTestObject) {
            case "PolarDB":
            case "神通数据库":
            case "OpenGauss":
            case "TDengine":
            case "InfluxDB":
            case "Lindorm":
                updateTestProjectSelectBox(selectedTestObject);
                configureDatabaseConnectionUI(rowIndex);
                break;
            case "GlusterFS":
            case "OceanFS":
                updateFileSystemTestProjectSelectBox();
                configureFileSystemUI(rowIndex);
                break;
        }

        // 添加确认按钮
        Button testObjectConnectConfirmButton = new Button("确认");
        testObjectConnectConfirmButton.setOnAction(event -> onTestObjectConnectConfirmButtonClicked());
        testObjectConfigPane.add(testObjectConnectConfirmButton, 1, rowIndex, 2, 2);
    }

    private void updateTestProjectSelectBox(String selectedTestObject) {
        if (selectedTestObject.equals("PolarDB") || selectedTestObject.equals("神通数据库") || selectedTestObject.equals("OpenGauss")) {
            testProjectSelectBox.getItems().addAll("TPC-C", "TPC-H", "可靠性", "适配性");
        } else {
            testProjectSelectBox.getItems().addAll("写入性能", "查询性能", "可靠性", "适配性");
        }
    }

    private void configureDatabaseConnectionUI(int rowIndex) {
        String[] databaseConfigParm = new String[]{"JDBC驱动选择", "数据库URL", "用户名", "密码"};

        Label jdbcDriverNameLabel = new Label("未选择驱动");
        testObjectConfigPane.add(new Label("JDBC驱动"), 0, rowIndex);
        testObjectConfigPane.add(jdbcDriverNameLabel, 1, rowIndex++);
        Button jdbcDriverButton = new Button("选择驱动");
        jdbcDriverButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择文件");
            // 设置初始目录为程序的当前工作目录
            fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.dir")));
            // 设置文件过滤器
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR 文件 (*.jar)", "*.jar"));
            // 通过从任何一个组件获取Stage
            Stage stage = (Stage) testObjectSelectBox.getScene().getWindow();
            java.io.File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                jdbcDriverNameLabel.setText(selectedFile.getAbsolutePath());
            }
        });

        testObjectConfigPane.add(jdbcDriverButton, 1, rowIndex++);

        Label dbURLLabel = new Label("数据库URL");
        TextField dbURLTextField = new TextField();
        dbURLTextField.setId("dbURLTextField");
        testObjectConfigPane.add(dbURLLabel, 0, rowIndex);
        testObjectConfigPane.add(dbURLTextField, 1, rowIndex++);

        Label dbUserLabel = new Label("用户名");
        TextField dbUserTextField = new TextField();
        dbUserTextField.setId("dbUserTextField");
        testObjectConfigPane.add(dbUserLabel, 0, rowIndex);
        testObjectConfigPane.add(dbUserTextField, 1, rowIndex++);

        Label dbPasswordLabel = new Label("密码");
        TextField dbPasswordTextField = new TextField();
        dbPasswordTextField.setId("dbPasswordTextField");
        testObjectConfigPane.add(dbPasswordLabel, 0, rowIndex);
        testObjectConfigPane.add(dbPasswordTextField, 1, rowIndex++);
    }

    private void updateFileSystemTestProjectSelectBox() {
        testProjectSelectBox.getItems().addAll("读写速度测试", "并发度测试", "可靠性测试");
    }

    private void configureFileSystemUI(int rowIndex) {
        String[] fileSystemConfigParm = new String[]{"服务器卷路径", "挂载目录"};
        Label fsServerPathLabel = new Label("服务器卷路径");
        TextField fsServerPathTextField = new TextField();
        fsServerPathTextField.setId("fsServerPathTextField");
        testObjectConfigPane.add(fsServerPathLabel, 0, rowIndex);
        testObjectConfigPane.add(fsServerPathTextField, 1, rowIndex++);

        Label fsMountPathLabel = new Label("挂载目录");
        TextField fsMountPathTextField = new TextField();
        fsMountPathTextField.setId("fsMountPathTextField");
        testObjectConfigPane.add(fsMountPathLabel, 0, rowIndex);
        testObjectConfigPane.add(fsMountPathTextField, 1, rowIndex++);
    }

    // 测试对象连接按钮
    private void onTestObjectConnectConfirmButtonClicked() {
        // 确认按钮的事件处理器
        // 这里可以处理配置数据，比如获取TextField的值等
        System.out.println("确认按钮被点击");
    }

    @FXML
    // 测试项目选择及参数配置显示
    private void onTestProjectSelect() {
        clearGridPaneRowsAfterFirst(testProjectConfigPane);
        String testProject = testProjectSelectBox.getValue();
        int rowIndex = 1;
        switch (testProject) {
            case "TPC-C":
                ComboBox<String> tpccWarehousesComboBox = new ComboBox<String>();
                tpccWarehousesComboBox.setId("tpccWarehousesComboBox");
                tpccWarehousesComboBox.getItems().addAll("20", "50", "100", "500", "1000");
                testProjectConfigPane.add(new Label("数据规模"), 0, rowIndex);
                testProjectConfigPane.add(tpccWarehousesComboBox, 1, rowIndex++);
                break;
            case "TPC-H":
                ComboBox<String> tpchDataScaleComboBox = new ComboBox<String>();
                tpchDataScaleComboBox.setId("tpccWarehousesComboBox");
                tpchDataScaleComboBox.getItems().addAll("5", "10", "20");
                testProjectConfigPane.add(new Label("数据规模"), 0, rowIndex);
                testProjectConfigPane.add(tpchDataScaleComboBox, 1, rowIndex++);
                break;
            case "可靠性测试":
                String testObject = testObjectSelectBox.getValue();
                switch (testObject) {
                    case "PolarDB":
                    case "神通数据库":
                    case "OpenGauss":
                        testProjectConfigPane.add(new Label("CSV数据文件设置："), 0, rowIndex++);
                        // 在这里补充代码
                        // CSV数据文件设置部分
                        Label fileNameLabel = new Label("未选择文件");
                        testProjectConfigPane.add(new Label("CSV数据文件"), 0, rowIndex);
                        testProjectConfigPane.add(fileNameLabel, 1, rowIndex++);
                        Button fileChooserButton = new Button("选择文件");
                        fileChooserButton.setOnAction(e -> {
                            FileChooser fileChooser = new FileChooser();
                            fileChooser.setTitle("选择CSV数据文件");
                            // 设置初始目录为程序的当前工作目录
                            fileChooser.setInitialDirectory(new java.io.File(System.getProperty("user.dir")));
                            // 设置文件过滤器，只允许CSV文件
                            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件 (*.csv)", "*.csv"));

                            // 显示文件选择器，并获取选择的文件
                            Stage stage = (Stage) testObjectSelectBox.getScene().getWindow(); // 获取当前窗口作为父窗口
                            java.io.File selectedFile = fileChooser.showOpenDialog(stage);
                            if (selectedFile != null) {
                                fileNameLabel.setText(selectedFile.getAbsolutePath());
                            }
                        });
                        testProjectConfigPane.add(fileChooserButton, 1, rowIndex++);

                        // 文件编码设置
                        ComboBox<String> encodingComboBox = new ComboBox<>();
                        encodingComboBox.getItems().addAll("UTF-8", "GBK", "ISO-8859-1", "UTF-16");
                        encodingComboBox.setValue("UTF-8"); // 默认选择UTF-8
                        testProjectConfigPane.add(new Label("文件编码"), 0, rowIndex);
                        testProjectConfigPane.add(encodingComboBox, 1, rowIndex++);

                        // 变量名称设置
                        TextField variableNameTextField = new TextField("query");
                        testProjectConfigPane.add(new Label("变量名称"), 0, rowIndex);
                        testProjectConfigPane.add(variableNameTextField, 1, rowIndex++);

                        // 分隔符设置
                        TextField delimiterTextField = new TextField("!");
                        testProjectConfigPane.add(new Label("分隔符"), 0, rowIndex);
                        testProjectConfigPane.add(delimiterTextField, 1, rowIndex++);

                        // 是否允许带引号设置
                        CheckBox allowQuotesCheckBox = new CheckBox();
                        allowQuotesCheckBox.setSelected(true); // 默认允许
                        testProjectConfigPane.add(new Label("是否允许带引号"), 0, rowIndex);
                        testProjectConfigPane.add(allowQuotesCheckBox, 1, rowIndex++);

                        break;
                    case "涛思数据库":
                    case "Lindorm":

                        break;
                    case "GlusterFS":
                    case "OceanFS":

                        break;
                }
            case "读写速度测试":
            case "并发度测试":

        }
        // 添加确认按钮
        Button testProjectParmConfirmButton = new Button("确认");
        testProjectParmConfirmButton.setOnAction(event -> onTestProjectParmConfirmButtonClicked());
        testProjectConfigPane.add(testProjectParmConfirmButton, 1, rowIndex, 1, 2);
    }

    // 测试对象开始测试按钮点击
    private void onTestProjectParmConfirmButtonClicked() {
        // 确认按钮的事件处理器
        // 这里可以处理配置数据，比如获取TextField的值等
        System.out.println("确认按钮被点击");
    }

    public void clearGridPaneRowsAfterFirst(GridPane gridPane) {
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
}
