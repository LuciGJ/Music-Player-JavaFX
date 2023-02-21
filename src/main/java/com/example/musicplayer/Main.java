package com.example.musicplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 410, 550);
        stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResourceAsStream("applicationicon.png"))));
        scene.getStylesheets().add(Objects.requireNonNull(Main.class.getResource("style.css")).toExternalForm());
        stage.setTitle("Music player");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}