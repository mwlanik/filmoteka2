package com.grodziec.filmoteka.service;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class PosterService {

    private static final String POSTERS_DIR = "data/posters";
    private final Path postersPath;

    public PosterService() {
        this.postersPath = Path.of(POSTERS_DIR);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(postersPath);
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się utworzyć folderu plakatów", e);
        }
    }

    /**
     * Pobiera plakat z URL i zapisuje jako lokalny plik.
     * Zwraca ścieżkę dostępną przez przeglądarkę, np. /posters/1.jpg
     */
    public String downloadAndSave(long filmId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        // Determine file extension from URL or default to .jpg
        String extension = extractExtension(imageUrl);
        String fileName = filmId + extension;
        Path targetFile = postersPath.resolve(fileName);

        // If file already exists, return the local path
        if (Files.exists(targetFile)) {
            return "/posters/" + fileName;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "Filmoteka/1.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try (InputStream in = response.body()) {
                    Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return "/posters/" + fileName;
            }
        } catch (Exception e) {
            // Download failed — return null, controller will use placeholder
        }

        return null;
    }

    private String extractExtension(String url) {
        int lastDot = url.lastIndexOf('.');
        int lastSlash = url.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot < url.length() - 1) {
            String ext = url.substring(lastDot).split("[?#]")[0];
            if (ext.length() <= 6) {
                return ext.toLowerCase();
            }
        }
        return ".jpg";
    }
}
