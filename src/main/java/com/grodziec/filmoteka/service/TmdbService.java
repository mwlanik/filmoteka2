package com.grodziec.filmoteka.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    private static final String BASE_URL = "https://api.themoviedb.org/3";

    private final String apiKey;

    public TmdbService(@Value("${tmdb.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Wyszukuje film w TMDb po tytule i zwraca pierwszy wynik.
     */
    public TmdbSearchResult searchMovie(String title) {
        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String url = BASE_URL + "/search/movie?query=" + encoded + "&api_key=" + apiKey + "&language=pl-PL";
        Map<String, Object> response = fetchJson(url);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            return null;
        }
        Map<String, Object> first = results.get(0);
        TmdbSearchResult result = new TmdbSearchResult();
        result.setId(((Number) first.get("id")).longValue());
        result.setTitle((String) first.get("title"));
        result.setReleaseDate((String) first.get("release_date"));
        return result;
    }

    /**
     * Pobiera listę serwisów streamingowych (tylko subskrypcje/flatrate) dla filmu z TMDb.
     */
    public List<TmdbWatchProvider> getWatchProviders(long tmdbMovieId, String region) {
        String url = BASE_URL + "/movie/" + tmdbMovieId + "/watch/providers?api_key=" + apiKey;
        Map<String, Object> response = fetchJson(url);
        Map<String, Object> results = (Map<String, Object>) response.get("results");
        if (results == null) {
            return List.of();
        }
        Map<String, Object> countryData = (Map<String, Object>) results.get(region);
        if (countryData == null) {
            return List.of();
        }
        List<Map<String, Object>> flatrate = (List<Map<String, Object>>) countryData.get("flatrate");
        if (flatrate == null) {
            return List.of();
        }
        return flatrate.stream().map(provider -> {
            TmdbWatchProvider wp = new TmdbWatchProvider();
            wp.setProviderName((String) provider.get("provider_name"));
            wp.setLogoPath((String) provider.get("logo_path"));
            wp.setProviderId(((Number) provider.get("provider_id")).intValue());
            return wp;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> fetchJson(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return parseJson(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Błąd połączenia z TMDb API: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            // Używamy prostego parsera JSON przez java.net.http + Jackson
            // Fallback: zwróć pustą mapę jeśli parsing się nie uda
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ── DTO classes ──

    public static class TmdbSearchResult {
        private long id;
        private String title;
        private String releaseDate;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    }

    public static class TmdbWatchProvider {
        private String providerName;
        private String logoPath;
        private int providerId;

        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }
        public String getLogoPath() { return logoPath; }
        public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
        public int getProviderId() { return providerId; }
        public void setProviderId(int providerId) { this.providerId = providerId; }
    }
}
