# Pixel Dungeon Wall Templates Design

**Goal:** Rework the Pixel Dungeon brick background into a rule-driven chapter template system that keeps the current fast full-theme startup, adds denser Pixel Dungeon style wall detail, and changes brick structure and decoration by time-of-day chapter.

**Reference Style:** Borrow the visual language of Magic Ling Pixel Dungeon's dungeon art and UI framing without copying source assets directly: <https://github.com/LingASDJ/Magic_Ling_Pixel_Dungeon>

**Constraints:**
- Keep direct full-theme rendering on startup. No lightweight placeholder phase.
- Keep the startup optimization that uses a bounded repeatable texture instead of a full-screen bitmap.
- Continue using programmatic pixel drawing only. Do not import external art assets.
- Preserve startup-stable randomness within one app process while changing layout on each cold start.

**Architecture:** Replace the current flat `WallDeco` driven surface decoration model with a chapter template pipeline. The renderer will first resolve the current hour into a base chapter template plus an hour-specific modifier, then use those rules to generate a medium-sized tiled bitmap whose brick segmentation, damage, and detail density are chapter-specific. Existing color palettes remain the source of per-hour tone, while structure and feature placement shift to template-defined rules.

**Components:**
- `ChapterWallTemplate`
  Defines a dungeon chapter's wall structure rules: brick length distribution, row staggering, mortar thickness bias, edge treatment, damage frequency, moisture level, special brick variants, and decoration density.
- `HourWallVariant`
  Applies hour-specific overrides on top of a base chapter template, such as stronger lava seams for Burning Fist, extra corrosion for Goo, or denser glyphs for Dwarf King.
- `HourToWallThemeResolver`
  Maps each of the 24 hour palettes to one base chapter template and one optional variant override.
- `BrickPatternRenderer`
  Generates the tiled bitmap using shared rendering stages driven by the resolved wall theme.

**Chapter Templates:**
- `Sewers`
  Damp, uneven walls with softened edges, bottom-heavy moss, stains, seepage, corrosion, and darker wet bricks.
- `Prison`
  More regular masonry with crisp mortar, cold edge highlights, rivet marks, chain anchors, soot, and battle scarring.
- `Caves`
  Chunkier stonework with irregular block sizing, cracks, chipped corners, mineral traces, and rough shading.
- `CrystalCaves`
  Extends cave stonework with crystal growths, cool reflection accents, and frost-like light splitting.
- `City`
  Ordered cut stone with engravings, metal fasteners, runic slots, cleaner geometry, and occasional arcane/mechanical signature bricks.
- `Halls`
  Heavy contrast demonic masonry with deep shadow seams, bone fragments, scorch marks, blood traces, heat cracks, and molten damage.

**Hour Mapping Strategy:**
- `00,16` -> `Halls`
- `01` -> `Halls` + burning modifier
- `02` -> `Sewers/Halls` decay-heavy modifier
- `03` -> `CrystalCaves` + frost modifier
- `04,05,06` -> `Sewers` with Goo/Garden specific overrides
- `07,08,09` -> `Prison` with Tengu battle damage override
- `10,11` -> `Caves` with Deep Mines ore density override
- `12` -> `CrystalCaves`
- `13,14,15` -> `City` with Dwarf King arcane density override
- `17` -> `Halls/City` dark arcane hybrid override
- `18` -> corruption-heavy poisonous variant
- `19` -> extreme `Halls`
- `20` -> `City/Halls` holy bright override
- `21` -> `Halls` shadow override
- `22` -> `Caves/Halls` rust override
- `23` -> endgame `Halls + City` mixed template

**Rendering Stages:**
1. Resolve `hour -> palette + chapter template + hour modifier`.
2. Build a per-row brick segmentation plan instead of repeating fixed-width bricks everywhere.
3. For each brick, derive a brick role such as normal, chipped, wet, engraved, scorched, ore-veined, rune, or corrupted.
4. Paint base mass, edge lighting, mortar, and shape damage first.
5. Paint chapter-driven clustered details second so dirt, moss, scorch, runes, crystals, or lava appear in chapter-appropriate regions rather than uniform scatter.
6. Cache the generated bitmap by effective wall theme key so it is only built once per hour per process seed.

**Detail Language Changes:**
- Brick widths and seam offsets vary by template instead of using one universal grid.
- Bricks can have chipped corners, cut edges, cracked faces, damp lower halves, or engraved center bands.
- Decorations are clustered by chapter rules.
  For example: sewer wetness pools low, prison scorch marks bias high, cave fractures travel diagonally, city carvings stay centered, halls lava traces cling to seams.
- Hour modifiers increase or suppress feature density rather than only changing color.

**Performance Plan:**
- Keep the bounded tiled bitmap approach.
- Share one renderer pipeline across all chapters and swap only rule tables.
- Limit chapter complexity to small integer rules and pixel operations; no image decoding or large per-pixel post-processing.
- Continue using the startup-level random seed so one process is visually stable and a new process gets a fresh arrangement.

**Testing:**
- Add unit tests for hour-to-template resolution.
- Add tests for stable output with the same seed and changed output with a different seed.
- Add tests that template rules differ in structural outputs such as row offset or brick segmentation.
- Keep existing texture size bounds covered.
- Validate with `:app:testDebugUnitTest` and `:app:assembleDebug` after implementation.
