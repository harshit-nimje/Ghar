package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * High-presence, studio-warm Indian household acoustic foley engine for Ghar.
 *
 * Synthesizes 5 multi-layered 44.1 kHz 16-bit PCM `.wav` assets with physical modal synthesis,
 * 3-tap courtyard/study early reflections, soft-knee warmth, and `-0.7 dBFS` peak normalization
 * into `cacheDir/ghar_sfx_v4/`, then plays them at zero latency via [SoundPool] (`USAGE_GAME`
 * so sounds track media volume cleanly with strong, full-bodied presence without stealing focus):
 *
 * 1. **Rasoi Door Kundi & Teak Swing (`playDoorKundiOpen`)** — Brass staple lift, solid teak bolt
 *    *clack*, and seasoned wooden door swing with courtyard resonance.
 * 2. **Earthen Gullak Coin Clink (`playGullakCoinClink`)** — Heavy bi-metallic coin strike on
 *    fired terracotta clay rim, deep Helmholtz pot cavity thump, and cascading coin-bed settle.
 * 3. **Crisp Rupee Note & Batua Rustle (`playCrispNoteRustle`)** — Tactile cotton-linen bank-note
 *    snap and stitched leather Batua fold when logging expenses or income.
 * 4. **Khadi Chitthi Paper Pin (`playPaperChitthiPin`)** — Handmade paper flick and soft wooden
 *    board tap when stashing a Chitthi or pinning a Habit Sankalp.
 * 5. **Warm Chai Cup Settle (`playChaiCupSettle`)** — Ceramic chai cup settling onto saucer &
 *    wooden table when sitting down with Maa or Papa.
 */
object GharSoundEffects {
  internal const val SAMPLE_RATE = 44_100
  private const val CACHE_SUBDIR = "ghar_sfx_v4"
  private val LEGACY_CACHE_SUBDIRS = arrayOf("ghar_sfx_v1", "ghar_sfx_v2", "ghar_sfx_v3")

  private const val DOOR_KUNDI_FILENAME = "rasoi_door_kundi_open_v4.wav"
  private const val GULLAK_CLINK_FILENAME = "gullak_coin_clink_v4.wav"
  private const val CRISP_NOTE_FILENAME = "batua_crisp_note_v4.wav"
  private const val CHITTHI_PIN_FILENAME = "khadi_chitthi_pin_v4.wav"
  private const val CHAI_CUP_FILENAME = "chai_cup_settle_v4.wav"

  private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val initStarted = AtomicBoolean(false)

  @Volatile
  private var soundPool: SoundPool? = null

  @Volatile private var doorKundiSoundId: Int = 0
  @Volatile private var gullakClinkSoundId: Int = 0
  @Volatile private var crispNoteSoundId: Int = 0
  @Volatile private var chitthiPinSoundId: Int = 0
  @Volatile private var chaiCupSoundId: Int = 0

  @Volatile private var doorKundiLoaded: Boolean = false
  @Volatile private var gullakClinkLoaded: Boolean = false
  @Volatile private var crispNoteLoaded: Boolean = false
  @Volatile private var chitthiPinLoaded: Boolean = false
  @Volatile private var chaiCupLoaded: Boolean = false

  @Volatile private var pendingDoorPlay: Boolean = false
  @Volatile private var pendingCoinPlay: Boolean = false
  @Volatile private var pendingNotePlay: Boolean = false
  @Volatile private var pendingChitthiPlay: Boolean = false
  @Volatile private var pendingChaiPlay: Boolean = false

  /**
   * Pre-synthesizes all WAV assets on background IO and stages them into [SoundPool]
   * so the very first interaction plays instantaneously at full fidelity.
   */
  fun prewarm(context: Context) {
    if (!initStarted.compareAndSet(false, true)) return
    val appCtx = context.applicationContext
    ioScope.launch {
      ensureInitializedSync(appCtx)
    }
  }

  /**
   * Plays the rich brass *kundi* unlatching, solid teak bolt clack, and warm wooden door swing
   * when stepping into the Rasoi.
   */
  fun playDoorKundiOpen(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    runCatching {
      val pool = soundPool
      if (pool != null && doorKundiLoaded && doorKundiSoundId != 0) {
        pool.play(doorKundiSoundId, 0.32f, 0.34f, 2, 0, 1.0f)
      } else {
        pendingDoorPlay = true
        triggerLazyInit(context)
      }
    }
  }

  /**
   * Plays a full-bodied bi-metallic coin strike against a hollow earthen Gullak pot
   * with cascading coin-bed shimmer when adding savings.
   */
  fun playGullakCoinClink(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    runCatching {
      val pool = soundPool
      if (pool != null && gullakClinkLoaded && gullakClinkSoundId != 0) {
        val rate = 0.975f + ((System.nanoTime() and 0xFFL) / 255f) * 0.055f
        pool.play(gullakClinkSoundId, 0.34f, 0.34f, 2, 0, rate)
      } else {
        pendingCoinPlay = true
        triggerLazyInit(context)
      }
    }
  }

  /**
   * Plays a crisp cotton-paper Rupee note snap and leather Batua rustle when cash enters or leaves.
   */
  fun playCrispNoteRustle(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    runCatching {
      val pool = soundPool
      if (pool != null && crispNoteLoaded && crispNoteSoundId != 0) {
        val rate = 0.97f + ((System.nanoTime() and 0xFFL) / 255f) * 0.06f
        pool.play(crispNoteSoundId, 0.30f, 0.32f, 1, 0, rate)
      } else {
        pendingNotePlay = true
        triggerLazyInit(context)
      }
    }
  }

  /**
   * Plays a warm handmade khadi paper flick and wooden board tap when stashing a Chitthi
   * or pinning a Habit Sankalp pledge.
   */
  fun playPaperChitthiPin(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    runCatching {
      val pool = soundPool
      if (pool != null && chitthiPinLoaded && chitthiPinSoundId != 0) {
        val rate = 0.98f + ((System.nanoTime() and 0xFFL) / 255f) * 0.045f
        pool.play(chitthiPinSoundId, 0.29f, 0.30f, 1, 0, rate)
      } else {
        pendingChitthiPlay = true
        triggerLazyInit(context)
      }
    }
  }

  /**
   * Plays a subtle ceramic chai cup & saucer settle on a wooden table when sitting down
   * for a chat with Maa or Papa.
   */
  fun playChaiCupSettle(context: Context, soundEnabled: Boolean) {
    if (!soundEnabled) return
    runCatching {
      val pool = soundPool
      if (pool != null && chaiCupLoaded && chaiCupSoundId != 0) {
        val rate = 0.985f + ((System.nanoTime() and 0xFFL) / 255f) * 0.035f
        pool.play(chaiCupSoundId, 0.29f, 0.29f, 1, 0, rate)
      } else {
        pendingChaiPlay = true
        triggerLazyInit(context)
      }
    }
  }

  private fun triggerLazyInit(context: Context) {
    val appCtx = context.applicationContext
    if (initStarted.compareAndSet(false, true)) {
      ioScope.launch { ensureInitializedSync(appCtx) }
    }
  }

  private fun ensureInitializedSync(appCtx: Context) {
    runCatching {
      for (oldDirName in LEGACY_CACHE_SUBDIRS) {
        runCatching { File(appCtx.cacheDir, oldDirName).deleteRecursively() }
      }

      val sfxDir = File(appCtx.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
      val doorFile = File(sfxDir, DOOR_KUNDI_FILENAME)
      val coinFile = File(sfxDir, GULLAK_CLINK_FILENAME)
      val noteFile = File(sfxDir, CRISP_NOTE_FILENAME)
      val chitthiFile = File(sfxDir, CHITTHI_PIN_FILENAME)
      val chaiFile = File(sfxDir, CHAI_CUP_FILENAME)

      if (!doorFile.exists() || doorFile.length() < 1024L) {
        doorFile.writeBytes(buildDoorKundiOpenWavBytes())
      }
      if (!coinFile.exists() || coinFile.length() < 1024L) {
        coinFile.writeBytes(buildGullakCoinClinkWavBytes())
      }
      if (!noteFile.exists() || noteFile.length() < 1024L) {
        noteFile.writeBytes(buildCrispNoteRustleWavBytes())
      }
      if (!chitthiFile.exists() || chitthiFile.length() < 1024L) {
        chitthiFile.writeBytes(buildPaperChitthiPinWavBytes())
      }
      if (!chaiFile.exists() || chaiFile.length() < 1024L) {
        chaiFile.writeBytes(buildChaiCupSettleWavBytes())
      }

      // USAGE_GAME routes through the full-range media stream (not attenuated touch feedback)
      // while coexisting peacefully without stealing system audio focus.
      val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

      val pool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(attrs)
        .build()

      pool.setOnLoadCompleteListener { sp, sampleId, status ->
        if (status != 0) return@setOnLoadCompleteListener
        when (sampleId) {
          doorKundiSoundId -> {
            doorKundiLoaded = true
            if (pendingDoorPlay) {
              pendingDoorPlay = false
              runCatching { sp.play(doorKundiSoundId, 0.76f, 0.78f, 2, 0, 1.0f) }
            }
          }
          gullakClinkSoundId -> {
            gullakClinkLoaded = true
            if (pendingCoinPlay) {
              pendingCoinPlay = false
              runCatching { sp.play(gullakClinkSoundId, 0.78f, 0.78f, 2, 0, 1.0f) }
            }
          }
          crispNoteSoundId -> {
            crispNoteLoaded = true
            if (pendingNotePlay) {
              pendingNotePlay = false
              runCatching { sp.play(crispNoteSoundId, 0.72f, 0.74f, 1, 0, 1.0f) }
            }
          }
          chitthiPinSoundId -> {
            chitthiPinLoaded = true
            if (pendingChitthiPlay) {
              pendingChitthiPlay = false
              runCatching { sp.play(chitthiPinSoundId, 0.70f, 0.72f, 1, 0, 1.0f) }
            }
          }
          chaiCupSoundId -> {
            chaiCupLoaded = true
            if (pendingChaiPlay) {
              pendingChaiPlay = false
              runCatching { sp.play(chaiCupSoundId, 0.68f, 0.70f, 1, 0, 1.0f) }
            }
          }
        }
      }

      soundPool = pool
      doorKundiSoundId = pool.load(doorFile.absolutePath, 1)
      gullakClinkSoundId = pool.load(coinFile.absolutePath, 1)
      crispNoteSoundId = pool.load(noteFile.absolutePath, 1)
      chitthiPinSoundId = pool.load(chitthiFile.absolutePath, 1)
      chaiCupSoundId = pool.load(chaiFile.absolutePath, 1)
    }
  }

  /**
   * Synthesizes a 540ms high-definition 16-bit PCM WAV modelling an Indian courtyard door:
   * - [0.002s..0.070s] Heavy brass *kundi* staple lift & metal-on-brass slide
   * - [0.082s..0.210s] Seasoned teak bolt *clack* & hollow wooden frame thump (178 Hz + 312 Hz + 890 Hz)
   * - [0.165s..0.525s] Warm wooden door swing open (organic wood-grain creak harmonics + courtyard air)
   */
  internal fun buildDoorKundiOpenWavBytes(): ByteArray {
    val durationSec = 0.54f
    val numSamples = (SAMPLE_RATE * durationSec).toInt()
    val rawBuf = DoubleArray(numSamples)
    var noiseSeed = 0x5A17_C309

    fun nextNoise(): Double {
      noiseSeed = noiseSeed xor (noiseSeed shl 13)
      noiseSeed = noiseSeed xor (noiseSeed ushr 17)
      noiseSeed = noiseSeed xor (noiseSeed shl 5)
      return ((noiseSeed and 0xFFFF) / 32768.0) - 1.0
    }

    var lpAir = 0.0
    var bpWood = 0.0
    for (i in 0 until numSamples) {
      val t = i.toDouble() / SAMPLE_RATE
      var s = 0.0
      val n = nextNoise()
      lpAir = lpAir * 0.88 + n * 0.12
      bpWood = bpWood * 0.72 + (n - lpAir) * 0.28

      // 1. Brass kundi staple lift & ring (2ms - 75ms)
      if (t >= 0.002 && t < 0.085) {
        val dt = t - 0.002
        val click = exp(-dt * 310.0) * n * 0.52
        val slide = sin(PI * (dt / 0.045).coerceIn(0.0, 1.0)) * bpWood * 0.28
        val brassModes = (
          0.46 * sin(2.0 * PI * 1540.0 * dt) * exp(-dt * 62.0) +
            0.34 * sin(2.0 * PI * 2480.0 * dt) * exp(-dt * 78.0) +
            0.22 * sin(2.0 * PI * 3790.0 * dt) * exp(-dt * 96.0) +
            0.25 * sin(2.0 * PI * 690.0 * dt) * exp(-dt * 54.0)
          ) * 0.72
        s += click + slide + brassModes
      }

      // 2. Solid teak bolt clack & frame resonance (84ms - 220ms)
      if (t >= 0.084 && t < 0.235) {
        val dt = t - 0.084
        val woodThump = (
          0.68 * sin(2.0 * PI * (182.0 - 24.0 * dt) * dt) * exp(-dt * 42.0) +
            0.42 * sin(2.0 * PI * 318.0 * dt) * exp(-dt * 58.0)
          ) * 0.88
        val ironLatch = (
          0.54 * sin(2.0 * PI * 865.0 * dt) * exp(-dt * 88.0) +
            0.36 * sin(2.0 * PI * 1380.0 * dt) * exp(-dt * 110.0) +
            0.20 * sin(2.0 * PI * 2120.0 * dt) * exp(-dt * 140.0)
          ) * 0.66
        val impactCrack = exp(-dt * 260.0) * bpWood * 0.65
        s += woodThump + ironLatch + impactCrack
      }

      // 3. Warm wooden door swinging open (162ms - 530ms)
      if (t >= 0.162) {
        val dt = t - 0.162
        val span = 0.365
        val norm = (dt / span).coerceIn(0.0, 1.0)
        val env = sin(PI * norm) * exp(-dt * 2.8)
        val f0 = 196.0 - 48.0 * norm + 3.5 * sin(2.0 * PI * 11.0 * dt)
        val woodCreak = (
          0.58 * sin(2.0 * PI * f0 * dt) +
            0.29 * sin(2.0 * PI * (f0 * 2.01) * dt) +
            0.13 * sin(2.0 * PI * (f0 * 3.03) * dt)
          ) * env * 0.36
        val breeze = lpAir * env * 0.34
        s += woodCreak + breeze
      }

      rawBuf[i] = s
    }

    val pcm = masterWithRoomReflections(rawBuf, roomMix = 0.22, targetPeak = 0.93)
    return encodeMono16BitWav(pcm, SAMPLE_RATE)
  }

  /**
   * Synthesizes a 520ms high-definition 16-bit PCM WAV modelling a heavy bi-metallic coin
   * dropping into a fired terracotta Gullak pot:
   * - [0.001s..0.380s] Crisp coin rim strike + deep clay pot Helmholtz cavity thump (368 -> 336 Hz)
   *   + bright inharmonic bronze/nickel bell partials
   * - [0.064s..0.440s] Secondary coin-on-coin bounce inside the pot
   * - [0.128s..0.505s] Final warm metallic shimmer settle on the coin bed
   */
  internal fun buildGullakCoinClinkWavBytes(): ByteArray {
    val durationSec = 0.52f
    val numSamples = (SAMPLE_RATE * durationSec).toInt()
    val rawBuf = DoubleArray(numSamples)
    var noiseSeed = 0x1E93_4B21

    fun nextNoise(): Double {
      noiseSeed = noiseSeed xor (noiseSeed shl 13)
      noiseSeed = noiseSeed xor (noiseSeed ushr 17)
      noiseSeed = noiseSeed xor (noiseSeed shl 5)
      return ((noiseSeed and 0xFFFF) / 32768.0) - 1.0
    }

    for (i in 0 until numSamples) {
      val t = i.toDouble() / SAMPLE_RATE
      var s = 0.0
      val n = nextNoise()

      // 1. Primary coin strike on terracotta slot + hollow clay body thump
      if (t >= 0.001) {
        val dt = t - 0.001
        val slotTransient = exp(-dt * 340.0) * n * 0.42
        val clayHelmholtz = (
          0.68 * sin(2.0 * PI * (368.0 - 28.0 * exp(-dt * 35.0)) * dt) * exp(-dt * 34.0) +
            0.38 * sin(2.0 * PI * 645.0 * dt) * exp(-dt * 52.0)
          ) * 0.52
        val primaryCoinBell = (
          0.42 * sin(2.0 * PI * 2420.0 * dt) * exp(-dt * 15.0) +
            0.34 * sin(2.0 * PI * 3890.0 * dt) * exp(-dt * 19.0) +
            0.22 * sin(2.0 * PI * 5740.0 * dt) * exp(-dt * 25.0) +
            0.13 * sin(2.0 * PI * 7620.0 * dt) * exp(-dt * 32.0) +
            0.08 * sin(2.0 * PI * 9480.0 * dt) * exp(-dt * 42.0)
          ) * 0.86
        s += slotTransient + clayHelmholtz + primaryCoinBell
      }

      // 2. First bounce onto inner coin pile (+66ms)
      if (t >= 0.066) {
        val dt = t - 0.066
        val bounceClick = exp(-dt * 390.0) * n * 0.26
        val potEcho = sin(2.0 * PI * 352.0 * dt) * exp(-dt * 44.0) * 0.24
        val bounceChime = (
          0.46 * sin(2.0 * PI * 2980.0 * dt) * exp(-dt * 20.0) +
            0.33 * sin(2.0 * PI * 4720.0 * dt) * exp(-dt * 26.0) +
            0.21 * sin(2.0 * PI * 6690.0 * dt) * exp(-dt * 34.0)
          ) * 0.56
        s += bounceClick + potEcho + bounceChime
      }

      // 3. Final coin-bed settle shimmer (+126ms)
      if (t >= 0.126) {
        val dt = t - 0.126
        val settleTick = exp(-dt * 440.0) * n * 0.16
        val settleShimmer = (
          0.50 * sin(2.0 * PI * 3360.0 * dt) * exp(-dt * 24.0) +
            0.32 * sin(2.0 * PI * 5290.0 * dt) * exp(-dt * 30.0) +
            0.18 * sin(2.0 * PI * 7410.0 * dt) * exp(-dt * 38.0)
          ) * 0.34
        s += settleTick + settleShimmer
      }

      rawBuf[i] = s
    }

    val pcm = masterWithRoomReflections(rawBuf, roomMix = 0.18, targetPeak = 0.94)
    return encodeMono16BitWav(pcm, SAMPLE_RATE)
  }

  /**
   * Synthesizes a 290ms tactile cotton-paper Rupee bank-note snap & leather Batua fold rustle.
   */
  internal fun buildCrispNoteRustleWavBytes(): ByteArray {
    val durationSec = 0.29f
    val numSamples = (SAMPLE_RATE * durationSec).toInt()
    val rawBuf = DoubleArray(numSamples)
    var noiseSeed = 0x7C41_9D03

    fun nextNoise(): Double {
      noiseSeed = noiseSeed xor (noiseSeed shl 13)
      noiseSeed = noiseSeed xor (noiseSeed ushr 17)
      noiseSeed = noiseSeed xor (noiseSeed shl 5)
      return ((noiseSeed and 0xFFFF) / 32768.0) - 1.0
    }

    var lp1 = 0.0
    var lp2 = 0.0
    for (i in 0 until numSamples) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = nextNoise()
      lp1 = lp1 * 0.52 + n * 0.48
      lp2 = lp2 * 0.86 + n * 0.14
      val crispBand = lp1 - lp2 // High-mid crisp cotton-fiber band

      var s = 0.0
      // Soft leather wallet pat (0ms..90ms)
      if (t >= 0.002 && t < 0.095) {
        val dt = t - 0.002
        val leatherBody = sin(2.0 * PI * (155.0 - 30.0 * dt) * dt) * exp(-dt * 55.0) * 0.54
        val slide = sin(PI * (dt / 0.085).coerceIn(0.0, 1.0)) * lp2 * 0.44
        s += leatherBody + slide
      }
      // First crisp note fan flick (28ms..150ms)
      if (t >= 0.028 && t < 0.160) {
        val dt = t - 0.028
        val snapEnv = sin(PI * (dt / 0.115).coerceIn(0.0, 1.0)) * exp(-dt * 14.0)
        val paperSnap = exp(-dt * 190.0) * crispBand * 0.92
        val fiberRustle = crispBand * snapEnv * 0.68
        val crispMode = sin(2.0 * PI * 1920.0 * dt) * exp(-dt * 95.0) * 0.18
        s += paperSnap + fiberRustle + crispMode
      }
      // Second note settle tuck (96ms..275ms)
      if (t >= 0.096) {
        val dt = t - 0.096
        val tuckEnv = sin(PI * (dt / 0.175).coerceIn(0.0, 1.0)) * exp(-dt * 11.0)
        s += crispBand * tuckEnv * 0.48 + lp2 * tuckEnv * 0.26
      }

      rawBuf[i] = s
    }

    val pcm = masterWithRoomReflections(rawBuf, roomMix = 0.12, targetPeak = 0.90)
    return encodeMono16BitWav(pcm, SAMPLE_RATE)
  }

  /**
   * Synthesizes a 240ms warm khadi paper note flick & wooden notice-board pin tap.
   */
  internal fun buildPaperChitthiPinWavBytes(): ByteArray {
    val durationSec = 0.24f
    val numSamples = (SAMPLE_RATE * durationSec).toInt()
    val rawBuf = DoubleArray(numSamples)
    var noiseSeed = 0x3F82_A617

    fun nextNoise(): Double {
      noiseSeed = noiseSeed xor (noiseSeed shl 13)
      noiseSeed = noiseSeed xor (noiseSeed ushr 17)
      noiseSeed = noiseSeed xor (noiseSeed shl 5)
      return ((noiseSeed and 0xFFFF) / 32768.0) - 1.0
    }

    var lpSlow = 0.0
    var lpFast = 0.0
    for (i in 0 until numSamples) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = nextNoise()
      lpFast = lpFast * 0.56 + n * 0.44
      lpSlow = lpSlow * 0.85 + n * 0.15
      val paperBand = lpFast - lpSlow

      var s = 0.0
      // Khadi paper curl flick (0ms..115ms)
      if (t >= 0.002 && t < 0.120) {
        val dt = t - 0.002
        val env = sin(PI * (dt / 0.110).coerceIn(0.0, 1.0)) * exp(-dt * 16.0)
        s += paperBand * env * 0.76
      }
      // Warm wooden board pin tap (58ms..225ms)
      if (t >= 0.058) {
        val dt = t - 0.058
        val woodTap = (
          0.62 * sin(2.0 * PI * 420.0 * dt) * exp(-dt * 68.0) +
            0.38 * sin(2.0 * PI * 980.0 * dt) * exp(-dt * 95.0) +
            0.22 * sin(2.0 * PI * 1840.0 * dt) * exp(-dt * 130.0)
          ) * 0.74
        val pinClick = exp(-dt * 320.0) * paperBand * 0.52
        s += woodTap + pinClick
      }

      rawBuf[i] = s
    }

    val pcm = masterWithRoomReflections(rawBuf, roomMix = 0.15, targetPeak = 0.90)
    return encodeMono16BitWav(pcm, SAMPLE_RATE)
  }

  /**
   * Synthesizes a 320ms cozy ceramic chai cup settling onto its saucer & wooden table.
   */
  internal fun buildChaiCupSettleWavBytes(): ByteArray {
    val durationSec = 0.32f
    val numSamples = (SAMPLE_RATE * durationSec).toInt()
    val rawBuf = DoubleArray(numSamples)
    var noiseSeed = 0x2B64_E115

    fun nextNoise(): Double {
      noiseSeed = noiseSeed xor (noiseSeed shl 13)
      noiseSeed = noiseSeed xor (noiseSeed ushr 17)
      noiseSeed = noiseSeed xor (noiseSeed shl 5)
      return ((noiseSeed and 0xFFFF) / 32768.0) - 1.0
    }

    for (i in 0 until numSamples) {
      val t = i.toDouble() / SAMPLE_RATE
      val n = nextNoise()
      var s = 0.0

      // Initial ceramic rim contact on saucer (2ms..220ms)
      if (t >= 0.002) {
        val dt = t - 0.002
        val rimTouch = exp(-dt * 380.0) * n * 0.32
        val porcelainModes = (
          0.52 * sin(2.0 * PI * 2140.0 * dt) * exp(-dt * 26.0) +
            0.34 * sin(2.0 * PI * 3490.0 * dt) * exp(-dt * 34.0) +
            0.18 * sin(2.0 * PI * 5180.0 * dt) * exp(-dt * 46.0)
          ) * 0.72
        val woodChaukiWarmth = sin(2.0 * PI * 265.0 * dt) * exp(-dt * 52.0) * 0.42
        s += rimTouch + porcelainModes + woodChaukiWarmth
      }

      // Gentle saucer-base micro-settle (+48ms)
      if (t >= 0.048) {
        val dt = t - 0.048
        val settle = (
          0.58 * sin(2.0 * PI * 2280.0 * dt) * exp(-dt * 36.0) +
            0.42 * sin(2.0 * PI * 3720.0 * dt) * exp(-dt * 48.0)
          ) * 0.38 + sin(2.0 * PI * 240.0 * dt) * exp(-dt * 60.0) * 0.24
        s += settle
      }

      rawBuf[i] = s
    }

    val pcm = masterWithRoomReflections(rawBuf, roomMix = 0.18, targetPeak = 0.90)
    return encodeMono16BitWav(pcm, SAMPLE_RATE)
  }

  /**
   * Adds warm 3-tap room early reflections (11ms, 23ms, 38ms low-passed wood/plaster taps),
   * applies soft-knee harmonic saturation for fullness on phone speakers, and peak-normalizes
   * to [targetPeak] (~ `-0.7 dBFS`) with a 4ms anti-click tail fade.
   */
  private fun masterWithRoomReflections(
    dry: DoubleArray,
    roomMix: Double,
    targetPeak: Double
  ): ShortArray {
    val n = dry.size
    val wet = DoubleArray(n)
    val tap1 = (0.011 * SAMPLE_RATE).toInt()
    val tap2 = (0.023 * SAMPLE_RATE).toInt()
    val tap3 = (0.038 * SAMPLE_RATE).toInt()

    var roomLp = 0.0
    for (i in 0 until n) {
      var refl = 0.0
      if (i >= tap1) refl += dry[i - tap1] * 0.52
      if (i >= tap2) refl += dry[i - tap2] * 0.31
      if (i >= tap3) refl += dry[i - tap3] * 0.17
      roomLp = roomLp * 0.68 + refl * 0.32
      val combined = dry[i] + roomLp * roomMix
      // Gentle soft-knee warmth: x / (1 + 0.22 * |x|) boosts body relative to sharp spikes
      wet[i] = combined / (1.0 + 0.22 * abs(combined))
    }

    var peak = 1e-6
    for (v in wet) {
      peak = max(peak, abs(v))
    }
    val gain = targetPeak / peak
    val fadeOutSamples = (0.005 * SAMPLE_RATE).toInt().coerceAtLeast(1)
    val pcm = ShortArray(n)
    for (i in 0 until n) {
      val tailEnv = if (i > n - fadeOutSamples) {
        (n - i).toDouble() / fadeOutSamples.toDouble()
      } else {
        1.0
      }
      val normalized = (wet[i] * gain * tailEnv).coerceIn(-0.98, 0.98)
      pcm[i] = (normalized * Short.MAX_VALUE).roundToInt().toShort()
    }
    return pcm
  }

  internal fun encodeMono16BitWav(samples: ShortArray, sampleRate: Int): ByteArray {
    val dataSize = samples.size * 2
    val totalFileSizeMinus8 = 36 + dataSize
    val out = ByteArrayOutputStream(44 + dataSize)
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put('R'.code.toByte())
    header.put('I'.code.toByte())
    header.put('F'.code.toByte())
    header.put('F'.code.toByte())
    header.putInt(totalFileSizeMinus8)
    header.put('W'.code.toByte())
    header.put('A'.code.toByte())
    header.put('V'.code.toByte())
    header.put('E'.code.toByte())
    header.put('f'.code.toByte())
    header.put('m'.code.toByte())
    header.put('t'.code.toByte())
    header.put(' '.code.toByte())
    header.putInt(16) // PCM fmt chunk length
    header.putShort(1) // AudioFormat = 1 (PCM)
    header.putShort(1) // NumChannels = 1 (Mono)
    header.putInt(sampleRate)
    header.putInt(sampleRate * 2) // ByteRate
    header.putShort(2) // BlockAlign
    header.putShort(16) // BitsPerSample
    header.put('d'.code.toByte())
    header.put('a'.code.toByte())
    header.put('t'.code.toByte())
    header.put('a'.code.toByte())
    header.putInt(dataSize)
    out.write(header.array())

    val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
    for (s in samples) {
      pcmBytes.putShort(s)
    }
    out.write(pcmBytes.array())
    return out.toByteArray()
  }
}
