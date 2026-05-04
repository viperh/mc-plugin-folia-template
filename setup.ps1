<#
.SYNOPSIS
    Renames this template into a fresh plugin project.

.DESCRIPTION
    Replaces the placeholder group ("org.me.qviperh"), package leaf ("mCTemplate"),
    main class ("MCTemplate"), and Maven artifactId across the project. Moves the
    Kotlin source tree to the new package, updates pom.xml, plugin.yml, and every
    .kt file's package/import declarations. Then deletes itself.

.PARAMETER GroupId
    New Maven groupId / Kotlin package root, e.g. "com.example".

.PARAMETER PluginName
    New plugin name, used as Maven artifactId, plugin.yml name/prefix, main class
    name, and the package leaf (lowercased). Must be a valid Java identifier.

.PARAMETER Description
    Optional plugin description for plugin.yml.

.PARAMETER Author
    Optional author name for plugin.yml.

.EXAMPLE
    .\setup.ps1 -GroupId com.example -PluginName CoolPlugin
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$GroupId,

    [Parameter(Mandatory = $true)]
    [string]$PluginName,

    [string]$Description,

    [string]$Author
)

$ErrorActionPreference = 'Stop'

if ($GroupId -notmatch '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$') {
    throw "GroupId '$GroupId' must be a dotted lowercase identifier (e.g. com.example)."
}
if ($PluginName -notmatch '^[A-Za-z][A-Za-z0-9]*$') {
    throw "PluginName '$PluginName' must start with a letter and contain only letters/digits."
}

$repoRoot = $PSScriptRoot

$oldGroup       = 'org.me.qviperh'
$oldLeaf        = 'mCTemplate'
$oldClass       = 'MCTemplate'
$oldArtifactId  = 'MCTemplate'
$oldFullPkg     = "$oldGroup.$oldLeaf"

$newGroup      = $GroupId
$newLeaf       = $PluginName.Substring(0, 1).ToLowerInvariant() + $PluginName.Substring(1)
$newClass      = $PluginName
$newArtifactId = $PluginName
$newFullPkg    = "$newGroup.$newLeaf"

if ($oldFullPkg -eq $newFullPkg -and $oldClass -eq $newClass) {
    Write-Host "Nothing to rename — names match the template defaults."
    return
}

Write-Host "Renaming template:"
Write-Host "  package : $oldFullPkg  ->  $newFullPkg"
Write-Host "  class   : $oldClass    ->  $newClass"
Write-Host "  groupId : $oldGroup    ->  $newGroup"
Write-Host "  artifact: $oldArtifactId -> $newArtifactId"

$kotlinRoot = Join-Path $repoRoot 'src\main\kotlin'
$oldPkgDir  = Join-Path $kotlinRoot ($oldFullPkg -replace '\.', '\')
$newPkgDir  = Join-Path $kotlinRoot ($newFullPkg -replace '\.', '\')

if (-not (Test-Path -LiteralPath $oldPkgDir)) {
    throw "Expected package directory not found: $oldPkgDir. Was the template already renamed?"
}

# Move the package tree to its new location.
$newPkgParent = Split-Path -Parent $newPkgDir
if (-not (Test-Path -LiteralPath $newPkgParent)) {
    New-Item -ItemType Directory -Path $newPkgParent -Force | Out-Null
}

# Move via a temp dir so we never overlap with existing path segments.
$tempMove = Join-Path $repoRoot ".rename-tmp-$([guid]::NewGuid().ToString('N'))"
Move-Item -LiteralPath $oldPkgDir -Destination $tempMove

# Clean up now-empty old parent directories (e.g. org/me/qviperh) up to kotlinRoot.
$cleanup = Split-Path -Parent $oldPkgDir
while ($cleanup -and ($cleanup -ne $kotlinRoot) -and (Test-Path -LiteralPath $cleanup)) {
    if (-not (Get-ChildItem -LiteralPath $cleanup -Force)) {
        Remove-Item -LiteralPath $cleanup -Force
        $cleanup = Split-Path -Parent $cleanup
    } else { break }
}

if (Test-Path -LiteralPath $newPkgDir) {
    throw "Destination already exists: $newPkgDir"
}
Move-Item -LiteralPath $tempMove -Destination $newPkgDir

# Rename the main class file.
$oldMainFile = Join-Path $newPkgDir "$oldClass.kt"
$newMainFile = Join-Path $newPkgDir "$newClass.kt"
if ((Test-Path -LiteralPath $oldMainFile) -and ($oldMainFile -ne $newMainFile)) {
    Move-Item -LiteralPath $oldMainFile -Destination $newMainFile
}

# Rewrite every .kt file: package decls, imports, and main class identifier.
$ktFiles = Get-ChildItem -Path $kotlinRoot -Recurse -Filter '*.kt' -File
foreach ($file in $ktFiles) {
    $text = [System.IO.File]::ReadAllText($file.FullName)
    $text = $text.Replace($oldFullPkg, $newFullPkg)
    if ($oldClass -ne $newClass) {
        # Word-bounded replace so we don't mangle substrings.
        $text = [regex]::Replace($text, "\b$([regex]::Escape($oldClass))\b", $newClass)
    }
    [System.IO.File]::WriteAllText($file.FullName, $text)
}

# pom.xml
$pomPath = Join-Path $repoRoot 'pom.xml'
$pom = [System.IO.File]::ReadAllText($pomPath)
$pom = $pom -replace '<groupId>org\.me\.qviperh</groupId>', "<groupId>$newGroup</groupId>"
$pom = $pom -replace '<artifactId>MCTemplate</artifactId>', "<artifactId>$newArtifactId</artifactId>"
$pom = $pom -replace '<name>MCTemplate</name>', "<name>$newArtifactId</name>"
$pom = $pom.Replace("$oldFullPkg.libs", "$newFullPkg.libs")
[System.IO.File]::WriteAllText($pomPath, $pom)

# plugin.yml
$pluginYmlPath = Join-Path $repoRoot 'src\main\resources\plugin.yml'
$yml = [System.IO.File]::ReadAllText($pluginYmlPath)
$yml = $yml -replace '(?m)^name:\s*MCTemplate\s*$', "name: $PluginName"
$yml = $yml -replace '(?m)^prefix:\s*MCTemplate\s*$', "prefix: $PluginName"
$yml = $yml -replace '(?m)^main:\s*org\.me\.qviperh\.mCTemplate\.MCTemplate\s*$', "main: $newFullPkg.$newClass"
if ($PSBoundParameters.ContainsKey('Description') -and $Description) {
    $escaped = $Description -replace '"', '\"'
    $yml = $yml -replace '(?m)^description:.*$', "description: $escaped"
}
if ($PSBoundParameters.ContainsKey('Author') -and $Author) {
    $yml = $yml -replace '(?ms)^authors:\s*\r?\n\s*-\s*qViperH\s*$', "authors:`n  - $Author"
}
[System.IO.File]::WriteAllText($pluginYmlPath, $yml)

# Drop the IntelliJ module file too — it'll regenerate on import.
$imlPath = Join-Path $repoRoot 'MCTemplate.iml'
if (Test-Path -LiteralPath $imlPath) {
    Remove-Item -LiteralPath $imlPath -Force
}

Write-Host ""
Write-Host "Done. Self-deleting setup.ps1 — re-import the project in your IDE."
Remove-Item -LiteralPath $PSCommandPath -Force
