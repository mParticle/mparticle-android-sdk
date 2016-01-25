#!/bin/bash
../gradlew clean test uploadArchives -Ptarget_maven_repo=$1
