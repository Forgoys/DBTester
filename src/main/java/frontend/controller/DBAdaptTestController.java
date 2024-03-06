package frontend.controller;

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
        // 检查数据库连接
        if (MainAppController.currentDBConnection == null || !MainAppController.currentDBConnection.isConnected()) {
            sqlOutputTextArea.setText("数据库未连接，请先连接数据库。");
            return;
        }
        // 执行SQL语句并展示结果
        String result = MainAppController.currentDBConnection.executeSQL(sqlText);
        sqlOutputTextArea.setText(result);
    }
}
