#!/bin/bash
../gradlew -c settings-core.gradle test uploadArchives -Ptarget_maven_repo=$1
../gradlew -c settings-kits.gradle test uploadArchives -Ptarget_maven_repo=$1 
../gradlew -c settings-allkits.gradle test uploadArchives -Ptarget_maven_repo=$1
