# 微服务网关Spring Cloud Gateway

这里也是启动第一节的注册中心和服务提供者，重点编写服务消费者。Spring Cloud Gateway的实现方式有两种：一种通过配置文件实现，另外一种通过路由编程实现。

## 配置方式实现

第一步：引入依赖

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
```

完整的依赖如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>consumer-gateway</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>consumer-gateway</name>
    <description>consumer-gateway</description>
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.0.2</spring-boot.version>
        <spring-cloud.version>2022.0.0-RC2</spring-cloud.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
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
                    <mainClass>com.example.consumergateway.ConsumerGatewayApplication</mainClass>
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

第二步：编辑配置文件

```yml
server:
  port: 8080
#配置本项目的ID
spring:
  application:
    name: consumer-gateway
  #配置gateway
  cloud:
    gateway:
      enabled: true
      routes:
        - id: eureka-movie
          # 如果输入一个固定的地址则为代理一个地址，
          # 如果输入lb://spring.application.name则为通过loadbalance实现负载均衡
          uri: #http://server1:8001
            lb://eureka-provider
          predicates:
            - Path=/aa/**
          filters:
            # 在转发后用于删除/aa/这个前缀，
            # 如：http://localhost:3002/aa/movie/1转发以后为：http://localhost:8002/movie/1
            - StripPrefix=1
#配置注册到服务器
eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://localhost:8761/eureka

logging:
  level:
    root: INFO
    cn.mrchi: DEBUG
  file:
    name: gateway.log

```

第三步：访问测试。

 ![image-20240818190418051](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181904080.png)

## Gateway路由编程方式实现

示例代码如下：

```java
package cn.mrchi.springcloud;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;

import cn.mrchi.springcloud.filters.OneFilter;
import cn.mrchi.springcloud.filters.TwoFilter;
@SpringBootApplication
@EnableEurekaClient
public class SpringCloudEurekaGatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringCloudEurekaGatewayApplication.class, args);
	}
	/**以下是使用Java代码配置的routes*/
	@Bean
	public RouteLocator routeLocator(RouteLocatorBuilder builder) {
		return builder.routes()//
				.route("eureka-provider", sp->sp.path("/cc/**").filters(gf->gf.stripPrefix(1)).uri("lb://eureka-provider").filters(new OneFilter(),new TwoFilter()))//添加两个GateWay过虑器
				.build();
	}
}

```

