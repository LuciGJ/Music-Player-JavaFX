package com.example.musicplayer.data;

import javafx.scene.media.Media;


import java.text.SimpleDateFormat;
import java.util.Date;

public class Song {
    private Media file;
    private String title;
    private String duration;

    public Media getFile() {
        return file;
    }

    public void setFile(Media file) {
        this.file = file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = (new SimpleDateFormat("mm:ss")).format(new Date(duration));
    }
}
