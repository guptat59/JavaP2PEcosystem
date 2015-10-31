@ECHO OFF
javac Server.java
start "Server" java Server 8000 10
pause