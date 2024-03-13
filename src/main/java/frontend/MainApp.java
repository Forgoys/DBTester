package frontend;

import frontend.controller.MainAppController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 1000);
        MainAppController controller = fxmlLoader.getController();

//        Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/SourceHanSansTC-Normal.ttf"), 13);
//        System.out.println(font);
        stage.setScene(scene);
        stage.setTitle("国产数据库与文件系统测试原型软件");
        scene.getStylesheets().add(getClass().getResource("/frontend/css/font.css").toExternalForm());
        Image appIcon = new Image(MainApp.class.getResourceAsStream("/icons/appIcon1024.png"));
        stage.getIcons().add(appIcon);

        stage.setOnCloseRequest(event -> controller.closeAll());

        stage.show();
    }
}