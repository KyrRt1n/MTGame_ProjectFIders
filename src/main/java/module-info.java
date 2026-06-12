module ua.fiders {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.fasterxml.jackson.databind;
    requires org.java_websocket;

    exports ua.fiders.ui;
    exports ua.fiders.data;
}