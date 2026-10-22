# Rebuild the original geometric icon; no external image assets required.
Add-Type -AssemblyName System.Drawing
$bitmap = [System.Drawing.Bitmap]::new(128, 128)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::FromArgb(25, 42, 48))
$web = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(232, 242, 236), 3)
$web.StartCap = $web.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$accent = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(111, 215, 170), 4)
$accent.StartCap = $accent.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
try {
    for ($i = 0; $i -lt 8; $i++) {
        $angle = $i * [Math]::PI / 4
        $graphics.DrawLine($web, 64, 64, [single](64 + 52 * [Math]::Cos($angle)), [single](64 + 52 * [Math]::Sin($angle)))
    }
    foreach ($radius in @(17, 33, 49)) {
        for ($i = 0; $i -lt 8; $i++) {
            # A gap and two loose strands suggest a web that is breaking.
            if ($i -eq 0 -and $radius -ge 33) { continue }
            $a = $i * [Math]::PI / 4
            $b = ($i + 1) * [Math]::PI / 4
            $graphics.DrawBezier($web,
                [single](64 + $radius * [Math]::Cos($a)), [single](64 + $radius * [Math]::Sin($a)),
                [single](64 + $radius * 0.78 * [Math]::Cos($a + 0.26)), [single](64 + $radius * 0.78 * [Math]::Sin($a + 0.26)),
                [single](64 + $radius * 0.78 * [Math]::Cos($b - 0.26)), [single](64 + $radius * 0.78 * [Math]::Sin($b - 0.26)),
                [single](64 + $radius * [Math]::Cos($b)), [single](64 + $radius * [Math]::Sin($b)))
        }
    }
    $graphics.DrawLine($accent, 101, 77, 109, 73)
    $graphics.DrawLine($accent, 90, 90, 91, 100)
    $destination = Join-Path $PSScriptRoot '../src/main/resources/assets/weaker_spiderwebs/icon.png'
    New-Item -ItemType Directory -Force (Split-Path $destination) | Out-Null
    $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $accent.Dispose()
    $web.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}
