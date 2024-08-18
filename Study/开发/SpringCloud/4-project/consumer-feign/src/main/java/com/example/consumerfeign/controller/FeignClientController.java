package com.example.consumerfeign.controller;

import com.example.consumerfeign.entity.Movie;
import com.example.consumerfeign.service.IMovieFeignClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class FeignClientController {

    @Autowired
    private IMovieFeignClient iMovieFeignClient;

    @Autowired
    private RestTemplate restTemplate;

    /** 以下测试restTemplate */
    @GetMapping("/testRestTemplate/{id}")
    @HystrixCommand(fallbackMethod = "movieByIdFallback",commandProperties = {
            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value = "5000")
    })
    public Movie movieById(@PathVariable Long id) {
        Movie mm = restTemplate.getForObject("http://localhost:6787/movie/" + id, Movie.class);
        return mm;
    }

    /** 定义一个相同的参数和返回类型的方法 */
    public Movie movieByIdFallback(Long id) {
        Movie movie = new Movie();
        movie.setId(-1L);
        movie.setName("未知电影");
        movie.setAuthor("未知导演");
        return movie;
    }

    /**
     * 测试Feign
     * @param id
     * @return
     */
    @GetMapping(value="/testFeign/{id}")
    @HystrixCommand(fallbackMethod = "movieByIdFallback")
    public Movie movieByIdFeign(@PathVariable(name="id") Long id) {
        Movie mm = iMovieFeignClient.getMovieById(id);
        return mm;
    }
}
