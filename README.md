# spring-6-restclient

This project acts as a client (similar to the spring-6-resttemplate) project to the spring-6-rest-mvc which runs on port 8081.
Required other modules up and running:
- this application runs on port 8085
- authentication server on port 9000
- spring-6-rest-mvc module running on port 8081 but we are accessing this via module via the gateway which runs on port 8080


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
