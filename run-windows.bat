@echo off
if not exist out mkdir out
javac -d out src\clinic\*.java
if errorlevel 1 exit /b 1
java -cp out clinic.Main
