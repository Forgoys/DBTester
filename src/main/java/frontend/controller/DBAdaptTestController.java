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

        // ??????SQL?????
        sqlOutputTextArea.setText("????SQL??????...");

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                if (testObject.equals("PolarDB") || testObject.equals("???????") || testObject.equals("OpenGauss")) {
                    if (MainAppController.currentDBConnection == null || !MainAppController.currentDBConnection.isConnected()) {
                        Platform.runLater(() -> Util.popUpInfo("?????????????????????", "??"));
                        return null;
                    }
                    // ??SQL???????
                    return MainAppController.currentDBConnection.executeSQL(sqlText);
                } else if (testObject.equals("TDengine")){
                    return DBConnection.tdengineExecSQL(sqlText);
                }
                return "??????????";
            }

            @Override
            protected void updateValue(String result) {
                // ????UI?????????????UI
                if (result != null) {
                    sqlOutputTextArea.setText(result);
                }
            }
        };

        // ????
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
