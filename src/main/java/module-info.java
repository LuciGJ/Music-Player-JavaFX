module com.example.musicplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.media;


    opens com.example.musicplayer to javafx.fxml, javafx.base;
    opens com.example.musicplayer.data to javafx.base;
    exports com.example.musicplayer;
    exports com.example.musicplayer.data;
    exports com.example.musicplayer.file;
}