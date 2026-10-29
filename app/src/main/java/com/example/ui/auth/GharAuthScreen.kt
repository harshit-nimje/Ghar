package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.ui.common.HandwrittenHomeNote
import com.example.ui.common.MaaAvatar
import com.example.ui.common.PapaAvatar
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.StickyNotePaperColors
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.common.rememberCachedGharPainter
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "GharAuthScreen"

/**
 * Interactive Google Sign-In launcher via Jetpack CredentialManager (`GetSignInWithGoogleOption`).
 * Never uses anonymous or email/password auth.
 */
suspend fun performInteractiveGoogleSignIn(context: Context): Result<FirebaseUser> {
  val webClientId = resolveWebClientId(context)
  if (webClientId.isBlank()) {
    return Result.failure(IllegalStateException("Google Sign-In client configuration is missing from resources."))
  }
  val credentialManager = CredentialManager.create(context)
  return try {
    val googleSignInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleSignInOption)
      .build()
    val result = credentialManager.getCredential(
      context = context,
      request = request
    )
    val credential = result.credential
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
      val firebaseCredential = GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
      val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
      val signedInUser = authResult.user
      if (signedInUser != null) {
        Result.success(signedInUser)
      } else {
        Result.failure(IllegalStateException("Google Sign-In completed without a user profile."))
      }
    } else {
      Result.failure(IllegalStateException("Unsupported credential returned. Please choose a Google account."))
    }
  } catch (e: GetCredentialCancellationException) {
    Log.w(TAG, "Google Sign-In sheet dismissed by user", e)
    Result.failure(e)
  } catch (e: NoCredentialException) {
    Log.w(TAG, "No Google account configured on device", e)
    Result.failure(
      IllegalStateException("No Google account found on this device. Add one in Android Settings or continue in Guest Mode.")
    )
  } catch (e: GetCredentialException) {
    Log.e(TAG, "Credential Manager sign-in failed", e)
    Result.failure(
      IllegalStateException(
        e.errorMessage?.toString()
          ?: e.localizedMessage
          ?: "Could not complete Google Sign-In. You can retry or continue in Guest Mode."
      )
    )
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Log.e(TAG, "Firebase Google Sign-In failed", e)
    Result.failure(e)
  }
}

/**
 * Final Onboarding Step (and reusable Sign-In / Guest Mode choice section):
 * Lets the user back up & sync their newly calibrated household to Google Cloud,
 * OR continue seamlessly in local Guest Mode with zero friction.
 */
@Composable
fun OptionalCloudVaultChoiceSection(
  userName: String,
  isAlreadySignedIn: Boolean,
  signedInLabel: String?,
  onSignedInProceed: (FirebaseUser?) -> Unit,
  onContinueAsGuest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val plainEnglish = LocalPlainEnglishMode.current
  val maaName = GharTerms.maaName(plainEnglish)
  val coroutineScope = rememberCoroutineScope()
  var isSigningIn by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val cleanName = userName.trim().ifBlank { "Friend" }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = if (plainEnglish) "Where Should Your Budget Be Stored?" else "Where Should Maa Keep Your Khata?",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.Bold
    )

    Text(
      text = if (plainEnglish) {
        "Sign in with Google to back up your daily budget, savings goals, and shortcuts across devices—or start right away in Guest Mode."
      } else {
        "Sign in with Google to back up your Batua, Gullak, and stamps across devices—or step right inside in Guest Mode."
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    HandwrittenHomeNote(
      isMintPaper = false,
      tiltDegrees = -0.6f
    ) {
      Column {
        Text(
          text = "$maaName's note for $cleanName",
          style = GharHandStyles.stickyNoteTitle,
          color = StickyNotePaperColors.InkAccentRed
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (plainEnglish) {
            "“Your monthly budget and savings goal are ready! Link your Google account if you want encrypted cloud backup, or use Guest Mode on this phone and link later from Settings anytime.”"
          } else {
            "“Beta, your budget and Gullak dream are already ready! Link your Google account if you want safe cloud backup, or use Guest Mode on this phone and link later from the Balcony anytime.”"
          },
          style = GharHandStyles.stickyNoteBody,
          color = StickyNotePaperColors.InkDark
        )
      }
    }

    // Cloud Backup Reassurance Card
    Surface(
      shape = PebbleCardShape,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.38f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            tint = PastelSage,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Google Cloud Vault (Optional)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Encrypted per-account sync so switching phones never loses a note.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Smartphone,
            contentDescription = null,
            tint = PastelTerracotta,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Guest Mode (On This Phone)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Saves everything locally on your device right away. No sign-in required.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    errorMessage?.let { msg ->
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("auth_error_banner")
      ) {
        Text(
          text = msg,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(14.dp)
        )
      }
    }

    if (isAlreadySignedIn) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = PastelSage.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, PastelSage.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PastelSage,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Connected with Google (${signedInLabel ?: "Synced"})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Button(
        onClick = { onSignedInProceed(Firebase.auth.currentUser) },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 56.dp)
          .testTag("walk_into_ghar_signed_in_button")
      ) {
        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = null)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Sync & Walk into Ghar",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    } else {
      // Primary: Sign in with Google
      Button(
        onClick = {
          if (isSigningIn) return@Button
          errorMessage = null
          isSigningIn = true
          coroutineScope.launch {
            val res = performInteractiveGoogleSignIn(context)
            isSigningIn = false
            res.fold(
              onSuccess = { firebaseUser ->
                onSignedInProceed(firebaseUser)
              },
              onFailure = { err ->
                if (err !is GetCredentialCancellationException) {
                  errorMessage = err.localizedMessage ?: "Sign-in could not complete. You can also continue as Guest."
                }
              }
            )
          }
        },
        enabled = !isSigningIn,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 56.dp)
          .testTag("google_sign_in_button")
      ) {
        if (isSigningIn) {
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.4.dp,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Connecting Google Vault...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        } else {
          Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              GoogleEmblemCanvas(modifier = Modifier.size(16.dp))
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Sign in with Google & Sync",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Secondary Optional Guest Mode CTA
      OutlinedButton(
        onClick = onContinueAsGuest,
        enabled = !isSigningIn,
        shape = CircleShape,
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 54.dp)
          .testTag("continue_as_guest_button")
      ) {
        Icon(
          imageVector = Icons.Default.MeetingRoom,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Continue as Guest (Skip Sign-In)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun GharAuthScreen(
  onContinueAsGuest: () -> Unit = {},
  onSignedInSuccess: (FirebaseUser?) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val bgColor = MaterialTheme.colorScheme.background

  Surface(
    modifier = modifier
      .fillMaxSize()
      .navigationBarsPadding()
      .testTag("ghar_auth_screen"),
    color = bgColor
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .pastelHomeBackdrop(
          primaryWash = PastelTerracotta,
          secondaryWash = PastelSage,
          tertiaryWash = PastelHoney
        )
        .verticalScroll(rememberScrollState())
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(296.dp)
      ) {
        Image(
          painter = rememberCachedGharPainter(R.drawable.img_living_courtyard_1792931331737),
          contentDescription = "Sunlit Ghar Threshold",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color(0xFF1F1610).copy(alpha = 0.28f),
                0.28f to Color.Transparent,
                0.60f to bgColor.copy(alpha = 0.24f),
                0.85f to bgColor.copy(alpha = 0.88f),
                1.0f to bgColor
              )
            )
        )

        Surface(
          shape = CircleShape,
          color = Color(0xFF231811).copy(alpha = 0.54f),
          border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.28f)),
          modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
          Text(
            text = "GHAR • FRONT VERANDAH",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.6.sp,
            color = Color(0xFFFFF8EC),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
          )
        }

        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            MaaAvatar(size = 42.dp, isSpeaking = false)
            PapaAvatar(size = 42.dp)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Come Home to Ghar",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp, vertical = 8.dp)
      ) {
        OptionalCloudVaultChoiceSection(
          userName = "Friend",
          isAlreadySignedIn = Firebase.auth.currentUser != null,
          signedInLabel = Firebase.auth.currentUser?.email ?: Firebase.auth.currentUser?.displayName,
          onSignedInProceed = onSignedInSuccess,
          onContinueAsGuest = onContinueAsGuest
        )
        Spacer(modifier = Modifier.height(20.dp))
      }
    }
  }
}

internal fun resolveWebClientId(context: Context): String {
  val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
  return if (resId != 0) context.getString(resId) else ""
}

@Composable
fun GoogleEmblemCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val stroke = size.minDimension * 0.22f
    val inset = stroke / 2f
    val arcSize = Size(size.width - stroke, size.height - stroke)
    val topLeft = Offset(inset, inset)

    drawArc(
      color = Color(0xFFEA4335),
      startAngle = 215f,
      sweepAngle = 105f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 145f,
      sweepAngle = 70f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 45f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -12f,
      sweepAngle = 57f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.52f, size.height * 0.50f),
      end = Offset(size.width * 0.94f, size.height * 0.50f),
      strokeWidth = stroke,
      cap = StrokeCap.Butt
    )
  }
}
