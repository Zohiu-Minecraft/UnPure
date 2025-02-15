#!/bin/bash

# "paru -Sy maven"

mvn package -f pom.xml
mv -f target/UnPure-1.0-SNAPSHOT.jar server/plugins/UnPure-1.0.jar