# blockfuse
solutions for blockfuse quiz
for the predictor project, i used java spring boot however everything is lumped in one, if there was much time i would have broken the file into different files handling the different components.

for the smart contract here is how to test it:
 Install MetaMask browser extension (metamask.io)
 Create or import a wallet
 Get some test ETH from a faucet if testing on testnet:
  * Goerli faucet: https://goerlifaucet.com/
  * Sepolia faucet: https://sepoliafaucet.com/

 DEPLOY USING REMIX
 Go to remix.ethereum.org and Create new file "SimpleTransfer.sol"
 Copy and compile the entire contract code

 Go to "Deploy & Run Transactions" tab
 Choose environment:
   * For testing: Use "JavaScript VM"
   * For testnet: Select "Injected Web3" (connects to MetaMask)
 Click "Deploy"




for the predictor use this approach to setup and test:



 Create a simple pom.xml:

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>football-prediction</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.0</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>

Create application.properties in src/main/resources:
spring.datasource.url=jdbc:mysql://localhost:3306/football_prediction
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

Install MySQL and create database:
CREATE DATABASE football_prediction;

Run using Maven:
mvn spring-boot:run

To test the endpoints:
1. API Testing (using curl):
curl http://localhost:8080/api/matches/upcoming

2. WebSocket Testing (using JavaScript):
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, frame => {
    stompClient.subscribe('/topic/predictions', prediction => {
        console.log(JSON.parse(prediction.body));
    });
});
