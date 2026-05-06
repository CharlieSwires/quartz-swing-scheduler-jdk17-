@echo off
echo Dummy Windows batch process started.

for /L %%i in (10,-1,1) do (
    echo Exiting in %%i second(s)...
    timeout /t 1 /nobreak >nul
)

echo Dummy Windows batch process finished.
exit /b 0
