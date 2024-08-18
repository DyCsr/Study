package com.example.consumerfeign.controller;

import com.example.consumerfeign.entity.Movie;
import com.example.consumerfeign.service.IMovieFeignClient;
import feign.Body;
import feign.Headers;
import feign.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeignClientController {
    @Autowired
    private IMovieFeignClient iMovieFeignClient;
    @GetMapping("feign-test1")
    public String feignTest1(){
        return iMovieFeignClient.test1();
    }

    @GetMapping("/feign-test2/{id}")
    public Movie movieById(@PathVariable(name="id")Long id){
        return iMovieFeignClient.getMovieById(id);
    }

    @PostMapping(value="/feign-test3")
    public Movie moviePost(@Param("body") Movie movie){
        Movie mm = iMovieFeignClient.postMovie(movie);
        return mm;
    }
}
