# Smart Bank Management System (SBMS) Build & Test Automation Script
param(
    [string]$Action = "all" # options: "compile", "test", "run", "all"
)

$ErrorActionPreference = "Stop"

$jdkPath = "C:\Program Files\Java\jdk-26.0.2"
if (-not (Test-Path $jdkPath)) {
    $jdkPath = (Get-ChildItem -Path "C:\Program Files\Java" -Filter "*jdk*" | Select-Object -First 1).FullName
}

$javac = Join-Path $jdkPath "bin\javac.exe"
$java = Join-Path $jdkPath "bin\java.exe"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetDir = Join-Path $projectRoot "target"
$classesDir = Join-Path $targetDir "classes"
$testClassesDir = Join-Path $targetDir "test-classes"
$libDir = Join-Path $targetDir "lib"
$junitJar = Join-Path $libDir "junit-platform-console-standalone-1.10.2.jar"

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Smart Bank Management System (SBMS) - Build & Test Runner" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "JDK: $jdkPath" -ForegroundColor Gray

# Ensure directories
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $testClassesDir | Out-Null
New-Item -ItemType Directory -Force -Path $libDir | Out-Null

# 1. Compile Main Sources
Write-Host "[1/3] Compiling main application sources..." -ForegroundColor Yellow
$mainSources = Get-ChildItem -Path (Join-Path $projectRoot "src\main\java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
& $javac -d $classesDir -encoding UTF-8 $mainSources
if ($LASTEXITCODE -ne 0) {
    Write-Error "Main source compilation failed!"
    exit 1
}
Write-Host "Main sources compiled successfully." -ForegroundColor Green

if ($Action -eq "compile") {
    exit 0
}

# Download JUnit & MySQL connector if missing
if (-not (Test-Path $junitJar)) {
    Write-Host "Fetching JUnit 5 Console Standalone Runner..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar" -OutFile $junitJar
}

$mysqlJar = Join-Path $libDir "mysql-connector-j-8.3.0.jar"
if (-not (Test-Path $mysqlJar)) {
    Write-Host "Fetching MySQL Connector/J JDBC Driver..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar" -OutFile $mysqlJar
}

# 3. Compile & Run Tests
if ($Action -eq "test" -or $Action -eq "all") {
    Write-Host "[2/3] Compiling test sources..." -ForegroundColor Yellow
    $testSources = Get-ChildItem -Path (Join-Path $projectRoot "src\test\java") -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
    & $javac -cp "$classesDir;$junitJar;$mysqlJar" -d $testClassesDir -encoding UTF-8 $testSources
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Test source compilation failed!"
        exit 1
    }

    Write-Host "[3/3] Executing JUnit 5 Test Suites & Concurrency Stress Test..." -ForegroundColor Yellow
    & $java -jar $junitJar --class-path "$classesDir;$testClassesDir;$mysqlJar" --scan-class-path --fail-if-no-tests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Unit or Concurrency Tests Failed!"
        exit 1
    }
    Write-Host "All unit and concurrency tests passed successfully!" -ForegroundColor Green
}

# 4. Launch GUI if requested
if ($Action -eq "run") {
    Write-Host "Launching Smart Bank AWT GUI Console..." -ForegroundColor Cyan
    & $java -cp "$classesDir;$mysqlJar" com.smartbank.Main
}

# 5. Launch Live MySQL Interactive Demo if requested
if ($Action -eq "db-demo") {
    Write-Host "Launching Smart Bank Live MySQL JDBC Demo..." -ForegroundColor Cyan
    & $java -cp "$classesDir;$mysqlJar" com.smartbank.DatabaseDemo
}
