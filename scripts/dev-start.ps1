param(
    [ValidateSet("start", "stop", "restart", "status")]
    [string]$Action = "start",
    [switch]$NoInfra,
    [switch]$StopInfra,
    [switch]$SkipInstall,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host @"
Usage: powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 [-Action <action>] [options]

Actions:
  start      Start the local dev stack. This is the default action.
  stop       Stop backend and admin web processes recorded in .run/*.pid.
  restart    Stop recorded processes, then start the local dev stack.
  status     Show recorded process status and Docker Compose service status.

Options:
  -NoInfra      Do not start PostgreSQL/Redis during start or restart.
  -StopInfra    Also stop PostgreSQL/Redis during stop or restart.
  -SkipInstall  Do not run npm install when frontend/admin/node_modules is missing.
  -Help         Show this help message.

Default ports:
  Backend:    http://localhost:18080   (BACKEND_PORT)
  Admin web:  http://localhost:15173   (ADMIN_WEB_PORT)
  PostgreSQL: localhost:5432           (POSTGRES_PORT)
  Redis:      localhost:6379           (REDIS_PORT)
  Public IP:  82.156.67.222            (PUBLIC_HOST)

Notes:
  - start runs backend/admin web in the background, writes logs to .run/logs,
    waits for readiness, prints SUCCESS, then exits.
  - The script checks ports before startup and exits if backend/admin ports are
    already in use.
  - PostgreSQL/Redis ports are allowed only when already used by this project's
    Docker Compose services.
  - The script clears proxy environment variables in its own process.
  - stop only stops processes recorded by this script; it does not kill unrelated
    processes that happen to use the same ports.
"@
}

if ($Help) {
    Show-Help
    exit 0
}

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$BackendDir = Join-Path $RootDir "backend"
$AdminDir = Join-Path $RootDir "frontend/admin"
$RunDir = Join-Path $RootDir ".run"
$LogDir = Join-Path $RunDir "logs"
$BackendPidFile = Join-Path $RunDir "backend.pid"
$FrontendPidFile = Join-Path $RunDir "admin-web.pid"
$BackendLog = Join-Path $LogDir "backend.log"
$BackendErrorLog = Join-Path $LogDir "backend.err.log"
$FrontendLog = Join-Path $LogDir "admin-web.log"
$FrontendErrorLog = Join-Path $LogDir "admin-web.err.log"
$BackendPort = if ($Env:BACKEND_PORT) { [int]$Env:BACKEND_PORT } else { 18080 }
$AdminWebPort = if ($Env:ADMIN_WEB_PORT) { [int]$Env:ADMIN_WEB_PORT } else { 15173 }
$PostgresPort = if ($Env:POSTGRES_PORT) { [int]$Env:POSTGRES_PORT } else { 5432 }
$RedisPort = if ($Env:REDIS_PORT) { [int]$Env:REDIS_PORT } else { 6379 }
$PublicHost = if ($Env:PUBLIC_HOST) { $Env:PUBLIC_HOST } else { "82.156.67.222" }
$StartTimeoutSeconds = if ($Env:START_TIMEOUT_SECONDS) { [int]$Env:START_TIMEOUT_SECONDS } else { 90 }

Remove-Item Env:http_proxy -ErrorAction SilentlyContinue
Remove-Item Env:https_proxy -ErrorAction SilentlyContinue
Remove-Item Env:all_proxy -ErrorAction SilentlyContinue
Remove-Item Env:HTTP_PROXY -ErrorAction SilentlyContinue
Remove-Item Env:HTTPS_PROXY -ErrorAction SilentlyContinue
Remove-Item Env:ALL_PROXY -ErrorAction SilentlyContinue

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

function Test-DockerAccess {
    Require-Command docker
    docker ps *> $null
    if ($LASTEXITCODE -eq 0) {
        return
    }

    throw @"
Cannot access Docker daemon from this PowerShell session.

On Linux, add the user to the docker group and refresh the login session:
  sudo usermod -aG docker `$USER
  newgrp docker

Immediate workaround:
  sudo docker compose up -d postgres redis
  powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action start -NoInfra
"@
}

function Test-PortInUse {
    param([int]$Port)
    if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
        return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    }
    $Pattern = "[:.]$Port\s"
    return [bool]((netstat -ano 2>$null | Select-String -Pattern $Pattern | Select-String -Pattern "LISTEN") -or
        (netstat -an 2>$null | Select-String -Pattern $Pattern | Select-String -Pattern "LISTEN"))
}

function Show-PortOwner {
    param([int]$Port)
    if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
        Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
            Select-Object LocalAddress, LocalPort, OwningProcess |
            Format-Table -AutoSize | Out-String | Write-Host
        return
    }
    netstat -ano 2>$null | Select-String -Pattern "[:.]$Port\s" | Write-Host
}

function Test-ComposeServiceRunning {
    param([string]$Service)
    Push-Location $RootDir
    try {
        $Services = docker compose ps --status running --services $Service 2>$null
        return $Services -contains $Service
    }
    finally {
        Pop-Location
    }
}

function Assert-PortFree {
    param([int]$Port, [string]$Label)
    if (Test-PortInUse $Port) {
        Write-Host "Port $Port is already in use; cannot start $Label." -ForegroundColor Red
        Show-PortOwner $Port
        throw "Stop the existing process or change its port before rerunning this script."
    }
}

function Assert-InfraPortAvailable {
    param([int]$Port, [string]$Label, [string]$Service)
    if (Test-PortInUse $Port) {
        if (Test-ComposeServiceRunning $Service) {
            Write-Host "$Label port $Port is already used by this project's Docker Compose service; reusing it."
            return
        }
        Write-Host "Port $Port is already in use; cannot start $Label." -ForegroundColor Red
        Show-PortOwner $Port
        throw "Stop the existing service, change $Label port, or use -NoInfra if infrastructure is already managed elsewhere."
    }
}

function Read-PidFile {
    param([string]$Path)
    if (Test-Path $Path) {
        $Text = (Get-Content $Path -Raw).Trim()
        if ($Text -match '^\d+$') {
            return [int]$Text
        }
    }
    return $null
}

function Get-RecordedProcess {
    param([string]$Path)
    $PidValue = Read-PidFile $Path
    if ($null -eq $PidValue) {
        return $null
    }
    return Get-Process -Id $PidValue -ErrorAction SilentlyContinue
}

function Show-OneStatus {
    param([string]$Name, [string]$Path)
    $PidValue = Read-PidFile $Path
    $Process = Get-RecordedProcess $Path
    if ($Process) {
        Write-Host "$Name: running (pid $PidValue)"
    }
    elseif ($PidValue) {
        Write-Host "$Name: stopped (stale pid $PidValue)"
    }
    else {
        Write-Host "$Name: stopped"
    }
}

function Stop-One {
    param([string]$Name, [string]$Path)
    $PidValue = Read-PidFile $Path
    $Process = Get-RecordedProcess $Path
    if ($Process) {
        Write-Host "Stopping $Name (pid $PidValue)..."
        if (Get-Command taskkill -ErrorAction SilentlyContinue) {
            taskkill /PID $PidValue /T /F *> $null
        }
        else {
            Stop-Process -Id $PidValue -Force -ErrorAction SilentlyContinue
        }
    }
    elseif ($PidValue) {
        Write-Host "$Name is not running (stale pid $PidValue)."
    }
    else {
        Write-Host "$Name is not running."
    }
    Remove-Item $Path -ErrorAction SilentlyContinue
}

function Stop-Stack {
    Stop-One "admin web" $FrontendPidFile
    Stop-One "backend" $BackendPidFile
    if ($StopInfra) {
        Test-DockerAccess
        Write-Host "Stopping PostgreSQL and Redis..."
        Push-Location $RootDir
        try {
            docker compose stop postgres redis
        }
        finally {
            Pop-Location
        }
    }
}

function Show-Status {
    Show-OneStatus "backend" $BackendPidFile
    Show-OneStatus "admin web" $FrontendPidFile
    Write-Host "Backend log:   $BackendLog"
    Write-Host "Admin web log: $FrontendLog"
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        Write-Host ""
        Write-Host "Docker Compose services:"
        Push-Location $RootDir
        try {
            docker compose ps postgres redis
        }
        catch {
            Write-Host "Docker Compose status is not available."
        }
        finally {
            Pop-Location
        }
    }
}

function Write-RecentLog {
    param([string]$Path, [string]$ErrorPath)
    if (Test-Path $Path) {
        Write-Host "Log: $Path"
        Get-Content $Path -Tail 80 -ErrorAction SilentlyContinue | Write-Host
    }
    if (Test-Path $ErrorPath) {
        Write-Host "Error log: $ErrorPath"
        Get-Content $ErrorPath -Tail 80 -ErrorAction SilentlyContinue | Write-Host
    }
}

function Wait-ForUrl {
    param([string]$Label, [string]$Url, [string]$LogPath, [string]$ErrorLogPath)
    $Deadline = (Get-Date).AddSeconds($StartTimeoutSeconds)
    while ((Get-Date) -lt $Deadline) {
        try {
            Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 *> $null
            return
        }
        catch {
            Start-Sleep -Seconds 1
        }
    }

    Write-Host "$Label did not become ready within ${StartTimeoutSeconds}s." -ForegroundColor Red
    Write-RecentLog $LogPath $ErrorLogPath
    throw "$Label startup timed out."
}

function Start-Stack {
    Require-Command java
    Require-Command mvn
    Require-Command node
    Require-Command npm
    if (-not $NoInfra) {
        Test-DockerAccess
        Assert-InfraPortAvailable $PostgresPort "PostgreSQL" "postgres"
        Assert-InfraPortAvailable $RedisPort "Redis" "redis"
    }

    Assert-PortFree $BackendPort "backend"
    Assert-PortFree $AdminWebPort "admin web"

    New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

    Write-Host "HIS dev stack"
    Write-Host "Root: $RootDir"
    Write-Host "Java: $(& java -version 2>&1 | Select-Object -First 1)"
    Write-Host "Node: $(& node -v)"
    Write-Host "npm: $(& npm -v)"

    if (-not $NoInfra) {
        Write-Host "Starting PostgreSQL and Redis..."
        Push-Location $RootDir
        try {
            docker compose up -d postgres redis
        }
        finally {
            Pop-Location
        }
    }

    if (-not $SkipInstall -and -not (Test-Path (Join-Path $AdminDir "node_modules"))) {
        Write-Host "Installing admin web dependencies..."
        Push-Location $AdminDir
        try {
            npm install
        }
        finally {
            Pop-Location
        }
    }

    Clear-Content -Path $BackendLog, $BackendErrorLog, $FrontendLog, $FrontendErrorLog -ErrorAction SilentlyContinue
    New-Item -ItemType File -Force -Path $BackendLog, $BackendErrorLog, $FrontendLog, $FrontendErrorLog | Out-Null

    Write-Host "Starting backend in background: http://localhost:$BackendPort"
    $BackendArgs = @("spring-boot:run", "-Dspring-boot.run.arguments=--server.address=0.0.0.0 --server.port=$BackendPort")
    $Backend = Start-Process -FilePath "mvn" -ArgumentList $BackendArgs -WorkingDirectory $BackendDir -PassThru -WindowStyle Hidden -RedirectStandardOutput $BackendLog -RedirectStandardError $BackendErrorLog
    Set-Content -Path $BackendPidFile -Value $Backend.Id

    Write-Host "Starting admin web in background: http://localhost:$AdminWebPort"
    $Env:BACKEND_PORT = "$BackendPort"
    $Env:VITE_API_PROXY_TARGET = "http://localhost:$BackendPort"
    $RunningOnWindows = $PSVersionTable.PSEdition -eq "Desktop" -or ($PSVersionTable.ContainsKey("Platform") -and $PSVersionTable.Platform -eq "Win32NT")
    $ViteExecutable = if ($RunningOnWindows) { Join-Path $AdminDir "node_modules/.bin/vite.cmd" } else { Join-Path $AdminDir "node_modules/.bin/vite" }
    $Frontend = Start-Process -FilePath $ViteExecutable -ArgumentList "--host", "0.0.0.0", "--port", "$AdminWebPort" -WorkingDirectory $AdminDir -PassThru -WindowStyle Hidden -RedirectStandardOutput $FrontendLog -RedirectStandardError $FrontendErrorLog
    Set-Content -Path $FrontendPidFile -Value $Frontend.Id

    try {
        Wait-ForUrl "Backend" "http://localhost:$BackendPort/api/v1/system/health" $BackendLog $BackendErrorLog
        Wait-ForUrl "Admin web" "http://localhost:$AdminWebPort" $FrontendLog $FrontendErrorLog
    }
    catch {
        Stop-One "admin web" $FrontendPidFile
        Stop-One "backend" $BackendPidFile
        throw
    }

    Write-Host ""
    Write-Host "SUCCESS: dev stack started."
    Write-Host "Backend local:    http://localhost:$BackendPort"
    Write-Host "Backend public:   http://${PublicHost}:$BackendPort"
    Write-Host "Admin web local:  http://localhost:$AdminWebPort"
    Write-Host "Admin web public: http://${PublicHost}:$AdminWebPort"
    Write-Host "PID files: $RunDir"
    Write-Host "Logs: $LogDir"
    Write-Host "Stop: powershell -ExecutionPolicy Bypass -File scripts/dev-start.ps1 -Action stop"
}

switch ($Action) {
    "start" { Start-Stack }
    "stop" { Stop-Stack }
    "restart" {
        Stop-Stack
        Start-Stack
    }
    "status" { Show-Status }
}
