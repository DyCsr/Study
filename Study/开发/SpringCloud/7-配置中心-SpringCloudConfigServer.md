# 配置中心-Spring Cloud Config Server

在微服务框架中，配置文件较多。在最开始是各自管理各自的配置，在开发阶段没有什么问题，但是到了生产环境中，管理配置时就较为麻烦，如果需要大规模更新某项配置，就变得非常困难。

为了方便服务配置文件统一管理，易于部署和维护，就引入了分布式配置中心组件。在Spring Cloud中，是Spring Cloud Config，他支持配置文件放在配置服务的内存中，也支持放在远程Git仓库中。引入Spring Cloud Config后，可以新建一个Config Server，用来管理所有的配置文件。

Spring Cloud Config有两个角色（类似Eureka）：Server和Client。Spring Cloud Config Server作为配置中心的服务端承担如下两个作用：

+ 拉取配置时更新Git仓库的副本，以保证配置为最新。
+ 支持从yml、JSON、properties等文件加载配置
+ 配合Eureka实现服务发现，配合Cloud Bus可实现配置推送更新
+ 默认配置存储基于Git仓库，从而支持配置的版本管理

而Spring Cloud Config Client的使用则非常方便，只需要在启动配置文件中增加使用Config Server上哪个配置文件即可。 

## 配置服务中心服务端

第一步：新建Spring项目，依赖包只选择Config Server，完整的依赖文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>config-server</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>config-server</name>
    <description>config-server</description>
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
            <artifactId>spring-cloud-config-server</artifactId>
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
                    <mainClass>com.example.configserver.ConfigServerApplication</mainClass>
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

第二步：在gitee上创建项目用来保存config server的配置文件

![image-20240818204441426](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182044456.png)

第三步：修改本地的配置文件

```properties
server.port=7001
spring.application.name=config-server
spring.cloud.config.server.git.uri=https://gitee.com/JbYmyd/spring-cloud-config.git

# 可选的，在git项目上创建一个子目录，用于保存某些分组的配置
spring.cloud.config.server.git.search-paths=config1
# 连接成功以后，会pull所有的配置文件到本地，可选的指定一个目录，默认会下载到临时目录
spring.cloud.config.server.git.basedir=D:\BaiduSyncdisk\Github\spring-cloud-config

# 默认使用.ssh下生成的私钥
# spring.cloud.config.server.git.username=XX
# spring.cloud.config.server.git.password=XX
```

第四步：在启动类上添加@EnableConfigServer注解

```java
package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

}
```

第五步：访问测试

 ![image-20240818204950978](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182049003.png)

 ![image-20240818205122922](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182051959.png)

访问规则，HTTP服务有以下形式的资源：

```
/{application}/{profile}[/{label}]
/{application}-{profile}.yml
/{label}/{application}-{profile}.yml
/{application}-{profile}.properties
/{label}/{application}-{profile}.properties
```

例如：

```
curl localhost:8888/foo/development
curl localhost:8888/foo/development/master
curl localhost:8888/foo/development,db/master
curl localhost:8888/foo-development.yml
curl localhost:8888/foo-db.properties
curl localhost:8888/master/foo-db.properties
```

## 客户端访问配置中心

第一步：新建spring项目，添加web和Config Client的依赖

完整的依赖文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>config-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>config-client</name>
    <description>config-client</description>
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
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bootstrap</artifactId>
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
                    <mainClass>com.example.configclient.ConfigClientApplication</mainClass>
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

第二步：编辑配置文件bootstrap.properties和application.properties，也可以合并写到bootstrap.properties中

```properties
## bootstrap.properties
#config server地址，用于指定Spring Cloud Config Server的地址即可
spring.cloud.config.uri=http://localhost:7001
#profile，指定使用的环境
spring.cloud.config.profile=dev
#使用git时可以指定分支，默认就是master
spring.cloud.config.label=master

## application.properties
server.port=7002
spring.application.name=foo
```

第三步：编写控制器测试

```java
package com.example.configclient.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${profile}")
    private String profile;

    @GetMapping("/profile")
    public String getProfile(){
        return profile;
    }

}
```

第四步：访问测试

 ![image-20240818213103563](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182131598.png)

## 综合应用

分布式配置的整个架构：

+ 远程git仓库：用来存储配置文件的地方，比如用来存储针对应用名为foo的多环境配置文件foo-{profile}.properties
+ Config Server：这是构建的分布式配置中心，在该工程中指定了所要连接的Git仓库位置以及账号、密码等信息
+ 本地Git仓库：在Config Server的文件系统中，每次客户端请求获取配置信息时，Config Server从Git仓库中获取最新配置到本地，然后在本地Git仓库中读取并返回。当远程仓库无法获取时，直接将本地内容返回。
+ Service A、Service B：具体的微服务应用，他们指定了Config Server的地址，从而实现从外部获取应用自己要用的配置信息。这些应用在启动的时候，会向Config Server请求获取配置信息来进行加载。

架构图：

![image-20240731212643163](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182134639.png)

客户端应用从配置管理中获取配置信息遵循下面的执行流程:

+ 应用启动时，根据 bootstrap.properties 中配置的应用名{application}、环境名{profile}、分支名 {label}，向 Confg Server 请求获取配置信息。
+ Confg Server 根据自己维护的 Git 仓库信息和客户端传递过来的配置定位信息，去查找配置信息。
+ 通过 git clone 命令将找到的配置信息下载到 Config Server 的文件系统中。Config Server 创建 Spring 的 Applicationcontext 实例，并从 Git 本地仓库中加载配置文件，最后将这些配置内容读取出来，返回给客户端应用。
+ 客户端应用在获得外部配置文件后加载到客户端的 ApplicationContext 实例,该配置内容的优先级高于客户端jar 包内部的配置内容，所以在jar 包中重复的内容将不再被加载。
+ Confg Server 巧妙地通过 git clone 将配置信息存储在本地，起到缓存的作用。即使当 Git服务端无法访问的时候，依然可以取 Config Server 中的缓存内容进行使用。

### 在git仓库中创建四个配置文件

![image-20240818213544783](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182135813.png)

### config服务端

这里需要注意spring.cloud.config.server.git.search-paths参数，需要将其修改为alpha，因为在gitee中，配置文件都在alpha目录下：

```properties
# 可选的，在git项目上创建一个子目录，用于保存某些分组的配置
spring.cloud.config.server.git.search-paths=alpha
```

参照配置服务中心服务端，启动应用程序以后访问测试：

```
http://localhost:7001/app/default/master
http://localhost:7001/app/dev/master
http://localhost:7001/springcloud_movies/dev/master
http://localhost:7001/springcloud_movies/default/master
```

 ![image-20240731214849384](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182137217.png)

### 配置客户端

现在需要在客户端上配置如下信息：

+ Config Server的地址
+ spring.application.name的值
+ 使用的profile，可以是defalut或dev，默认是default

#### 创建Spring Client项目

需要添加Config Client、Web、Lombok的依赖。

编辑配置文件：

```properties
#spring.application.name=config-client
#spring.cloud.config.label=master
#spring.cloud.config.profile=dev
##spring.cloud.config.uri= http://localhost:8888/
#
#eureka.client.serviceUrl.defaultZone=http://localhost:8889/eureka/
#spring.cloud.config.discovery.enabled=true
#spring.cloud.config.discovery.serviceId=config-server

server.port=7002
spring.application.name=springcloud_movies
#config server地址，用于指定Spring Cloud Config Server的地址即可
spring.cloud.config.uri=http://localhost:7001
#profile，指定使用的环境，可以更改为dev
spring.cloud.config.profile=default
#使用git时可以指定分支，默认就是master
spring.cloud.config.label=master
```

此时不再需要applicaiton配置文件。

#### 编写控制器测试

```java
package cn.mrchi.springcloud.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class DemoController {
    /**
     * 将会读取git中config1目录下的foo-dev.yml文件中的配置
     */
    @Value("${Name}")
    private String profile;
    @RequestMapping("/profile")
    public String getProfile(){
        return profile;
    }
}

```

 ```
http://localhost:7002/profile
 ```

 ![image-20240731220230785](https://jbymy-1300285860.cos.ap-nanjing.myqcloud.com/img/202408182139696.png)





















