#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/file-lookup-*.war "$TOMCAT_APPS/file-lookup.war"
cp -- /tmp/src/conf/ocp/file-lookup.xml "$TOMCAT_APPS/file-lookup.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/file-lookup.war")
