# Builds packaging/knight.ico from the knight image used by the board.
#
# Windows picks a different size depending on where it draws the icon (16 px in the title
# bar, 256 px in a large Explorer view), so the file holds every size at once and lets
# Windows choose. Each one is stored as PNG, which .ico has allowed since Vista.
#
# Run it again after changing the source image:
#   powershell -ExecutionPolicy Bypass -File packaging/make-icon.ps1

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root   = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'src\main\resources\knight2.png'
$target = Join-Path $PSScriptRoot 'knight.ico'
$sizes  = 16, 24, 32, 48, 64, 128, 256

$original = [System.Drawing.Image]::FromFile($source)
$images = New-Object System.Collections.ArrayList
try {
    foreach ($size in $sizes) {
        $canvas = New-Object System.Drawing.Bitmap $size, $size
        $g = [System.Drawing.Graphics]::FromImage($canvas)
        $g.InterpolationMode   = 'HighQualityBicubic'
        $g.PixelOffsetMode     = 'HighQuality'
        $g.SmoothingMode       = 'AntiAlias'
        $g.CompositingQuality  = 'HighQuality'
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($original, 0, 0, $size, $size)
        $g.Dispose()

        $buffer = New-Object System.IO.MemoryStream
        $canvas.Save($buffer, [System.Drawing.Imaging.ImageFormat]::Png)
        [void]$images.Add([PSCustomObject]@{ Size = $size; Bytes = $buffer.ToArray() })
        $buffer.Dispose()
        $canvas.Dispose()
    }
} finally {
    $original.Dispose()
}

$out = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter $out
$w.Write([uint16]0)               # reserved
$w.Write([uint16]1)               # 1 = icon
$w.Write([uint16]$images.Count)

# The directory comes first, so every image offset counts from past the end of it.
$offset = 6 + 16 * $images.Count
foreach ($image in $images) {
    # 256 does not fit in a byte and is written as 0, which readers know means 256.
    $dimension = $image.Size
    if ($dimension -ge 256) { $dimension = 0 }

    $w.Write([byte]$dimension)     # width
    $w.Write([byte]$dimension)     # height
    $w.Write([byte]0)              # palette size, 0 for true colour
    $w.Write([byte]0)              # reserved
    $w.Write([uint16]1)            # colour planes
    $w.Write([uint16]32)           # bits per pixel
    $w.Write([uint32]$image.Bytes.Length)
    $w.Write([uint32]$offset)
    $offset += $image.Bytes.Length
}
foreach ($image in $images) { $w.Write($image.Bytes) }
$w.Flush()

[System.IO.File]::WriteAllBytes($target, $out.ToArray())
$w.Dispose()
$out.Dispose()

Write-Output ("Wrote {0} ({1} sizes, {2:N0} bytes)" -f $target, $images.Count, (Get-Item $target).Length)
