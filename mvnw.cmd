@echo off
rem Mini "Maven wrapper" para CMD / PowerShell: descarga Maven la primera vez y lo ejecuta.
rem Uso:  .\mvnw.cmd compile exec:java
setlocal
set "MVN_VERSION=3.9.11"
set "WRAPPER_DIR=%USERPROFILE%\.m2\wrapper"
set "BASE=%WRAPPER_DIR%\apache-maven-%MVN_VERSION%"
set "URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip"

if exist "%BASE%\bin\mvn.cmd" goto run

echo Descargando Maven %MVN_VERSION%, solo la primera vez...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force -Path '%WRAPPER_DIR%' | Out-Null; Invoke-WebRequest -UseBasicParsing -Uri '%URL%' -OutFile '%WRAPPER_DIR%\maven.zip'; Expand-Archive -Force '%WRAPPER_DIR%\maven.zip' '%WRAPPER_DIR%'; Remove-Item '%WRAPPER_DIR%\maven.zip'"
if errorlevel 1 (
  echo No se pudo descargar Maven. Revisa tu conexion a internet.
  exit /b 1
)

:run
call "%BASE%\bin\mvn.cmd" %*
