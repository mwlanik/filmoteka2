package com.grodziec.filmoteka.model;

import lombok.Data;

@Data
public class Film {
   private Long id;
   private String title;
   private int year;
   private String director;
   private String genre;
   private String description;
   private String posterUrl;
}
