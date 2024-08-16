package com.example.eurekaconsumer.controller;

import com.example.eurekaconsumer.entity.Movie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TestController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${user.movieurl}")
    private String movieurl;

    @GetMapping("/movies/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        Movie movie = restTemplate.getForObject(movieurl + "/movie/" + id, Movie.class);
        return movie;
    }

}
