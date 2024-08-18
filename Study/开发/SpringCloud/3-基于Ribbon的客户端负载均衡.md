# 基于Ribbon的客户端负载均衡

## RestTemplate调用服务的示例

首先还是利用第一节中的注册中心和服务提供者。这里只编写一个服务消费者，具体过程参考第一节的服务消费者，这里不在赘述。

编写一个控制器：

```java
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
    
    @GetMapping("/movies/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        Movie movie = restTemplate.getForObject("http://127.0.0.1:6789/movie/" + id, Movie.class);
        return movie;
    }

}

```

测试控制器：

![image-20240818110819797](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181108838.png)



### 注意事项

当控制器中直接硬编码写服务地址的时候，启动类中不可以添加@LoadBalanced参数，否则出现如下报错：

![image-20240818111025940](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181110017.png)

## 负载均衡

### 服务端

这里还采用第一节中的服务提供者的代码，启动项目时勾选右上角Allow parallel run，每次修改配置文件中的端口号，启动多个服务。

![image-20240818111549256](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181115327.png)

访问注册中心，发现服务已经启动了三个实例：

![image-20240818111658154](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181116181.png)

### 消费者

使用Ribbon作为负载均衡的方法比较简单，只需要两步：

第一步：在创建RestTemplate时，只需要添加@LoadBlance注册

第二步：调用restTemplate访问服务端请求时，只需要将服务端的服务名称作为参数即可。

完整示例如下，首先需要添加Ribbon的依赖，一个完整的依赖文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>eureka-consumer</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>eureka-consumer</name>
    <description>eureka-consumer</description>
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>2.6.13</spring-boot.version>
        <spring-cloud.version>2021.0.5</spring-cloud.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
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
                    <mainClass>com.example.eurekaconsumer.EurekaConsumerApplication</mainClass>
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

</project>

```

然后添加@LoadBlance注解：

```java
package com.example.eurekaconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class EurekaConsumerApplication {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
    public static void main(String[] args) {
        SpringApplication.run(EurekaConsumerApplication.class, args);
    }

}
```

编写配置文件，其中spring.application.name也被称为虚拟ip：

```yml
server:
  port: 8080
spring:
  application:
    name: eureka-consumer
eureka:
  instance:
    prefer-ip-address: false
    appname: eureka-consumer
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
user:
  movieurl: http://eureka-provider
```

使用虚拟ip的值调用服务：

```java
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
```

测试负载均衡，多次发送请求，发现每次调用的微服务为不同的微服务：

 ![image-20240818112500177](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181125217.png)

![image-20240818112549801](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181125848.png)

## 自定义配置负载均衡策略

### 通过配置文件实现

第一步：编辑配置文件application.yml

```yml
eureka-provider: #在服务消费者配置服务提供者的服务名
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RandomRule
```

第二步：修改启动类：

```java
@Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        RestTemplate RestTemplate = builder.build();
        return RestTemplate;
    }

```

### 通过配置类实现

第一步：创建配置类，这里需要与启动类不在同一个包下

```java
package com.ribbon.config;

import org.springframework.context.annotation.Bean;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;

public class RandonRuleConfig {
    @Bean
    public IRule iRule(){
        System.out.println(">rule");
        return new RandomRule();
    }
}

```

第二步：在启动类添加注解，并修改restTemplate类：

```java
package com.example.eurekaconsumer;

import com.ribbon.config.RandonRuleConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RibbonClients(value = {@RibbonClient(name = "ribbonclient",configuration = RandonRuleConfig.class)})
public class EurekaConsumerApplication {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        RestTemplate RestTemplate = builder.build();
        return RestTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(EurekaConsumerApplication.class, args);
    }

}

```

第三步：启动项目进行测试，发现自定义的负载均衡生效。

 ![image-20240818115745029](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181157064.png)











