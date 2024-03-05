package frontend.controller;

import backend.dataset.ArgumentProperty;
import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import backend.tester.TestItem;
import backend.tester.fileSystem.FioParallelTest;
import backend.tester.fileSystem.FioReadWriteTest;
import backend.tester.fileSystem.MiniFileTest;
import backend.tester.fileSystem.ReliableTest;
import frontend.connection.DBConnection;
import frontend.connection.FSConnection;
import frontend.connection.SSHConnection;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MainAppController {
    public static SSHConnection currentSSHConnection;
    /**
     * 数据库连接
     */
    public static DBConnection currentDBConnection;
    /**
     * 文件系统连接
     */
    public static FSConnection currentFSConnection;
    /**
     * 数据库适配性测试界面的控制器
     */
    DBAdaptTestController dbAdaptTestController;
    /**
     * 数据库其他测试结果界面的控制器
     */
    DBOtherTestController dbOtherTestController;
    /**
     * 文件系统可靠性测试结果界面的控制器
     */
    FSReliabilityTestController fsReliabilityTestController;
    /**
     * 文件系统并发度结果界面的控制器
     */
    FSReadWriteTestController fsReadWriteTestController;
    /**
     * 文件系统读写速度测试结果界面的控制器
     */
    FSOtherTestController fsOtherTestController;

    TestItem testItem;
    @FXML
    private TitledPane sshConnectionTitledPane;
    @FXML
    private TitledPane testObjectConfigTitledPane;
    @FXML
    private TitledPane testProjectConfigTitledPane;
    // ssh 连接参数
    @FXML
    private TextField sshIPInput;
    @FXML
    private TextField sshPortInput;
    @FXML
    private TextField sshUserNameInput;
    @FXML
    private TextField sshPasswordInput;

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

    TestResult testResult;
    List<List<Double>> testTimeData;

    @FXML
    private void initialize() {
        // 初始化时不允许展开
//        sshConnectionTitledPane.setExpanded(true);
        testObjectConfigTitledPane.setDisable(false);
        testProjectConfigTitledPane.setDisable(false);
//        testObjectConfigTitledPane.setDisable(true);
//        testProjectConfigTitledPane.setDisable(true);
    }

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
            if (currentSSHConnection.getIp().equals(ip) && currentSSHConnection.getPort() == port && currentSSHConnection.getUserName().equals(userName)) {
                Util.popUpInfo("当前已连接到相同的服务器，无需重新连接。", "SSH连接信息");
                return; // 早期返回，避免进一步的处理
            } else {
                Optional<ButtonType> result = Util.popUpChoose("当前已有其他SSH连接，是否断开并重新连接到新的服务器？", "确认新的SSH连接");
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    currentSSHConnection.sshDisconnect(); // 断开当前连接
                } else {
                    return; // 用户选择不断开当前连接，直接返回
                }
            }
        }

        // 尝试连接到新的服务器
        if (currentSSHConnection == null) {
            currentSSHConnection = new SSHConnection(ip, port, userName, password); // 假设SSHConnection有无参数的构造函数
        }
        boolean connected = currentSSHConnection.sshConnect(); // 假设这个方法返回一个boolean值表示是否连接成功

        // 根据连接结果更新UI
        if (connected) {
            Util.popUpInfo("成功连接到服务器。", "SSH连接成功");
            testObjectConfigTitledPane.setDisable(false);
        } else {
            Util.popUpInfo("无法连接到服务器，请检查输入的参数。", "SSH连接失败");
        }
    }

    // ================================ 数据库连接或文件系统挂载 ===========================================

    /**
     * 根据选择的测试对象，显示对应的连接参数，更新可选择的测试项目
     */
    @FXML
    private void onTestObjectSelect() {
        String selectedTestObject = testObjectSelectBox.getValue();
        Util.clearGridPaneRowsAfterFirst(testObjectConfigPane);
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
                testProjectSelectBox.getItems().addAll("读写速度测试", "并发度测试", "小文件测试", "可靠性测试");
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
        // 获取UI上的参数
        String jdbcDriverPath = ((Label) testObjectConfigPane.lookup("#jdbcDriverNameLabel")).getText();
//        String jdbcDriverClassName = ((PasswordField) testObjectConfigPane.lookup("#jdbcDriverClassPasswordField")).getText();
        String dbURL = ((TextField) testObjectConfigPane.lookup("#dbURLTextField")).getText();
        String username = ((TextField) testObjectConfigPane.lookup("#dbUserTextField")).getText();
        String password = ((TextField) testObjectConfigPane.lookup("#dbPasswordTextField")).getText();

        // 创建DBConnection对象
        DBConnection dbConnection = new DBConnection();
        dbConnection.setJdbcDriverPath(jdbcDriverPath);
//        dbConnection.setJdbcDriverClassName(jdbcDriverClassName);
        dbConnection.setDbURL(dbURL);
        dbConnection.setUsername(username);
        dbConnection.setPassword(password);

        // 尝试连接数据库
        if (dbConnection.connect()) {
            Util.popUpInfo("数据库连接成功！", "连接成功");
            testProjectConfigTitledPane.setDisable(false);
        } else {
            Util.popUpInfo("数据库连接失败，请检查参数！", "连接失败");
        }
    }

    private void mountFileSystem() {
        // 获取UI上的参数
        String fsUrl = ((TextField) testObjectConfigPane.lookup("#fsServerPathTextField")).getText();
        String mountPath = ((TextField) testObjectConfigPane.lookup("#fsMountPathTextField")).getText();

        // 创建FSConnection对象
        FSConnection fsConnection = new FSConnection(fsUrl, mountPath);

        // 假设mountFS()方法实际执行挂载逻辑
        fsConnection.mountFS();

        // 检查是否挂载成功，这里需要在FSConnection的mountFS方法内实现具体的挂载逻辑和成功失败的检测
        if (fsConnection.isMounted()) {
            Util.popUpInfo("文件系统挂载成功！", "挂载成功");
            testProjectConfigTitledPane.setDisable(false);
        } else {
            Util.popUpInfo("文件系统挂载失败，请检查参数！", "挂载失败");
        }
    }

    /**
     * 设置数据库连接参数配置的相关UI
     *
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
            fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            // 设置文件过滤器
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR 文件 (*.jar)", "*.jar"));
            // 通过从任何一个组件获取Stage
            Stage stage = (Stage) testObjectSelectBox.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                jdbcDriverNameLabel.setText(selectedFile.getAbsolutePath());
            }
        });

        testObjectConfigPane.add(jdbcDriverButton, 1, rowIndex++);

//        Label jdbcDriverClassNameLabel = new Label("JDBC驱动类名");
//        PasswordField jdbcDriverClassPasswordField = new PasswordField();
//        jdbcDriverClassPasswordField.setId("jdbcDriverClassPasswordField");
//        testObjectConfigPane.add(jdbcDriverClassNameLabel, 0, rowIndex);
//        testObjectConfigPane.add(jdbcDriverClassPasswordField, 1, rowIndex++);

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
     *
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
        loadTestProjectResultView(testProjectSelectBox.getValue());  // 设置结果显示UI，适配性显示SQL执行和结果框
    }

    /**
     * 根据TestArguments自动设置参数配置UI
     */
    private void configureTestProjectParmUI() {
        Util.clearGridPaneRowsAfterFirst(testProjectConfigPane);
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

        // 添加确认按钮，适配性不用这个按钮
        if (!testProject.equals("适配性")) {
            Button confirmButton = new Button("开始测试");
            confirmButton.setId("testProjectParmConfirmButton");
            confirmButton.setOnAction(event -> {
                try {
                    onTestProjectParmConfirmButtonClicked();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            testProjectConfigPane.add(confirmButton, 1, rowIndex);
        }
    }

    /**
     * 测试对象参数确认按钮点击
     */
    private void onTestProjectParmConfirmButtonClicked() throws IOException, InterruptedException {
        // 首先清空旧的参数值
        TestArguments testArguments = new TestArguments();

        for (Node node : testProjectConfigPane.getChildren()) {
            // 只处理TextField和ComboBox
            if (node instanceof TextField textField) {
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

        String testProject = testProjectSelectBox.getValue();
        String message;
        Task<Void> task;
//        StringBuilder statusText = new StringBuilder();
        switch (testProjectSelectBox.getValue()) {
            case "TPC-C":
//                testItem = new TPCCTester();
//                testItem.testEnvPrepare();
//                testItem.startTest();
//                testItem.getTimeData();
//                testItem.getTestResults();
                break;
            case "TPC-H":
                break;
            case "写入性能":
                break;
            case "查询性能":
                break;
            case "可靠性":
                if (testObjectSelectBox.getValue().equals("InfluxDB") || testObjectSelectBox.getValue().equals("Lindorm") || testObjectSelectBox.getValue().equals("TDengine")) {
                    ;
                } else {
                    ;
                }
                break;
//            case "适配性":
//
//
//                break;
            case "读写速度测试":
                task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        TextArea currentStepTextArea = fsReadWriteTestController.currentStepTextArea;
                        Platform.runLater(() -> currentStepTextArea.appendText("开始fio读写性能测试\n"));

                        Platform.runLater(() -> currentStepTextArea.appendText("测试中....\n"));
                        testItem = new FioReadWriteTest(testArguments.values.get(0), testArguments.values.get(1), testArguments.values.get(2), testArguments.values.get(3));
                        testItem.startTest();
                        Platform.runLater(() -> currentStepTextArea.appendText("测试完成\n"));
                        Platform.runLater(() -> {
                            currentStepTextArea.appendText("开始生成测试结果\n");
                            testResult = testItem.getTestResults();
                            fsReadWriteTestController.displayTestResults(testResult);
                            currentStepTextArea.appendText("生成完毕\n");
                        });
                        return null;
                    }
                };

                // 可选：绑定任务属性到UI组件，比如进度条、状态标签等
                fsReadWriteTestController.currentStepTextArea.textProperty().bind(task.messageProperty());

                // 在新线程中执行任务
                new Thread(task).start();
                break;
            case "并发度测试":
                task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        TextArea currentStepTextArea = fsOtherTestController.currentStepTextArea;
                        Platform.runLater(() -> currentStepTextArea.appendText("开始并发度测试\n"));

                        Platform.runLater(() -> currentStepTextArea.appendText("测试中....\n"));
                        testItem = new FioParallelTest(testArguments.values.get(0), testArguments.values.get(1));
                        testItem.startTest();
                        Platform.runLater(() -> currentStepTextArea.appendText("测试完成\n"));
                        Platform.runLater(() -> {
                            currentStepTextArea.appendText("开始生成测试结果\n");
                            testResult = testItem.getTestResults();
                            fsOtherTestController.displayTestResults(testResult);
                            currentStepTextArea.appendText("生成完毕\n");
                        });
                        return null;
                    }
                };

                // 可选：绑定任务属性到UI组件，比如进度条、状态标签等
                    fsOtherTestController.currentStepTextArea.textProperty().bind(task.messageProperty());
                // 在新线程中执行任务
                new Thread(task).start();
                break;
            case "小文件测试":
                task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        TextArea currentStepTextArea = fsOtherTestController.currentStepTextArea;
                        Platform.runLater(() -> currentStepTextArea.appendText("开始小文件测试\n"));

                        Platform.runLater(() -> currentStepTextArea.appendText("测试中....\n"));
                        testItem = new MiniFileTest(testArguments.values.get(0));
                        testItem.startTest();
                        Platform.runLater(() -> currentStepTextArea.appendText("测试完成\n"));
                        Platform.runLater(() -> {
                            currentStepTextArea.appendText("开始生成测试结果\n");
                            testResult = testItem.getTestResults();
                            fsOtherTestController.displayTestResults(testResult);
                            currentStepTextArea.appendText("生成完毕\n");
                        });
                        return null;
                    }
                };

                // 可选：绑定任务属性到UI组件，比如进度条、状态标签等
                fsOtherTestController.currentStepTextArea.textProperty().bind(task.messageProperty());
                // 在新线程中执行任务
                new Thread(task).start();
                break;
            case "可靠性测试":
                task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        TextArea currentStepTextArea = fsReliabilityTestController.currentStepTextArea;
                        Platform.runLater(() -> currentStepTextArea.appendText("开始可靠性测试\n"));

                        Platform.runLater(() -> currentStepTextArea.appendText("测试中....\n"));
                        testItem = new ReliableTest(testArguments.values.get(0), testArguments.values.get(0));
                        testItem.startTest();
                        Platform.runLater(() -> currentStepTextArea.appendText("测试完成\n"));
                        Platform.runLater(() -> {
                            currentStepTextArea.appendText("开始生成测试结果\n");
                            testTimeData = testItem.getTimeData();
                            fsReliabilityTestController.setTimeData(testTimeData);
                            currentStepTextArea.appendText("生成完毕\n");
                        });
                        return null;
                    }
                };

                // 可选：绑定任务属性到UI组件，比如进度条、状态标签等
                fsReliabilityTestController.currentStepTextArea.textProperty().bind(task.messageProperty());
                // 在新线程中执行任务
                new Thread(task).start();
        }

    }

    /**
     * 根据测试项目名称动态加载视图到ScrollPane中，并更新对应的控制器引用
     *
     * @param testProject 测试项目名称
     */
    private void loadTestProjectResultView(String testProject) {
        String fxmlFile;
        Object controller;
        // 确定要加载的视图和控制器
        switch (testProject) {
            case "TPC-C":
            case "TPC-H":
            case "写入性能":
            case "查询性能":
            case "可靠性":
                if (testObjectSelectBox.getValue().equals("GlusterFS") || testObjectSelectBox.getValue().equals("OceanFS")) {
                    fxmlFile = "fsReliabilityTestView.fxml";
                } else {
                    fxmlFile = "dbOtherTestView.fxml";
                }
                break;
            case "适配性":
                fxmlFile = "dbAdaptTestView.fxml";
                break;
            case "读写速度测试":
                fxmlFile = "fsReadWriteTestView.fxml";
                break;
            case "并发度测试":
            case "小文件测试":
                fxmlFile = "fsOtherTestView.fxml";
                break;
            case "可靠性测试":
                fxmlFile = "fsReliabilityTestView.fxml";
                break;
            default:
                System.out.println("Unknown test project: " + testProject);
                return; // 未知的测试项目，直接返回
        }

        // 动态加载视图并设置控制器
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/frontend/" + fxmlFile));
            Node view = loader.load();
            controller = loader.getController();

            // 更新控制器引用
            if (fxmlFile.equals("dbAdaptTestView.fxml")) {
                dbAdaptTestController = (DBAdaptTestController) controller;
            } else if (fxmlFile.equals("dbOtherTestView.fxml")) {
                dbOtherTestController = (DBOtherTestController) controller;
            } else if (fxmlFile.equals("fsReliabilityTestView.fxml")) {
                fsReliabilityTestController = (FSReliabilityTestController) controller;
            } else if (fxmlFile.equals("fsReadWriteTestView.fxml")) {
                fsReadWriteTestController = (FSReadWriteTestController) controller;
            } else if (fxmlFile.equals("fsOtherTestView.fxml")) {
                fsOtherTestController = (FSOtherTestController) controller;
            }

            rightScrollPane.setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // ===============================结果文件导入和导出====================================
    @FXML
    private void exportTestResultClick() {

    }

    @FXML
    private void importTestResultClick() {

    }

    public void closeSSH() {
        if (currentSSHConnection != null && !currentSSHConnection.getStatus()) {
            currentSSHConnection.sshDisconnect();
        }
    }
}
