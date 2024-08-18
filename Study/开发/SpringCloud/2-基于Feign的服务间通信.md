# 基于Feign的服务间通信

在第一节中利用了RestTemplate对微服务进行了调用，这里也可以使用Feign对微服务进行调用。

首先还是启动第一节中的注册中心和服务提供者，这里只编写一个服务消费者的示例代码。

第一步：创建一个SpringBoot项目，添加OpenFeign和Eureka Client的依赖。

 ![image-20240817094335915](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408170943969.png)

完整的依赖文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>consumer-feign</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>consumer-feign</name>
    <description>consumer-feign</description>
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.0.2</spring-boot.version>
        <spring-cloud.version>2022.0.0-RC2</spring-cloud.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-httpclient</artifactId>
            <version>9.5.1</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <configuration>
                    <mainClass>com.example.consumerfeign.ConsumerFeignApplication</mainClass>
                    <skip>true</skip>
                </configuration>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    <repositories>
        <repository>
            <id>netflix-candidates</id>
            <name>Netflix Candidates</name>
            <url>https://artifactory-oss.prod.netflix.net/artifactory/maven-oss-candidates</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

</project>

```

第二步：编写实体类。

```java
package com.example.consumerfeign.entity;

public class Movie {
    private Long id;
    private String name;
    private String author;

    public Movie() {
        super();
    }
    public Movie(Long id, String name, String author) {
        super();
        this.id = id;
        this.name = name;
        this.author = author;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    @Override
    public String toString() {
        return "Movie [id=" + id + ", name=" + name + ", author=" + author + "]";
    }
}


```

第三步：编写FeignClient的接口，表明每个方法调用哪个服务的接口，示例代码如下：

```java
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

```

第四步：编写控制器。

```java
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

```

第五步：编写配置文件application.yml

```yml
server:
  port: 8080

spring:
  application:
    name: consumer-feign
feign:
  httpclient:
    enabled: true
eureka:
  instance:
    appname: consumer-feign
    prefer-ip-address: false
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    enabled: true
```

第六步：编写主类，添加@EnableFeignClients注解：

```java
package com.example.consumerfeign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ConsumerFeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerFeignApplication.class, args);
    }

}
```

第七步：测试接口

 ![image-20240818104804062](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181048122.png)

## 问题解决：Feign用Post传参，参数为空的解决方法

在服务端与消费端的方法中都增加了@param注解：

```java
@PostMapping(value="/feign-test3")
    public Movie moviePost(@Param("body") Movie movie){
        Movie mm = iMovieFeignClient.postMovie(movie);
        return mm;
    }
```























