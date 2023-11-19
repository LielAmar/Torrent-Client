@echo off
@SET mainfile=project.PeerProcess
@SET abspath=%~dp0
::@ECHO %abspath%

@SET builddir=%abspath%Build
@SET rundir=%abspath%RunDir

::@ECHO %builddir%
::@ECHO %rundir%

::@echo Running
rmdir /S /Q RunDir
xcopy RunDirClean RunDir /E /I

java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% %1