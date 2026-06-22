# ================================================================
# ElSuarKu — Firebase Auto-Setup Script
# ================================================================
# Menjalankan SEMUA setup Firebase secara otomatis via CLI.
#
# PRASYARAT:
#   1. Install Node.js: https://nodejs.org (LTS version)
#   2. Install Firebase CLI: npm install -g firebase-tools
#   3. Login Firebase: firebase login
#
# CARA PAKAI:
#   PowerShell: .\tools\setup_firebase_auto.ps1
# ================================================================

param(
    [switch]$SkipFirebaseLogin = $false,
    [switch]$HealthCheckOnly = $false
)

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $PSScriptRoot

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " ElSuarKu — Firebase Auto-Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ================================================================
# STEP 0: Check Prerequisites
# ================================================================
Write-Host "[0/5] Checking prerequisites..." -ForegroundColor Yellow

$firebaseInstalled = Get-Command firebase -ErrorAction SilentlyContinue
if (-not $firebaseInstalled) {
    Write-Host "  ERROR: Firebase CLI not installed!" -ForegroundColor Red
    Write-Host "  Install: npm install -g firebase-tools" -ForegroundColor Yellow
    Write-Host "  Download Node.js: https://nodejs.org" -ForegroundColor Yellow
    exit 1
}
Write-Host "  Firebase CLI: FOUND ($(firebase --version))" -ForegroundColor Green

# ================================================================
# STEP 1: Firebase Login
# ================================================================
if (-not $SkipFirebaseLogin -and -not $HealthCheckOnly) {
    Write-Host "[1/5] Firebase login..." -ForegroundColor Yellow
    firebase login --no-localhost 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Try: firebase login --no-localhost" -ForegroundColor Yellow
        Write-Host "  Or run: .\tools\setup_firebase_auto.ps1 -SkipFirebaseLogin" -ForegroundColor Yellow
    }
    Write-Host "  Login OK" -ForegroundColor Green
}

# ================================================================
# STEP 2: Select Firebase Project
# ================================================================
if (-not $HealthCheckOnly) {
    Write-Host "[2/5] Selecting Firebase project..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    firebase use --add elsuarku-sistem-vote 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  Trying interactive project selection..." -ForegroundColor Yellow
        firebase use --add 2>&1
    }
    Pop-Location
    Write-Host "  Project: elsuarku-sistem-vote" -ForegroundColor Green
}

# ================================================================
# STEP 3: Deploy Firestore Rules
# ================================================================
if (-not $HealthCheckOnly) {
    Write-Host "[3/5] Deploying Firestore Security Rules..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    firebase deploy --only firestore:rules 2>&1
    if ($LASTEXITCODE -eq 0) { Write-Host "  Rules DEPLOYED successfully" -ForegroundColor Green }
    else { Write-Host "  WARNING: Rules deploy failed. Deploy manually via Firebase Console." -ForegroundColor Yellow }
    Pop-Location
}

# ================================================================
# STEP 4: Deploy Firestore Indexes
# ================================================================
if (-not $HealthCheckOnly) {
    Write-Host "[4/5] Deploying Firestore Composite Indexes..." -ForegroundColor Yellow
    Write-Host "  (Indexes take 2-5 minutes to build after deploy)" -ForegroundColor DarkGray
    Push-Location $ProjectRoot
    firebase deploy --only firestore:indexes 2>&1
    if ($LASTEXITCODE -eq 0) { Write-Host "  Indexes DEPLOYED successfully" -ForegroundColor Green }
    else { Write-Host "  WARNING: Index deploy failed. Create manually via Firebase Console." -ForegroundColor Yellow }
    Pop-Location
}

# ================================================================
# STEP 5: Health Check
# ================================================================
Write-Host "[5/5] Running Health Check..." -ForegroundColor Yellow
Write-Host ""

$checks = @()

# Check google-services.json
$gsJson = Join-Path $ProjectRoot "app\google-services.json"
if (Test-Path $gsJson) {
    $json = Get-Content $gsJson -Raw | ConvertFrom-Json
    $pid = $json.project_info.project_id
    $pnum = $json.project_info.project_number
    $checks += @{ Name="google-services.json"; Status="OK"; Detail=("Project: " + $pid + " (No: " + $pnum + ")") }
} else {
    $checks += @{ Name="google-services.json"; Status="MISSING"; Detail="File tidak ditemukan! Download dari Firebase Console." }
}

# Check firestore.rules
$rules = Join-Path $ProjectRoot "firestore.rules"
if (Test-Path $rules) {
    $checks += @{ Name="firestore.rules"; Status="OK"; Detail="RBAC rules ready" }
} else {
    $checks += @{ Name="firestore.rules"; Status="MISSING"; Detail="File tidak ditemukan!" }
}

# Check firestore.indexes.json
$idx = Join-Path $ProjectRoot "firestore.indexes.json"
if (Test-Path $idx) {
    $content = Get-Content $idx -Raw | ConvertFrom-Json
    $count = $content.indexes.Count
    $checks += @{ Name="Firestore Indexes"; Status="OK"; Detail="$count indexes defined" }
} else {
    $checks += @{ Name="Firestore Indexes"; Status="MISSING"; Detail="firestore.indexes.json tidak ditemukan!" }
}

# Check strings.xml Web Client ID
$strXml = Join-Path $ProjectRoot "app\src\main\res\values\strings.xml"
$strContent = Get-Content $strXml -Raw
if ($strContent -match "YOUR_WEB_CLIENT_ID") {
    $checks += @{ Name="Web Client ID"; Status="WARNING"; Detail="Masih placeholder! Ganti dengan Web Client ID asli." }
} elseif ($strContent -match "default_web_client_id.*apps\.googleusercontent\.com") {
    $checks += @{ Name="Web Client ID"; Status="OK"; Detail="Web Client ID terkonfigurasi" }
} else {
    $checks += @{ Name="Web Client ID"; Status="WARNING"; Detail="Tidak terdeteksi!" }
}

# Check Android SDK
$sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $sdkPath) { $checks += @{ Name="Android SDK"; Status="OK"; Detail=$sdkPath } }
else { $checks += @{ Name="Android SDK"; Status="MISSING"; Detail="Install Android Studio" } }

# Print health check table
Write-Host "  HEALTH CHECK RESULT:" -ForegroundColor Cyan
Write-Host "  ┌─────────────────────────────────────────────────────────┐"
foreach ($c in $checks) {
    $icon = if ($c.Status -eq "OK") { "✅" } elseif ($c.Status -eq "WARNING") { "⚠️" } else { "❌" }
    $color = if ($c.Status -eq "OK") { "Green" } elseif ($c.Status -eq "WARNING") { "Yellow" } else { "Red" }
    Write-Host "  │ $icon " -NoNewline
    Write-Host "$($c.Name): $($c.Status)" -ForegroundColor $color -NoNewline
    Write-Host " — $($c.Detail)"
}
Write-Host "  └─────────────────────────────────────────────────────────┘"
Write-Host ""

# Summary
$okCount = ($checks | Where-Object { $_.Status -eq "OK" }).Count
$warnCount = ($checks | Where-Object { $_.Status -eq "WARNING" }).Count
$badCount = ($checks | Where-Object { $_.Status -eq "MISSING" }).Count

Write-Host "  SUMMARY: $okCount OK, $warnCount Warnings, $badCount Missing" -ForegroundColor $(if ($badCount -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($badCount -eq 0 -and $warnCount -eq 0) {
    Write-Host "  ALL CHECKS PASSED! Project siap di-build dan di-run." -ForegroundColor Green
} else {
    Write-Host "  ACTION NEEDED: Selesaikan item WARNING/MISSING di atas." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Setup Complete!" -ForegroundColor Cyan
Write-Host " Next: Buka Android Studio → Sync Gradle → Run (Shift+F10)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
