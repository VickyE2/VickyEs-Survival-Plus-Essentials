Param (
    [Parameter(Mandatory = $false)]
    [Int]$Depth = 3,

    [Parameter(Mandatory = $false)]
    [String]$Path = $PSScriptRoot,

    [Parameter(Mandatory = $false)]
    [Switch]$IncludeFiles
)

$sb = New-Object System.Text.StringBuilder

Function Get-Tree {
    Param ([String]$Path, [Int]$LevelMax, [Int]$Level = 0, [Bool]$LastOfTheList=$true, [String]$Lead="")

    if ($Level -eq 0) {$sb.AppendLine($Path)}
    if ($Level -eq $LevelMax) { Return }

    $Lead = if($LastOfTheList){"$Lead   "}else{"$Lead$([char]0x2502)  "}

    [Array]$Items = Get-ChildItem -Path $Path | Sort-Object { -not $_.PSIsContainer }, Name

    For ($x = 0; $x -lt $Items.Count; $x++) {
        $item = $Items[$x]
        $isLast = ($x -eq $Items.Count - 1)
        $connector = if ($isLast) { "$([char]0x2514)$([char]0x2500)$([char]0x2500)" } else { "$([char]0x251c)$([char]0x2500)$([char]0x2500)" }

        $sb.AppendLine("$Lead$connector$($item.Name)") | Out-Null

        # If it's a directory, recurse
        if ($item.PSIsContainer) {
            Get-Tree -Path $item.FullName -LevelMax $LevelMax -Level ($Level + 1) -Lead $Lead -LastOfTheList $isLast
        }
    }
}

Get-Tree -Path $Path -LevelMax $Depth
$sb.ToString() | Out-File "MyOutputFile.txt" -Encoding UTF8