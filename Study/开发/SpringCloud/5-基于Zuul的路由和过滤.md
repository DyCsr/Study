# 基于Zuul的路由和过滤

路由时微服务架构中必须的一部分，比如，“/”可能映射到我们的web程序中，“/api/users"可能映射到我们的用户服务中，”/api/shop“可能映射到我们的商品服务中。

Zuul是Netflix出品的一个基于JVM路由和服务端的负载均衡器。Zuul功能很多，包括压力测试、动态路由、负载削减等。使用Zuul可以对微服务提供的API进行路由和保护。

## Zuul路由快速使用实例

本项目有三个文件夹，一个注册中心，一个服务提供者，一个使用Zuul进行代理路由的服务消费者。注册中心和服务提供者继续使用第一节中的代码。这里只编写服务消费者的代码：

第一步：添加依赖

```xml
<!-- 网关zuul -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
        </dependency>
```

完整的依赖文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>consumer-zuul</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>consumer-zuul</name>
    <description>consumer-zuul</description>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.3.RELEASE</version>
        <relativePath /> <!-- lookup parent from repository -->
    </parent>
    <properties>
        <java.version>1.8</java.version>
        <spring-cloud.version>Greenwich.RELEASE</spring-cloud.version>
    </properties>
    <dependencies>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
        </dependency>
        <!-- 网关zuul -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
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
                <configuration>
                    <mainClass>com.example.consumerzuul.ConsumerZuulApplication</mainClass>
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

第二步：编写配置文件

```yml
server:
  port: 8080

eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://localhost:8761/eureka

spring:
  application:
    name: consumer-zuul
  logging:
    file: eureka-zuul.log
    level:
root: INFO

```

第三步：启动类添加@EnableZuulProxy注解

```java
package com.example.consumerzuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
@EnableEurekaClient
public class ConsumerZuulApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerZuulApplication.class, args);
    }

}

```

访问测试：

 ![image-20240818181507350](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181815392.png)

## 配置Zuul路由规则

### 服务名+路径规则

第一种配置方式：可以采用服务名称+访问路径

配置示例：

```yml
server:
  port: 8080

eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://localhost:8761/eureka

spring:
  application:
    name: consumer-zuul
  logging:
    file: eureka-zuul.log
    level:

zuul:
  ignored-services:
    - '*' # 配置忽略的service，但不包含下面通过routes配置的规则
  routes: 
    eureka-provider: /movie/** # 将符合/movie/**规则的请求转发至eureka-provider服务
    
root: INFO
```

访问示例：

 ![image-20240818182144284](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181821331.png)

### serviceid+path

也可以使用serviceId+path的方式来指定路由的规则，以下是一个配置示例：

```yml
server:
  port: 8080

eureka:
  client:
    enabled: true
    service-url:
      defaultZone: http://localhost:8761/eureka

spring:
  application:
    name: consumer-zuul
  logging:
    file: eureka-zuul.log
    level:

zuul:
  ignored-services:
    - '*'
  routes:
    eureka-movie: # 此名称可以任意
      path: /movie/**
      serviceId: eureka-provider


root: INFO
```

## 路由配置路径前缀

路由路径其实是一个逻辑路径，可以人为定义。例如：zuul.prefix可以为所有的匹配增加前缀，例如/api，代理前缀默认会从请求路径中移除（通过zuul.stripPrefix=false可以关闭这个功能），zuul.stripPrefix默认为true。

zuul.stripPrefix与zuul.prefix共同使用表示全局的设置。

```yml
zuul:
  ignored-services:
  - '*'
  prefix: /abc
  routes:
    movie: #此名称可以任意
      path: /mvs/**
      serviceId: eureka-movie
      strip-prefix: true
```

 ![image-20240729214600420](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181826893.png)

 ![image-20240729215012351](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181826887.png)

## 查看所有的路由映射

当Zuul和Springboot Actuator配合使用时，zuul会暴露出一个路由管理端点/routes，借助这个端点，可以方便查看和管理zuul路由。使用GET方法访问该节点，即可返回zuul当前映射的路由列表，而使用POST方法访问该节点时，就会强制刷新zuul当前映射的路由列表，路由会自动刷新。

配置依赖：

```xml
<!-- 网关zuul -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
```

配置节点：

```yml
management:
  endpoints:
    web:
      exposure:
        include:
        - '*'
```

此时访问actuator/routes/details可以显示出每个映射的详细信息：

 ![image-20240729220400963](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408181826801.png)

## 