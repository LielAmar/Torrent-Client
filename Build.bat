@echo off
@SET mainfile=PeerProcess
@SET abspath=%~dp0
::@ECHO %abspath%

@SET builddir=%abspath%Build
if not exist "%builddir%" mkdir "%builddir%"
@SET rundir=%abspath%RunDir

::@ECHO %builddir%
::@ECHO %rundir%

@echo compiling
cd "%abspath%/src"
javac -d "%builddir%" "*.java"

::@echo Running
::java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% 1001

cd "%abspath%"