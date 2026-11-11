package com.grodziec.filmoteka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.grodziec.filmoteka.model.Film;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class FilmStorageService {

    private static final String DATA_DIR = "data";
    private static final String FILMS_FILE = DATA_DIR + "/films.json";
    private static final String FILMS_VIDEO_DIR = DATA_DIR + "/film";

    private final ObjectMapper objectMapper;
    private final Path filePath;
    private final AtomicLong nextId;
    private List<Film> filmy;

    public FilmStorageService() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        this.filePath = Path.of(FILMS_FILE);
        this.nextId = new AtomicLong(1L);
        this.filmy = Collections.emptyList();
    }

    @PostConstruct
    public void init() {
        load();
    }

    public synchronized void load() {
        File file = filePath.toFile();
        if (!file.exists() || file.length() == 0) {
            filmy = Collections.emptyList();
            nextId.set(1L);
            return;
        }

        try {
            Film[] loaded = objectMapper.readValue(file, Film[].class);
            filmy = List.of(loaded);
            if (!filmy.isEmpty()) {
                long maxId = filmy.stream()
                        .mapToLong(Film::getId)
                        .max()
                        .orElse(0L);
                nextId.set(maxId + 1);
            }
        } catch (IOException e) {
            filmy = Collections.emptyList();
            nextId.set(1L);
        }
    }

    public List<Film> getFilms() {
        return filmy;
    }

    public synchronized long addFilm(Film film) {
        long id = nextId.getAndIncrement();
        film.setId(id);

        List<Film> updated = new java.util.ArrayList<>(filmy);
        updated.add(film);
        filmy = Collections.unmodifiableList(updated);

        save();
        return id;
    }

    public synchronized void removeFilm(long id) {
        List<Film> updated = filmy.stream()
                .filter(f -> !f.getId().equals(id))
                .toList();
        filmy = Collections.unmodifiableList(updated);
        save();
    }

    public synchronized void saveFilm(Film film) {
        List<Film> updated = filmy.stream()
                .map(f -> f.getId().equals(film.getId()) ? film : f)
                .toList();
        filmy = Collections.unmodifiableList(updated);
        save();
    }

    public Optional<Film> getFilmById(long id) {
        return filmy.stream()
                .filter(f -> f.getId().equals(id))
                .findFirst();
    }

    public List<String> getAvailableVideoFiles() {
        File dir = new File(FILMS_VIDEO_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        String[] extensions = {".mp4", ".mkv", ".avi", ".mov", ".wmv", ".webm", ".flv"};
        File[] files = dir.listFiles(f -> {
            String name = f.getName().toLowerCase();
            return Arrays.stream(extensions).anyMatch(name::endsWith);
        });
        if (files == null) return Collections.emptyList();
        return Arrays.stream(files)
                .map(File::getName)
                .sorted()
                .toList();
    }

    private void save() {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writeValue(filePath.toFile(), filmy.toArray(new Film[0]));
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się zapisać filmów do JSON", e);
        }
    }
}
