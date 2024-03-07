module org.example.dbtester {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires jsch;
    requires java.sql;
    requires org.apache.commons.csv;
//    requires rt;
//    requires jsch;

    opens frontend to javafx.fxml;
    exports frontend;
    exports frontend.connection;
    opens frontend.connection to javafx.fxml;
    exports frontend.controller;
    opens frontend.controller to javafx.fxml;
    opens backend.dataset to javafx.base;
}