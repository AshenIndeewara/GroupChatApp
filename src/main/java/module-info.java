module lk.ijse.chatapp02 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens lk.ijse.chatapp02.controller to javafx.fxml;
    exports lk.ijse.chatapp02;
}