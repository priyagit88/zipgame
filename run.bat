@echo off
set "PATH=%~dp0jdk-17.0.17+10\bin;%PATH%"
if not exist "classes" mkdir classes
echo Compiling...
javac -d classes -cp "lib/*" src/main/java/com/zipgame/*.java
echo Starting Server...
java -cp "classes;lib/*" com.zipgame.SimpleServer %*
pause
