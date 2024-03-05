package frontend.controller;

import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class DBOtherTestController {
    @FXML
    ProgressBar progressBar;
    @FXML
    Label progressPercentLabel;
    @FXML
    Label currentStepLabel;

    public DBOtherTestController() { }


}
