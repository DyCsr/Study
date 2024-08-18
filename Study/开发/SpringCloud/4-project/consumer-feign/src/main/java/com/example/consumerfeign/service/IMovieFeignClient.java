package com.example.consumerfeign.service;

import com.example.consumerfeign.entity.Movie;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value="eureka-provider")
public interface IMovieFeignClient {
    @GetMapping("/port")
    public String test1();
    @GetMapping("/movie/{id}")
    public Movie getMovieById(@PathVariable(name="id")Long id);
    @PostMapping("/movie/post")
    public Movie postMovie(Movie movie);
}
