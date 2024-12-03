# spring-6-restclient

This project acts as a client (similar to the spring-6-resttemplate) project to the spring-6-rest-mvc which runs on port 8081.
Required other modules up and running:
- this application runs on port 8085
- authentication server on port 9000
- spring-6-rest-mvc module running on port 8081 but we are accessing this via module via the gateway which runs on port 8080

When running the BeerClientImplTest you need to start all dependent backends which includes the gateway (on port 8080), 
the authentication server (on port 9000) and the mvc module (on port 8085). That's why the tests in the BeerClientImplTest
are disabled because they will fail when running in the github pipeline.
Consider to start the necessary servers automatically. Possible solution would be feature like spring boot docker compose.

```plaintext
+---------+               +----------------+               +--------------------+
| Client  |               | Gateway Server |               | Authentication     |
| (makes  |  -----------> | (Port 8080)    |  -----------> | Server (Port 9000) |
| request)|  <----------- |                |  <----------- | (returns token)    |
+---------+               +----------------+               +--------------------+
                                |   ^  
                                |   |
                                v   |
                           +----------------+               
                           | MVC Backend    |
                           | (Port 8081)    |
                           | (Executes      |
                           | query and      |
                           | creates        |
                           | response)      |
                           +----------------+
```


This repository is for an example application built in [Spring Framework 6 - Beginner to Guru](https://www.udemy.com/course/spring-framework-6-beginner-to-guru/?referralCode=2BD0B7B7B6B511D699A9) online course

The application is a simple Spring Boot 3 / Spring Framework 6 web application. It is used to help students learn how
to use the Spring Framework. Step by step instructions and detailed explanations can be found within the course.

As you work through the course, please feel free to fork this repository to your out GitHub repo. Most links contain links
to source code changes. If you encounter a problem you can compare your code to the lesson code. [See this link for help with compares](https://github.com/springframeworkguru/spring5webapp/wiki#getting-an-error-but-cannot-find-what-is-different-from-lesson-source-code)

## Spring Framework 6: Beginner to Guru Course Wiki
Got a question about your Spring Framework 6 course? [Checkout these FAQs!](https://github.com/springframeworkguru/spring5webapp/wiki)

## Getting Your Development Environment Setup
### Recommended Versions
| Recommended             | Reference                                                                                                                                                     | Notes                                                                                                                                                                                                                  |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Oracle Java 21 JDK      | [Download](https://www.oracle.com/java/technologies/downloads/#java21) | Java 17 or higher is required for Spring Framework 6. Java 21 is recommended for the course.                                                                                                                           |
| IntelliJ 2024 or Higher | [Download](https://www.jetbrains.com/idea/download/)                                                                                                          | Ultimate Edition recommended. Students can get a free 120 trial license [here](https://github.com/springframeworkguru/spring5webapp/wiki/Which-IDE-to-Use%3F#how-do-i-get-the-free-120-day-trial-to-intellij-ultimate) |
| Maven 3.9.6 or higher   | [Download](https://maven.apache.org/download.cgi)                                                                                                             | [Installation Instructions](https://maven.apache.org/install.html)                                                                                                                                                     |
| Gradle 8.7 or higher    | [Download](https://gradle.org/install/)                                                                                                                       |                                                                                                                                                                     |
| Git 2.39 or higher      | [Download](https://git-scm.com/downloads)                                                                                                                     |                                                                                                                                                                                                                        | 
| Git GUI Clients         | [Downloads](https://git-scm.com/downloads/guis)                                                                                                               | Not required. But can be helpful if new to Git. SourceTree is a good option for Mac and Windows users.                                                                                                                 |

