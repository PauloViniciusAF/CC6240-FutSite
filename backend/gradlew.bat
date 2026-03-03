@rem Gradle wrapper script for Windows
@if "%DEBUG%"=="" @echo off

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Download wrapper if not present
if not exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" (
    echo Downloading Gradle wrapper...
    mkdir "%APP_HOME%\gradle\wrapper" 2>NUL
    powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar' -OutFile '%APP_HOME%\gradle\wrapper\gradle-wrapper.jar'"
)

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
goto fail

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
exit /b %ERRORLEVEL%

:fail
exit /b 1
