@echo off
@SET mainfile=PeerProcess
@SET abspath=%~dp0
::@ECHO %abspath%

@SET builddir=%abspath%Build
@SET rundir=%abspath%RunDir

::@ECHO %builddir%
::@ECHO %rundir%

::@echo Running
java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% %1