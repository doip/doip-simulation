#!/bin/bash

version=$1

if [ -z "$version" ]
then
	echo "ERROR: Missing argument <version>"
	exit 1
fi


sed -i s/'version.*=.*'/'version = '\'"$version"\'/g build.gradle
sed -i s/'<version>.*<\/version>'/'<version>'"$version"'<\/version>'/g pom.xml
