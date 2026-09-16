@echo off
REM LikeU backend launcher (dev profile: H2 + Mock Redis)
cd /d %~dp0
call mvn spring-boot:run -Dspring-boot.run.profiles=dev
pause
