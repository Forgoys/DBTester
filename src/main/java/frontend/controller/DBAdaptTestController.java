package frontend.controller;

import frontend.connection.DBConnection;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;


public class DBAdaptTestController {
    @FXML
    TextArea sqlTextField;
    @FXML
    TextArea sqlOutputTextArea;

    public DBAdaptTestController() {}

    public void onExecuteSqlButtonClick(ActionEvent actionEvent) {
        // 清空输出文本区域
        sqlOutputTextArea.clear();
        // 获取SQL文本
        String sqlText = sqlTextField.getText().trim();
        // 检查SQL文本是否为空
        if (sqlText.isEmpty()) {
            sqlOutputTextArea.setText("SQL语句不能为空！");
            return;
        }

        String testObject = MainAppController.getTestObject();

        if (testObject == null || testObject.equals("GlustFS") || testObject.equals("OceanFS")) {
            Util.popUpInfo("数据库未正确选择。", "错误");
            return;
        }

        // 显示正在执行SQL语句的消息
        sqlOutputTextArea.setText("正在执行SQL语句，请稍候...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                if (testObject.equals("PolarDB") || testObject.equals("神通数据库") || testObject.equals("OpenGauss")) {
                    if (MainAppController.currentDBConnection == null || !MainAppController.currentDBConnection.isConnected()) {
                        Platform.runLater(() -> Util.popUpInfo("数据库连接未建立或已断开，请检查连接设置。", "错误"));
                        return null;
                    }
                    // 执行SQL语句并返回结果
                    // 把之前建的测试表删掉
                    MainAppController.currentDBConnection.executeSQL("DROP TABLE IF EXISTS DataTypeSupportTest;");
                    return MainAppController.currentDBConnection.executeSQL(sqlText);
                } else if (testObject.equals("TDengine")){
                    DBConnection.tdengineExecSQL("DROP TABLE IF EXISTS DataTypeSupportTest;", MainAppController.currentDBConnection);
                    return DBConnection.tdengineExecSQL(sqlText, MainAppController.currentDBConnection);
                }
                return "未知的测试对象类型。";
            }

            @Override
            protected void updateValue(String result) {
                // 此方法在UI线程中调用，可以安全地更新UI
                if (result != null) {
                    sqlOutputTextArea.setText(result);
                }
            }
        };

        // 启动任务
        new Thread(task).start();

//        if (testObject.equals("PolarDB") || testObject.equals("神通数据库") || testObject.equals("OpenGauss")) {
//            if (MainAppController.currentDBConnection == null || !MainAppController.currentDBConnection.isConnected()) {
//                Util.popUpInfo("数据库未连接，请先连接数据库。", "错误");
//                return;
//            }
//            // 执行SQL语句并展示结果
//            String result = MainAppController.currentDBConnection.executeSQL(sqlText);
//            sqlOutputTextArea.setText(result);
//        } else if (testObject.equals("TDengine")){
//            String result = DBConnection.tdengineExecSQL(sqlText);
//            sqlOutputTextArea.setText(result);
//        }
        // 检查数据库连接
    }
}
