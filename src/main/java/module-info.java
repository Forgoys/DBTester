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
    requires rt;
//    requires jsch;

    opens org.example.dbtester to javafx.fxml;
    exports org.example.dbtester;
}