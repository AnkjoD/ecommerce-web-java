$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$stateFile = Join-Path $projectRoot '.demo/processes.json'
if (Test-Path -LiteralPath $stateFile) {
    foreach ($item in @(Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json)) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($item.Id)" -ErrorAction SilentlyContinue
        # PIDs can be reused. Only stop the recorded processes if their command still belongs to this project.
        if ($process -and $process.CommandLine -and $process.CommandLine.Contains($projectRoot)) {
            Stop-Process -Id $item.Id -ErrorAction SilentlyContinue
        }
    }
}
docker stop ecommerce-java-demo-db | Out-Null
Write-Host 'Demo stopped. Database data remains in the demo container.'
