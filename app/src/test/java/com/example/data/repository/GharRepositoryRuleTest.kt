package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.core.model.BudgetProfile
import com.example.core.model.Category
import com.example.core.model.ChatSender
import com.example.core.model.ChitthiAuthor
import com.example.core.model.ChitthiNote
import com.example.core.model.CommitmentType
import com.example.core.model.Direction
import com.example.core.model.GroundingSourceChip
import com.example.core.model.PakkaCommitment
import com.example.core.model.QuickChip
import com.example.core.model.SavingsGoal
import com.example.core.model.StampColorKey
import com.example.core.model.Transaction
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GharRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun householdAndOnboardingName_authenticatedOwner_createsAndUpdatesCleanly() = runBlocking {
    val uid = signInTestUser(HARSHIT_EMAIL)
    val budgetRepo = BudgetRepository(firestore, uid, "Harshit Sharma")

    withTimeout(DEFAULT_TIMEOUT_MS) {
      budgetRepo.saveHouseholdCalibration(
        userName = "Harshit",
        onboardingCompleted = true,
        profile = BudgetProfile(monthlyIncomePaise = 75_000_00L),
        habitLeaks = setOf("CHAI_SNACKS", "FOOD_DELIVERY")
      )
    }

    val saved = withTimeout(FLOW_TIMEOUT_MS) {
      budgetRepo.observeHousehold().first { it != null }
    }
    assertNotNull(saved)
    assertEquals("Harshit", saved?.userName)
    assertEquals(true, saved?.onboardingCompleted)
    assertEquals(75_000_00L, saved?.profile?.monthlyIncomePaise)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      budgetRepo.updateUserName("Harshit Bhai")
    }
    val renamed = withTimeout(DEFAULT_TIMEOUT_MS) { budgetRepo.getHouseholdSync() }
    assertEquals("Harshit Bhai", renamed?.userName)
  }

  @Test
  fun transactionsAndGoals_authenticatedOwner_emitsRealtimeAndSupportsCrudAndUndoBalance() = runBlocking {
    val uid = signInTestUser(HARSHIT_EMAIL)
    val txRepo = TransactionRepository(firestore, uid)
    val goalRepo = GoalRepository(firestore, uid)

    val uniqueId = "tx_${UUID.randomUUID()}"
    val tx = Transaction(
      id = uniqueId,
      amountPaise = 35_00L,
      direction = Direction.DEBIT,
      merchant = "Tapri Ginger Chai",
      category = Category.CHAI_SNACKS
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      txRepo.insert(tx)
    }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      txRepo.observeTransactions().first { list -> list.any { it.id == uniqueId } }
    }
    assertTrue(emitted.any { it.id == uniqueId && it.merchant == "Tapri Ginger Chai" })

    withTimeout(DEFAULT_TIMEOUT_MS) {
      txRepo.update(tx.copy(merchant = "Adrak Chai Stall", amountPaise = 40_00L))
    }
    val updated = withTimeout(DEFAULT_TIMEOUT_MS) { txRepo.getById(uniqueId) }
    assertEquals("Adrak Chai Stall", updated?.merchant)
    assertEquals(40_00L, updated?.amountPaise)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      goalRepo.saveGoal(
        SavingsGoal(
          id = "primary_goal",
          name = "Himachal Trip",
          targetAmountPaise = 40_000_00L,
          currentAmountPaise = 10_000_00L
        )
      )
      goalRepo.addToGullak(500_00L)
    }
    val syncedGoal = withTimeout(DEFAULT_TIMEOUT_MS) { goalRepo.getGoalSync() }
    assertEquals(10_500_00L, syncedGoal.currentAmountPaise)

    // Verify exact Gullak Undo / Dip balance restoration via setGullakBalance
    withTimeout(DEFAULT_TIMEOUT_MS) {
      goalRepo.setGullakBalance(10_000_00L)
    }
    val restoredGoal = withTimeout(DEFAULT_TIMEOUT_MS) { goalRepo.getGoalSync() }
    assertEquals(10_000_00L, restoredGoal.currentAmountPaise)
  }

  @Test
  fun chitthiRepository_authenticatedOwner_stashesUpdatesDeletesAndDeniesCrossUser() = runBlocking {
    val harshitUid = signInTestUser(HARSHIT_EMAIL)
    val chitthiRepo = ChitthiRepository(firestore, harshitUid)

    val noteId = "chitthi_${UUID.randomUUID().toString().take(8)}"
    val maaNote = ChitthiNote(
      id = noteId,
      author = ChitthiAuthor.MAA,
      title = "Maa's Rasoi Coffee Rule",
      body = "Beat coffee in a steel glass at home, beta.",
      tag = "Nostalgic Wit"
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      chitthiRepo.upsert(maaNote)
    }

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      chitthiRepo.observeChitthis().first { list -> list.any { it.id == noteId } }
    }
    assertTrue(observed.any { it.id == noteId && it.author == ChitthiAuthor.MAA })

    // Cross-user Bob must be denied reading Harshit's stashed Chitthis
    signInTestUser(BOB_EMAIL)
    val crossUserChitthiRepo = ChitthiRepository(firestore, harshitUid)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { crossUserChitthiRepo.getChitthisSync() }
      fail("Expected PERMISSION_DENIED when Bob queries Harshit's Chitthis")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }

    // Owner Harshit deletes stashed Chitthi cleanly
    signInTestUser(HARSHIT_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      chitthiRepo.delete(noteId)
    }
    val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) { chitthiRepo.getChitthisSync() }
    assertTrue(afterDelete.none { it.id == noteId })
  }

  @Test
  fun maaChatRepository_authenticatedOwner_appendsAndListsMessages() = runBlocking {
    val uid = signInTestUser(HARSHIT_EMAIL)
    val chatRepo = MaaChatRepository(firestore, uid)

    val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
      chatRepo.appendMessage(ChatSender.USER, "Maa, can I order dinner tonight?")
    }
    assertTrue(msgId.isNotBlank())

    val list = withTimeout(FLOW_TIMEOUT_MS) {
      chatRepo.observeMessages().first { msgs -> msgs.any { it.id == msgId } }
    }
    assertTrue(list.any { it.id == msgId })
  }

  @Test
  fun crossUserAccess_bobReadingHarshitData_failsWithPermissionDenied() = runBlocking {
    val harshitUid = signInTestUser(HARSHIT_EMAIL)
    val harshitTxRepo = TransactionRepository(firestore, harshitUid)
    val txId = "tx_${UUID.randomUUID()}"
    withTimeout(DEFAULT_TIMEOUT_MS) {
      harshitTxRepo.insert(
        Transaction(
          id = txId,
          amountPaise = 120_00L,
          merchant = "Private Khata Entry",
          category = Category.GROCERIES
        )
      )
    }

    signInTestUser(BOB_EMAIL)
    val crossUserRepo = TransactionRepository(firestore, harshitUid)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { crossUserRepo.getById(txId) }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun observeTransactions_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    val harshitUid = signInTestUser(HARSHIT_EMAIL)
    auth.signOut()

    val unauthRepo = TransactionRepository(firestore, harshitUid)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        unauthRepo.observeTransactions().first()
      }
      fail("Expected PERMISSION_DENIED when observing transactions while signed out")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }
  }

  @Test
  fun quickChipRepository_authenticatedOwner_supportsCustomizeReorderAndReset() = runBlocking {
    val uid = signInTestUser(HARSHIT_EMAIL)
    val chipRepo = QuickChipRepository(firestore, uid)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      chipRepo.resetToDefaults()
    }

    val seeded = withTimeout(FLOW_TIMEOUT_MS) {
      chipRepo.observeQuickChips().first { it.size >= QuickChip.defaultChips().size }
    }
    assertEquals(QuickChip.defaultChips().size, seeded.size)

    val customId = "chip_custom_${UUID.randomUUID().toString().take(8)}"
    val customChip = QuickChip(
      id = customId,
      label = "Filter Kaapi",
      merchant = "MTR Corner",
      amountPaise = 45_00L,
      category = Category.CHAI_SNACKS,
      direction = Direction.DEBIT,
      colorKey = StampColorKey.PLUM,
      sortOrder = seeded.size
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      chipRepo.upsert(customChip)
    }

    val afterCreate = withTimeout(FLOW_TIMEOUT_MS) {
      chipRepo.observeQuickChips().first { list -> list.any { it.id == customId } }
    }
    assertTrue(afterCreate.any { it.id == customId && it.label == "Filter Kaapi" })

    withTimeout(DEFAULT_TIMEOUT_MS) {
      chipRepo.upsert(customChip.copy(label = "Strong Kaapi", amountPaise = 50_00L))
    }
    val afterEdit = withTimeout(DEFAULT_TIMEOUT_MS) { chipRepo.getChipsSync() }
    val edited = afterEdit.firstOrNull { it.id == customId }
    assertNotNull(edited)
    assertEquals("Strong Kaapi", edited?.label)
    assertEquals(50_00L, edited?.amountPaise)

    val reorderedList = listOfNotNull(edited) + afterEdit.filterNot { it.id == customId }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      chipRepo.updateSortOrders(reorderedList)
    }
    val afterReorder = withTimeout(DEFAULT_TIMEOUT_MS) { chipRepo.getChipsSync() }
    assertEquals(customId, afterReorder.first().id)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      chipRepo.delete(customId)
    }
    val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) { chipRepo.getChipsSync() }
    assertTrue(afterDelete.none { it.id == customId })
  }

  @Test
  fun pakkaCommitmentRepository_authenticatedOwner_supportsCrudTypesAndDeniesCrossUser() = runBlocking {
    val harshitUid = signInTestUser(HARSHIT_EMAIL)
    val commitmentRepo = PakkaCommitmentRepository(firestore, harshitUid)

    val subId = "pakka_sub_${UUID.randomUUID().toString().take(8)}"
    val recId = "pakka_rec_${UUID.randomUUID().toString().take(8)}"

    val subItem = PakkaCommitment(
      id = subId,
      title = "YouTube Premium Family",
      amountPaise = 299_00L,
      type = CommitmentType.SUBSCRIPTION,
      category = Category.ENTERTAINMENT,
      dueDayOfMonth = 12,
      notes = "Shared family plan",
      isActive = true
    )
    val recItem = PakkaCommitment(
      id = recId,
      title = "Design Retainer",
      amountPaise = 15_000_00L,
      type = CommitmentType.RECEIVABLE,
      category = Category.ALLOWANCE_INCOME,
      dueDayOfMonth = 28,
      notes = "Monthly client invoice",
      isActive = true
    )

    withTimeout(DEFAULT_TIMEOUT_MS) {
      commitmentRepo.upsert(subItem)
      commitmentRepo.upsert(recItem)
    }

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      commitmentRepo.observeCommitments().first { list ->
        list.any { it.id == subId } && list.any { it.id == recId }
      }
    }
    assertTrue(observed.any { it.id == subId && it.type == CommitmentType.SUBSCRIPTION })
    assertTrue(observed.any { it.id == recId && it.type == CommitmentType.RECEIVABLE })

    // Edit amount, dueDay, and pause status
    withTimeout(DEFAULT_TIMEOUT_MS) {
      commitmentRepo.upsert(
        subItem.copy(
          title = "YouTube Premium Annual Split",
          amountPaise = 189_00L,
          dueDayOfMonth = 15,
          isActive = false
        )
      )
    }
    val afterEdit = withTimeout(DEFAULT_TIMEOUT_MS) { commitmentRepo.getCommitmentsSync() }
    val editedSub = afterEdit.firstOrNull { it.id == subId }
    assertNotNull(editedSub)
    assertEquals("YouTube Premium Annual Split", editedSub?.title)
    assertEquals(189_00L, editedSub?.amountPaise)
    assertEquals(15, editedSub?.dueDayOfMonth)
    assertEquals(false, editedSub?.isActive)

    // Cross-user Bob must be denied reading Harshit's commitments
    signInTestUser(BOB_EMAIL)
    val crossUserCommitmentRepo = PakkaCommitmentRepository(firestore, harshitUid)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { crossUserCommitmentRepo.getCommitmentsSync() }
      fail("Expected PERMISSION_DENIED when Bob queries Harshit's Pakke commitments")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }

    // Owner Harshit deletes commitments cleanly
    signInTestUser(HARSHIT_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      commitmentRepo.delete(subId)
      commitmentRepo.delete(recId)
    }
    val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) { commitmentRepo.getCommitmentsSync() }
    assertTrue(afterDelete.none { it.id == subId || it.id == recId })
  }

  @Test
  fun papaChatAndPapaChitthi_authenticatedOwner_appendsGroundedSourcesClearsAndDeniesCrossUser() = runBlocking {
    val harshitUid = signInTestUser(HARSHIT_EMAIL)
    val papaRepo = PapaChatRepository(firestore, harshitUid)
    val chitthiRepo = ChitthiRepository(firestore, harshitUid)

    val userMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
      papaRepo.appendMessage(
        sender = ChatSender.USER,
        text = "Papa, what are safe FD vs PPF options for ₹50,000?"
      )
    }
    assertTrue(userMsgId.isNotBlank())

    val groundedSources = listOf(
      GroundingSourceChip(
        title = "RBI Retail Direct",
        uri = "https://rbiretaildirect.org.in",
        domain = "rbiretaildirect.org.in"
      )
    )
    val replyMsgId = withTimeout(DEFAULT_TIMEOUT_MS) {
      papaRepo.appendMessage(
        sender = ChatSender.PAPA,
        text = "Beta, keep 3 months of fixed bills liquid in a sweep-in FD (~7%) and use PPF (~7.1% tax-free) for long horizons.",
        sources = groundedSources
      )
    }
    assertTrue(replyMsgId.isNotBlank())

    val observedChats = withTimeout(FLOW_TIMEOUT_MS) {
      papaRepo.observeMessages().first { msgs ->
        msgs.any { it.id == userMsgId } && msgs.any { it.id == replyMsgId }
      }
    }
    val observedReply = observedChats.first { it.id == replyMsgId }
    assertEquals(ChatSender.PAPA, observedReply.sender)
    assertEquals(1, observedReply.sources.size)
    assertEquals("rbiretaildirect.org.in", observedReply.sources.first().domain)

    // PAPA-authored Chitthi stashes and reads cleanly
    val papaNoteId = "chitthi_papa_${UUID.randomUUID().toString().take(8)}"
    withTimeout(DEFAULT_TIMEOUT_MS) {
      chitthiRepo.upsert(
        ChitthiNote(
          id = papaNoteId,
          author = ChitthiAuthor.PAPA,
          title = "Papa's Inflation Shield",
          body = "Idle cash at 3% loses to 6% household inflation—put long-term money to work.",
          tag = "Papa's Math"
        )
      )
    }
    val stashedNotes = withTimeout(DEFAULT_TIMEOUT_MS) { chitthiRepo.getChitthisSync() }
    assertTrue(stashedNotes.any { it.id == papaNoteId && it.author == ChitthiAuthor.PAPA })

    // Cross-user Bob must be denied reading Harshit's Papa chats
    signInTestUser(BOB_EMAIL)
    val crossUserPapaRepo = PapaChatRepository(firestore, harshitUid)
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        crossUserPapaRepo.observeMessages().first()
      }
      fail("Expected PERMISSION_DENIED when Bob observes Harshit's Papa chats")
    } catch (e: Exception) {
      val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }

    // Owner clears Papa chat history and deletes Papa Chitthi cleanly
    signInTestUser(HARSHIT_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      papaRepo.clearHistory()
      chitthiRepo.delete(papaNoteId)
    }
    val afterClear = withTimeout(FLOW_TIMEOUT_MS) {
      papaRepo.observeMessages().first { it.isEmpty() }
    }
    assertTrue(afterClear.isEmpty())
  }

  private companion object {
    const val HARSHIT_EMAIL = "harshit@ghar.test"
    const val BOB_EMAIL = "bob@ghar.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
