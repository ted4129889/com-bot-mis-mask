@echo off
setlocal EnableDelayedExpansion

set /p OLD_TAG=輸入前一版的tag (如 release-20260110): 
set /p NEW_TAG=輸入最新版的tag (如 release-20260111): 


REM 檢查tag
git rev-parse "%OLD_TAG%" >nul 2>&1
IF ERRORLEVEL 1 (
    echo "%OLD_TAG%" tag 不存在.
    pause
    exit /b 1
)

REM 檢查tag
git rev-parse "%NEW_TAG%" >nul 2>&1
IF ERRORLEVEL 1 (
    echo "%NEW_TAG%" tag 不存在.
    pause
    exit /b 1
)

REM 輸出檔案名稱(
SET RAW_FILE=diff.txt
SET SORTED_FILE=diff_list_%NEW_TAG%.txt

REM 產生差異清單
echo.
echo 產生差異清單的版本來自 old tag: %OLD_TAG% , new tag: %NEW_TAG%
git diff --name-status %OLD_TAG%..%NEW_TAG% > %RAW_FILE%

IF ERRORLEVEL 1 (
    echo ERROR: git diff failed. Please check tag names.
    exit /b 1
)

REM 先清空檔案內容
type nul > %SORTED_FILE%

REM 列出新增的程式清單
echo Added:>>%SORTED_FILE%
for /f "tokens=1,* delims=	 " %%A in (%RAW_FILE%) do (
    if "%%A"=="A" (
        echo - %%B>>%SORTED_FILE%
    )
)
echo.>>%SORTED_FILE%

REM 列出修改的程式清單
echo Modified:>>%SORTED_FILE%
for /f "tokens=1,* delims=	 " %%A in (%RAW_FILE%) do (
    if "%%A"=="M" (
        echo - %%B>>%SORTED_FILE%
    )
)
echo.>>%SORTED_FILE%

REM 列出刪除的程式清單
echo Deleted:>>%SORTED_FILE%
for /f "tokens=1,* delims=	 " %%A in (%RAW_FILE%) do (
    if "%%A"=="D" (
        echo - %%B>>%SORTED_FILE%
    )
)

REM 輸出排序清單後，將原清單刪除
if exist "%RAW_FILE%" del "%RAW_FILE%"

REM 完成訊息
echo 產生差異清單
echo 比較的版本tag : %OLD_TAG% 和 %NEW_TAG%
echo 輸出清單  : %SORTED_FILE%

endlocal
pause
