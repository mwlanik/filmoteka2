#!/bin/bash
./mvnw clean package -DskipTests
scp compose.yaml target/filmoteka.jar serwerpi:/home/mwalnik/Filmoteka/
