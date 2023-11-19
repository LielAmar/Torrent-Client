@echo off
@SET mainfile=project.PeerProcess
@SET abspath=%~dp0
::@ECHO %abspath%

@SET builddir=%abspath%Build
@SET rundir=%abspath%RunDir

@SET peerIDStart=1001
@SET peerIDEnd=1002
::@ECHO %builddir%
::@ECHO %rundir%

::@echo Running
::java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% %peerID%
rmdir /S /Q RunDir
xcopy RunDirClean RunDir /E /I

for /L %%i IN (%peerIDStart%, 1, %peerIDEnd%) DO (
    ping 192.0.2.2 -n 1 -w 1000 > nul
    start "Peer %%i" java -Duser.dir="%rundir%" -cp "%builddir%" %mainfile% %%i
)

