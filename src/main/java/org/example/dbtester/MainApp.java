package org.example.dbtester;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        scene.getStylesheets().add(getClass().getResource("/fonts/font.css").toExternalForm());
        stage.setTitle("国产数据库与文件系统测试评估软件");
        Image appIcon = new Image(MainApp.class.getResourceAsStream("/icons/appIcon32.jpg"));
        stage.getIcons().add(appIcon);

//        Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/SourceHanSansTC-Normal.ttf"), 13);
//        System.out.println(font);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}