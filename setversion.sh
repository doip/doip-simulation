#!/bin/bash

version=$1

if [ -z "$version" ]
then
	echo "ERROR: Missing argument <version>"
	exit 1
fi

# Modify build.gradle
sed -i s/'version.*=.*'/'version = '\'"$version"\'/g build.gradle

