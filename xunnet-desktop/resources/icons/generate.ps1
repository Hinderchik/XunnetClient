Add-Type -AssemblyName System.Drawing

function New-AppIcon {
    param(
        [int]$Size,
        [int]$R1, [int]$G1, [int]$B1,
        [int]$R2, [int]$G2, [int]$B2,
        [string]$Symbol,    # 'dot', 'check', 'cross', 'circle', 'none'
        [string]$Out
    )
    $bmp = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias

    # Rounded rectangle path
    $r = [int]($Size * 0.18)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc(0, 0, $r*2, $r*2, 180, 90)
    $path.AddArc($Size - $r*2, 0, $r*2, $r*2, 270, 90)
    $path.AddArc($Size - $r*2, $Size - $r*2, $r*2, $r*2, 0, 90)
    $path.AddArc(0, $Size - $r*2, $r*2, $r*2, 90, 90)
    $path.CloseFigure()

    # Gradient fill clipped to rounded rect
    $color1 = [System.Drawing.Color]::FromArgb(255, $R1, $G1, $B1)
    $color2 = [System.Drawing.Color]::FromArgb(255, $R2, $G2, $B2)
    $gradBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Rectangle 0, 0, $Size, $Size),
        $color1, $color2,
        [System.Drawing.Drawing2D.LinearGradientMode]::Vertical
    )
    $g.FillPath($gradBrush, $path)

    # Symbol
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::White), ([single]($Size * 0.10))
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round

    switch ($Symbol) {
        'check' {
            $pts = @(
                (New-Object System.Drawing.Point ([int]($Size*0.25), [int]($Size*0.52))),
                (New-Object System.Drawing.Point ([int]($Size*0.45), [int]($Size*0.72))),
                (New-Object System.Drawing.Point ([int]($Size*0.78), [int]($Size*0.32)))
            )
            $g.DrawLines($pen, $pts)
        }
        'cross' {
            $g.DrawLine($pen, [int]($Size*0.28), [int]($Size*0.28), [int]($Size*0.72), [int]($Size*0.72))
            $g.DrawLine($pen, [int]($Size*0.72), [int]($Size*0.28), [int]($Size*0.28), [int]($Size*0.72))
        }
        'circle' {
            $c = [int]($Size*0.30)
            $g.DrawEllipse($pen, $Size/2 - $c/2, $Size/2 - $c/2, $c, $c)
            $g.DrawLine($pen, [int]($Size*0.50), [int]($Size*0.62), [int]($Size*0.50), [int]($Size*0.78))
        }
        default { }
    }

    $bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $bmp.Dispose()
    Write-Host "  + $Out"
}

# Determine icons dir relative to this script
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$iconsDir = $scriptDir

$sizes = @(16, 24, 32, 48, 64, 128, 256)

# Default icon (indigo gradient + circle)
foreach ($s in $sizes) {
    New-AppIcon -Size $s -R1 99 -G1 102 -B1 241 -R2 139 -G2 92 -B2 246 -Symbol "circle" -Out (Join-Path $iconsDir "app_${s}.png")
}

# Connected icon (green + check)
foreach ($s in $sizes) {
    New-AppIcon -Size $s -R1 34 -G1 197 -B1 94 -R2 16 -G2 185 -B2 129 -Symbol "check" -Out (Join-Path $iconsDir "app-connected_${s}.png")
}

# Error icon (red + X)
foreach ($s in $sizes) {
    New-AppIcon -Size $s -R1 239 -G1 68 -B1 68 -R2 220 -G2 38 -B2 38 -Symbol "cross" -Out (Join-Path $iconsDir "app-error_${s}.png")
}

# Use 256px as the main .png files
Copy-Item -Force (Join-Path $iconsDir "app_256.png") (Join-Path $iconsDir "app.png")
Copy-Item -Force (Join-Path $iconsDir "app-connected_256.png") (Join-Path $iconsDir "app-connected.png")
Copy-Item -Force (Join-Path $iconsDir "app-error_256.png") (Join-Path $iconsDir "app-error.png")

# Build multi-resolution ICO from the PNGs we just wrote
$icoSizes = @(16, 24, 32, 48, 64, 128, 256)
$icoPath = Join-Path $iconsDir "app.ico"
$pngImages = @()
foreach ($s in $icoSizes) {
    $pngImages += [System.Drawing.Image]::FromFile((Join-Path $iconsDir "app_${s}.png"))
}

$fs = [System.IO.File]::Create($icoPath)
$bw = New-Object System.IO.BinaryWriter $fs
$bw.Write([uint16]0)               # reserved
$bw.Write([uint16]1)               # type: 1 = icon
$bw.Write([uint16]$icoSizes.Count) # count

# ICONDIRENTRY (16 bytes each)
$offset = 6 + (16 * $icoSizes.Count)
$imageData = @()
foreach ($size in $icoSizes) {
    $ms = New-Object System.IO.MemoryStream
    $img = $pngImages | Where-Object { $_.Width -eq $size } | Select-Object -First 1
    $img.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $data = $ms.ToArray()
    $ms.Dispose()
    $imageData += ,$data

    $w = if ($size -ge 256) { 0 } else { $size }
    $bw.Write([byte]$w)             # width (0 = 256)
    $bw.Write([byte]$w)             # height
    $bw.Write([byte]0)              # color palette
    $bw.Write([byte]0)              # reserved
    $bw.Write([uint16]1)            # color planes
    $bw.Write([uint16]32)           # bits per pixel
    $bw.Write([uint32]$data.Length) # size of image data
    $bw.Write([uint32]$offset)      # offset
    $offset += $data.Length
}

# Image data
foreach ($data in $imageData) { $bw.Write($data) }

$bw.Close()
$fs.Close()
foreach ($img in $pngImages) { $img.Dispose() }

Write-Host ""
Write-Host "Built app.ico with sizes: $($icoSizes -join ', ')"
Write-Host "Files in icons/:"
Get-ChildItem $iconsDir -File | Where-Object { $_.Name -like "*.png" -or $_.Name -like "*.ico" } | Sort-Object Name | ForEach-Object { Write-Host "  $($_.Name) ($($_.Length) bytes)" }
