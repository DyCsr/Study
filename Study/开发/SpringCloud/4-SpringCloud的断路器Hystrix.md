# Hystrix断路器

在一个分布式系统中，许多依赖不可避免地会调用失败，比如超时、异常等等，如何能够保证在一个依赖出问题的情况下，不会导致整体服务失败，这个就算断路器的作用，这里介绍的是Hystrix组件。

Hystrix提供了熔断、隔离、Fallback、cache、监控等功能，能够在一个或者多个依赖同时出问题时，保证系统依然可用。

## Hystrix Fallback

Fallback指的是为了给系统更好的保护，采用的降级技术。所谓降级，就是指在Hystrix执行非核心链路功能失败的情况下如何处理，比如返回默认值等等。

使用Hystrix实现降级功能是通过覆写HystrixCommand中的getFallback方法，在其中实现自定义的降级逻辑来实现的，下面四种情况会导致Hystrix执行fallback：

+ 主方法抛出异常
+ 主方法执行超时
+ 线程池拒绝
+ 断路器打开

简单理解就是：如果被调用的微服务失败，则调用Fallback指定的方法。

这里启动第一节的注册中心，这里不需要启动服务提供者，只编写一个服务消费者的代码，来创造出服务异常的状态。

第一步：添加hystrix的依赖

```xml
<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-hystrix-dashboard</artifactId>
            <version>2.2.10.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
            <version>1.5.18</version>
        </dependency>
```

完整依赖如下：

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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-hystrix-dashboard</artifactId>
            <version>2.2.10.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
            <version>1.5.18</version>
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

第二步：在启动类添加注解@EnableHystrix，开启Hystrix：

```java
package com.example.consumerfeign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableFeignClients
@SpringBootApplication
@EnableHystrix
public class ConsumerFeignApplication {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(ConsumerFeignApplication.class, args);
    }

}

```

第三步：在控制器中添加HystrixCommand注解，并编写回调函数：

```java
package com.example.consumerfeign.controller;

import com.example.consumerfeign.entity.Movie;
import com.example.consumerfeign.service.IMovieFeignClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
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
    @HystrixCommand(fallbackMethod = "movieByIdFallback")
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

```

第四步：编辑配置文件，启用hystrix

```yml
server:
  port: 8080

spring:
  application:
    name: consumer-feign
feign:
  hystrix:
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

第五步：测试熔断器。

当服务提供者正常时，两个请求均可以正常返回数据：

 ![image-20240818154354464](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181543541.png)

当服务提供者异常时，返回自定义的Fallback函数。

 ![image-20240818154442804](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181544847.png)

## Hystrix的超时配置

Hystrix默认超时事件为1000毫秒。

### 配置HystrixProperty属性

```java
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

```

这里修改了服务提供者的代码，在方法执行时进行了3s的延时：

```java
@GetMapping("/movie/{id}")
    public Movie movieById(@PathVariable Long id) throws InterruptedException {
        Thread.sleep(1000*3); // 休眠3秒
        Movie movie = new Movie();
        movie.setId(new Random().nextLong());
        movie.setName("端口:"+port);
        movie.setAuthor("姜文");
        return movie;
    }
```

此时进行测试发现，testRestTemplate的接口配置了超时5s，可以正常返回数据：

 ![image-20240818161918909](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181619945.png)

testFeign接口未配置超时，无法返回数据：

 ![image-20240818161939798](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181619828.png)

## Hystrix健康检查-hystrix.stream

在开启了springboot的Actuator监控功能以后，可以使用Hystrix对各个微服务进行健康检查。

第一步：在微服务中添加依赖：

```xml
<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
```

完整的依赖如下：

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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-hystrix-dashboard</artifactId>
            <version>2.2.10.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
            <version>1.5.18</version>
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

第二步：在需要被监控的微服务的配置文件中添加如下内容：

```yml
#添加hystrix.stream
management:
  endpoints:
    web:
      exposure:
        include:
          hystrix.stream,info,health
  endpoint:
    health:
      show-details: ALWAYS
```

第三步：启动项目测试。如果没有人访问接口，则只会显示ping。

![image-20240818163125080](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181631147.png)

## Hystrix的Dashboard

Hystrix Dashboard仪表盘是根据系统一段时间内发生的请求情况来展示信息的可视化面板，这些信息是每个Hystrix Command执行过程中产生的信息，这些信息是一个指标集合，表明具体的系统运行情况。

### 添加Dashboard依赖

第一步：添加依赖

```xml
<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-hystrix-dashboard</artifactId>
            <version>2.2.10.RELEASE</version>
        </dependency>
```

完整的依赖文件如下：

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
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-hystrix-dashboard</artifactId>
            <version>2.2.10.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
            <version>1.5.18</version>
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

第二步：修改配置文件：

```yml
hystrix:
  dashboard:
    proxy-stream-allow-list: "*"
```

完整的示例配置文件如下：

```yml
server:
  port: 8080

spring:
  application:
    name: consumer-feign
feign:
  hystrix:
    enabled: true

eureka-provider:
  ribbon:
    ReadTimeout: 300000 #5分钟
    ConnectTimeout: 300000

eureka:
  instance:
    appname: consumer-feign
    prefer-ip-address: false
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    enabled: true

hystrix:
  dashboard:
    proxy-stream-allow-list: "*"

management:
  endpoints:
    web:
      exposure:
        include:
          hystrix.stream,info,health
  endpoint:
    health:
      show-details: ALWAYS
```

第三步：访问测试

![image-20240818164013716](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181640786.png)

![image-20240818164030293](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181640348.png)









