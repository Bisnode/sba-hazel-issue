# Spring Boot Admin + Hazelcast playground

This project demonstrates issue described in [GH-1429](https://github.com/codecentric/spring-boot-admin/issues/1429)

## Prerequisites

- Java 11 installed
- JAVA_HOME configured

## Running

1. Run Hazelcast

```
./runHazel.sh
```

Hazelcast will be available on [http://localhost:5701](http://localhost:5701).

Shutdown hook is listening on [http://localhost:8082/shutdown](http://localhost:8082/shutdown) but you can just kill the app with CTRL + C.

2. Run Spring Boot Admin

```
./runAdmin.sh
```

The application is avaliable on [http://localhost:8080](http://localhost:8080)

3. Run Spring Boot Admin Client

```
./runClient.sh
```

The application is avaliable on [http://localhost:8081](http://localhost:8081)

To deregister it from SBA call [GET http://localhost:8081/deregister](http://localhost:8081/deregister)

To register again call [GET http://localhost:8081/register](http://localhost:8081/register)

## Reproducing the issue

1. Run all the applications as described above.
2. Notice how on Spring Boot Admin's dashboard ([http://localhost:8080](http://localhost:8080)) registered application has status UNKNOWN
3. Deregister client via [GET http://localhost:8081/deregister](http://localhost:8081/deregister)
4. Observe logs of SBA