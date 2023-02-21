package com.example.musicplayer.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFileVisitor implements FileVisitor<Object> {
    private final List<File> foundFiles;

    public DirectoryFileVisitor() {
        foundFiles = new ArrayList<>();
    }

    public boolean checkExtension(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            extension = fileName.substring(i + 1);
        }
        return extension.equals("wav") || extension.equals("mp3");
    }

    public void checkFile(Object file) {
        if (file instanceof Path) {
            File newFile = ((Path) file).toFile();
            if (checkExtension(newFile.toString())) {
                foundFiles.add(newFile);
            }
        }

    }

    @Override
    public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) {
        checkFile(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Object file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Object dir, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    public List<File> getFoundFiles() {
        return foundFiles;
    }
}
