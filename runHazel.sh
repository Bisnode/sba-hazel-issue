#!/bin/bash

hazel/gradlew -p hazel clean build fatJar && java -jar hazel/build/libs/hzcast-all.jar

