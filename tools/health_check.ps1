# ElSuarKu - Health Check (No Firebase login required)
param([switch]$Json)

$root = Split-Path -Parent $PSScriptRoot
$results = @()

# 1. google-services.json
$gs = "$root\app\google-services.json"
if (Test-Path $gs) {
    $j = Get-Content $gs -Raw | ConvertFrom-Json
    $results += @{n="google-services.json"; s="OK"; d=$j.project_info.project_id}
} else { $results += @{n="google-services.json"; s="MISSING"; d="Download dari Firebase Console"} }

# 2. Web Client ID
$xml = Get-Content "$root\app\src\main\res\values\strings.xml" -Raw
if ($xml -match "YOUR_WEB_CLIENT_ID") { $results += @{n="Web Client ID"; s="WARN"; d="Placeholder - update dengan Web Client ID asli"} }
elseif ($xml -match "apps\.googleusercontent\.com") { $results += @{n="Web Client ID"; s="OK"; d="Terkonfigurasi"} }
else { $results += @{n="Web Client ID"; s="WARN"; d="Tidak terdeteksi"} }

# 3. Firestore Rules
if (Test-Path "$root\firestore.rules") { $results += @{n="firestore.rules"; s="OK"; d="RBAC rules ready"} }
else { $results += @{n="firestore.rules"; s="MISSING"; d="File tidak ditemukan"} }

# 4. Firestore Indexes
if (Test-Path "$root\firestore.indexes.json") {
    $idxContent = Get-Content "$root\firestore.indexes.json" -Raw | ConvertFrom-Json
    $results += @{n="Firestore Indexes"; s="OK"; d="$($idxContent.indexes.Count) indexes defined"}
} else { $results += @{n="Firestore Indexes"; s="MISSING"; d="File tidak ditemukan"} }

# 5. Android SDK
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $sdk) { $results += @{n="Android SDK"; s="OK"; d=$sdk} }
else { $results += @{n="Android SDK"; s="MISSING"; d="Install via Android Studio"} }

# 6. Gradle Wrapper
if (Test-Path "$root\gradlew.bat") { $results += @{n="Gradle Wrapper"; s="OK"; d="Ready"} }
else { $results += @{n="Gradle Wrapper"; s="MISSING"; d="Missing"} }

# 7. Kotlin source files
$ktCount = (Get-ChildItem "$root\app\src\main\java" -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue).Count
$results += @{n="Kotlin Files"; s="OK"; d="$ktCount files"}

if ($Json) {
    $results | ConvertTo-Json
} else {
    Write-Host ""
    Write-Host "ElSuarKu - Health Check" -ForegroundColor Cyan
    Write-Host "========================" -ForegroundColor Cyan
    foreach ($r in $results) {
        $icon = if ($r.s -eq "OK") { "[OK]" } elseif ($r.s -eq "WARN") { "[WARN]" } else { "[FAIL]" }
        $color = if ($r.s -eq "OK") { "Green" } elseif ($r.s -eq "WARN") { "Yellow" } else { "Red" }
        Write-Host "$icon $($r.n): $($r.s) - $($r.d)" -ForegroundColor $color
    }
    $ok = ($results | Where-Object { $_.s -eq "OK" }).Count
    $bad = ($results | Where-Object { $_.s -ne "OK" }).Count
    Write-Host ""
    if ($bad -eq 0) { Write-Host "ALL CLEAN - Ready to build!" -ForegroundColor Green }
    else { Write-Host "$ok/$($results.Count) OK - $bad issues need attention" -ForegroundColor Yellow }
    Write-Host ""
}
