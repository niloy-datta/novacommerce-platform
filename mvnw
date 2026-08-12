#!/bin/sh

set -eu
MAVEN_PROJECTBASEDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -classpath "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
