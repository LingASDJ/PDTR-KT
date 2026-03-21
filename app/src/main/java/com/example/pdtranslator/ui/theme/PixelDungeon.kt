package com.example.pdtranslator.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pdtranslator.R
import java.util.Calendar
import kotlin.math.sin

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Zone palettes — directly from MLPD level sources
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
data class ZonePalette(
  val color1: Color, val color2: Color,
  val wall: Color, val wallLight: Color, val wallDark: Color,
  val floor: Color, val mortar: Color
)

private val SEWERS = ZonePalette(Color(0xFF48763C), Color(0xFF59994A), Color(0xFF2A4428), Color(0xFF3A5A35), Color(0xFF1A2E18), Color(0xFF0E1A0C), Color(0xFF0A120A))
private val PRISON = ZonePalette(Color(0xFF6A723D), Color(0xFF88924C), Color(0xFF3D3F26), Color(0xFF555838), Color(0xFF282A18), Color(0xFF141510), Color(0xFF0C0D0A))
private val CAVES  = ZonePalette(Color(0xFF534F3E), Color(0xFFB9D661), Color(0xFF3A3628), Color(0xFF504A38), Color(0xFF24221A), Color(0xFF12110C), Color(0xFF0A0A08))
private val CITY   = ZonePalette(Color(0xFF4B6636), Color(0xFFF2F2F2), Color(0xFF3A3A3A), Color(0xFF505050), Color(0xFF222222), Color(0xFF141414), Color(0xFF0A0A0A))
private val HALLS  = ZonePalette(Color(0xFF801500), Color(0xFFA68521), Color(0xFF3D1A0E), Color(0xFF5A2814), Color(0xFF280E06), Color(0xFF140A04), Color(0xFF0E0604))

// 6 segments, 4 hours each: Halls→Sewers→Prison→Caves→City→Halls
private val ALL_ZONES = listOf(HALLS, SEWERS, PRISON, CAVES, CITY, HALLS)

/** Time segment index 0..5 and fraction within that segment */
fun getTimeSegment(): Pair<Int, Float> {
  val cal = Calendar.getInstance()
  val totalMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  val seg = (totalMin / 240).coerceIn(0, 5)
  val frac = (totalMin - seg * 240) / 240f
  return Pair(seg, frac)
}

/**
 * Composable state that ticks every 60s, triggering recomposition.
 * Returns current totalMinutes as an observable int.
 */
@Composable
fun rememberTimeTick(): Int {
  val cal = Calendar.getInstance()
  var tick by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(60_000L)
      val now = Calendar.getInstance()
      tick = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }
  }
  return tick
}

fun currentZonePalette(): ZonePalette {
  val (seg, frac) = getTimeSegment()
  val from = ALL_ZONES[seg]; val to = ALL_ZONES[(seg + 1).coerceAtMost(5)]
  return ZonePalette(
    lerp(from.color1, to.color1, frac), lerp(from.color2, to.color2, frac),
    lerp(from.wall, to.wall, frac), lerp(from.wallLight, to.wallLight, frac),
    lerp(from.wallDark, to.wallDark, frac), lerp(from.floor, to.floor, frac),
    lerp(from.mortar, to.mortar, frac)
  )
}

data class TimeTint(val accent1: Color, val accent2: Color, val surfaceTint: Color)
fun currentTimeTint(): TimeTint {
  val p = currentZonePalette()
  return TimeTint(p.color1, p.color2, lerp(p.floor, Color.Black, 0.3f))
}

/** Zone names for easter eggs / display */
fun currentZoneName(): String {
  return when (getTimeSegment().first) {
    0 -> "Demon Halls"  // 0-3
    1 -> "Sewers"       // 4-7
    2 -> "Prison"       // 8-11
    3 -> "Caves"        // 12-15
    4 -> "Dwarf City"   // 16-19
    5 -> "Demon Halls"  // 20-23
    else -> "Dungeon"
  }
}

/** Bottom nav icon res IDs that change with time of day */
fun pdNavIcons(): Triple<Int, Int, Int> {
  return when (getTimeSegment().first) {
    0, 5 -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_sword, R.drawable.ic_pd_potion) // Halls - standard
    1    -> Triple(R.drawable.ic_pd_chest, R.drawable.ic_pd_key, R.drawable.ic_pd_potion)    // Sewers - exploration
    2    -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_sword, R.drawable.ic_pd_book)   // Prison - study
    3    -> Triple(R.drawable.ic_pd_chest, R.drawable.ic_pd_sword, R.drawable.ic_pd_amulet)  // Caves - treasure
    4    -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_wand, R.drawable.ic_pd_potion)  // City - magic
    else -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_sword, R.drawable.ic_pd_potion)
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Global brick wall bitmap cache — survives navigation
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private var cachedBrickBitmap: ImageBitmap? = null
private var cachedBrickKey: Triple<Int, Int, Int>? = null // (seg, w, h)

private fun generateBrickBitmap(w: Int, h: Int, palette: ZonePalette): ImageBitmap {
  val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
  val brickW = 28; val brickH = 14; val gap = 2
  val mortarArgb = palette.mortar.toArgb()
  val wallArgb = palette.wall.toArgb()
  val lightArgb = palette.wallLight.toArgb()
  val darkArgb = palette.wallDark.toArgb()
  val speckArgb = palette.color1.copy(alpha = 0.2f).toArgb()

  // Fill mortar
  bmp.eraseColor(mortarArgb)

  val rows = h / brickH + 2; val cols = w / brickW + 2
  for (row in 0 until rows) {
    val offX = if (row % 2 == 1) brickW / 2 else 0
    for (col in -1 until cols) {
      val bx = col * brickW + offX; val by = row * brickH
      val hash = (row * 137 + col * 269) and 0xFF
      // Body
      for (py in (by + gap) until (by + brickH - gap)) {
        if (py !in 0 until h) continue
        for (px in (bx + gap) until (bx + brickW - gap)) {
          if (px in 0 until w) bmp.setPixel(px, py, wallArgb)
        }
      }
      // Highlight top+left
      val topY = by + gap; val leftX = bx + gap
      if (topY in 0 until h) for (px in (bx + gap) until (bx + brickW - gap)) { if (px in 0 until w) bmp.setPixel(px, topY, lightArgb) }
      if (leftX in 0 until w) for (py in (by + gap) until (by + brickH - gap)) { if (py in 0 until h) bmp.setPixel(leftX, py, lightArgb) }
      // Shadow bottom+right
      val botY = by + brickH - gap - 1; val rightX = bx + brickW - gap - 1
      if (botY in 0 until h) for (px in (bx + gap) until (bx + brickW - gap)) { if (px in 0 until w) bmp.setPixel(px, botY, darkArgb) }
      if (rightX in 0 until w) for (py in (by + gap) until (by + brickH - gap)) { if (py in 0 until h) bmp.setPixel(rightX, py, darkArgb) }
      // Specks
      if (hash % 5 == 0) {
        val sx = (bx + gap + 2 + hash % ((brickW - gap * 2 - 4).coerceAtLeast(1))).coerceIn(0, w - 1)
        val sy = (by + gap + 2 + (hash / 3) % ((brickH - gap * 2 - 4).coerceAtLeast(1))).coerceIn(0, h - 1)
        bmp.setPixel(sx, sy, speckArgb)
      }
    }
  }
  return bmp.asImageBitmap()
}

private fun Color.toArgb(): Int {
  return (((alpha * 255).toInt() shl 24) or ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt())
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Composables
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun PixelBrickBackground(modifier: Modifier = Modifier) {
  val config = LocalConfiguration.current
  val density = LocalDensity.current
  val widthPx = with(density) { config.screenWidthDp.dp.toPx().toInt() }.coerceAtLeast(1)
  val heightPx = with(density) { config.screenHeightDp.dp.toPx().toInt() }.coerceAtLeast(1)
  val tick = rememberTimeTick()
  // Regenerate when time segment (4hr block) changes
  val seg = (tick / 240).coerceIn(0, 5)

  // Global cache — bitmap survives navigation, only regenerates on segment change
  val key = Triple(seg, widthPx, heightPx)
  val brickBitmap = if (cachedBrickKey == key && cachedBrickBitmap != null) {
    cachedBrickBitmap!!
  } else {
    generateBrickBitmap(widthPx, heightPx, currentZonePalette()).also {
      cachedBrickBitmap = it
      cachedBrickKey = key
    }
  }

  Canvas(modifier.fillMaxSize()) {
    drawImage(brickBitmap)
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Pixel torch with handle + pixelated flame
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun TorchFlame(modifier: Modifier = Modifier.size(32.dp, 56.dp)) {
  val transition = rememberInfiniteTransition(label = "torch")
  val time by transition.animateFloat(
    0f, 6.2832f,
    infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
    label = "flame"
  )

  Canvas(modifier = modifier) {
    val px = size.width / 8f  // pixel unit (8x grid)
    val cx = size.width / 2f

    // ── Torch handle (bottom) ──
    val handleTop = size.height * 0.65f
    // Brown wooden handle
    drawRect(Color(0xFF8B4513), Offset(cx - px, handleTop), Size(px * 2, size.height - handleTop))
    // Dark end cap
    drawRect(Color(0xFF5D3A0E), Offset(cx - px, size.height - px), Size(px * 2, px))
    // Bracket
    drawRect(Color(0xFF3D2E18), Offset(cx - px * 1.5f, handleTop), Size(px * 3, px))

    // ── Ember glow ──
    val flameBase = handleTop - px
    drawCircle(
      brush = Brush.radialGradient(
        listOf(Color(0x55FF6600), Color(0x22FF4400), Color.Transparent),
        center = Offset(cx, flameBase), radius = size.width * 1.0f
      ), center = Offset(cx, flameBase), radius = size.width * 1.0f
    )

    // ── Pixelated flame particles (blocks, not ovals) ──
    // Each "particle" is a colored pixel-block that shifts with time + tilt
    data class FlamePixel(val relX: Float, val relY: Float, val color: Color, val phase: Float)

    val particles = listOf(
      // Base layer — red/dark orange, wide
      FlamePixel(-2f, 0f, Color(0xFFCC0000), 0f),
      FlamePixel(-1f, 0f, Color(0xFFEE4400), 0.5f),
      FlamePixel(0f, 0f, Color(0xFFFF6600), 1f),
      FlamePixel(1f, 0f, Color(0xFFEE4400), 1.5f),
      FlamePixel(2f, 0f, Color(0xFFCC0000), 2f),
      // Mid layer — orange
      FlamePixel(-1.5f, -1f, Color(0xFFFF6600), 0.3f),
      FlamePixel(-0.5f, -1f, Color(0xFFFF8800), 0.8f),
      FlamePixel(0.5f, -1f, Color(0xFFFF8800), 1.3f),
      FlamePixel(1.5f, -1f, Color(0xFFFF6600), 1.8f),
      // Upper mid — bright orange/yellow
      FlamePixel(-1f, -2f, Color(0xFFFF8800), 0.4f),
      FlamePixel(0f, -2f, Color(0xFFFFAA00), 1.0f),
      FlamePixel(1f, -2f, Color(0xFFFF8800), 1.6f),
      // Core — yellow
      FlamePixel(-0.5f, -3f, Color(0xFFFFFF44), 0.6f),
      FlamePixel(0.5f, -3f, Color(0xFFFFFF44), 1.2f),
      // Tip — white/pale
      FlamePixel(0f, -4f, Color(0xFFFFFF88), 0.9f),
      FlamePixel(0f, -5f, Color(0xFFFFFFCC), 1.5f),
    )

    for (p in particles) {
      val flicker = sin(time * 4f + p.phase * 2f)
      val sway = sin(time * 2.5f + p.phase) * px * 0.4f
      val jumpY = flicker * px * 0.3f
      val alpha = (0.75f + flicker * 0.25f).coerceIn(0.4f, 1f)

      drawRect(
        color = p.color.copy(alpha = alpha),
        topLeft = Offset(cx + p.relX * px + sway, flameBase + p.relY * px + jumpY),
        size = Size(px, px)
      )
    }
  }
}

/** Torch glow + vignette + actual flame sprites at corners */
@Composable
fun TorchGlowOverlay(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "glow")
  val flicker by transition.animateFloat(
    0.4f, 0.7f,
    infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
    label = "flicker"
  )
  val tick = rememberTimeTick()
  val seg = (tick / 240).coerceIn(0, 5)
  // Torch color shifts with zone
  val torchColor = when (seg) {
    0, 5 -> Color(0xFFFF4400) // Halls — deep red fire
    1    -> Color(0xFF44FF88) // Sewers — eerie green
    2    -> Color(0xFFFFCC44) // Prison — warm yellow
    3    -> Color(0xFFFF8800) // Caves — orange
    4    -> Color(0xFFFFFFAA) // City — bright pale
    else -> Color(0xFFFF8800)
  }

  Box(modifier = modifier.fillMaxSize()) {
    // Glow spots on brick wall
    Canvas(Modifier.fillMaxSize()) {
      // Top-left torch glow
      drawCircle(
        brush = Brush.radialGradient(
          listOf(torchColor.copy(alpha = flicker * 0.25f), Color.Transparent),
          center = Offset(size.width * 0.08f, size.height * 0.06f), radius = size.width * 0.5f
        ), center = Offset(size.width * 0.08f, size.height * 0.06f), radius = size.width * 0.5f
      )
      // Top-right torch glow
      drawCircle(
        brush = Brush.radialGradient(
          listOf(torchColor.copy(alpha = flicker * 0.2f), Color.Transparent),
          center = Offset(size.width * 0.92f, size.height * 0.06f), radius = size.width * 0.45f
        ), center = Offset(size.width * 0.92f, size.height * 0.06f), radius = size.width * 0.45f
      )
      // Bottom center — cyan magic pool glow
      drawCircle(
        brush = Brush.radialGradient(
          listOf(Color(0xFF00FFFF).copy(alpha = flicker * 0.08f), Color.Transparent),
          center = Offset(size.width * 0.5f, size.height * 0.92f), radius = size.width * 0.5f
        ), center = Offset(size.width * 0.5f, size.height * 0.92f), radius = size.width * 0.5f
      )

      // Vignette — dark dungeon edges
      drawRect(
        Brush.radialGradient(
          listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
          Offset(size.width / 2f, size.height / 2f), size.width * 0.8f
        )
      )
    }

    // Actual animated pixel torch flames at top corners
    TorchFlame(
      Modifier.align(Alignment.TopStart).offset(x = 6.dp, y = 6.dp).size(32.dp, 56.dp)
    )
    TorchFlame(
      Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp).size(32.dp, 56.dp)
    )
  }
}
