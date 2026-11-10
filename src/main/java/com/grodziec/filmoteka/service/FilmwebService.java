package com.grodziec.filmoteka.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FilmwebService {

    private static final String BASE_URL = "https://www.filmweb.pl/api/v1";
    private static final String CDN_URL = "https://fwcdn.pl/fpo";

    private final RestClient restClient;

    public FilmwebService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "Filmoteka/1.0")
                .build();
    }

    /**
     * Wyszukuje filmy po tytule.
     * Jeśli pierwsze zapytanie nie zwróci wyników, próbuje wariantów
     * (np. "dwóch" → "2", bez polskich znaków) aby znaleźć filmy
     * których tytuł różni się od wpisanego przez użytkownika.
     */
    public List<FilmSearchHit> search(String query) {
        List<FilmSearchHit> results = searchExact(query);
        if (!results.isEmpty()) {
            return results;
        }

        // Fallback 1: usuń polskie znaki diakrytyczne
        String normalized = normalizePolish(query);
        if (!normalized.equals(query)) {
            results = searchExact(normalized);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 2: zamień polskie liczby na cyfry ("dwóch" → "2")
        String withNumbers = replacePolishNumbers(query);
        if (!withNumbers.equals(query)) {
            results = searchExact(withNumbers);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 3: normalizacja + cyfry
        String combined = replacePolishNumbers(normalized);
        if (!combined.equals(query)) {
            results = searchExact(combined);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 4: usuń podwójne litery ("Killerów" → "Kilerów")
        String deduped = deduplicateLetters(query);
        if (!deduped.equals(query)) {
            results = searchExact(deduped);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 5: dedup + cyfry ("Killerów dwóch" → "Kilerów 2")
        String dedupWithNumbers = replacePolishNumbers(deduped);
        if (!dedupWithNumbers.equals(query)) {
            results = searchExact(dedupWithNumbers);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 6: dedup + normalizacja + cyfry
        String fullCombined = replacePolishNumbers(normalizePolish(deduped));
        if (!fullCombined.equals(query)) {
            results = searchExact(fullCombined);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 7: tylko pierwsze 3 słowa (krótsze zapytanie)
        String shortQuery = takeFirstWords(query, 3);
        if (!shortQuery.equals(query)) {
            results = searchExact(shortQuery);
            if (!results.isEmpty()) {
                return results;
            }
        }

        // Fallback 8: krótsze + dedup + cyfry
        String shortDedup = replacePolishNumbers(deduplicateLetters(shortQuery));
        if (!shortDedup.equals(query)) {
            results = searchExact(shortDedup);
            if (!results.isEmpty()) {
                return results;
            }
        }

        return List.of();
    }

    private List<FilmSearchHit> searchExact(String query) {
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("type", "film")
                        .queryParam("limit", 5)
                        .build())
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hits = (List<Map<String, Object>>) response.get("searchHits");
        if (hits == null) {
            return List.of();
        }

        return hits.stream()
                .map(this::toSearchHit)
                .collect(Collectors.toList());
    }

    /**
     * Usuwa polskie znaki diakrytyczne, zamieniając je na ASCII.
     * Np. "Killerów dwóch" → "Killerow dwoch"
     */
    private String normalizePolish(String text) {
        return text
                .replace("ą", "a").replace("Ą", "A")
                .replace("ć", "c").replace("Ć", "C")
                .replace("ę", "e").replace("Ę", "E")
                .replace("ł", "l").replace("Ł", "L")
                .replace("ń", "n").replace("Ń", "N")
                .replace("ó", "o").replace("Ó", "O")
                .replace("ś", "s").replace("Ś", "S")
                .replace("ź", "z").replace("Ź", "Z")
                .replace("ż", "z").replace("Ż", "Z");
    }

    /**
     * Usuwa podwójne litery, które często pojawiają się w literówkach.
     * Np. "Killerów" → "Kilerów", "Matrixx" → "Matrix"
     */
    private String deduplicateLetters(String text) {
        return text.replaceAll("(.)\\1{1,}", "$1");
    }

    /**
     * Zamienia polskie liczby porządkowe na cyfry.
     * Np. "dwóch" → "2", "trzech" → "3"
     */
    private String replacePolishNumbers(String text) {
        return text
                .replaceAll("(?i)\\bdwóch\\b", "2")
                .replaceAll("(?i)\\bdwojga\\b", "2")
                .replaceAll("(?i)\\btrzech\\b", "3")
                .replaceAll("(?i)\\btrzy\\b", "3")
                .replaceAll("(?i)\\bczterech\\b", "4")
                .replaceAll("(?i)\\bcztery\\b", "4")
                .replaceAll("(?i)\\bpieciu\\b", "5")
                .replaceAll("(?i)\\bpięciu\\b", "5")
                .replaceAll("(?i)\\bpięć\\b", "5")
                .replaceAll("(?i)\\bsześciu\\b", "6")
                .replaceAll("(?i)\\bsześć\\b", "6")
                .replaceAll("(?i)\\bsiedmiu\\b", "7")
                .replaceAll("(?i)\\bsiedem\\b", "7")
                .replaceAll("(?i)\\bośmiu\\b", "8")
                .replaceAll("(?i)\\bosem\\b", "8")
                .replaceAll("(?i)\\bdziewieciu\\b", "9")
                .replaceAll("(?i)\\bdziewięć\\b", "9")
                .replaceAll("(?i)\\bdziesięciu\\b", "10")
                .replaceAll("(?i)\\bdziesięć\\b", "10");
    }

    /**
     * Bierze tylko pierwsze N słów zapytania.
     * Krótsze zapytanie ma większą szansę na dopasowanie.
     */
    private String takeFirstWords(String text, int count) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= count) {
            return text;
        }
        return String.join(" ", java.util.Arrays.copyOfRange(words, 0, count));
    }

    /**
     * Pobiera szczegóły filmu po ID z Filmweb.
     */
    public FilmDetails getDetails(long filmId) {
        Map<String, Object> response = restClient.get()
                .uri("/film/{id}/preview", filmId)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            return null;
        }

        FilmDetails details = new FilmDetails();
        details.setId(filmId);

        @SuppressWarnings("unchecked")
        Map<String, Object> titleObj = (Map<String, Object>) response.get("title");
        details.setTitle(titleObj != null ? (String) titleObj.get("title") : "");

        details.setYear(((Number) response.get("year")).intValue());

        // Directors
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> directors = (List<Map<String, Object>>) response.get("directors");
        if (directors != null && !directors.isEmpty()) {
            List<String> names = directors.stream()
                    .map(d -> (String) d.get("name"))
                    .collect(Collectors.toList());
            details.setDirector(String.join(", ", names));
        }

        // Genres
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> genres = (List<Map<String, Object>>) response.get("genres");
        if (genres != null && !genres.isEmpty()) {
            List<String> genreNames = genres.stream()
                    .map(g -> {
                        Map<String, Object> nameObj = (Map<String, Object>) g.get("name");
                        return nameObj != null ? (String) nameObj.get("text") : "";
                    })
                    .collect(Collectors.toList());
            details.setGenre(String.join(", ", genreNames));
        }

        // Synopsis / description
        @SuppressWarnings("unchecked")
        Map<String, Object> plot = (Map<String, Object>) response.get("plot");
        if (plot != null) {
            details.setDescription((String) plot.get("synopsis"));
        } else {
            // Fallback: plotOrDescriptionSynopsis field
            details.setDescription((String) response.get("plotOrDescriptionSynopsis"));
        }

        // Poster URL
        @SuppressWarnings("unchecked")
        Map<String, Object> poster = (Map<String, Object>) response.get("poster");
        if (poster != null) {
            String path = (String) poster.get("path");
            if (path != null && !path.isEmpty()) {
                // Replace $ with size number (3 = medium)
                String cleanPath = path.replace("$", "3");
                details.setPosterUrl(CDN_URL + cleanPath);
            }
        }

        return details;
    }

    private FilmSearchHit toSearchHit(Map<String, Object> hit) {
        FilmSearchHit result = new FilmSearchHit();
        result.setId(((Number) hit.get("id")).longValue());
        result.setTitle((String) hit.get("matchedTitle"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cast = (List<Map<String, Object>>) hit.get("filmMainCast");
        if (cast != null && !cast.isEmpty()) {
            List<String> names = cast.stream()
                    .map(c -> (String) c.get("name"))
                    .collect(Collectors.toList());
            result.setMainCast(String.join(", ", names));
        }

        return result;
    }

    // ── DTO classes ──

    public static class FilmSearchHit {
        private long id;
        private String title;
        private String mainCast;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMainCast() { return mainCast; }
        public void setMainCast(String mainCast) { this.mainCast = mainCast; }
    }

    public static class FilmDetails {
        private long id;
        private String title;
        private int year;
        private String director;
        private String genre;
        private String description;
        private String posterUrl;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public String getDirector() { return director; }
        public void setDirector(String director) { this.director = director; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPosterUrl() { return posterUrl; }
        public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    }
}
