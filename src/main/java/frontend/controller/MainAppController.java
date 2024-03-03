package frontend.controller;

import backend.dataset.ArgumentProperty;
import backend.dataset.TestArguments;
import backend.tester.TestItem;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public static SSHConnection currentSSHConnection;

    /**
     * 数据库连接
     */
    public static DBConnection currentDBConnection;

    private TestItem testItem;

    /**
     * 测试对象选择下拉列表
     */
    @FXML
    private ComboBox<String> testObjectSelectBox;
    /**
     * 测试对象配置输入表格
     */
    @FXML
    private GridPane testObjectConfigPane;

    /**
     * 测试项目选择下拉列表
     */
    @FXML
    private ComboBox<String> testProjectSelectBox;
    /**
     * 测试项目配置输入表格
     */
    @FXML
    private GridPane testProjectConfigPane;

    /**
     * 右侧测试结果UI
     */
    @FXML
    private ScrollPane rightScrollPane;

    /**
     * SQL执行界面的控制器
     */
    AdaptTestController adaptTestController;

    /**
     * 其他测试结果界面的控制器
     */
    OtherTestController otherTestController;

    // =================================== ssh连接服务器 =================================================

    /**
     * ssh连接按钮确认，连接服务器
     */
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
                    currentSSHConnection.connectToNewSSH(ip, port, userName, password); // 连接到新的服务器
                }
            }
        } else {
            // 如果没有当前连接，直接连接
            if (currentSSHConnection != null) {
                currentSSHConnection.connectToNewSSH(ip, port, userName, password);
            }
        }
    }

    // ================================ 数据库连接或文件系统挂载 ===========================================

    /**
     * 根据选择的测试对象，显示对应的连接参数，更新可选择的测试项目
     */
    @FXML
    private void onTestObjectSelect() {
        String selectedTestObject = testObjectSelectBox.getValue();
        clearGridPaneRowsAfterFirst(testObjectConfigPane);
        testProjectSelectBox.getItems().clear();

        int rowIndex = 1;
        rowIndex = switch (selectedTestObject) {
            case "PolarDB", "神通数据库", "OpenGauss" -> {
                testProjectSelectBox.getItems().addAll("TPC-C", "TPC-H", "可靠性", "适配性");
                yield configureDBConnectUI(rowIndex);
            }
            case "TDengine", "InfluxDB", "Lindorm" -> {
                testProjectSelectBox.getItems().addAll("写入性能", "查询性能", "可靠性", "适配性");
                yield configureDBConnectUI(rowIndex);
            }
            case "GlusterFS", "OceanFS" -> {
                testProjectSelectBox.getItems().addAll("读写速度测试", "并发度测试", "可靠性测试");
                yield configureFSConnectUI(rowIndex);
            }
            default -> 1;
            // 根据选定的测试对象更新测试项目下拉菜单，并配置相关UI
        };

        // 添加确认按钮
        Button testObjectConnectConfirmButton = new Button("确认");
        testObjectConnectConfirmButton.setOnAction(event -> onTestObjectConnectConfirmButtonClicked());
        testObjectConfigPane.add(testObjectConnectConfirmButton, 1, rowIndex, 2, 2);
    }

    /**
     * 测试对象连接按钮
     */
    private void onTestObjectConnectConfirmButtonClicked() {
        System.out.println("确认按钮被点击");
        String selectedTestObject = testObjectSelectBox.getValue();

        // 根据selectedTestObject的值来决定执行哪种类型的连接操作
        switch (selectedTestObject) {
            case "PolarDB":
            case "神通数据库":
            case "OpenGauss":
            case "TDengine":
            case "InfluxDB":
            case "Lindorm":
                // 这里调用连接数据库的方法
                connectDatabase();
                break;
            case "GlusterFS":
            case "OceanFS":
                // 这里调用挂载文件系统的方法
                mountFileSystem();
                break;
        }
    }

    private void connectDatabase() {
        // 实现连接数据库的逻辑
        System.out.println("连接数据库...");
        // 根据之前设置的数据库配置参数进行数据库连接
        // 可能需要访问保存这些参数的变量或控件
    }

    private void mountFileSystem() {
        // 实现挂载文件系统的逻辑
        System.out.println("挂载文件系统...");
        // 根据之前设置的文件系统配置参数进行文件系统挂载
        // 可能需要访问保存这些参数的变量或控件
    }

    /**
     * 设置数据库连接参数配置的相关UI
     * @param rowIndex 从gridPane的第几行开始装参数配置的“参数名-输入框”对
     */
    private int configureDBConnectUI(int rowIndex) {
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

        return rowIndex;
    }

    /**
     * 设置文件系统挂载参数配置的相关UI
     * @param rowIndex 从gridPane的第几行开始装参数配置的“参数名-输入框”对
     */
    private int configureFSConnectUI(int rowIndex) {
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

        return rowIndex;
    }

    // ================================== 测试项目参数配置 ==========================================

    /**
     * 测试项目选择及参数配置显示
     */
    @FXML
    private void onTestProjectSelect() {
        configureTestProjectParmUI();  // 设置参数配置UI
        configureTestProjectResultUI(testProjectSelectBox.getValue());  // 设置结果显示UI，适配性显示SQL执行和结果框
    }

    /**
     * 根据TestArguments自动设置参数配置UI
     */
    private void configureTestProjectParmUI() {
        clearGridPaneRowsAfterFirst(testProjectConfigPane);
        String testProject = testProjectSelectBox.getValue();
        ArgumentProperty[] properties = TestArguments.getArgPropertiesForTest(testProject);

        int rowIndex = 1;
        for (ArgumentProperty property : properties) {
            Label label = new Label(property.getName());
            if (!property.hasCandidate()) {
                // 没有候选项，使用TextField
                TextField textField = new TextField();
                textField.setId(property.getName().replaceAll("\\s+", "") + "TextField");
                testProjectConfigPane.add(label, 0, rowIndex);
                testProjectConfigPane.add(textField, 1, rowIndex);
            } else {
                // 有候选项，使用ComboBox
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.setId(property.getName().replaceAll("\\s+", "") + "ComboBox");
                comboBox.getItems().addAll(property.getOptions());
                testProjectConfigPane.add(label, 0, rowIndex);
                testProjectConfigPane.add(comboBox, 1, rowIndex);
            }
            rowIndex++;
        }

        // 添加确认按钮
        Button confirmButton = new Button("确认");
        confirmButton.setId("testProjectParmConfirmButton");
        confirmButton.setOnAction(event -> onTestProjectParmConfirmButtonClicked());
        testProjectConfigPane.add(confirmButton, 1, rowIndex);
    }

    /**
     * 测试对象参数确认按钮点击
     */
    private void onTestProjectParmConfirmButtonClicked() {
        // 首先清空旧的参数值
        TestArguments testArguments = new TestArguments();

        for (Node node : testProjectConfigPane.getChildren()) {
            // 只处理TextField和ComboBox
            if (node instanceof TextField) {
                TextField textField = (TextField) node;
                testArguments.values.add(textField.getText()); // 添加TextField的值
            } else if (node instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> comboBox = (ComboBox<String>) node;
                String selected = comboBox.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    testArguments.values.add(selected); // 添加ComboBox选中的值
                } else {
                    testArguments.values.add(""); // 或者处理未选择的情况
                }
            }
        }

        // testItem = new TPCCTest(....., testArguments)

    }

    private void configureTestProjectParmUI_backup() {
        clearGridPaneRowsAfterFirst(testProjectConfigPane);
        String testProject = testProjectSelectBox.getValue();
        int rowIndex = 1;
        switch (testProject) {
            case "TPC-C":
                ComboBox<String> tpccWarehousesComboBox = new ComboBox<>();
                tpccWarehousesComboBox.setId("tpccWarehousesComboBox");
                tpccWarehousesComboBox.getItems().addAll("20", "50", "100", "500", "1000");
                testProjectConfigPane.add(new Label("数据规模"), 0, rowIndex);
                testProjectConfigPane.add(tpccWarehousesComboBox, 1, rowIndex++);
                break;
            case "TPC-H":
                ComboBox<String> tpchDataScaleComboBox = new ComboBox<>();
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
            case "适配性测试":
                break;
            case "读写速度测试":
            case "并发度测试":

        }
        // 添加确认按钮
        Button testProjectParmConfirmButton = new Button("确认");
        testProjectParmConfirmButton.setOnAction(event -> onTestProjectParmConfirmButtonClicked());
        testProjectConfigPane.add(testProjectParmConfirmButton, 1, rowIndex, 1, 2);
    }

    /**
     * 设置右侧的UI
     * @param testProject
     */
    private void configureTestProjectResultUI(String testProject) {
        if (Objects.equals(testProject, "适配性")) {
            loadView("adaptTestView.fxml");
        } else {
            loadView("otherTestView.fxml");
        }
    }

    /**
     * 根据FXML文件名动态加载视图到ScrollPane中
     * @param fxmlFile
     */
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL fxmlUrl = getClass().getResource("/frontend/" + fxmlFile); // 注意路径的更改
            if (fxmlUrl == null) {
                throw new FileNotFoundException("FXML file not found: " + fxmlFile);
            }
            loader.setLocation(fxmlUrl);
            Node view = loader.load();
            // 以下逻辑保持不变
            if (fxmlFile.equals("adaptTestView.fxml")) {
                adaptTestController = loader.getController();
            } else if (fxmlFile.equals("otherTestView.fxml")) {
                otherTestController = loader.getController();
            }
            rightScrollPane.setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除gridPane第一行之后的所有行，第一行是测试对象或项目的下拉列表，后面是相关的参数配置
     * @param gridPane 装了测试对象或项目的gridPane
     */
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

    // ===============================结果文件导入和导出====================================
    @FXML
    private void exportTestResultClick() {

    }

    @FXML
    private void importTestResultClick() {

    }
}
