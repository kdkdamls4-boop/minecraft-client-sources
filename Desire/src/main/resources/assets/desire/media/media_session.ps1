param([string]$CmdFile, [string]$CoverDir, [int]$ParentPid)
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing

$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]

function Await($op, $type) {
    $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
    [void]$t.Wait(5000)
    $t.Result
}

[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
[void][Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
[void][Windows.Storage.Streams.IInputStream, Windows.Storage.Streams, ContentType = WindowsRuntime]
$asStream = [System.IO.WindowsRuntimeStreamExtensions].GetMethod('AsStreamForRead', [Type[]]@([Windows.Storage.Streams.IInputStream]))
$mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]
$propType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties]
$streamType = [Windows.Storage.Streams.IRandomAccessStreamWithContentType]

$mgr = Await ($mgrType::RequestAsync()) $mgrType
$lastKey = ''
$coverPath = ''
$coverId = 0
$tick = 0
$loop = 0

while ($true) {
    $loop++
    if ($loop % 8 -eq 0) {
        try { [void][System.Diagnostics.Process]::GetProcessById($ParentPid) } catch { exit }
    }

    if (Test-Path $CmdFile) {
        $cmd = ([string](Get-Content $CmdFile -Raw)).Trim()
        Remove-Item $CmdFile -Force
        $s = $mgr.GetCurrentSession()
        if ($s) {
            if ($cmd -eq 'prev') { [void](Await ($s.TrySkipPreviousAsync()) ([bool])) }
            elseif ($cmd -eq 'next') { [void](Await ($s.TrySkipNextAsync()) ([bool])) }
            elseif ($cmd -eq 'toggle') { [void](Await ($s.TryTogglePlayPauseAsync()) ([bool])) }
        }
        $tick = 0
    }

    if ($tick -le 0) {
        $tick = 4
        $s = $mgr.GetCurrentSession()
        if (-not $s) {
            Write-Output '{"active":false}'
        } else {
            $p = Await ($s.TryGetMediaPropertiesAsync()) $propType
            $title = [string]$p.Title
            $artist = [string]$p.Artist
            $key = "$title|$artist"
            if ($key -ne $lastKey) {
                $lastKey = $key
                $coverPath = ''
                try {
                    if ($p.Thumbnail) {
                        $rs = Await ($p.Thumbnail.OpenReadAsync()) $streamType
                        if ($rs) {
                            $st = $asStream.Invoke($null, @($rs))
                            $ms = New-Object System.IO.MemoryStream
                            $st.CopyTo($ms)
                            $ms.Position = 0
                            $img = [System.Drawing.Image]::FromStream($ms)
                            $coverId++
                            $coverPath = Join-Path $CoverDir "desire_cover_$coverId.png"
                            $img.Save($coverPath, [System.Drawing.Imaging.ImageFormat]::Png)
                            $img.Dispose()
                            $ms.Dispose()
                        }
                    }
                } catch { $coverPath = '' }
            }

            $tl = $s.GetTimelineProperties()
            $playing = ($s.GetPlaybackInfo().PlaybackStatus -eq 'Playing')
            $dur = ($tl.EndTime - $tl.StartTime).TotalMilliseconds
            $pos = ($tl.Position - $tl.StartTime).TotalMilliseconds
            if ($playing) { $pos += ([DateTimeOffset]::Now - $tl.LastUpdatedTime).TotalMilliseconds }
            if ($pos -gt $dur) { $pos = $dur }
            if ($pos -lt 0) { $pos = 0 }

            $o = [ordered]@{
                active = $true; title = $title; artist = $artist; playing = $playing
                pos = [long]$pos; dur = [long]$dur; cover = $coverPath
            }
            Write-Output ($o | ConvertTo-Json -Compress)
        }
    }
    $tick--
    Start-Sleep -Milliseconds 250
}
