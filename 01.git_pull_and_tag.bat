@echo off
setlocal EnableDelayedExpansion


REM 檢查是否有git repo
git rev-parse --is-inside-work-tree >nul 2>&1
IF ERRORLEVEL 1 (
    echo 當前資料夾無 git repository
    echo.
    pause
    exit /b 1
)

REM 顯示分支
for /f "delims=" %%B in ('git branch --show-current') do set CUR_BRANCH=%%B
echo 目前正確的分支: %CUR_BRANCH%

IF /I NOT "%CUR_BRANCH%"=="main" (
    echo 只能在 main 分支建立 tag，目前分支是 %CUR_BRANCH%
    echo.
    pause
    exit /b 1
)

REM 更新版本
echo.
echo 更新程式版本從： origin/%CUR_BRANCH% ...
git pull origin %CUR_BRANCH%

IF ERRORLEVEL 1 (
    echo 更新失敗
    echo.
    pause
    exit /b 1
)


REM 檢查檔案是否為空
git status
echo.
set /p CONTINUE=Continue tagging? (y/n):
if /I not "%CONTINUE%"=="y" (
    echo Tagging cancelled.
    pause
    exit /b 0
)

echo 當前專案的tag版次(以最新時間排序)，如下：
git tag --sort=-creatordate

REM 輸入這次版本tag名稱
echo.
set /p TAG_NAME=請輸入tag名稱：

REM tag不能是空的
IF "%TAG_NAME%"=="" (
    echo tag名稱不能是空的.
    echo.
    pause
    exit /b 1
)

REM 檢查tag是否已經存在
git rev-parse "%TAG_NAME%" >nul 2>&1
IF NOT ERRORLEVEL 1 (
    echo Tag "%TAG_NAME%" 已經存在.
    echo.
    pause
    exit /b 1
)

REM 開始建立tag
echo.
echo 建立tag "%TAG_NAME%" ...
git tag -a "%TAG_NAME%" -m "%TAG_NAME%"

IF %ERRORLEVEL% NEQ 0 (
    echo tag建立失敗.
    echo.
    pause
    exit /b 1
)

REM 推送tag
echo 推送 tag "%TAG_NAME%" 到遠端庫 ...
git push origin "%TAG_NAME%"

IF ERRORLEVEL 1 (
    echo 推送失敗.
    echo.
    pause
    exit /b 1
)

REM 完成
echo.
echo Tag "%TAG_NAME%" 創建與推送完成
echo.
pause
endlocal
