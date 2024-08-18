# Spring Cloud 链路追踪-server-zipkin

随着项目的业务复杂度变高，系统拆分导致系统调用的链路愈加复杂，一个前端请求可能最终需要调用很多次后端服务才能完成，当整个请求变慢或者不可用时，我们无法得知该请求是由某个或某些后端服务引起的，这是就需要解决如何快速定位服务故障点。

本案例由三个工程组成，一个server-zipkin，他的主要作用是使用Zipkin Server的功能，收集调用数据并展示，一个service-hi，对外暴露hi接口；一个service-miya，对外暴露miya接口。service-hi和service-miya这两个service可以互相调用，并且只有这两个service调用了，server-zipkin才会收集数据，这就是为什么叫做服务追踪了。

## 构建server-zipkin

首先从maven下载zipkin：

```
https://repo1.maven.org/maven2/io/zipkin/zipkin-server/2.14.1/zipkin-server-2.14.1-exec.jar
```

然后运行：

```
java -jar zipkin-server-2.14.1-exec.jar
```

访问本机的9411端口：

![image-20240731224237132](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182200353.png)

## 创建service-hi

在本项目中需要引入spring-cloud-starter-zipkin依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>com.mrchi</groupId>
	<artifactId>service-zipkin</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>jar</packaging>

	<name>service-hi</name>
	<description>Demo project for Spring Boot</description>

	<parent>
		<groupId>com.mrchi</groupId>
		<artifactId>springcloud-sleuth</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</parent>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>
```

在配置文件中指定Zipkin Server的地址：

```properties
server.port=8988
spring.zipkin.base-url=http://localhost:9411
spring.application.name=service-hi
```

对外暴露接口：

```java
package com.mrchi;

import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
@RestController
public class ServiceHiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceHiApplication.class, args);
	}

	private static final Logger LOG = Logger.getLogger(ServiceHiApplication.class.getName());

	@Autowired
	private RestTemplate restTemplate;

	@Bean
	public RestTemplate getRestTemplate(){
		return new RestTemplate();
	}

	@RequestMapping("/hi")
	public String callHome(){
		LOG.log(Level.INFO, "calling trace service-hi  ");
		return restTemplate.getForObject("http://localhost:8989/miya", String.class);
	}
	@RequestMapping("/info")
	public String info(){
		LOG.log(Level.INFO, "calling trace service-hi ");

		return "i'm service-hi";

	}

	@Bean
	public Sampler defaultSampler() {
		return Sampler.ALWAYS_SAMPLE;
	}

}

```

## 构建service-miya

该项目创建过程跟service-hi相同。暴露接口：

```java
package com.mrchi;

import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
@RestController
public class ServiceMiyaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceMiyaApplication.class, args);
	}

	private static final Logger LOG = Logger.getLogger(ServiceMiyaApplication.class.getName());


	@RequestMapping("/hi")
	public String home(){
		LOG.log(Level.INFO, "hi is being called");
		return "hi i'm miya!";
	}

	@RequestMapping("/miya")
	public String info(){
		LOG.log(Level.INFO, "info is being called");
		return restTemplate.getForObject("http://localhost:8988/info",String.class);
	}

	@Autowired
	private RestTemplate restTemplate;

	@Bean
	public RestTemplate getRestTemplate(){
		return new RestTemplate();
	}


	@Bean
	public Sampler defaultSampler() {
		return Sampler.ALWAYS_SAMPLE;
	}
}

```

## 测试

启动工程，访问9411端口：

![image-20240731225004917](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182200345.png)

尝试访问8989端口的miya：

 ![image-20240731225040322](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182200325.png)

![image-20240731225058653](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182200311.png)