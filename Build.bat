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
javac -d "%builddir%" src/*.java

::@echo Running
::java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% 1001