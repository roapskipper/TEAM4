@echo off
REM Script chạy tests với Java 21
SET JAVA_HOME=C:\Program Files\Java\jdk-21
SET PATH=%JAVA_HOME%\bin;%PATH%
echo [INFO] Đang dùng: 
java -version
echo.
echo [INFO] Chạy mvn test (exclude integration tests)...
mvn test
