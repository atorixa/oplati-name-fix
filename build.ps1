# Builds the Oplati Name Fix Xposed module APK without Gradle.
# Requires: JDK 17+, Android SDK build-tools (d8 / aapt2 / zipalign / apksigner)
# and platforms/android-36/android.jar.
#
#   pwsh -NoProfile -File build.ps1
#   pwsh -NoProfile -File build.ps1 -Sdk "C:\Android\sdk"
param(
    [string]$Sdk = $(if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:LOCALAPPDATA\Android\Sdk" })
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root   = Split-Path -Parent $MyInvocation.MyCommand.Path
$app    = Join-Path $root 'app'
$out    = Join-Path $root 'build'
$isWin  = if ($null -ne $IsWindows) { $IsWindows } else { $env:OS -eq 'Windows_NT' }
$aj     = Join-Path $Sdk 'platforms\android-36\android.jar'
$btDir  = Join-Path $Sdk 'build-tools'
$bt     = Get-ChildItem -Directory $btDir | Sort-Object Name -Descending | Select-Object -First 1
$ks     = if ($env:KEYSTORE_FILE) { $env:KEYSTORE_FILE } else { Join-Path $root 'keystore.jks' }
$ksPass = if ($env:KEYSTORE_PASS) { $env:KEYSTORE_PASS } else { 'android' }
$ksAlias = if ($env:KEYSTORE_ALIAS) { $env:KEYSTORE_ALIAS } else { 'test' }

if (-not (Test-Path $aj)) { throw "android.jar not found: $aj" }
if (-not $bt) { throw "build-tools not found under $btDir" }
if (-not (Test-Path $ks)) { throw "keystore.jks not found in project root (see README)" }

$bt = $bt.FullName
$classes = Join-Path $out 'classes'
$dex     = Join-Path $out 'dex'
$aligned = Join-Path $out 'aligned.apk'
$unsigned = Join-Path $out 'unsigned.apk'
$final   = Join-Path $out 'OplatiNameFix.apk'

if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Path $classes, $dex, $out -Force | Out-Null

$stubs = @(Get-ChildItem -Recurse -File (Join-Path $root 'stubs') -Filter *.java | ForEach-Object { $_.FullName })
$srcs  = @(Get-ChildItem -Recurse -File (Join-Path $app 'src') -Filter *.java | ForEach-Object { $_.FullName })

Write-Host "javac"
& javac -source 11 -target 11 -nowarn -cp $aj -d $classes @stubs @srcs
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host "d8"
$d8 = if ($isWin) { Join-Path $bt 'd8.bat' } else { Join-Path $bt 'd8' }
$cls = @(Get-ChildItem -Recurse -File (Join-Path $classes 'by') -Filter *.class | ForEach-Object { $_.FullName })
& $d8 --release --min-api 28 --lib $aj --output $dex @cls
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host "aapt2 link"
$aapt2 = if ($isWin) { Join-Path $bt 'aapt2.exe' } else { Join-Path $bt 'aapt2' }
& $aapt2 link -o $unsigned --manifest (Join-Path $app 'AndroidManifest.xml') `
    -I $aj --min-sdk-version 28 --target-sdk-version 36
if ($LASTEXITCODE -ne 0) { throw 'aapt2 failed' }

Write-Host "packing classes.dex + assets"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($unsigned, 'Update')
try {
    $entries = @(
        @{ file = (Join-Path $dex 'classes.dex'); entry = 'classes.dex' },
        @{ file = (Join-Path $app 'assets\xposed_init'); entry = 'assets/xposed_init' }
    )
    foreach ($e in $entries) {
        $old = $zip.GetEntry($e.entry)
        if ($old) { $old.Delete() }
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $e.file, $e.entry, [System.IO.Compression.CompressionLevel]::Optimal)
    }
} finally {
    $zip.Dispose()
}

Write-Host "zipalign"
$zipalign = if ($isWin) { Join-Path $bt 'zipalign.exe' } else { Join-Path $bt 'zipalign' }
& $zipalign -p -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

Write-Host "apksigner"
$apksigner = if ($isWin) { Join-Path $bt 'apksigner.bat' } else { Join-Path $bt 'apksigner' }
& $apksigner sign --ks $ks --ks-key-alias $ksAlias `
    --ks-pass "pass:$ksPass" --key-pass "pass:$ksPass" --out $final $aligned
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

$apk = Get-Item $final
Write-Host ("OK: {0} ({1} bytes)" -f $apk.FullName, $apk.Length)
