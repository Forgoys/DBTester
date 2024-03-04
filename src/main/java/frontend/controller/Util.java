package frontend.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

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

}
