@echo off
@SET mainfile=project.PeerProcess
@SET abspath=%~dp0
::@ECHO %abspath%

@SET builddir=%abspath%Build
if not exist "%builddir%" mkdir "%builddir%"
@SET rundir=%abspath%RunDir

::@ECHO %builddir%
::@ECHO %rundir%


XCopy "%abspath%/src" "%builddir%" /T /E
@echo compiling
cd "%abspath%/src"

javac -d "%builddir%" "%mainfile:.=/%.java"
::@echo Running
::java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% 1001
::del "%builddir%\empty.class"
cd "%abspath%"