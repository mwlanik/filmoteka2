package com.grodziec.filmoteka.controller;

import com.grodziec.filmoteka.model.Film;
import com.grodziec.filmoteka.service.FilmStorageService;
import com.grodziec.filmoteka.service.FilmwebService;
import com.grodziec.filmoteka.service.PosterService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class FilmController {

    private final FilmStorageService filmStorageService;
    private final PosterService posterService;
    private final FilmwebService filmwebService;

    public FilmController(FilmStorageService filmStorageService, PosterService posterService, FilmwebService filmwebService) {
        this.filmStorageService = filmStorageService;
        this.posterService = posterService;
        this.filmwebService = filmwebService;
    }

    @GetMapping("/")
    public String listaFilmow(Model model) {
        model.addAttribute("filmy", filmStorageService.getFilms());
        return "index";
    }

    @GetMapping("/add")
    public String formularzDodawania(Model model) {
        model.addAttribute("film", new Film());
        return "add";
    }

    @PostMapping("/add")
    public String dodajFilm(
            @RequestParam String title,
            @RequestParam int year,
            @RequestParam String director,
            @RequestParam String genre,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String posterUrl) {

        Film film = new Film();
        film.setTitle(title);
        film.setYear(year);
        film.setDirector(director);
        film.setGenre(genre);
        film.setDescription(description != null ? description : "");

        // Najpierw zapisz film, aby uzyskać ID
        long id = filmStorageService.addFilm(film);

        // Pobierz plakat z URL i zapisz lokalnie (jeśli podano URL)
        if (posterUrl != null && !posterUrl.isBlank()) {
            String localPoster = posterService.downloadAndSave(id, posterUrl);
            film.setPosterUrl(localPoster != null ? localPoster : posterUrl);
        } else {
            film.setPosterUrl(generatePlaceholder(title));
        }

        // Zaktualizuj rekord z ostatecznym posterUrl
        filmStorageService.saveFilm(film);

        return "redirect:/";
    }

    // ── Filmweb API endpoints ──

    @GetMapping("/api/search")
    @ResponseBody
    public List<FilmwebService.FilmSearchHit> searchFilms(@RequestParam String q) {
        return filmwebService.search(q);
    }

    @GetMapping("/api/film/{id}")
    @ResponseBody
    public ResponseEntity<FilmwebService.FilmDetails> getFilmDetails(@PathVariable long id) {
        FilmwebService.FilmDetails details = filmwebService.getDetails(id);
        if (details == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(details);
    }

    // ── Delete film ──

    @PostMapping("/delete/{id}")
    public String usunFilm(@PathVariable long id) {
        filmStorageService.removeFilm(id);
        return "redirect:/";
    }

    private String generatePlaceholder(String title) {
        return "https://placehold.co/500x750/1f1f1f/e5e5e5?text=" + title;
    }
}
