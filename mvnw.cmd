@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM
@REM Maven Wrapper startup batch script
@REM

@IF "%__MVNW_ARG0_NAME%"=="" SET __MVNW_ARG0_NAME=%~nx0
@SET __MVNW_ARG0=%0
@SET MVNW_USERNAME=%USERNAME%
@SET WRAPPER_JAR="%~dp0\.mvn\wrapper\maven-wrapper.jar"

@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that don't commit the wrapper jar
@REM to their repository.

@SET MVNW_VERBOSE=false
@IF NOT "%MVNW_VERBOSE%"=="true" SET MVNW_QUIET_UNZIP=-q

@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

@REM Begin: download maven-wrapper.jar
@IF EXIST %WRAPPER_JAR% GOTO doneDownload

@REM Download the wrapper jar
@ECHO Downloading Maven Wrapper...
powershell -Command "&{"^
    $webclient = new-object System.Net.WebClient;"^
    $webclient.DownloadFile('%DOWNLOAD_URL%', '%WRAPPER_JAR%');"^
    "}"

:doneDownload
@REM End: download maven-wrapper.jar

@REM Determine the Java command to use to start the JVM.
@SET JAVA_EXE=java.exe
@IF DEFINED JAVA_HOME SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"

@REM Find project base dir
@SET MAVEN_PROJECTBASEDIR=%~dp0

@REM Find the .mvn directory
@SET MAVEN_OPTS_FILE=%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config
@IF EXIST "%MAVEN_OPTS_FILE%" (
    @FOR /F "usebackq delims=" %%A IN ("%MAVEN_OPTS_FILE%") DO @SET MAVEN_OPTS=%%A %MAVEN_OPTS%
)

%JAVA_EXE% ^
  %MAVEN_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  org.apache.maven.wrapper.MavenWrapperMain %*
