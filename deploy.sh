#!/bin/bash
./mvnw clean package -DskipTests
scp compose.yaml .env/.env target/filmoteka.jar serwerpi:/home/mwalnik/Filmoteka/
