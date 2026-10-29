package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.engine.ChitthiWisdomBank
import com.example.core.engine.WisdomChitthiSeed
import com.example.core.localization.GharTerms
import com.example.core.localization.LocalPlainEnglishMode
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.util.DateUtils
import com.example.ui.theme.GharHandStyles
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta
import com.example.ui.theme.contrastingContentColor
import kotlin.random.Random

/**
 * Interactive Sticky Note ("Chitthi" / "Saved Note") card from Maa/Mom or Papa/Dad.
 */
@Composable
fun InteractiveChitthiStickyCard(
  seed: WisdomChitthiSeed,
  isStashed: Boolean,
  stashedCount: Int,
  onCycleNext: () -> Unit,
  onToggleStash: () -> Unit,
  onOpenDabba: () -> Unit,
  onQuickGullakAction: ((Long) -> Unit)? = null,
  onCardBodyClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val isPapa = seed.author == ChitthiAuthor.PAPA
  val titleInk = if (isPapa) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed
  val authorLabel = seed.author.displayLabel(plainEnglish)
  val noteNoun = if (plainEnglish) "note" else "chitthi"

  HandwrittenHomeNote(
    isMintPaper = isPapa,
    tiltDegrees = if (isPapa) 0.6f else -0.7f,
    modifier = modifier
      .let { base ->
        if (onCardBodyClick != null) base.clickable { onCardBodyClick() } else base
      }
      .testTag("interactive_chitthi_card_${seed.author.name.lowercase()}")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (isPapa) {
            PapaAvatar(size = 36.dp)
          } else {
            MaaAvatar(size = 36.dp)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = seed.title,
              style = GharHandStyles.stickyNoteTitle,
              color = titleInk,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "$authorLabel's $noteNoun • ${seed.tag}",
              style = GharHandStyles.stickyNoteMeta,
              color = StickyNotePaperColors.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onCycleNext,
            modifier = Modifier
              .size(48.dp)
              .testTag("cycle_chitthi_${seed.author.name.lowercase()}")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Another $authorLabel note",
              tint = StickyNotePaperColors.InkMuted,
              modifier = Modifier.size(19.dp)
            )
          }

          IconButton(
            onClick = onToggleStash,
            modifier = Modifier
              .size(48.dp)
              .testTag("stash_chitthi_${seed.author.name.lowercase()}")
          ) {
            Icon(
              imageVector = if (isStashed) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
              contentDescription = if (isStashed) "Saved in Notes" else "Save note",
              tint = if (isStashed) StickyNotePaperColors.InkAccentSage else titleInk,
              modifier = Modifier.size(21.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "“${seed.body}”",
        style = GharHandStyles.stickyNoteBody,
        color = StickyNotePaperColors.InkDark
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = Color.White.copy(alpha = 0.58f),
          border = BorderStroke(0.8.dp, StickyNotePaperColors.InkMuted.copy(alpha = 0.28f)),
          modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .clickable { onOpenDabba() }
            .testTag("open_chitthi_dabba_from_note")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.BookmarkAdded,
              contentDescription = null,
              tint = titleInk,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = GharTerms.notesBadgeLabel(plainEnglish, stashedCount),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = StickyNotePaperColors.InkDark,
              maxLines = 1
            )
          }
        }

        if (seed.suggestedGullakRupees != null && seed.actionLabel != null && onQuickGullakAction != null) {
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = CircleShape,
            color = titleInk,
            modifier = Modifier
              .defaultMinSize(minHeight = 40.dp)
              .clip(CircleShape)
              .clickable { onQuickGullakAction(seed.suggestedGullakRupees) }
              .testTag("chitthi_quick_action_${seed.author.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                tint = Color(0xFFFFFBF2),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = seed.actionLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFBF2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

private enum class ChitthiFilter(val label: String, val plainLabel: String) {
  ALL("All Notes", "All Notes"),
  MAA("Maa", "Mom"),
  PAPA("Papa", "Dad");

  fun displayLabel(plainEnglish: Boolean): String = if (plainEnglish) plainLabel else label
}

@Composable
fun ChitthiDabbaDialog(
  stashedNotes: List<ChitthiNote>,
  onStashSurpriseNote: (ChitthiNote) -> Unit,
  onRemoveNote: (ChitthiNote) -> Unit,
  onDismiss: () -> Unit
) {
  val plainEnglish = LocalPlainEnglishMode.current
  var filter by remember { mutableStateOf(ChitthiFilter.ALL) }

  val filteredNotes = remember(stashedNotes, filter) {
    when (filter) {
      ChitthiFilter.ALL -> stashedNotes
      ChitthiFilter.MAA -> stashedNotes.filter { it.author == ChitthiAuthor.MAA }
      ChitthiFilter.PAPA -> stashedNotes.filter { it.author == ChitthiAuthor.PAPA }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.2.dp, PastelHoney.copy(alpha = 0.42f)),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.86f)
        .widthIn(max = 540.dp)
        .padding(vertical = 10.dp)
        .testTag("chitthi_dabba_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (plainEnglish) "Saved Notes" else stringResource(R.string.chitthi_dabba_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (plainEnglish) {
                "Practical tips, quotes & reminders saved from Mom and Dad"
              } else {
                stringResource(R.string.chitthi_dabba_subtitle)
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(48.dp)
              .testTag("close_chitthi_dabba_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Chitthi Dabba",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips + Surprise Chitthi Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ChitthiFilter.entries.forEach { item ->
            val selected = filter == item
            Surface(
              shape = CircleShape,
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier
                .defaultMinSize(minHeight = 42.dp)
                .clip(CircleShape)
                .clickable { filter = item }
                .testTag("chitthi_filter_${item.name.lowercase()}")
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = item.displayLabel(plainEnglish),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
              }
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          Surface(
            shape = CircleShape,
            color = PastelHoney.copy(alpha = 0.22f),
            border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.55f)),
            modifier = Modifier
              .defaultMinSize(minHeight = 42.dp)
              .clip(CircleShape)
              .clickable {
                val existingIds = stashedNotes.map { it.id }.toSet()
                val maaPool = ChitthiWisdomBank.getMaaSeeds(plainEnglish)
                val papaPool = ChitthiWisdomBank.getPapaSeeds(plainEnglish)
                val pool = (maaPool + papaPool).filterNot { it.id in existingIds }
                val picked = if (pool.isNotEmpty()) {
                  pool[Random.nextInt(pool.size)].toChitthiNote()
                } else {
                  val fallback = maaPool[Random.nextInt(maaPool.size)]
                  fallback.toChitthiNote().copy(id = "${fallback.id}_${System.currentTimeMillis()}")
                }
                onStashSurpriseNote(picked)
              }
              .testTag("draw_surprise_chitthi_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PastelTerracotta,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "+ Draw",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("chitthi_dabba_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(vertical = 6.dp)
        ) {
          if (filteredNotes.isEmpty()) {
            item(key = "empty_chitthi_dabba") {
              Surface(
                shape = RoundedCornerShape(22.dp),
                color = PastelHoney.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, PastelHoney.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(22.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  MaaAvatar(size = 44.dp)
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = if (plainEnglish) "No saved notes yet" else "Your tin dabba is waiting for its first Chitthi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = if (plainEnglish) {
                      "Tap the bookmark icon on any note from Mom or Dad—or tap '+ Draw' above for a practical money tip."
                    } else {
                      "Tap the bookmark icon on any sticky note from Maa or Papa—or tap '+ Draw' above for a fresh piece of household wisdom."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(14.dp))
                  val ctaContent = PastelTerracotta.contrastingContentColor()
                  Button(
                    onClick = {
                      val seed = ChitthiWisdomBank.pickMaaSeed(Random.nextInt(), plainEnglish)
                      onStashSurpriseNote(seed.toChitthiNote())
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = PastelTerracotta,
                      contentColor = ctaContent
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                  ) {
                    Text(
                      text = if (plainEnglish) "Draw a Note from Mom" else "Draw a Chitthi from Maa",
                      color = ctaContent,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          } else {
            items(filteredNotes, key = { it.id }) { note ->
              val isPapa = note.author == ChitthiAuthor.PAPA
              val inkColor = if (isPapa) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed
              HandwrittenHomeNote(
                isMintPaper = isPapa,
                tiltDegrees = if (note.id.hashCode() % 2 == 0) -0.5f else 0.5f,
                modifier = Modifier.testTag("stashed_chitthi_${note.id}")
              ) {
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      if (isPapa) PapaAvatar(size = 32.dp) else MaaAvatar(size = 32.dp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = note.title,
                          style = GharHandStyles.stickyNoteTitle.copy(fontSize = 18.sp),
                          color = inkColor,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = "From ${note.author.displayLabel(plainEnglish)} • ${note.tag} • ${DateUtils.formatRelativeDate(note.savedAt)}",
                          style = GharHandStyles.stickyNoteMeta,
                          color = StickyNotePaperColors.InkMuted,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }

                    IconButton(
                      onClick = { onRemoveNote(note) },
                      modifier = Modifier
                        .size(48.dp)
                        .testTag("remove_chitthi_${note.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Chitthi",
                        tint = StickyNotePaperColors.InkMuted,
                        modifier = Modifier.size(19.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = "“${note.body}”",
                    style = GharHandStyles.stickyNoteBody,
                    color = StickyNotePaperColors.InkDark
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
   * Detail dialog opened when clicking on a Chitthi sticky note card on the home/room screen.
   */
@Composable
fun ChitthiDetailDialog(
  seed: WisdomChitthiSeed,
  isStashed: Boolean,
  onToggleStash: () -> Unit,
  onOpenDabba: () -> Unit,
  onAskAuthor: (WisdomChitthiSeed) -> Unit,
  onDismiss: () -> Unit
) {
  val plainEnglish = LocalPlainEnglishMode.current
  val isPapa = seed.author == ChitthiAuthor.PAPA
  val titleInk = if (isPapa) StickyNotePaperColors.InkAccentSage else StickyNotePaperColors.InkAccentRed
  val authorLabel = seed.author.displayLabel(plainEnglish)
  val noteNoun = if (plainEnglish) "note" else "chitthi"

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = JharokhaCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      border = BorderStroke(1.2.dp, PastelHoney.copy(alpha = 0.42f)),
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 480.dp)
        .padding(16.dp)
        .testTag("chitthi_detail_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            if (isPapa) {
              PapaAvatar(size = 40.dp)
            } else {
              MaaAvatar(size = 40.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = seed.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = titleInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "$authorLabel's $noteNoun • ${seed.tag}",
                style = MaterialTheme.typography.bodySmall,
                color = StickyNotePaperColors.InkMuted,
                maxLines = 1
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(48.dp)
              .testTag("close_chitthi_detail")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Note",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isPapa) PastelSage.copy(alpha = 0.12f) else PastelHoney.copy(alpha = 0.18f),
          border = BorderStroke(1.dp, titleInk.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "“${seed.body}”",
            style = GharHandStyles.stickyNoteBody.copy(fontSize = 17.sp),
            color = StickyNotePaperColors.InkDark,
            modifier = Modifier.padding(18.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onToggleStash,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isStashed) PastelSage else titleInk
              ),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("detail_stash_button")
            ) {
              Icon(
                imageVector = if (isStashed) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isStashed) "Saved" else "Save Note",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
            }

            Button(
              onClick = { onAskAuthor(seed) },
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .testTag("detail_ask_author_button")
            ) {
              Text(
                text = if (plainEnglish) "Ask ${GharTerms.papaName(true)}" else "Ask ${seed.author.displayLabel(false)}",
                fontWeight = FontWeight.Bold,
                maxLines = 1
              )
            }
          }

          Button(
            onClick = {
              onDismiss()
              onOpenDabba()
            },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
              .fillMaxWidth()
              .defaultMinSize(minHeight = 48.dp)
              .testTag("detail_open_dabba_button")
          ) {
            Text(
              text = "Open Collection / Dabba",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
