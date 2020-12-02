#!/usr/bin/env bash

cp -- /tmp/src/conf/ocp/logback.xml "$CONF_DIR/logback.xml"
cp -- /tmp/src/conf/file-lookup.yaml "$CONF_DIR/file-lookup.yaml"
 
ln -s -- "$TOMCAT_APPS/file-lookup.xml" "$DEPLOYMENT_DESC_DIR/file-lookup.xml"
