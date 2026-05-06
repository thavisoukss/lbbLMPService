param(
    [Parameter(Mandatory=$true)][string]$AppEnv,
    [string]$Platform = ""
)

$ErrorActionPreference = "Stop"

$REGISTRY = "172.16.4.62:5000/noh/lmps-payment"

function Info($msg)    { Write-Host "==> $msg" -ForegroundColor Cyan }
function Success($msg) { Write-Host $msg -ForegroundColor Green }
function Fail($msg)    { Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

if ($AppEnv -notin @("dev","uat","prod")) {
    Write-Host "Usage: .\build.ps1 <env> [platform]"
    Write-Host "  env:      dev | uat | prod"
    Write-Host "  platform: linux/amd64 | linux/arm64  (default: linux/amd64)"
    exit 1
}

# Windows is always amd64
if (-not $Platform) { $Platform = "linux/amd64" }

if (($AppEnv -eq "uat" -or $AppEnv -eq "prod") -and $Platform -ne "linux/amd64") {
    Info "Forcing linux/amd64 for $AppEnv (registry server is x86)"
    $Platform = "linux/amd64"
}

switch ($AppEnv) {
    "dev" {
        $SHORT_HASH = (git rev-parse --short HEAD).Trim()
        $TAG = "${REGISTRY}:dev-${SHORT_HASH}"
    }
    "uat" {
        $SHORT_HASH = (git rev-parse --short HEAD).Trim()
        $TAG = "${REGISTRY}:dev-${SHORT_HASH}"
    }
    "prod" {
        $VERSION = (git describe --tags --abbrev=0 2>&1).ToString().Trim()
        if ($LASTEXITCODE -ne 0) { Fail "No git tag found. Tag a commit first: git tag v1.0.0" }
        $TAG = "${REGISTRY}:${VERSION}"
        $confirm = Read-Host "Push $TAG to PRODUCTION? [y/N]"
        if ($confirm -notmatch "^[Yy]$") { Write-Host "Aborted."; exit 0 }
    }
}

# Java 21 — on Windows, dot-source your profile or set JAVA_HOME manually:
# . "$env:USERPROFILE\.j21.ps1"
# $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
# $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Info "Switch to Java 21"
j21.ps1

Info "Java & Maven versions"
java -version
mvn -v

Info "Running: mvn clean compile package"
mvn clean compile package -DskipTests

Info "Docker build (platform=$Platform)"
docker build --platform $Platform -f Dockerfile -t $TAG .

Info "Docker push -> registry"
docker push $TAG

Success "Build & push completed: $TAG"
