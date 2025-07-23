@echo off
SET ANDROID_NDK=C:\Users\Ionut\AppData\Local\Android\Sdk\ndk\25.1.8937393
SET ABI=x86_64
SET API_LEVEL=21
SET BUILD_DIR=build

REM Verificare existență cale NDK
IF NOT EXIST "%ANDROID_NDK%" (
    echo NDK path not found at: %ANDROID_NDK%
    exit /b 1
)

REM Verificare existență toolchain
SET TOOLCHAIN=%ANDROID_NDK%\toolchains\llvm\prebuilt\windows-x86_64
IF NOT EXIST "%TOOLCHAIN%" (
    echo Toolchain not found at: %TOOLCHAIN%
    echo Available directories in prebuilt:
    dir "%ANDROID_NDK%\toolchains\llvm\prebuilt"
    exit /b 1
)

REM Creați directorul de build dacă nu există
IF NOT EXIST %BUILD_DIR% mkdir %BUILD_DIR%
cd %BUILD_DIR%

REM Specificați explicit calea către compilatoare
cmake .. ^
    -G "Ninja" ^
    -DCMAKE_TOOLCHAIN_FILE=%ANDROID_NDK%\build\cmake\android.toolchain.cmake ^
    -DANDROID_ABI=%ABI% ^
    -DANDROID_PLATFORM=android-%API_LEVEL% ^
    -DANDROID_STL=c++_shared ^
    -DCMAKE_BUILD_TYPE=Release ^
    -DBUILD_SHARED_LIBS=ON ^
    -DOQS_BUILD_ONLY_LIB=ON ^
    -DOQS_USE_OPENSSL=OFF ^
    -DCMAKE_C_COMPILER=%TOOLCHAIN%\bin\clang.exe ^
    -DCMAKE_ASM_COMPILER=%TOOLCHAIN%\bin\clang.exe

cmake --build .

cd ..