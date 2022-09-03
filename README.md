# Ecommerce Backend 
### A distributed architecture with microservices and saga pattern

## Features

- Check user Balance
- Check Warehouse disponibility
- Order your items
- Delete the order
- Receive confirmations via mail

## Stack

Technologies used

- [Spring WebFlux] - Limited thread pool. Requires async programming
- [Kotlin] - JVM language that allows writing Reactive programming and handles the backpressure via coroutines.
- [Redis] - Fast caching mechanism that store order data used during the order sagas.
- [MongoDB] - NoSQL Database to achieve better performance in a distributed architecture due to sharding.
- [Apache Kafka] - As Asynchronous Message Queues Machanism used during saga pattern.
- [Debezium] - A Kafka Connect software that is used to preserve the state of the database in case of a microservice failure.
- [Spring Security] - To add authorization and aauthentication to the architecture.
- [Docker] Every microservice is dockerized
- [Docker-compose] used to start and manage the interaction of all the docker containers

## Paradigms
 - Reactive programming.
 - Functional programming.
 - Orchestration-based saga pattern.
 - Event Based State Machine
 - Asynchronous programming
 
## Installation

```sh
docker-compose up -d
./script.sh
```





