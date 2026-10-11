param(
  [Parameter(Mandatory = $true, Position = 0)]
  [ValidateSet('test', 'config', 'build', 'start', 'smoke', 'stop')]
  [string] $Action
)

$composeFile = Join-Path $PSScriptRoot 'compose.yaml'
$projectName = 'autumn-analytics-lab'

function Invoke-Compose([string[]] $ComposeArgs) {
  & docker compose --project-name $projectName --file $composeFile @ComposeArgs
  if ($LASTEXITCODE -ne 0) { throw "docker compose $($ComposeArgs -join ' ') failed with exit code $LASTEXITCODE" }
}

function Assert-DockerEngine {
  $null = & docker info --format '{{.ServerVersion}}' 2>$null
  if ($LASTEXITCODE -ne 0) { throw 'Docker Engine is unavailable; start Docker Desktop and retry.' }
}

switch ($Action) {
  'test' {
    $repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
    $wrapperJar = Join-Path $repo '.mvn\wrapper\maven-wrapper.jar'
    $wrapperRoot = "-Dmaven.multiModuleProjectDirectory=$repo"
    & java $wrapperRoot -classpath $wrapperJar org.apache.maven.wrapper.MavenWrapperMain -q -f (Join-Path $PSScriptRoot 'pom.xml') test
    if ($LASTEXITCODE -ne 0) { throw "analytics lab tests failed with exit code $LASTEXITCODE" }
  }
  'config' { Invoke-Compose @('config', '--quiet') }
  'build' { Assert-DockerEngine; Invoke-Compose @('build', 'lab') }
  'start' { Assert-DockerEngine; Invoke-Compose @('up', '-d', 'zookeeper') }
  'smoke' {
    Assert-DockerEngine
    try {
      Invoke-Compose @('build', 'lab')
      Invoke-Compose @('up', '-d', '--force-recreate', 'zookeeper')
      Invoke-Compose @('run', '--rm', '--no-deps', 'lab')
    } finally {
      Invoke-Compose @('down', '--remove-orphans')
    }
  }
  'stop' { Assert-DockerEngine; Invoke-Compose @('down', '--remove-orphans') }
}
