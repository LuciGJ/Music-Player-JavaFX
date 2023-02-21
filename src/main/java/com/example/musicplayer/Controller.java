package com.example.musicplayer;

import com.example.musicplayer.data.Song;
import com.example.musicplayer.file.DirectoryFileVisitor;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Callback;
import javafx.util.Duration;

public class Controller {

    private final ObservableList<Song> songs = FXCollections.observableArrayList();
    private final List<Song> playedSongs = new ArrayList<>();
    private Song currentSong;

    private MediaPlayer mediaPlayer;

    private final SimpleStringProperty timeString = new SimpleStringProperty();

    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Slider volumeSlider;

    @FXML
    private Button newFile;

    @FXML
    private Button pause;

    @FXML
    private ToggleButton repeat;

    @FXML
    private ToggleButton shuffle;

    @FXML
    private TableView<Song> songsList;

    @FXML
    private Label volumeLabel;

    @FXML
    private Slider timeSlider;

    @FXML
    private Label timeLabel;

    @FXML
    private ContextMenu contextMenu;

    @FXML
    private Label nowPlayingLabel;

    @FXML
    private ImageView volumeImage;
    private boolean isMute;

    public void setTimeString() {
        if (mediaPlayer == null) {
            timeString.set("0:00 / 0:00");
        } else {
            String current = new SimpleDateFormat("mm:ss").format(new Date((long) mediaPlayer.getCurrentTime().toMillis()));
            String total = new SimpleDateFormat("mm:ss").format(new Date((long) mediaPlayer.getStopTime().toMillis()));
            timeString.set(current + "/" + total);
        }
    }

    private boolean isPaused;


    public void initialize() {
        songsList.setPlaceholder(new Label("No songs added"));
        TableColumn<Song, String> numberCol = new TableColumn<>("Track");
        numberCol.setPrefWidth(0.2);
        numberCol.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Song, String> call(TableColumn p) {
                return new TableCell<>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(null);
                        setText(empty ? null : getIndex() + 1 + "");
                    }
                };
            }
        });
        numberCol.setMaxWidth(50);
        songsList.getColumns().add(0, numberCol);
        volumeSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> volumeLabel.textProperty().setValue(String.format("%.0f", newValue.doubleValue() * 100)));
        volumeSlider.setValue(1);


        setTimeString();
        timeLabel.textProperty().bind(timeString);

        timeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                if (mediaPlayer != null) {
                    mediaPlayer.seek(Duration.seconds(timeSlider.getValue()));
                }
            }
        });

        timeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!timeSlider.isValueChanging()) {
                if (mediaPlayer != null) {
                    double currentTime = mediaPlayer.getCurrentTime().toSeconds();
                    if (Math.abs(currentTime - newValue.doubleValue()) > 0.5) {
                        mediaPlayer.seek(Duration.seconds(newValue.doubleValue()));
                    }
                } else {
                    timeSlider.setValue(0);
                }


            }
        });
        songsList.setItems(songs);
        contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(actionEvent -> {
            Song item = songsList.getSelectionModel().getSelectedItem();
            deleteItem(item);
        });
        contextMenu.getItems().setAll(deleteItem);
        songsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        songsList.setRowFactory(tableView -> {
            final TableRow<Song> row = new TableRow<>();
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(contextMenu));

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Song rowData = row.getItem();
                    playSong(rowData);
                }
            });

            return row;
        });


    }

    @FXML
    public void handleKeyPressed(KeyEvent event) {
        Song selectedItem = songsList.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            if (event.getCode().equals(KeyCode.DELETE)) {
                deleteItem(selectedItem);
            } else if (event.getCode().equals(KeyCode.ENTER)) {
                playSong(selectedItem);
            }
        }
    }

    private void deleteItem(Song item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete item");
        alert.setHeaderText("Delete item " + item.getTitle());
        alert.setContentText("Are you sure? Use OK to confirm or Cancel to stop");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int itemIndex = songs.indexOf(item);
            songs.remove(item);
            playedSongs.remove(item);
            if (item == currentSong && mediaPlayer != null) {
                mediaPlayer.stop();
                if (itemIndex < songs.size() && itemIndex >= 0) {
                    playSong(songs.get(itemIndex));
                }
            }
        }

    }

    @FXML
    public void chooseSongs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select songs");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Audio files", "*.mp3", "*.wav");
        fileChooser.getExtensionFilters().add(filter);
        List<File> files = fileChooser.showOpenMultipleDialog(newFile.getScene().getWindow());
        if (files != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call()  {
                    progressIndicator.setVisible(true);
                    files.forEach(file -> {
                        Media song = new Media(file.toURI().toString());
                        MediaPlayer songPlayer = new MediaPlayer(song);
                        songPlayer.setOnReady(() -> {
                            Song tempSong = new Song();
                            tempSong.setFile(song);
                            tempSong.setTitle(file.getName());
                            tempSong.setDuration((long) song.getDuration().toMillis());
                            songs.add(tempSong);
                            songs.sort(Comparator.comparing(Song::getTitle));
                        });
                    });
                    return null;
                }
            };
           task.setOnSucceeded((e) -> progressIndicator.setVisible(false));

            task.setOnFailed((e) -> progressIndicator.setVisible(false));

            new Thread(task).start();

        }

    }

    @FXML
    public void chooseSongsDirectory() throws IOException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File selectedDirectory = directoryChooser.showDialog(newFile.getScene().getWindow());
        if (selectedDirectory != null) {
            Path directoryPath = Paths.get(selectedDirectory.getPath());
            DirectoryFileVisitor directoryFileVisitor = new DirectoryFileVisitor();
            Files.walkFileTree(directoryPath, directoryFileVisitor);
            if (directoryFileVisitor.getFoundFiles() != null) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call()  {
                        directoryFileVisitor.getFoundFiles().forEach(file -> {
                            Media song = new Media(file.toURI().toString());
                            MediaPlayer songPlayer = new MediaPlayer(song);
                            songPlayer.setOnReady(() -> {
                                Song tempSong = new Song();
                                tempSong.setFile(song);
                                tempSong.setTitle(file.getName());
                                tempSong.setDuration((long) song.getDuration().toMillis());
                                songs.add(tempSong);
                                songs.sort(Comparator.comparing(Song::getTitle));
                            });
                        });
                        return null;
                    }
                };
                task.setOnSucceeded((e) -> progressIndicator.setVisible(false));

                task.setOnFailed((e) -> progressIndicator.setVisible(false));
                new Thread(task).start();

            }

        }
    }

    public void playSong(Song song) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        mediaPlayer = new MediaPlayer(song.getFile());
        mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty());
        mediaPlayer.currentTimeProperty().addListener((ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) -> {
            if (!timeSlider.isValueChanging()) {
                timeSlider.setValue(newValue.toSeconds());
                setTimeString();
            }
        });

        timeSlider.setMax(song.getFile().getDuration().toSeconds());
        mediaPlayer.setOnReady(() -> {
            if (!playedSongs.contains(song)) {
                playedSongs.add(song);
            }
            currentSong = song;
            songsList.getSelectionModel().select(song);
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("pausebutton.png")));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(25);
            imageView.setFitHeight(20);
            pause.setGraphic(imageView);
            nowPlayingLabel.setText(song.getTitle());
            mediaPlayer.setMute(isMute);
            mediaPlayer.play();
        });
        mediaPlayer.setOnEndOfMedia(this::nextSong);

    }

    @FXML
    public void toggleRepeat() {
        if (shuffle.isSelected()) {
            shuffle.setSelected(false);
        }
    }

    @FXML
    public void toggleShuffle() {
        if (repeat.isSelected()) {
            repeat.setSelected(false);
        }
    }

    @FXML
    public void nextSong() {
        int currentIndex = songs.indexOf(currentSong);
        if (songs.isEmpty()) {
            return;
        }
        if (repeat.isSelected()) {
            if (!songs.isEmpty()) {
                playSong(currentSong);
            }
        } else if (shuffle.isSelected()) {
            if (!songs.isEmpty()) {
                if (playedSongs.size() == songs.size()) {
                    playedSongs.clear();
                }
                int nextInt;
                do {
                    nextInt = ThreadLocalRandom.current().nextInt(0, songs.size());
                }
                while (playedSongs.contains(songs.get(nextInt)));
                playSong(songs.get(nextInt));
            }

        } else {
            if (++currentIndex < songs.size()) {
                playSong(songs.get(currentIndex));
            } else {
                playSong(songs.get(0));
            }
        }
    }

    @FXML
    public void previousSong() {
        int currentIndex = songs.indexOf(currentSong);
        if (songs.isEmpty()) {
            return;
        }
        if (repeat.isSelected()) {
            if (!songs.isEmpty()) {
                playSong(currentSong);
            }
        } else if (shuffle.isSelected()) {
            if (!songs.isEmpty()) {
                if (playedSongs.size() == songs.size()) {
                    playedSongs.clear();
                }
                int nextInt;
                do {
                    nextInt = ThreadLocalRandom.current().nextInt(0, songs.size());
                }
                while (playedSongs.contains(songs.get(nextInt)));
                playSong(songs.get(nextInt));
            }

        } else {
            if (--currentIndex < 0) {
                playSong(songs.get(songs.size() - 1));
            } else {
                playSong(songs.get(currentIndex));
            }
        }
    }

    @FXML
    public void pauseSong() {
        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("pausebutton.png")));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(25);
        imageView.setFitHeight(20);

        Image imagePlay = new Image(Objects.requireNonNull(getClass().getResourceAsStream("playbutton.png")));
        ImageView imageViewPlay = new ImageView(imagePlay);
        imageViewPlay.setFitWidth(25);
        imageViewPlay.setFitHeight(20);

        if (mediaPlayer != null) {
            if (isPaused) {
                mediaPlayer.play();
                isPaused = false;
                pause.setGraphic(imageView);
            } else {
                mediaPlayer.pause();
                isPaused = true;
                pause.setGraphic(imageViewPlay);
            }
        } else {
            Song selected = songsList.getSelectionModel().getSelectedItem();
            if (!songs.isEmpty()) {
                pause.setGraphic(imageView);
                if (selected == null) {
                    playSong(songs.get(0));
                } else {
                    playSong(selected);
                }
            }
        }
    }

    @FXML
    public void tenSecondsNext() {
        if (mediaPlayer != null) {
            Duration current = mediaPlayer.getCurrentTime();
            double currentTime = current.toSeconds();
            if (currentTime + 10D < mediaPlayer.getCycleDuration().toSeconds()) {
                mediaPlayer.seek(Duration.seconds(currentTime + 10D));
            } else {
                mediaPlayer.seek(mediaPlayer.getStopTime());
            }
        }
    }

    @FXML
    public void tenSecondsBack() {
        if (mediaPlayer != null) {
            Duration current = mediaPlayer.getCurrentTime();
            double currentTime = current.toSeconds();
            if (currentTime - 10D > 0D) {
                mediaPlayer.seek(Duration.seconds(currentTime - 10D));
            } else {
                mediaPlayer.seek(Duration.seconds(0D));
            }
        }
    }

    @FXML
    public void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer = null;
            Image imagePlay = new Image(Objects.requireNonNull(getClass().getResourceAsStream("playbutton.png")));
            ImageView imageViewPlay = new ImageView(imagePlay);
            imageViewPlay.setFitWidth(25);
            imageViewPlay.setFitHeight(20);
            pause.setGraphic(imageViewPlay);
            nowPlayingLabel.setText("No song playing");
        }
    }

    @FXML
    public void muteSong() {
        if (isMute) {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("volumeicon.png")));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(20);
            imageView.setFitHeight(20);
            volumeImage.setImage(image);
            isMute = false;
        } else {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("volumestoppedicon.png")));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(20);
            imageView.setFitHeight(20);
            volumeImage.setImage(image);
            isMute = true;
        }
        if (mediaPlayer != null) {
            mediaPlayer.setMute(isMute);
        }

    }

}