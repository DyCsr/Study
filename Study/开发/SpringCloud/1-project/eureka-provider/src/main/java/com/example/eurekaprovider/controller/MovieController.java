package com.example.eurekaprovider.controller;

import com.example.eurekaprovider.entity.Movie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class MovieController {
    @Value("${server.port}")
    private String port;
    @GetMapping("/port")
    public String getPort() {
        return "返回自："+port;
    }

    @GetMapping("/movie/{id}")
    public Movie movieById(@PathVariable Long id) {
        Movie movie = new Movie();
        movie.setId(new Random().nextLong());
        movie.setName("端口:"+port);
        movie.setAuthor("姜文");
        return movie;
    }
}
