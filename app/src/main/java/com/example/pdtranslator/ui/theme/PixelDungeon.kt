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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.pdtranslator.R
import java.util.Calendar
import kotlin.math.sin
import kotlin.random.Random

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Zone palettes — sourced from MLPD level sources,
// Window.java, CharSprite.java, FogOfWar.java,
// mob sprites, boss encounters, particle effects
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
data class ZonePalette(
  val color1: Color, val color2: Color,
  val wall: Color, val wallLight: Color, val wallDark: Color,
  val floor: Color, val mortar: Color
)

// 24 hourly zone palettes — each hour has a unique dungeon atmosphere
// Sourced from real MLPD color values across all chapters, bosses, and special levels

// ── 0:00 恶魔大厅深层 Deep Demon Halls ──
// HallsLevel color1=0x801500, color2=0xA68521, FistSprite.Dark blood=0x4A2F53
private val HOUR_00 = ZonePalette(
  Color(0xFF801500), Color(0xFFA68521),
  Color(0xFF3D1A0E), Color(0xFF5A2814), Color(0xFF280E06),
  Color(0xFF140A04), Color(0xFF0E0604)
)

// ── 1:00 燃烧拳魔 Burning Fist ──
// FistSprite.Burning blood=0xFFDD34, Halls lava red
private val HOUR_01 = ZonePalette(
  Color(0xFFFFDD34), Color(0xFFEE7722),
  Color(0xFF4A2200), Color(0xFF6B3300), Color(0xFF2E1500),
  Color(0xFF180C02), Color(0xFF100800)
)

// ── 2:00 腐烂拳魔 Rotting Fist ──
// FistSprite.Rotting blood=0xB8BBA1, organic decay
private val HOUR_02 = ZonePalette(
  Color(0xFFB8BBA1), Color(0xFF7F5424),
  Color(0xFF3A3828), Color(0xFF504C38), Color(0xFF24221A),
  Color(0xFF141310), Color(0xFF0C0B08)
)

// ── 3:00 冰霜拳魔 Ice Fist ──
// FistSprite.Ice blood=0x26CCC2, HaloFist=0x34C9C9
private val HOUR_03 = ZonePalette(
  Color(0xFF26CCC2), Color(0xFF34C9C9),
  Color(0xFF1A3838), Color(0xFF2A5050), Color(0xFF0E2424),
  Color(0xFF0A1818), Color(0xFF061010)
)

// ── 4:00 下水道入口 Sewers Entrance ──
// SewerLevel color1=0x48763C, color2=0x59994A
private val HOUR_04 = ZonePalette(
  Color(0xFF48763C), Color(0xFF59994A),
  Color(0xFF2A4428), Color(0xFF3A5A35), Color(0xFF1A2E18),
  Color(0xFF0E1A0C), Color(0xFF0A120A)
)

// ── 5:00 下水道深处 Deep Sewers (Goo) ──
// GooSprite blood=0x000000, toxic green particles=0x50FF60
private val HOUR_05 = ZonePalette(
  Color(0xFF50FF60), Color(0xFF22BB55),
  Color(0xFF1E3820), Color(0xFF2E4E30), Color(0xFF122416),
  Color(0xFF0C180E), Color(0xFF080E08)
)

// ── 6:00 花园 Garden Level ──
// LeafParticle 0x004400..0x88CC44, garden greens
private val HOUR_06 = ZonePalette(
  Color(0xFF88CC44), Color(0xFF66BB6A),
  Color(0xFF2E4420), Color(0xFF3E5A30), Color(0xFF1C2E14),
  Color(0xFF101C0C), Color(0xFF0A140A)
)

// ── 7:00 监狱入口 Prison Entrance ──
// PrisonLevel color1=0x6A723D, color2=0x88924C
private val HOUR_07 = ZonePalette(
  Color(0xFF6A723D), Color(0xFF88924C),
  Color(0xFF3D3F26), Color(0xFF555838), Color(0xFF282A18),
  Color(0xFF141510), Color(0xFF0C0D0A)
)

// ── 8:00 监狱牢房 Prison Cells ──
// PrisonLevel torch=0xFFFFCC, lantern atmosphere
private val HOUR_08 = ZonePalette(
  Color(0xFFFFFFCC), Color(0xFFDDAA22),
  Color(0xFF44422A), Color(0xFF5C5A3A), Color(0xFF2E2C1C),
  Color(0xFF18170E), Color(0xFF100F0A)
)

// ── 9:00 天狗BOSS Tengu Boss ──
// Tengu title=0xFF0000, DangerIndicator=0xC03838
private val HOUR_09 = ZonePalette(
  Color(0xFFC03838), Color(0xFFFF0000),
  Color(0xFF3E2020), Color(0xFF583030), Color(0xFF281414),
  Color(0xFF180C0C), Color(0xFF100808)
)

// ── 10:00 洞穴入口 Caves Entrance ──
// CavesLevel color1=0x534F3E, color2=0xB9D661
private val HOUR_10 = ZonePalette(
  Color(0xFF534F3E), Color(0xFFB9D661),
  Color(0xFF3A3628), Color(0xFF504A38), Color(0xFF24221A),
  Color(0xFF12110C), Color(0xFF0A0A08)
)

// ── 11:00 矿洞深处 Deep Mines ──
// DM300 blood=0xFFFF88, mining orange, crystal wisps=0x66B3FF
private val HOUR_11 = ZonePalette(
  Color(0xFFFFFF88), Color(0xFF66B3FF),
  Color(0xFF3A3420), Color(0xFF504830), Color(0xFF241E14),
  Color(0xFF14100A), Color(0xFF0C0A06)
)

// ── 12:00 水晶洞窟 Crystal Caves ──
// CrystalWispSprite blood=0x66B3FF/0x2EE62E, STORM=0x8AD8D8
private val HOUR_12 = ZonePalette(
  Color(0xFF66B3FF), Color(0xFF8AD8D8),
  Color(0xFF2A3040), Color(0xFF3A4458), Color(0xFF1A2030),
  Color(0xFF0E1420), Color(0xFF080E18)
)

// ── 13:00 矮人城入口 Dwarf City Entrance ──
// CityLevel color1=0x4B6636, color2=0xF2F2F2
private val HOUR_13 = ZonePalette(
  Color(0xFF4B6636), Color(0xFFF2F2F2),
  Color(0xFF3A3A3A), Color(0xFF505050), Color(0xFF222222),
  Color(0xFF141414), Color(0xFF0A0A0A)
)

// ── 14:00 矮人城市中心 Dwarf City Center ──
// City steel, LootIndicator=0x185898, machinery
private val HOUR_14 = ZonePalette(
  Color(0xFF185898), Color(0xFFB0BEC5),
  Color(0xFF2E3A44), Color(0xFF44525C), Color(0xFF1C2630),
  Color(0xFF101820), Color(0xFF0A1018)
)

// ── 15:00 矮人国王 Dwarf King Boss ──
// WardSprite blood=0xCC33FF, GolemSprite blood=0x80706C
private val HOUR_15 = ZonePalette(
  Color(0xFFCC33FF), Color(0xFF80706C),
  Color(0xFF342838), Color(0xFF4A3A50), Color(0xFF201A24),
  Color(0xFF141018), Color(0xFF0C0A10)
)

// ── 16:00 恶魔大厅入口 Demon Halls Entrance ──
// Halls base red glow, GDX_COLOR=0xE44D3C
private val HOUR_16 = ZonePalette(
  Color(0xFFE44D3C), Color(0xFFA68521),
  Color(0xFF3D1A0E), Color(0xFF5A2814), Color(0xFF280E06),
  Color(0xFF140A04), Color(0xFF0E0604)
)

// ── 17:00 魔眼层 Evil Eyes ──
// DeepPK_COLOR=0x792F9E, dark magic purple
private val HOUR_17 = ZonePalette(
  Color(0xFF792F9E), Color(0xFFFF1493),
  Color(0xFF2E1838), Color(0xFF442650), Color(0xFF1A0E24),
  Color(0xFF100A18), Color(0xFF0A0610)
)

// ── 18:00 蝎子巢穴 Scorpio Nest ──
// PoisonParticle 0x00FF00..0x8844FF, toxic + arcane
private val HOUR_18 = ZonePalette(
  Color(0xFF00FF00), Color(0xFF8844FF),
  Color(0xFF1E2E18), Color(0xFF2E4426), Color(0xFF121C0E),
  Color(0xFF0C140A), Color(0xFF080E06)
)

// ── 19:00 犹格索托斯 Yog-Dzewa Boss ──
// Yog-Zot title=0xFF0000, flash=0xE44D3C, hellfire
private val HOUR_19 = ZonePalette(
  Color(0xFFFF0000), Color(0xFFE44D3C),
  Color(0xFF440000), Color(0xFF661000), Color(0xFF2A0000),
  Color(0xFF1A0000), Color(0xFF100000)
)

// ── 20:00 明亮拳魔 Bright Fist ──
// FistSprite.Bright blood=0xFFFFFF, holy light
private val HOUR_20 = ZonePalette(
  Color(0xFFFFFFFF), Color(0xFFFFFFAA),
  Color(0xFF3A3828), Color(0xFF504C38), Color(0xFF242218),
  Color(0xFF161410), Color(0xFF0E0C0A)
)

// ── 21:00 暗影拳魔 Dark Fist ──
// FistSprite.Dark blood=0x4A2F53, shadow magic
private val HOUR_21 = ZonePalette(
  Color(0xFF4A2F53), Color(0xFFB085D5),
  Color(0xFF221828), Color(0xFF34263C), Color(0xFF140E18),
  Color(0xFF0C0810), Color(0xFF080608)
)

// ── 22:00 锈蚀拳魔 Rusted Fist ──
// FistSprite.Rusted blood=0x7F7F7F, CORROSION 0xAAAAAA→0xFF8800
private val HOUR_22 = ZonePalette(
  Color(0xFFAAAAAA), Color(0xFFFF8800),
  Color(0xFF363636), Color(0xFF4A4A4A), Color(0xFF222222),
  Color(0xFF141414), Color(0xFF0C0C0C)
)

// ── 23:00 最终之厅 Last Level ──
// NewLastLevel halos=0xE44D3C, gold TEXT_WIN=0xFFFF88
private val HOUR_23 = ZonePalette(
  Color(0xFFFFFF88), Color(0xFFE44D3C),
  Color(0xFF3D2010), Color(0xFF5A3018), Color(0xFF281408),
  Color(0xFF180C04), Color(0xFF100804)
)

// 24 hourly zones
private val HOURLY_ZONES = listOf(
  HOUR_00, HOUR_01, HOUR_02, HOUR_03, HOUR_04, HOUR_05,
  HOUR_06, HOUR_07, HOUR_08, HOUR_09, HOUR_10, HOUR_11,
  HOUR_12, HOUR_13, HOUR_14, HOUR_15, HOUR_16, HOUR_17,
  HOUR_18, HOUR_19, HOUR_20, HOUR_21, HOUR_22, HOUR_23
)

/** Current hour index 0..23 */
fun getTimeSegment(): Pair<Int, Float> {
  val cal = Calendar.getInstance()
  val hour = cal.get(Calendar.HOUR_OF_DAY)
  val minute = cal.get(Calendar.MINUTE)
  return Pair(hour, minute / 60f)
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

/** Direct zone palette — each hour has its own distinct look */
fun currentZonePalette(): ZonePalette {
  val (hour, _) = getTimeSegment()
  return HOURLY_ZONES[hour]
}

data class TimeTint(val accent1: Color, val accent2: Color, val surfaceTint: Color)
fun currentTimeTint(): TimeTint {
  val p = currentZonePalette()
  return TimeTint(p.color1, p.color2, p.floor)
}

/** Zone names — each hour maps to a dungeon area/encounter */
fun currentZoneName(): String {
  return when (getTimeSegment().first) {
    0  -> "恶魔大厅深层 Deep Demon Halls"
    1  -> "燃烧拳魔 Burning Fist"
    2  -> "腐烂拳魔 Rotting Fist"
    3  -> "冰霜拳魔 Ice Fist"
    4  -> "下水道 Sewers"
    5  -> "下水道BOSS Goo"
    6  -> "花园 Garden"
    7  -> "监狱 Prison"
    8  -> "监狱牢房 Prison Cells"
    9  -> "天狗 Tengu"
    10 -> "洞穴 Caves"
    11 -> "矿洞深处 Deep Mines"
    12 -> "水晶洞窟 Crystal Caves"
    13 -> "矮人城 Dwarf City"
    14 -> "矮人城中心 City Center"
    15 -> "矮人国王 Dwarf King"
    16 -> "恶魔大厅 Demon Halls"
    17 -> "魔眼层 Evil Eyes"
    18 -> "蝎子巢穴 Scorpio Nest"
    19 -> "犹格索托斯 Yog-Dzewa"
    20 -> "光明拳魔 Bright Fist"
    21 -> "暗影拳魔 Dark Fist"
    22 -> "锈蚀拳魔 Rusted Fist"
    23 -> "最终之厅 Last Level"
    else -> "地牢 Dungeon"
  }
}

/** Bottom nav icon res IDs that change with time of day */
fun pdNavIcons(): Triple<Int, Int, Int> {
  return when (getTimeSegment().first) {
    // Demon Halls / Boss hours: scroll + sword + potion (classic adventure)
    0, 1, 2, 3, 16, 19 -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_sword, R.drawable.ic_pd_potion)
    // Sewers / Garden: chest + key + potion (exploration)
    4, 5, 6 -> Triple(R.drawable.ic_pd_chest, R.drawable.ic_pd_key, R.drawable.ic_pd_potion)
    // Prison: scroll + sword + book (study & combat)
    7, 8, 9 -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_sword, R.drawable.ic_pd_book)
    // Caves / Mines: chest + sword + amulet (treasure hunting)
    10, 11, 12 -> Triple(R.drawable.ic_pd_chest, R.drawable.ic_pd_sword, R.drawable.ic_pd_amulet)
    // Dwarf City: scroll + wand + potion (magic)
    13, 14, 15 -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_wand, R.drawable.ic_pd_potion)
    // Evil Eyes / Scorpio: wand + sword + ring (magic combat)
    17, 18 -> Triple(R.drawable.ic_pd_wand, R.drawable.ic_pd_sword, R.drawable.ic_pd_ring)
    // Fists / Last Level: amulet + sword + torch (endgame)
    20, 21, 22, 23 -> Triple(R.drawable.ic_pd_amulet, R.drawable.ic_pd_sword, R.drawable.ic_pd_torch)
    else -> Triple(R.drawable.ic_pd_scroll, R.drawable.ic_pd_sword, R.drawable.ic_pd_potion)
  }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Global brick wall bitmap cache — survives navigation
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private var cachedBrickBitmap: ImageBitmap? = null
private var cachedBrickKey: BrickCacheKey? = null

private const val BRICK_WIDTH_PX = 28
private const val BRICK_HEIGHT_PX = 14
private const val BRICK_GAP_PX = 2
private val BRICK_LAYOUT_SEED = Random.nextInt()
private val BRICK_ROW_OFFSET_STEPS = intArrayOf(
  0,
  BRICK_WIDTH_PX / 4,
  BRICK_WIDTH_PX / 2,
  (BRICK_WIDTH_PX * 3) / 4
)

data class BrickTextureSpec(
  val textureWidthPx: Int,
  val textureHeightPx: Int
)

fun brickTextureSpec(screenWidthPx: Int, screenHeightPx: Int): BrickTextureSpec {
  return BrickTextureSpec(
    textureWidthPx = (BRICK_WIDTH_PX * 16).coerceAtMost(screenWidthPx.coerceAtLeast(BRICK_WIDTH_PX * 16)),
    textureHeightPx = (BRICK_HEIGHT_PX * 18).coerceAtMost(screenHeightPx.coerceAtLeast(BRICK_HEIGHT_PX * 18))
  )
}

private fun brickPatternHash(row: Int, col: Int, hour: Int, layoutSeed: Int = BRICK_LAYOUT_SEED, salt: Int = 0): Int {
  val mixed = (row + 37) * 137 + (col + 73) * 269 + (hour + 11) * 97 + salt * 53 + layoutSeed * 17
  return mixed and 0xFF
}

private fun wallRuleHash(row: Int, col: Int, hour: Int, layoutSeed: Int = BRICK_LAYOUT_SEED, salt: Int = 0): Int {
  var mixed = layoutSeed
  mixed = mixed xor ((row + 1) * 0x45D9F3B)
  mixed = mixed xor ((col + 11) * 0x27D4EB2D)
  mixed = mixed xor ((hour + 7) * 0x165667B1)
  mixed = mixed xor (salt * 0x9E3779B9.toInt())
  mixed = mixed xor (mixed ushr 16)
  return mixed and Int.MAX_VALUE
}

fun brickRowOffset(row: Int, hour: Int, layoutSeed: Int = BRICK_LAYOUT_SEED): Int {
  val theme = resolveWallTheme(hour)
  val offsetSteps = if (theme.template.offsetSteps.isNotEmpty()) {
    theme.template.offsetSteps
  } else {
    BRICK_ROW_OFFSET_STEPS.toList()
  }
  val randomOffset = offsetSteps[
    wallRuleHash(row = row, col = -1, hour = hour, layoutSeed = layoutSeed, salt = 17) % offsetSteps.size
  ]
  return randomOffset % BRICK_WIDTH_PX
}

/**
 * Zone-specific decoration types — controls what details are painted on bricks.
 * Inspired by MLPD's per-chapter tile decorations:
 * Sewers=moss/water, Prison=torch_scorch/chains, Caves=ore/cracks,
 * City=engravings/smoke, Halls=bones/lava, Bosses=themed.
 */
internal enum class WallDeco {
  MOSS,           // Sewers: green moss patches, dripping water stains
  VINES,          // Garden: hanging vines, leaf litter
  TORCH_SCORCH,   // Prison: torch burn marks, chain bolts
  CRACKS,         // Caves: rock fractures, ore veins sparkling
  CRYSTAL,        // Crystal caves: blue/cyan crystal growths on walls
  ENGRAVINGS,     // City: carved runes, metal rivets, soot
  ARCANE,         // Dwarf King / magic: glowing glyphs
  BONES,          // Halls: bone fragments, blood drips
  LAVA_DRIP,      // Yog / deep halls: lava seams, heat distortion
  FROST,          // Ice Fist: ice crystals, frozen cracks
  CORRUPTION,     // Rotting / poison: corrosion spots, slime
  HOLY,           // Bright Fist: light cracks, golden veins
  SHADOW,         // Dark Fist: void patches, purple wisps
  RUSTED,         // Rusted Fist: rust streaks, oxidation
  GOLD_TRIM       // Last level: golden inlays, amulet symbols
}

internal enum class WallChapter {
  SEWERS,
  PRISON,
  CAVES,
  CRYSTAL_CAVES,
  CITY,
  HALLS
}

internal data class ChapterWallTemplate(
  val chapter: WallChapter,
  val segmentWidths: List<Int>,
  val offsetSteps: List<Int>,
  val courseHeightPx: Int,
  val mortarThicknessPx: Int,
  val highlightDepthPx: Int,
  val shadowDepthPx: Int,
  val moistureBias: Int = 0,
  val engravingBias: Int = 0,
  val damageBias: Int = 0,
  val crackBias: Int = 0,
  val runeBias: Int = 0,
  val sootBias: Int = 0,
  val wallDecos: List<WallDeco>
)

internal data class HourWallVariant(
  val wallDecos: List<WallDeco> = emptyList(),
  val moistureBoost: Int = 0,
  val engravingBoost: Int = 0,
  val damageBoost: Int = 0,
  val crackBoost: Int = 0,
  val runeBoost: Int = 0,
  val scorchBoost: Int = 0,
  val lavaBoost: Int = 0,
  val corruptionBoost: Int = 0,
  val frostBoost: Int = 0,
  val crystalBoost: Int = 0,
  val holyBoost: Int = 0,
  val shadowBoost: Int = 0,
  val rustBoost: Int = 0
)

internal data class ResolvedWallTheme(
  val chapter: WallChapter,
  val template: ChapterWallTemplate,
  val variant: HourWallVariant
)

internal data class BrickRowPlan(
  val offsetPx: Int,
  val courseHeightPx: Int,
  val mortarThicknessPx: Int,
  val highlightDepthPx: Int,
  val shadowDepthPx: Int,
  val segmentWidths: List<Int>
)

private data class BrickCacheKey(
  val hour: Int,
  val layoutSeed: Int
)

private enum class BrickRole {
  NORMAL,
  CHIPPED,
  WET,
  ENGRAVED,
  CRACKED,
  SCORCHED,
  ORE_VEIN,
  RUNE_CUT,
  CORRODED,
  CRYSTALLIZED,
  HOLY_SCAR,
  SHADOWED,
  RUST_STAINED,
  LAVA_SCAR
}

private fun resolveBrickRole(hash: Int, hash2: Int, theme: ResolvedWallTheme): BrickRole {
  val variantRoll = (hash + hash2) % 100
  return when {
    theme.variant.holyBoost > 0 && variantRoll < theme.variant.holyBoost -> BrickRole.HOLY_SCAR
    theme.variant.shadowBoost > 0 && variantRoll < theme.variant.shadowBoost -> BrickRole.SHADOWED
    theme.variant.rustBoost > 0 && variantRoll < theme.variant.rustBoost -> BrickRole.RUST_STAINED
    theme.variant.lavaBoost > 0 && variantRoll < theme.variant.lavaBoost -> BrickRole.LAVA_SCAR
    theme.variant.crystalBoost > 0 && variantRoll < theme.variant.crystalBoost -> BrickRole.CRYSTALLIZED
    theme.variant.corruptionBoost > 0 && variantRoll < theme.variant.corruptionBoost -> BrickRole.CORRODED
    theme.variant.scorchBoost > 0 && variantRoll < theme.variant.scorchBoost -> BrickRole.SCORCHED
    else -> when (theme.chapter) {
      WallChapter.SEWERS -> when {
        variantRoll < theme.template.moistureBias + theme.variant.moistureBoost -> BrickRole.WET
        variantRoll < theme.template.moistureBias + theme.variant.moistureBoost + 18 -> BrickRole.CORRODED
        variantRoll < theme.template.moistureBias + theme.template.damageBias + 18 -> BrickRole.CHIPPED
        else -> BrickRole.NORMAL
      }
      WallChapter.PRISON -> when {
        variantRoll < theme.template.sootBias + 10 -> BrickRole.SCORCHED
        variantRoll < theme.template.sootBias + theme.template.crackBias + 8 -> BrickRole.CRACKED
        variantRoll < theme.template.sootBias + theme.template.crackBias + theme.template.damageBias + 10 -> BrickRole.CHIPPED
        else -> BrickRole.NORMAL
      }
      WallChapter.CAVES -> when {
        variantRoll < theme.template.crackBias + theme.variant.crackBoost -> BrickRole.CRACKED
        variantRoll < theme.template.crackBias + theme.variant.crackBoost + 18 -> BrickRole.ORE_VEIN
        variantRoll < theme.template.crackBias + theme.variant.crackBoost + theme.template.damageBias + 12 -> BrickRole.CHIPPED
        else -> BrickRole.NORMAL
      }
      WallChapter.CRYSTAL_CAVES -> when {
        variantRoll < 18 + theme.variant.crystalBoost -> BrickRole.CRYSTALLIZED
        variantRoll < 18 + theme.variant.crystalBoost + theme.template.crackBias -> BrickRole.CRACKED
        variantRoll < 18 + theme.variant.crystalBoost + theme.template.crackBias + 14 -> BrickRole.ORE_VEIN
        else -> BrickRole.NORMAL
      }
      WallChapter.CITY -> when {
        variantRoll < theme.template.engravingBias + theme.variant.engravingBoost -> BrickRole.ENGRAVED
        variantRoll < theme.template.engravingBias + theme.variant.engravingBoost + theme.template.runeBias + theme.variant.runeBoost -> BrickRole.RUNE_CUT
        variantRoll < theme.template.engravingBias + theme.variant.engravingBoost + theme.template.runeBias + theme.variant.runeBoost + 12 -> BrickRole.CHIPPED
        else -> BrickRole.NORMAL
      }
      WallChapter.HALLS -> when {
        variantRoll < theme.template.sootBias + 8 -> BrickRole.SCORCHED
        variantRoll < theme.template.sootBias + 8 + theme.template.damageBias -> BrickRole.CHIPPED
        variantRoll < theme.template.sootBias + 8 + theme.template.damageBias + theme.template.crackBias -> BrickRole.CRACKED
        else -> BrickRole.NORMAL
      }
    }
  }
}

private fun paintBrickRole(
  role: BrickRole,
  bx: Int,
  by: Int,
  brickW: Int,
  brickH: Int,
  gap: Int,
  innerW: Int,
  innerH: Int,
  bodyArgb: Int,
  lightArgb: Int,
  darkArgb: Int,
  mortarArgb: Int,
  palette: ZonePalette,
  hash: Int,
  hash2: Int,
  px: (Int, Int, Int) -> Unit
) {
  when (role) {
    BrickRole.NORMAL -> Unit

    BrickRole.CHIPPED -> {
      val chip = hash % 4
      when (chip) {
        0 -> {
          px(bx + gap, by + gap, mortarArgb)
          px(bx + gap + 1, by + gap, mortarArgb)
          px(bx + gap, by + gap + 1, mortarArgb)
          px(bx + gap + 1, by + gap + 1, darkArgb)
        }
        1 -> {
          px(bx + brickW - gap - 1, by + gap, mortarArgb)
          px(bx + brickW - gap - 2, by + gap, mortarArgb)
          px(bx + brickW - gap - 1, by + gap + 1, mortarArgb)
          px(bx + brickW - gap - 2, by + gap + 1, darkArgb)
        }
        2 -> {
          px(bx + gap, by + brickH - gap - 1, mortarArgb)
          px(bx + gap, by + brickH - gap - 2, mortarArgb)
          px(bx + gap + 1, by + brickH - gap - 1, mortarArgb)
          px(bx + gap + 1, by + brickH - gap - 2, darkArgb)
        }
        else -> {
          px(bx + brickW - gap - 1, by + brickH - gap - 1, mortarArgb)
          px(bx + brickW - gap - 2, by + brickH - gap - 1, mortarArgb)
          px(bx + brickW - gap - 1, by + brickH - gap - 2, mortarArgb)
          px(bx + brickW - gap - 2, by + brickH - gap - 2, darkArgb)
        }
      }
    }

    BrickRole.WET -> {
      val wetColor = blendColors(bodyArgb, mortarArgb, 0.35f)
      val sheenColor = shiftBrightness(wetColor, 14)
      for (x in (bx + gap + 1) until (bx + brickW - gap - 1)) {
        px(x, by + brickH - gap - 3, wetColor)
        px(x, by + brickH - gap - 2, wetColor)
      }
      for (x in (bx + gap + 2) until (bx + brickW - gap - 2) step 3) {
        px(x, by + brickH - gap - 4, sheenColor)
      }
    }

    BrickRole.ENGRAVED -> {
      val groove = shiftBrightness(darkArgb, 6)
      val glyph = shiftBrightness(lightArgb, -10)
      val midY = by + gap + innerH / 2
      for (x in (bx + gap + 3) until (bx + brickW - gap - 3) step 2) {
        px(x, midY, groove)
      }
      px(bx + brickW / 2, midY - 1, glyph)
      px(bx + brickW / 2 - 1, midY, glyph)
      px(bx + brickW / 2 + 1, midY, glyph)
    }

    BrickRole.CRACKED -> {
      val crack = shiftBrightness(darkArgb, -8)
      val startX = bx + gap + 1 + hash2 % (innerW - 2).coerceAtLeast(1)
      val startY = by + gap + 1
      for (i in 0 until minOf(5, innerW - 1, innerH - 1)) {
        px(startX + (i / 2), startY + i, crack)
        if (i % 2 == 0) px(startX + (i / 2) + 1, startY + i, crack)
      }
    }

    BrickRole.SCORCHED -> {
      val soot = shiftBrightness(darkArgb, -18)
      val hotAsh = shiftBrightness(soot, 10)
      val cx = bx + brickW / 2
      px(cx, by + gap + 1, soot)
      px(cx - 1, by + gap + 1, soot)
      px(cx + 1, by + gap + 1, soot)
      px(cx, by + gap + 2, hotAsh)
      px(cx - 1, by + gap + 2, soot)
      px(cx + 1, by + gap + 2, soot)
    }

    BrickRole.ORE_VEIN -> {
      val vein = blendColors(palette.color2.toArgb(), 0xFFFFE08A.toInt(), 0.4f)
      val startX = bx + gap + 2
      val startY = by + gap + 2 + hash % (innerH - 3).coerceAtLeast(1)
      for (i in 0 until minOf(5, innerW - 2)) {
        px(startX + i, startY + (i % 2), if (i % 2 == 0) vein else shiftBrightness(vein, -10))
      }
    }

    BrickRole.RUNE_CUT -> {
      val rune = blendColors(palette.color1.toArgb(), lightArgb, 0.35f)
      val slot = shiftBrightness(darkArgb, -4)
      val cx = bx + brickW / 2
      val cy = by + brickH / 2
      px(cx, cy - 2, rune)
      px(cx - 1, cy - 1, slot); px(cx + 1, cy - 1, slot)
      px(cx - 1, cy, rune); px(cx + 1, cy, rune)
      px(cx, cy + 1, slot); px(cx, cy + 2, rune)
    }

    BrickRole.CORRODED -> {
      val rot = blendColors(bodyArgb, palette.color2.toArgb(), 0.45f)
      for (x in 0..2) {
        px(bx + gap + 1 + x, by + brickH - gap - 2, rot)
        px(bx + brickW - gap - 2 - x, by + brickH - gap - 2, shiftBrightness(rot, x * 6))
      }
      px(bx + brickW / 2, by + brickH - gap - 3, shiftBrightness(rot, 12))
      px(bx + brickW / 2 - 1, by + brickH - gap - 2, mortarArgb)
    }

    BrickRole.CRYSTALLIZED -> {
      val crystal = blendColors(palette.color1.toArgb(), 0xFFAADDFF.toInt(), 0.55f)
      val crystalTip = shiftBrightness(crystal, 20)
      val cx = bx + gap + 3 + hash2 % (innerW - 4).coerceAtLeast(1)
      val cy = by + brickH - gap - 2
      px(cx, cy, crystal)
      px(cx, cy - 1, crystal)
      px(cx, cy - 2, crystalTip)
      px(cx - 1, cy, shiftBrightness(crystal, -10))
      px(cx + 1, cy, crystalTip)
    }

    BrickRole.HOLY_SCAR -> {
      val gold = 0xFFFFDD66.toInt()
      val bright = 0xFFFFFFCC.toInt()
      val startX = bx + gap + 2
      val startY = by + gap + 2
      for (i in 0 until minOf(4, innerW - 2, innerH - 2)) {
        px(startX + i, startY + i, if (i % 2 == 0) gold else bright)
      }
      px(startX + 1, startY, bright)
    }

    BrickRole.SHADOWED -> {
      val void = 0xFF0A0610.toInt()
      val edge = blendColors(bodyArgb, void, 0.55f)
      val cx = bx + brickW / 2
      val cy = by + brickH / 2
      px(cx, cy, void)
      px(cx + 1, cy, void)
      px(cx, cy + 1, void)
      px(cx - 1, cy, edge)
      px(cx + 1, cy + 1, edge)
    }

    BrickRole.RUST_STAINED -> {
      val rust = blendColors(0xFFAA5500.toInt(), palette.color2.toArgb(), 0.25f)
      val rx = bx + gap + 2 + hash % (innerW - 4).coerceAtLeast(1)
      for (i in 0 until minOf(4, innerH - 2)) {
        px(rx + (i % 2), by + gap + 1 + i, shiftBrightness(rust, i * 4))
      }
    }

    BrickRole.LAVA_SCAR -> {
      val lava = 0xFFFF5500.toInt()
      val glow = 0xFFFFAA44.toInt()
      val crackY = by + brickH / 2
      for (x in (bx + gap + 1) until (bx + brickW - gap - 1) step 2) {
        px(x, crackY, if ((x + hash) % 3 == 0) glow else lava)
      }
      px(bx + brickW / 2, crackY - 1, glow)
    }
  }
}

private fun paintChapterGeometry(
  theme: ResolvedWallTheme,
  bx: Int,
  by: Int,
  brickW: Int,
  brickH: Int,
  gap: Int,
  innerW: Int,
  innerH: Int,
  bodyArgb: Int,
  lightArgb: Int,
  darkArgb: Int,
  mortarArgb: Int,
  palette: ZonePalette,
  hash: Int,
  hash2: Int,
  px: (Int, Int, Int) -> Unit
) {
  when (theme.chapter) {
    WallChapter.SEWERS -> {
      val damp = blendColors(bodyArgb, mortarArgb, 0.38f)
      val slime = blendColors(palette.color2.toArgb(), damp, 0.4f)
      for (x in (bx + gap + 1) until (bx + brickW - gap - 1)) {
        px(x, by + brickH - gap - 3, damp)
        px(x, by + brickH - gap - 2, if ((x + hash) % 4 == 0) slime else damp)
      }
      if (hash % 3 == 0) {
        val notchX = bx + gap + 2 + hash2 % (innerW - 3).coerceAtLeast(1)
        px(notchX, by + brickH - gap - 1, mortarArgb)
        px(notchX + 1, by + brickH - gap - 1, mortarArgb)
      }
    }

    WallChapter.PRISON -> {
      val groove = blendColors(darkArgb, mortarArgb, 0.2f)
      val strap = 0xFF8A8A8A.toInt()
      val bandY = by + gap + innerH / 2
      for (x in (bx + gap + 2) until (bx + brickW - gap - 2)) {
        px(x, bandY, groove)
      }
      px(bx + gap + 2, bandY, strap)
      px(bx + brickW - gap - 3, bandY, strap)
    }

    WallChapter.CAVES -> {
      val chasm = shiftBrightness(darkArgb, -8)
      val notchX = bx + gap + 1 + hash % (innerW - 2).coerceAtLeast(1)
      px(notchX, by + gap, mortarArgb)
      px(notchX + 1, by + gap, mortarArgb)
      for (i in 0 until minOf(4, innerW - 2, innerH - 2)) {
        px(bx + gap + 2 + i, by + gap + 1 + i, chasm)
      }
    }

    WallChapter.CRYSTAL_CAVES -> {
      val seam = shiftBrightness(darkArgb, -6)
      val glint = blendColors(palette.color1.toArgb(), 0xFFAADDFF.toInt(), 0.55f)
      for (i in 0 until minOf(4, innerW - 2, innerH - 2)) {
        px(bx + gap + 2 + i, by + gap + 1 + i, seam)
      }
      px(bx + brickW / 2, by + gap + 1, glint)
      px(bx + brickW / 2 + 1, by + gap + 2, glint)
    }

    WallChapter.CITY -> {
      val inset = shiftBrightness(darkArgb, 10)
      val plaque = shiftBrightness(lightArgb, -14)
      val left = bx + gap + 2
      val right = bx + brickW - gap - 3
      val top = by + gap + 1
      val bottom = by + brickH - gap - 2
      for (x in left..right) {
        px(x, top, inset)
        px(x, bottom, inset)
      }
      for (y in top..bottom) {
        px(left, y, inset)
        px(right, y, inset)
      }
      for (x in (left + 2) until right step 2) {
        px(x, by + brickH / 2, plaque)
      }
    }

    WallChapter.HALLS -> {
      val abyss = shiftBrightness(darkArgb, -18)
      val ember = blendColors(palette.color1.toArgb(), 0xFFFF6600.toInt(), 0.25f)
      for (x in (bx + gap) until (bx + brickW - gap)) {
        px(x, by + brickH - gap - 2, abyss)
        px(x, by + brickH - gap - 1, abyss)
      }
      for (y in (by + gap) until (by + brickH - gap)) {
        px(bx + brickW - gap - 2, y, abyss)
      }
      if (hash % 2 == 0) {
        val slitX = bx + brickW / 2
        for (y in (by + gap + 1) until (by + brickH - gap - 1)) {
          px(slitX, y, if ((y + hash2) % 3 == 0) ember else abyss)
        }
      }
    }
  }
}

private val SEWERS_TEMPLATE = ChapterWallTemplate(
  chapter = WallChapter.SEWERS,
  segmentWidths = listOf(20, 30, 24, 32, 22, 28),
  offsetSteps = listOf(0, 7, 14, 21),
  courseHeightPx = 18,
  mortarThicknessPx = 3,
  highlightDepthPx = 1,
  shadowDepthPx = 1,
  moistureBias = 24,
  damageBias = 18,
  crackBias = 10,
  wallDecos = listOf(WallDeco.MOSS, WallDeco.CORRUPTION)
)

private val PRISON_TEMPLATE = ChapterWallTemplate(
  chapter = WallChapter.PRISON,
  segmentWidths = listOf(28, 28, 30, 28, 28, 30),
  offsetSteps = listOf(0, 7, 14),
  courseHeightPx = 12,
  mortarThicknessPx = 2,
  highlightDepthPx = 1,
  shadowDepthPx = 1,
  damageBias = 14,
  crackBias = 16,
  sootBias = 14,
  wallDecos = listOf(WallDeco.TORCH_SCORCH, WallDeco.CRACKS)
)

private val CAVES_TEMPLATE = ChapterWallTemplate(
  chapter = WallChapter.CAVES,
  segmentWidths = listOf(22, 34, 24, 30, 26, 32),
  offsetSteps = listOf(0, 7, 14),
  courseHeightPx = 16,
  mortarThicknessPx = 2,
  highlightDepthPx = 1,
  shadowDepthPx = 2,
  damageBias = 18,
  crackBias = 28,
  wallDecos = listOf(WallDeco.CRACKS)
)

private val CRYSTAL_CAVES_TEMPLATE = ChapterWallTemplate(
  chapter = WallChapter.CRYSTAL_CAVES,
  segmentWidths = listOf(24, 32, 22, 34, 26, 30),
  offsetSteps = listOf(0, 7, 14),
  courseHeightPx = 16,
  mortarThicknessPx = 2,
  highlightDepthPx = 1,
  shadowDepthPx = 2,
  damageBias = 16,
  crackBias = 22,
  runeBias = 10,
  wallDecos = listOf(WallDeco.CRYSTAL, WallDeco.CRACKS)
)

private val CITY_TEMPLATE = ChapterWallTemplate(
  chapter = WallChapter.CITY,
  segmentWidths = listOf(30, 30, 28, 32, 30, 28),
  offsetSteps = listOf(0, 7, 14),
  courseHeightPx = 12,
  mortarThicknessPx = 2,
  highlightDepthPx = 1,
  shadowDepthPx = 2,
  engravingBias = 24,
  damageBias = 8,
  runeBias = 18,
  wallDecos = listOf(WallDeco.ENGRAVINGS, WallDeco.ARCANE)
)

private val HALLS_TEMPLATE = ChapterWallTemplate(
  chapter = WallChapter.HALLS,
  segmentWidths = listOf(24, 30, 26, 34, 22, 28),
  offsetSteps = listOf(0, 7, 14),
  courseHeightPx = 20,
  mortarThicknessPx = 2,
  highlightDepthPx = 1,
  shadowDepthPx = 3,
  damageBias = 24,
  crackBias = 20,
  sootBias = 22,
  wallDecos = listOf(WallDeco.BONES, WallDeco.LAVA_DRIP)
)

internal fun resolveWallTheme(hour: Int): ResolvedWallTheme {
  val normalizedHour = hour.coerceIn(0, 23)
  return when (normalizedHour) {
    0, 16 -> ResolvedWallTheme(WallChapter.HALLS, HALLS_TEMPLATE, HourWallVariant())
    1 -> ResolvedWallTheme(
      WallChapter.HALLS,
      HALLS_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.TORCH_SCORCH), scorchBoost = 20, lavaBoost = 26)
    )
    2 -> ResolvedWallTheme(
      WallChapter.SEWERS,
      SEWERS_TEMPLATE,
      HourWallVariant(corruptionBoost = 24, moistureBoost = 8, damageBoost = 6)
    )
    3 -> ResolvedWallTheme(
      WallChapter.CRYSTAL_CAVES,
      CRYSTAL_CAVES_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.FROST), frostBoost = 26, crystalBoost = 16)
    )
    4 -> ResolvedWallTheme(WallChapter.SEWERS, SEWERS_TEMPLATE, HourWallVariant(moistureBoost = 10))
    5 -> ResolvedWallTheme(
      WallChapter.SEWERS,
      SEWERS_TEMPLATE,
      HourWallVariant(corruptionBoost = 22, moistureBoost = 14, damageBoost = 8)
    )
    6 -> ResolvedWallTheme(
      WallChapter.SEWERS,
      SEWERS_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.VINES), moistureBoost = 12)
    )
    7, 8 -> ResolvedWallTheme(WallChapter.PRISON, PRISON_TEMPLATE, HourWallVariant())
    9 -> ResolvedWallTheme(
      WallChapter.PRISON,
      PRISON_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.BONES), scorchBoost = 12, damageBoost = 12)
    )
    10, 11 -> ResolvedWallTheme(
      WallChapter.CAVES,
      CAVES_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.TORCH_SCORCH), crackBoost = 10)
    )
    12 -> ResolvedWallTheme(
      WallChapter.CRYSTAL_CAVES,
      CRYSTAL_CAVES_TEMPLATE,
      HourWallVariant(crystalBoost = 20, frostBoost = 8)
    )
    13, 14 -> ResolvedWallTheme(WallChapter.CITY, CITY_TEMPLATE, HourWallVariant())
    15 -> ResolvedWallTheme(
      WallChapter.CITY,
      CITY_TEMPLATE,
      HourWallVariant(engravingBoost = 6, runeBoost = 18)
    )
    17 -> ResolvedWallTheme(
      WallChapter.HALLS,
      HALLS_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.ARCANE, WallDeco.SHADOW), runeBoost = 18, shadowBoost = 24)
    )
    18 -> ResolvedWallTheme(
      WallChapter.CAVES,
      CAVES_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.CORRUPTION, WallDeco.BONES), corruptionBoost = 20)
    )
    19 -> ResolvedWallTheme(
      WallChapter.HALLS,
      HALLS_TEMPLATE,
      HourWallVariant(scorchBoost = 18, lavaBoost = 28, damageBoost = 10)
    )
    20 -> ResolvedWallTheme(
      WallChapter.CITY,
      CITY_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.HOLY), holyBoost = 24, engravingBoost = 6)
    )
    21 -> ResolvedWallTheme(
      WallChapter.HALLS,
      HALLS_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.SHADOW, WallDeco.ARCANE), shadowBoost = 26, runeBoost = 14)
    )
    22 -> ResolvedWallTheme(
      WallChapter.CAVES,
      CAVES_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.RUSTED), rustBoost = 28, damageBoost = 8)
    )
    23 -> ResolvedWallTheme(
      WallChapter.HALLS,
      HALLS_TEMPLATE,
      HourWallVariant(wallDecos = listOf(WallDeco.GOLD_TRIM), lavaBoost = 12, engravingBoost = 10)
    )
    else -> ResolvedWallTheme(WallChapter.CAVES, CAVES_TEMPLATE, HourWallVariant())
  }
}

private fun resolvedWallDecos(theme: ResolvedWallTheme): List<WallDeco> {
  return (theme.template.wallDecos + theme.variant.wallDecos).distinct()
}

internal fun brickRowPlan(
  row: Int,
  hour: Int,
  theme: ResolvedWallTheme = resolveWallTheme(hour),
  layoutSeed: Int = BRICK_LAYOUT_SEED
): BrickRowPlan {
  val template = theme.template
  val rotation = wallRuleHash(row = row, col = -3, hour = hour, layoutSeed = layoutSeed, salt = 41) % template.segmentWidths.size
  val varianceRange = when (theme.chapter) {
    WallChapter.PRISON -> 0
    WallChapter.CITY -> 1
    WallChapter.CRYSTAL_CAVES -> 2
    WallChapter.CAVES, WallChapter.HALLS -> 3
    WallChapter.SEWERS -> 4
  }
  val segmentWidths = template.segmentWidths.indices.map { index ->
    val base = template.segmentWidths[(index + rotation) % template.segmentWidths.size]
    val jitter = (((wallRuleHash(row = row, col = index, hour = hour, layoutSeed = layoutSeed, salt = 97) % 5) - 2) * 2)
      .coerceIn(-varianceRange * 2, varianceRange * 2)
    val chippedPenalty = if (wallRuleHash(row = row, col = index, hour = hour, layoutSeed = layoutSeed, salt = 131) % 100 <
      template.damageBias + theme.variant.damageBoost) {
      2
    } else {
      0
    }
    (base + jitter - chippedPenalty).coerceIn(18, 36)
  }
  return BrickRowPlan(
    offsetPx = brickRowOffset(row = row, hour = hour, layoutSeed = layoutSeed),
    courseHeightPx = template.courseHeightPx,
    mortarThicknessPx = template.mortarThicknessPx.coerceIn(1, 3),
    highlightDepthPx = template.highlightDepthPx.coerceIn(1, 2),
    shadowDepthPx = template.shadowDepthPx.coerceIn(1, 3),
    segmentWidths = segmentWidths
  )
}

private fun generateBrickBitmap(w: Int, h: Int, palette: ZonePalette, hour: Int = 0): ImageBitmap {
  val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
  val theme = resolveWallTheme(hour)
  val baseBrickH = theme.template.courseHeightPx
  val mortarArgb = palette.mortar.toArgb()
  val wallArgb = palette.wall.toArgb()
  val lightArgb = palette.wallLight.toArgb()
  val darkArgb = palette.wallDark.toArgb()
  val decos = resolvedWallDecos(theme)

  // Fill mortar
  bmp.eraseColor(mortarArgb)

  // Helper to safely set a pixel
  fun px(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) bmp.setPixel(x, y, c) }

  val rows = h / baseBrickH + 2
  for (row in 0 until rows) {
    val rowPlan = brickRowPlan(row = row, hour = hour, theme = theme)
    val brickH = rowPlan.courseHeightPx
    val gap = rowPlan.mortarThicknessPx
    var bx = -BRICK_WIDTH_PX + rowPlan.offsetPx
    var segmentIndex = 0
    while (bx < w + BRICK_WIDTH_PX) {
      val brickW = rowPlan.segmentWidths[segmentIndex % rowPlan.segmentWidths.size]
      val by = row * brickH
      val hash = brickPatternHash(row = row, col = segmentIndex - 2, hour = hour, salt = rowPlan.offsetPx + brickW)
      val hash2 = brickPatternHash(row = row, col = segmentIndex - 2, hour = hour, salt = 91 + rowPlan.offsetPx + brickW)
      val innerW = (brickW - gap * 2).coerceAtLeast(6)
      val innerH = (brickH - gap * 2).coerceAtLeast(4)

      // ── Brick body with subtle per-brick color variation ──
      val variation = ((hash % 7) - 3) * 2  // -6..+6 brightness shift
      val bodyArgb = shiftBrightness(wallArgb, variation)
      val role = resolveBrickRole(hash = hash, hash2 = hash2, theme = theme)
      for (py in (by + gap) until (by + brickH - gap)) {
        if (py !in 0 until h) continue
        for (ppx in (bx + gap) until (bx + brickW - gap)) {
          if (ppx in 0 until w) bmp.setPixel(ppx, py, bodyArgb)
        }
      }

      // ── Highlight top + left edge ──
      val topY = by + gap; val leftX = bx + gap
      for (depth in 0 until rowPlan.highlightDepthPx) {
        val highlightY = topY + depth
        val highlightX = leftX + depth
        if (highlightY in 0 until h) for (ppx in (bx + gap + depth) until (bx + brickW - gap - depth)) { if (ppx in 0 until w) bmp.setPixel(ppx, highlightY, lightArgb) }
        if (highlightX in 0 until w) for (py in (by + gap + depth) until (by + brickH - gap - depth)) { if (py in 0 until h) bmp.setPixel(highlightX, py, lightArgb) }
      }

      // ── Shadow bottom + right edge ──
      val botY = by + brickH - gap - 1; val rightX = bx + brickW - gap - 1
      for (depth in 0 until rowPlan.shadowDepthPx) {
        val shadowY = botY - depth
        val shadowX = rightX - depth
        val shadowColor = shiftBrightness(darkArgb, -depth * 6)
        if (shadowY in 0 until h) for (ppx in (bx + gap + depth) until (bx + brickW - gap - depth)) { if (ppx in 0 until w) bmp.setPixel(ppx, shadowY, shadowColor) }
        if (shadowX in 0 until w) for (py in (by + gap + depth) until (by + brickH - gap - depth)) { if (py in 0 until h) bmp.setPixel(shadowX, py, shadowColor) }
      }

      // ── Inner surface texture: 50% common alt, 5% rare alt (like MLPD tile variance) ──
      if (hash >= 128) {
        // Common variant: subtle noise dots on brick surface
        val noiseCount = 2 + hash % 3
        for (n in 0 until noiseCount) {
          val nx = bx + gap + 1 + ((hash * (n + 1) * 7) % (innerW - 2).coerceAtLeast(1))
          val ny = by + gap + 1 + ((hash2 * (n + 1) * 5) % (innerH - 2).coerceAtLeast(1))
          px(nx, ny, shiftBrightness(bodyArgb, if (n % 2 == 0) -8 else 6))
        }
      }
      if (hash >= 243) {
        // Rare variant (5%): diagonal scratch mark across brick
        val sx = bx + gap + 2; val sy = by + gap + 1
        for (i in 0 until minOf(3, innerW - 3, innerH - 2)) {
          px(sx + i, sy + i, darkArgb)
        }
      }

      // ━━━━ Zone-specific decorations ━━━━
      paintBrickRole(
        role = role,
        bx = bx,
        by = by,
        brickW = brickW,
        brickH = brickH,
        gap = gap,
        innerW = innerW,
        innerH = innerH,
        bodyArgb = bodyArgb,
        lightArgb = lightArgb,
        darkArgb = darkArgb,
        mortarArgb = mortarArgb,
        palette = palette,
        hash = hash,
        hash2 = hash2,
        px = ::px
      )

      paintChapterGeometry(
        theme = theme,
        bx = bx,
        by = by,
        brickW = brickW,
        brickH = brickH,
        gap = gap,
        innerW = innerW,
        innerH = innerH,
        bodyArgb = bodyArgb,
        lightArgb = lightArgb,
        darkArgb = darkArgb,
        mortarArgb = mortarArgb,
        palette = palette,
        hash = hash,
        hash2 = hash2,
        px = ::px
      )

      for (deco in decos) {
        when (deco) {
          WallDeco.MOSS -> {
            // Mossy patches on ~25% of bricks, more at bottom
            val mossChance = if (by > h / 2) 4 else 6
            if (hash % mossChance == 0) {
              val mossColor = blendColors(palette.color1.toArgb(), 0xFF2D5A1E.toInt(), 0.5f)
              // Cluster of 3-6 moss pixels along bottom/corner of brick
              val mossCount = 3 + hash2 % 4
              for (m in 0 until mossCount) {
                val mx = bx + gap + 1 + (hash2 * (m + 1) * 3) % (innerW - 2).coerceAtLeast(1)
                val my = by + brickH - gap - 2 - (m % 2)
                px(mx, my, mossColor)
                if (m % 3 == 0) px(mx, my - 1, shiftBrightness(mossColor, 15)) // lighter moss tip
              }
            }
            // Drip stain from mortar gaps (~10%)
            if (hash % 10 == 1) {
              val dripX = bx + brickW / 2 + (hash2 % 5 - 2)
              val dripColor = blendColors(palette.color2.toArgb(), palette.mortar.toArgb(), 0.4f)
              for (dy in 0..2) px(dripX, by + gap + 1 + dy, dripColor)
            }
          }

          WallDeco.VINES -> {
            // Hanging vine tendrils from top of brick (~20%)
            if (hash % 5 == 0) {
              val vineColor = 0xFF3A7A2A.toInt()
              val vineX = bx + gap + 2 + hash2 % (innerW - 4).coerceAtLeast(1)
              val vineLen = 3 + hash % 4
              for (vy in 0 until vineLen) {
                px(vineX + (vy % 2), by + gap + vy, vineColor)
              }
              // Leaf at end
              px(vineX - 1, by + gap + vineLen, shiftBrightness(vineColor, 20))
              px(vineX + 1, by + gap + vineLen, shiftBrightness(vineColor, 10))
            }
          }

          WallDeco.TORCH_SCORCH -> {
            // Burn marks radiating from center-top of some bricks (~15%)
            if (hash % 7 == 0) {
              val scorchColor = shiftBrightness(darkArgb, -10)
              val cx = bx + brickW / 2
              val cy = by + gap + 1
              px(cx, cy, scorchColor); px(cx - 1, cy, scorchColor); px(cx + 1, cy, scorchColor)
              px(cx, cy + 1, scorchColor); px(cx - 1, cy + 1, shiftBrightness(scorchColor, 5))
              px(cx + 1, cy + 1, shiftBrightness(scorchColor, 5))
            }
            // Chain bolt rivet (~8%)
            if (hash2 % 12 == 0) {
              val rivetColor = 0xFF8A8A8A.toInt()
              val rx = bx + gap + 3 + hash % (innerW - 6).coerceAtLeast(1)
              val ry = by + gap + 2 + hash2 % (innerH - 4).coerceAtLeast(1)
              px(rx, ry, rivetColor); px(rx + 1, ry, 0xFF6A6A6A.toInt())
              px(rx, ry + 1, 0xFF6A6A6A.toInt()); px(rx + 1, ry + 1, 0xFF555555.toInt())
            }
          }

          WallDeco.CRACKS -> {
            // Fracture lines across brick (~20%)
            if (hash % 5 == 0) {
              val crackColor = shiftBrightness(darkArgb, -5)
              val sx = bx + gap + 1 + hash % (innerW / 2).coerceAtLeast(1)
              val sy = by + gap + 1 + hash2 % (innerH - 2).coerceAtLeast(1)
              val crackLen = 3 + hash % 3
              for (i in 0 until crackLen) {
                val dir = (hash2 + i) % 3 // 0=right, 1=down-right, 2=down
                when (dir) {
                  0 -> px(sx + i, sy, crackColor)
                  1 -> px(sx + i, sy + i / 2, crackColor)
                  2 -> px(sx + i / 2, sy + i, crackColor)
                }
              }
            }
            // Ore vein sparkle (~5%)
            if (hash % 20 == 0) {
              val oreColor = blendColors(palette.color2.toArgb(), 0xFFFFDD44.toInt(), 0.3f)
              val ox = bx + gap + 2 + hash2 % (innerW - 4).coerceAtLeast(1)
              val oy = by + gap + 2 + hash % (innerH - 4).coerceAtLeast(1)
              px(ox, oy, oreColor); px(ox + 1, oy, shiftBrightness(oreColor, -10))
            }
          }

          WallDeco.CRYSTAL -> {
            // Crystal growths jutting from brick (~12%)
            if (hash % 8 == 0) {
              val crystalBase = blendColors(palette.color1.toArgb(), 0xFF66B3FF.toInt(), 0.6f)
              val crystalTip = blendColors(crystalBase, 0xFFAADDFF.toInt(), 0.5f)
              val cx = bx + gap + 3 + hash2 % (innerW - 6).coerceAtLeast(1)
              val cy = by + brickH - gap - 2
              // Small triangular crystal pointing up
              px(cx, cy, crystalBase); px(cx, cy - 1, crystalBase); px(cx, cy - 2, crystalTip)
              px(cx - 1, cy, shiftBrightness(crystalBase, -10))
              px(cx + 1, cy, shiftBrightness(crystalBase, 10))
            }
          }

          WallDeco.ENGRAVINGS -> {
            // Carved horizontal line / rune on brick (~18%)
            if (hash % 6 == 0) {
              val engraveColor = shiftBrightness(darkArgb, 8)
              val ey = by + gap + innerH / 2
              val eStart = bx + gap + 3; val eEnd = bx + brickW - gap - 3
              for (ex in eStart..eEnd step 2) px(ex, ey, engraveColor)
            }
            // Metal rivet at corners (~10%)
            if (hash2 % 10 == 0) {
              val rivetColor = 0xFFB0BEC5.toInt()
              px(bx + gap + 1, by + gap + 1, rivetColor)
              px(bx + brickW - gap - 2, by + brickH - gap - 2, shiftBrightness(rivetColor, -15))
            }
          }

          WallDeco.ARCANE -> {
            // Glowing glyph pixel cluster (~10%)
            if (hash % 10 == 0) {
              val glyphColor = blendColors(palette.color1.toArgb(), 0xFFCC33FF.toInt(), 0.4f)
              val gx = bx + brickW / 2 - 1; val gy = by + brickH / 2 - 1
              // Small cross glyph
              px(gx + 1, gy, glyphColor)
              px(gx, gy + 1, glyphColor); px(gx + 1, gy + 1, shiftBrightness(glyphColor, 20)); px(gx + 2, gy + 1, glyphColor)
              px(gx + 1, gy + 2, glyphColor)
            }
          }

          WallDeco.BONES -> {
            // Bone fragment on brick (~15%)
            if (hash % 7 == 0) {
              val boneColor = 0xFFD4C9A8.toInt()
              val boneShadow = 0xFFA09880.toInt()
              val boneX = bx + gap + 2 + hash2 % (innerW - 5).coerceAtLeast(1)
              val boneY = by + brickH - gap - 3
              px(boneX, boneY, boneColor); px(boneX + 1, boneY, boneColor); px(boneX + 2, boneY, boneColor)
              px(boneX + 1, boneY + 1, boneShadow)
            }
            // Blood drip (~10%)
            if (hash2 % 10 == 0) {
              val bloodColor = 0xFF880000.toInt()
              val bx2 = bx + gap + 4 + hash % (innerW - 6).coerceAtLeast(1)
              for (dy in 0..2) px(bx2, by + gap + 1 + dy, shiftBrightness(bloodColor, dy * 8))
            }
          }

          WallDeco.LAVA_DRIP -> {
            // Lava seam in mortar (~20% of mortar gaps)
            if (hash % 5 == 0) {
              val lavaColor = 0xFFFF4400.toInt()
              val lavaGlow = 0xFFFF8800.toInt()
              // Horizontal lava seam along bottom mortar gap
              val seamY = by + brickH - 1
              val seamStart = bx + gap; val seamEnd = bx + brickW - gap
              if (seamY in 0 until h) {
                for (sx in seamStart until seamEnd step 3) {
                  px(sx, seamY, if ((sx + hash) % 2 == 0) lavaColor else lavaGlow)
                }
              }
            }
            // Heat shimmer dot on brick (~8%)
            if (hash2 % 12 == 0) {
              val heatColor = blendColors(palette.color1.toArgb(), 0xFFFF6600.toInt(), 0.3f)
              px(bx + brickW / 2, by + gap + 2, heatColor)
            }
          }

          WallDeco.FROST -> {
            // Ice crystal pattern on brick (~20%)
            if (hash % 5 == 0) {
              val iceColor = 0xFF8AD8D8.toInt()
              val iceBright = 0xFFAAFFFF.toInt()
              val ix = bx + gap + 2 + hash2 % (innerW - 4).coerceAtLeast(1)
              val iy = by + gap + 1 + hash % (innerH - 3).coerceAtLeast(1)
              // Small ice star
              px(ix, iy - 1, iceBright)
              px(ix - 1, iy, iceColor); px(ix, iy, iceBright); px(ix + 1, iy, iceColor)
              px(ix, iy + 1, iceColor)
            }
            // Frozen mortar (~15%)
            if (hash % 7 == 0) {
              val frozenMortar = blendColors(mortarArgb, 0xFF66DDDD.toInt(), 0.3f)
              val fy = by + brickH - 1
              for (fx in bx + gap until bx + brickW - gap step 2) px(fx, fy, frozenMortar)
            }
          }

          WallDeco.CORRUPTION -> {
            // Slime / corrosion patches (~20%)
            if (hash % 5 == 0) {
              val slimeColor = blendColors(palette.color2.toArgb(), 0xFF556B2F.toInt(), 0.4f)
              val cx = bx + gap + 1 + hash2 % (innerW - 3).coerceAtLeast(1)
              val cy = by + brickH - gap - 2
              px(cx, cy, slimeColor); px(cx + 1, cy, slimeColor)
              px(cx, cy + 1, shiftBrightness(slimeColor, -10)); px(cx + 1, cy + 1, shiftBrightness(slimeColor, 5))
              if (hash % 3 == 0) px(cx + 2, cy, shiftBrightness(slimeColor, 10))
            }
          }

          WallDeco.HOLY -> {
            // Light cracks with golden glow (~12%)
            if (hash % 8 == 0) {
              val lightColor = 0xFFFFEEAA.toInt()
              val goldColor = 0xFFFFDD44.toInt()
              val lx = bx + gap + 2 + hash2 % (innerW - 4).coerceAtLeast(1)
              val ly = by + gap + 2
              px(lx, ly, goldColor); px(lx + 1, ly + 1, lightColor); px(lx + 2, ly + 2, goldColor)
              px(lx + 1, ly, shiftBrightness(lightColor, 10))
            }
          }

          WallDeco.SHADOW -> {
            // Void patches — darker than dark (~15%)
            if (hash % 7 == 0) {
              val voidColor = 0xFF0A0610.toInt()
              val voidEdge = blendColors(bodyArgb, voidColor, 0.5f)
              val vx = bx + gap + 2 + hash2 % (innerW - 5).coerceAtLeast(1)
              val vy = by + gap + 2 + hash % (innerH - 4).coerceAtLeast(1)
              px(vx, vy, voidColor); px(vx + 1, vy, voidColor)
              px(vx, vy + 1, voidColor); px(vx + 1, vy + 1, voidEdge)
              px(vx + 2, vy, voidEdge); px(vx - 1, vy + 1, voidEdge)
            }
          }

          WallDeco.RUSTED -> {
            // Rust streaks running down brick (~22%)
            if (hash % 5 == 0) {
              val rustColor = blendColors(0xFFAA5500.toInt(), palette.color2.toArgb(), 0.3f)
              val rx = bx + gap + 2 + hash2 % (innerW - 4).coerceAtLeast(1)
              val rustLen = 2 + hash % 3
              for (ry in 0 until rustLen) {
                px(rx + (ry % 2), by + gap + 1 + ry, shiftBrightness(rustColor, ry * 5))
              }
            }
            // Oxidation patch (~10%)
            if (hash2 % 10 == 0) {
              val oxColor = 0xFF668866.toInt()
              val ox = bx + brickW / 2; val oy = by + brickH / 2
              px(ox, oy, oxColor); px(ox + 1, oy, shiftBrightness(oxColor, 10))
            }
          }

          WallDeco.GOLD_TRIM -> {
            // Golden inlay line along brick center (~15%)
            if (hash % 7 == 0) {
              val goldColor = 0xFFDDAA22.toInt()
              val goldBright = 0xFFFFDD44.toInt()
              val gy = by + brickH / 2
              for (gx in (bx + gap + 2) until (bx + brickW - gap - 2) step 2) {
                px(gx, gy, if ((gx + hash) % 3 == 0) goldBright else goldColor)
              }
            }
            // Tiny amulet symbol (~5%)
            if (hash % 20 == 0) {
              val symColor = 0xFFFFCC44.toInt()
              val sx = bx + brickW / 2; val sy = by + brickH / 2 - 1
              px(sx, sy, symColor); px(sx - 1, sy + 1, symColor); px(sx + 1, sy + 1, symColor)
              px(sx, sy + 2, symColor)
            }
          }
        }
      }
      bx += brickW
      segmentIndex++
    }
  }
  return bmp.asImageBitmap()
}

/** Shift RGB brightness by delta (-255..+255), clamping channels */
private fun shiftBrightness(argb: Int, delta: Int): Int {
  val a = (argb ushr 24) and 0xFF
  val r = (((argb ushr 16) and 0xFF) + delta).coerceIn(0, 255)
  val g = (((argb ushr 8) and 0xFF) + delta).coerceIn(0, 255)
  val b = ((argb and 0xFF) + delta).coerceIn(0, 255)
  return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Blend two ARGB colors by ratio (0.0 = colorA, 1.0 = colorB) */
private fun blendColors(colorA: Int, colorB: Int, ratio: Float): Int {
  val inv = 1f - ratio
  val a = (((colorA ushr 24) and 0xFF) * inv + ((colorB ushr 24) and 0xFF) * ratio).toInt()
  val r = (((colorA ushr 16) and 0xFF) * inv + ((colorB ushr 16) and 0xFF) * ratio).toInt()
  val g = (((colorA ushr 8) and 0xFF) * inv + ((colorB ushr 8) and 0xFF) * ratio).toInt()
  val b = ((colorA and 0xFF) * inv + (colorB and 0xFF) * ratio).toInt()
  return (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
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
  val hour = (tick / 60).coerceIn(0, 23)
  val spec = brickTextureSpec(widthPx, heightPx)

  // Global cache — bitmap survives navigation, only regenerates on hour change
  val key = BrickCacheKey(hour = hour, layoutSeed = BRICK_LAYOUT_SEED)
  val brickBitmap = if (cachedBrickKey == key && cachedBrickBitmap != null) {
    cachedBrickBitmap!!
  } else {
    generateBrickBitmap(spec.textureWidthPx, spec.textureHeightPx, currentZonePalette(), hour).also {
      cachedBrickBitmap = it
      cachedBrickKey = key
    }
  }

  Canvas(modifier.fillMaxSize()) {
    val textureWidth = brickBitmap.width
    val textureHeight = brickBitmap.height
    var y = 0
    while (y < size.height.toInt()) {
      var x = 0
      while (x < size.width.toInt()) {
        drawImage(brickBitmap, topLeft = Offset(x.toFloat(), y.toFloat()))
        x += textureWidth
      }
      y += textureHeight
    }
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
  val tick = rememberTimeTick()
  val hour = (tick / 60).coerceIn(0, 23)

  // Zone-specific flame color palettes — sourced from MLPD particle/sprite colors
  data class FlameColors(val base: Color, val mid: Color, val bright: Color, val core: Color, val tip: Color, val glow: Color)
  val flames = when (hour) {
    // Demon Halls hours — hellfire red/orange
    0, 16 -> FlameColors(Color(0xFFAA0000), Color(0xFFDD2200), Color(0xFFFF4400), Color(0xFFFF8844), Color(0xFFFFBB88), Color(0xFFFF2200))
    // Burning Fist — intense yellow-orange fire
    1 -> FlameColors(Color(0xFFCC6600), Color(0xFFEE8800), Color(0xFFFFAA00), Color(0xFFFFDD34), Color(0xFFFFEE88), Color(0xFFFFBB00))
    // Rotting Fist — sickly yellow-green
    2 -> FlameColors(Color(0xFF665500), Color(0xFF887722), Color(0xFFAAAA44), Color(0xFFCCCC66), Color(0xFFDDDD88), Color(0xFF999944))
    // Ice Fist — cyan-teal frost
    3 -> FlameColors(Color(0xFF006666), Color(0xFF008888), Color(0xFF26CCC2), Color(0xFF66EEEE), Color(0xFFAAFFFF), Color(0xFF34C9C9))
    // Sewers — toxic green
    4, 5 -> FlameColors(Color(0xFF005500), Color(0xFF008833), Color(0xFF22BB55), Color(0xFF66FF88), Color(0xFFAAFFCC), Color(0xFF33FF66))
    // Garden — warm green-yellow
    6 -> FlameColors(Color(0xFF446600), Color(0xFF668800), Color(0xFF88CC44), Color(0xFFAAEE66), Color(0xFFCCFF88), Color(0xFF88CC44))
    // Prison — lantern warm yellow
    7, 8 -> FlameColors(Color(0xFF886600), Color(0xFFBB8800), Color(0xFFDDAA22), Color(0xFFFFDD44), Color(0xFFFFEE88), Color(0xFFFFDD33))
    // Tengu — combat red-orange
    9 -> FlameColors(Color(0xFF990000), Color(0xFFCC2222), Color(0xFFEE4444), Color(0xFFFF6666), Color(0xFFFF9999), Color(0xFFEE3333))
    // Caves — standard mining torch orange
    10, 11 -> FlameColors(Color(0xFFCC4400), Color(0xFFEE6600), Color(0xFFFF8800), Color(0xFFFFAA00), Color(0xFFFFDD66), Color(0xFFFF9900))
    // Crystal Caves — blue crystal glow
    12 -> FlameColors(Color(0xFF224488), Color(0xFF3366AA), Color(0xFF5588CC), Color(0xFF77AAEE), Color(0xFFAADDFF), Color(0xFF6699DD))
    // Dwarf City — magical blue-white
    13, 14 -> FlameColors(Color(0xFF6666AA), Color(0xFF8888CC), Color(0xFFAAAAEE), Color(0xFFCCCCFF), Color(0xFFEEEEFF), Color(0xFFCCCCFF))
    // Dwarf King — arcane purple
    15 -> FlameColors(Color(0xFF660088), Color(0xFF8822AA), Color(0xFFAA44CC), Color(0xFFCC66EE), Color(0xFFEE99FF), Color(0xFFCC33FF))
    // Evil Eyes — deep purple-pink
    17 -> FlameColors(Color(0xFF550066), Color(0xFF7722AA), Color(0xFF9944CC), Color(0xFFBB66EE), Color(0xFFDD88FF), Color(0xFF9933CC))
    // Scorpio Nest — poison green-purple
    18 -> FlameColors(Color(0xFF004400), Color(0xFF006600), Color(0xFF00BB00), Color(0xFF44FF44), Color(0xFF88FF88), Color(0xFF00FF00))
    // Yog-Dzewa — demonic deep red
    19 -> FlameColors(Color(0xFF880000), Color(0xFFBB0000), Color(0xFFEE0000), Color(0xFFFF4444), Color(0xFFFF8888), Color(0xFFFF0000))
    // Bright Fist — holy white-gold
    20 -> FlameColors(Color(0xFFAA9944), Color(0xFFCCBB66), Color(0xFFEEDD88), Color(0xFFFFEEAA), Color(0xFFFFFFDD), Color(0xFFFFFFAA))
    // Dark Fist — shadow purple
    21 -> FlameColors(Color(0xFF2A1533), Color(0xFF3D2050), Color(0xFF502B66), Color(0xFF6A3D88), Color(0xFF8855AA), Color(0xFF5533AA))
    // Rusted Fist — gray-orange corrosion
    22 -> FlameColors(Color(0xFF885500), Color(0xFFAA7722), Color(0xFFCC9944), Color(0xFFDDBB66), Color(0xFFEEDD88), Color(0xFFBB8833))
    // Last Level — gold-red final
    23 -> FlameColors(Color(0xFFCC6600), Color(0xFFDD8800), Color(0xFFEEAA22), Color(0xFFFFCC44), Color(0xFFFFEE88), Color(0xFFEEAA00))
    else -> FlameColors(Color(0xFFCC0000), Color(0xFFFF6600), Color(0xFFFF8800), Color(0xFFFFFF44), Color(0xFFFFFFCC), Color(0xFFFF6600))
  }

  Canvas(modifier = modifier) {
    val px = size.width / 8f
    val cx = size.width / 2f

    // ── Torch handle ──
    val handleTop = size.height * 0.65f
    val handleShift = px * 0.5f
    drawRect(Color(0xFF8B4513), Offset(cx - px + handleShift, handleTop), Size(px * 2, size.height - handleTop))
    drawRect(Color(0xFF5D3A0E), Offset(cx - px + handleShift, size.height - px), Size(px * 2, px))
    drawRect(Color(0xFF3D2E18), Offset(cx - px * 1.5f + handleShift, handleTop), Size(px * 3, px))

    // ── Ember glow ──
    val flameBase = handleTop - px
    drawCircle(
      brush = Brush.radialGradient(
        listOf(flames.glow.copy(alpha = 0.35f), flames.glow.copy(alpha = 0.12f), Color.Transparent),
        center = Offset(cx, flameBase), radius = size.width * 1.0f
      ), center = Offset(cx, flameBase), radius = size.width * 1.0f
    )

    // ── Pixelated flame particles with zone colors ──
    data class FlamePixel(val relX: Float, val relY: Float, val color: Color, val phase: Float)

    val f = flames
    val particles = listOf(
      // Base layer — wide
      FlamePixel(-2f, 0f, f.base, 0f), FlamePixel(-1f, 0f, f.mid, 0.5f),
      FlamePixel(0f, 0f, f.mid, 1f), FlamePixel(1f, 0f, f.mid, 1.5f), FlamePixel(2f, 0f, f.base, 2f),
      // Mid layer
      FlamePixel(-1.5f, -1f, f.mid, 0.3f), FlamePixel(-0.5f, -1f, f.bright, 0.8f),
      FlamePixel(0.5f, -1f, f.bright, 1.3f), FlamePixel(1.5f, -1f, f.mid, 1.8f),
      // Upper
      FlamePixel(-1f, -2f, f.bright, 0.4f), FlamePixel(0f, -2f, f.core, 1.0f), FlamePixel(1f, -2f, f.bright, 1.6f),
      // Core
      FlamePixel(-0.5f, -3f, f.core, 0.6f), FlamePixel(0.5f, -3f, f.core, 1.2f),
      // Tip
      FlamePixel(0f, -4f, f.tip, 0.9f), FlamePixel(0f, -5f, f.tip, 1.5f),
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
  val hour = (tick / 60).coerceIn(0, 23)
  val palette = currentZonePalette()

  // Torch glow color — zone's signature glow, sourced from MLPD lighting
  val torchColor = when (hour) {
    0, 16    -> Color(0xFFFF2200)  // Halls — hellfire red
    1        -> Color(0xFFFFBB00)  // Burning — intense yellow
    2        -> Color(0xFF999944)  // Rotting — sickly yellow-green
    3        -> Color(0xFF34C9C9)  // Ice — cyan-teal
    4, 5     -> Color(0xFF33FF66)  // Sewers — toxic green
    6        -> Color(0xFF88CC44)  // Garden — leaf green
    7, 8     -> Color(0xFFFFDD33)  // Prison — lantern yellow
    9        -> Color(0xFFEE3333)  // Tengu — combat red
    10, 11   -> Color(0xFFFF9900)  // Caves — mining torch orange
    12       -> Color(0xFF6699DD)  // Crystal — blue glow
    13, 14   -> Color(0xFFEEEEFF)  // City — magical white-blue
    15       -> Color(0xFFCC33FF)  // Dwarf King — arcane purple
    17       -> Color(0xFF9933CC)  // Evil Eyes — deep purple
    18       -> Color(0xFF00FF00)  // Scorpio — poison green
    19       -> Color(0xFFFF0000)  // Yog — demonic red
    20       -> Color(0xFFFFFFAA)  // Bright — holy white
    21       -> Color(0xFF5533AA)  // Dark — shadow purple
    22       -> Color(0xFFBB8833)  // Rusted — corrosion orange
    23       -> Color(0xFFEEAA00)  // Last — gold
    else     -> Color(0xFFFF8800)
  }

  // Secondary glow — magic pool at bottom, color varies by chapter
  val poolColor = when (hour) {
    in 0..3, in 19..23 -> Color(0xFFFF4400).copy(alpha = flicker * 0.06f)   // Halls — lava pool
    in 4..6            -> Color(0xFF00FF88).copy(alpha = flicker * 0.08f)    // Sewers — green sewer water
    in 7..9            -> Color(0xFFFFDD33).copy(alpha = flicker * 0.06f)    // Prison — candlelight
    in 10..12          -> Color(0xFF66B3FF).copy(alpha = flicker * 0.10f)    // Caves — crystal reflection
    in 13..15          -> Color(0xFF00FFFF).copy(alpha = flicker * 0.08f)    // City — MLPD cyan magic
    in 16..18          -> Color(0xFFFF0044).copy(alpha = flicker * 0.07f)    // Halls — blood pool
    else               -> Color(0xFF00FFFF).copy(alpha = flicker * 0.08f)
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
      // Bottom center — zone-specific magic pool glow
      drawCircle(
        brush = Brush.radialGradient(
          listOf(poolColor, Color.Transparent),
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
