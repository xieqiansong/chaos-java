@echo off
setlocal
REM 包装脚本：约定把下载解压后的 Nacos 放在本目录的 nacos\ 下
set NACOS_HOME=%~dp0nacos
if not exist "%NACOS_HOME%\bin\startup.cmd" (
    echo [error] cannot find nacos binary.
    echo download nacos-server-2.2.3.zip and extract into:
    echo   %NACOS_HOME%
    echo url: https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.zip
    exit /b 1
)
echo [start] starting nacos in standalone mode...
call "%NACOS_HOME%\bin\startup.cmd" -m standalone
echo [done] console: http://REDACTED:8848/nacos
