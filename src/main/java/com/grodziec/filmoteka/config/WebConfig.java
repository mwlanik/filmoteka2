package com.grodziec.filmoteka.config;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

@Controller
public class WebConfig {

    private static final Path POSTERS_DIR = Path.of("data", "posters").toAbsolutePath().normalize();
    private static final Path VIDEOS_DIR = Path.of("data", "film").toAbsolutePath().normalize();

    // Mapowanie rozszerzeń plików do MIME types
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put(".mp4", "video/mp4");
        MIME_TYPES.put(".mkv", "video/x-matroska");
        MIME_TYPES.put(".avi", "video/x-msvideo");
        MIME_TYPES.put(".mov", "video/quicktime");
        MIME_TYPES.put(".webm", "video/webm");
        MIME_TYPES.put(".flv", "video/x-flv");
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".jpeg", "image/jpeg");
        MIME_TYPES.put(".png", "image/png");
        MIME_TYPES.put(".gif", "image/gif");
        MIME_TYPES.put(".webp", "image/webp");
    }

    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> getVideo(@PathVariable String filename) {
        // Spring automatycznie URL-dekoduje @PathVariable
        Path filePath = VIDEOS_DIR.resolve(filename).normalize();
        if (!filePath.startsWith(VIDEOS_DIR)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String ext = getExtension(filename);
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/posters/{filename:.+}")
    public ResponseEntity<Resource> getPoster(@PathVariable String filename) {
        Path filePath = POSTERS_DIR.resolve(filename).normalize();
        if (!filePath.startsWith(POSTERS_DIR)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String ext = getExtension(filename);
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                .body(resource);
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
