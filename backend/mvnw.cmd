@REM ----------------------------------------------------------------------------
@REM  Maven Start Up Batch script for Windows
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set JAVA_EXE="C:\Program Files\Java\jdk-17\bin\java.exe"
set WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%~dp0.mvn\wrapper\maven-wrapper.properties"

%JAVA_EXE% ^
  -Dmaven.multiModuleProjectDirectory="%~dp0" ^
  -jar %WRAPPER_JAR% %*
