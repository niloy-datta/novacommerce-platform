@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0."
if not defined JAVA_HOME goto useJavaExe
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
goto execute

:useJavaExe
set "JAVA_EXE=java.exe"

:execute
"%JAVA_EXE%" -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
endlocal
