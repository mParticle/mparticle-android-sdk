#!/bin/bash
../gradlew -c settings-core.gradle clean test uploadArchives -Ptarget_maven_repo=$1
../gradlew -c settings-kits.gradle clean test uploadArchives -Ptarget_maven_repo=$1 
../gradlew -c settings-allkits.gradle clean test uploadArchives -Ptarget_maven_repo=$1
