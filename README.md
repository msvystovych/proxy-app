=================Proxy App=================

A reactive web proxy application that fetches content from external services, modifies HTML content, and caches responses for improved performance.

=================Features=================

Content Proxying: Fetches content from external services and serves it to clients
HTML Modification:
Adds trademark symbol (™) to every six-letter word in the content
Rewrites internal links to go through the proxy
Caching: Caches modified pages for improved performance
Request/Response Logging: Stores request and response data for analysis
Error Handling: Custom error handling for upstream client and server errors



=================Build and run the application:=================

./mvnw clean package
Run the application:

./mvnw spring-boot:run
The application will be available at http://localhost:8080

=================Running with Docker Compose=================

docker-compose up -d
The application will be available at http://localhost:8080

=================Docker Image=================

DOCKER IMAGE: https://github.com/users/msvystovych/packages/container/proxy-app/versions


=================Usage=================

To proxy a request to an external service, use the following URL format:

http://localhost:8080/proxy/{path}
Where {path} is the path to the resource on the external service.

For example, to proxy a request to https://spring.io/microservices, use:

http://localhost:8080/proxy/microservices

=================Configuration=================

The application can be configured using the following environment variables:

SPRING_DATASOURCE_URL: JDBC URL for PostgreSQL database
SPRING_DATASOURCE_USERNAME: PostgreSQL username
SPRING_DATASOURCE_PASSWORD: PostgreSQL password
SPRING_DATA_MONGODB_URI: MongoDB connection URI


=================Testing=================

Run the tests using Maven:

./mvnw test

=================Project Structure=================

src/main/java/org/company/Main.java: Application entry point

src/main/java/org/company/controller/ProxyController.java: REST controller for handling proxy requests

src/main/java/org/company/service/ProxyService.java: Service for fetching content from external services

src/main/java/org/company/service/HtmlModifierService.java: Service for modifying HTML content

src/main/java/org/company/service/CacheService.java: Service for caching modified pages

src/main/java/org/company/service/RequestResponseService.java: Service for storing request/response data
