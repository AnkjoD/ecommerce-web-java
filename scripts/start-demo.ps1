param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$demoDir = Join-Path $projectRoot '.demo'
New-Item -ItemType Directory -Path $demoDir -Force | Out-Null
foreach ($demoPort in @(3000, 6000, 8081)) {
    if (Get-NetTCPConnection -LocalPort $demoPort -State Listen -ErrorAction SilentlyContinue) {
        throw "Port $demoPort is already in use. Stop the existing demo before starting another instance."
    }
}
$dbExists = docker container ls -a --filter 'name=^/ecommerce-java-demo-db$' --format '{{.Names}}'
if ($dbExists -eq 'ecommerce-java-demo-db') {
    docker start ecommerce-java-demo-db | Out-Null
} else {
    docker run -d --name ecommerce-java-demo-db -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo_local_only -e POSTGRES_DB=ecommerce_demo -p 127.0.0.1:55432:5432 pgvector/pgvector:pg17 | Out-Null
}
if ($LASTEXITCODE -ne 0) { throw 'Could not start demo database' }
$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    docker exec ecommerce-java-demo-db pg_isready -U demo -d ecommerce_demo *> $null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw 'Demo database did not become ready' }
$serverDir = Join-Path $projectRoot 'server/ecommerce'
if (-not $SkipBuild) {
    Push-Location $serverDir
    try {
        & .\gradlew.bat bootJar --console=plain --max-workers=1 -I (Join-Path $PSScriptRoot 'demo-build.gradle')
        if ($LASTEXITCODE -ne 0) { throw 'Java build failed' }
    } finally { Pop-Location }
}
$builtJar = Join-Path $serverDir 'build/demo/libs/ecommerce-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $builtJar)) { throw 'Build the demo first; the JAR is missing' }
$runtimeJar = Join-Path $demoDir 'app.jar'
Copy-Item -LiteralPath $builtJar -Destination $runtimeJar -Force
$savedEnvironment = @{}
$demoEnvironment = @{
    DATABASE_URL='jdbc:postgresql://localhost:55432/ecommerce_demo'; DB_USERNAME='demo'; DB_PASSWORD='demo_local_only';
    VITE_PROXY_TARGET='http://127.0.0.1:6000'; SERVER_PORT='6000'; SERVER_ADDRESS='127.0.0.1'; SPRING_PROFILES_ACTIVE='demo'; COOKIE_SECURE='false';
    JWT_ACCESS_SECRET='demo_local_access_secret_0123456789_0123456789'; JWT_REFRESH_SECRET='demo_local_refresh_secret_0123456789_0123456789'
}
$demoProcesses = @()
try {
    foreach ($entry in $demoEnvironment.GetEnumerator()) {
        $savedEnvironment[$entry.Key] = [Environment]::GetEnvironmentVariable($entry.Key, 'Process')
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
    $javaCommand = (Get-Command java).Source
    $serverProcess = Start-Process -FilePath $javaCommand -ArgumentList @('-Xmx256m', '-jar', ('"' + $runtimeJar + '"')) -WorkingDirectory $serverDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $demoDir 'server.log') -RedirectStandardError (Join-Path $demoDir 'server-error.log')
    $demoProcesses += @{ Id=$serverProcess.Id; Kind='server' }
    $serverReady = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($serverProcess.HasExited) { throw 'Java server exited; see .demo/server-error.log' }
        try {
            $result = Invoke-RestMethod -Uri 'http://127.0.0.1:6000/api/products' -TimeoutSec 5
            if ($result.success) { $serverReady=$true; break }
        } catch { }
        Start-Sleep -Seconds 1
    }
    if (-not $serverReady) { throw 'Server did not become ready; see .demo/server.log' }
    $nodeCommand = (Get-Command node).Source
    foreach ($frontend in @(@{Name='client'; Port=3000}, @{Name='admin'; Port=8081})) {
        $frontendDir = Join-Path $projectRoot $frontend.Name
        $vitePath = Join-Path $frontendDir 'node_modules/vite/bin/vite.js'
        if (-not (Test-Path -LiteralPath $vitePath)) { throw "Install dependencies in $($frontend.Name) first with npm ci" }
        $process = Start-Process -FilePath $nodeCommand -ArgumentList @(('"' + $vitePath + '"'), '--host', '127.0.0.1', '--port', $frontend.Port, '--strictPort') -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $demoDir "$($frontend.Name).log") -RedirectStandardError (Join-Path $demoDir "$($frontend.Name)-error.log")
        $demoProcesses += @{Id=$process.Id; Kind=$frontend.Name}
    }
    $demoProcesses | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $demoDir 'processes.json')
    Write-Host 'Shop: http://localhost:3000 | Admin: http://localhost:8081'
} catch {
    foreach ($item in $demoProcesses) { Stop-Process -Id $item.Id -ErrorAction SilentlyContinue }
    throw
} finally {
    foreach ($entry in $savedEnvironment.GetEnumerator()) { [Environment]::SetEnvironmentVariable($entry.Key,$entry.Value,'Process') }
}
