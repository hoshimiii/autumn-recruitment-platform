@echo off
java -Dmaven.multiModuleProjectDirectory="%CD%" -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain -f "%~dp0apps\server\pom.xml" %*
