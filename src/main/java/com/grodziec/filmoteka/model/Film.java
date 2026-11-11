package com.grodziec.filmoteka.model;

import lombok.Data;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Data
public class Film {
   private Long id;
   private String title;
   private int year;
   private String director;
   private String genre;
   private String description;
   private String posterUrl;
   private String videoFile;

   /**
    * Gotowy URL do odtwarzania pliku wideo, z zakodowaną nazwą pliku.
    * Używany w szablonach Thymeleaf, gdzie dostęp do statycznych klas jest zablokowany.
    */
   public String getVideoUrl() {
       if (videoFile == null || videoFile.isEmpty()) {
           return null;
       }
       // URLEncoder.encode koduje spacje jako "+" — zamieniamy na "%20" dla poprawnego URL path
       // Koduje również polskie znaki i nawiasy
       String encoded = URLEncoder.encode(videoFile, StandardCharsets.UTF_8)
               .replace("+", "%20")
               .replace("*", "%2A")
               .replace("/", "%2F")
               .replace("\\", "%5C")
               .replace("{", "%7B")
               .replace("}", "%7D")
               .replace("|", "%7C")
               .replace("\"", "%22")
               .replace("<", "%3C")
               .replace(">", "%3E")
               .replace("`", "%60")
               .replace("[", "%5B")
               .replace("]", "%5D")
               .replace("^", "%5E");
       return "/videos/" + encoded;
   }
}
