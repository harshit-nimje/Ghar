package com.example

import android.content.Context
import androidx.compose.runtime.Stable
import com.example.core.ai.MaaMiniModelManager
import com.example.core.model.CaptureSource
import com.example.core.model.Category
import com.example.core.model.Direction
import com.example.core.model.SavingsGoal
import com.example.core.model.Transaction
import com.example.data.local.LocalHouseholdVault
import com.example.data.preferences.PreferencesRepository
import com.example.data.repository.BudgetRepository
import com.example.data.repository.BudgetStore
import com.example.data.repository.ChitthiRepository
import com.example.data.repository.ChitthiStore
import com.example.data.repository.CloudSyncCoordinator
import com.example.data.repository.CloudSyncedBudgetStore
import com.example.data.repository.CloudSyncedChitthiStore
import com.example.data.repository.CloudSyncedCommitmentStore
import com.example.data.repository.CloudSyncedGoalStore
import com.example.data.repository.CloudSyncedMaaChatStore
import com.example.data.repository.CloudSyncedPapaChatStore
import com.example.data.repository.CloudSyncedQuickChipStore
import com.example.data.repository.CloudSyncedTransactionStore
import com.example.data.repository.CommitmentStore
import com.example.data.repository.GoalRepository
import com.example.data.repository.GoalStore
import com.example.data.repository.LocalBudgetStore
import com.example.data.repository.LocalChitthiStore
import com.example.data.repository.LocalCommitmentStore
import com.example.data.repository.LocalGoalStore
import com.example.data.repository.LocalMaaChatStore
import com.example.data.repository.LocalPapaChatStore
import com.example.data.repository.LocalQuickChipStore
import com.example.data.repository.LocalTransactionStore
import com.example.data.repository.MaaChatRepository
import com.example.data.repository.MaaChatStore
import com.example.data.repository.PakkaCommitmentRepository
import com.example.data.repository.PapaChatRepository
import com.example.data.repository.PapaChatStore
import com.example.data.repository.QuickChipRepository
import com.example.data.repository.QuickChipStore
import com.example.data.repository.TransactionRepository
import com.example.data.repository.TransactionStore
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Application-level container holding shared singletons, a persistent Guest container,
 * and cached authenticated user containers via [forUser].
 */
@Stable
interface RootAppContainer {
  val appScope: CoroutineScope
  val preferencesRepository: PreferencesRepository
  val maaMiniModelManager: MaaMiniModelManager
  val guestContainer: AppContainer
  fun forUser(userId: String, displayName: String? = null): AppContainer
}

/**
 * Session-scoped container exposing instant hot StateFlows and non-cancellable background persistence
 * for both Guest Mode (`isCloudAuthenticated == false`) and Google Cloud Mode (`isCloudAuthenticated == true`).
 */
@Stable
interface AppContainer {
  val userId: String
  val isCloudAuthenticated: Boolean
  val userEmailOrDisplayName: String?
  val appScope: CoroutineScope
  val transactionRepository: TransactionStore
  val budgetRepository: BudgetStore
  val goalRepository: GoalStore
  val maaChatRepository: MaaChatStore
  val papaChatRepository: PapaChatStore
  val quickChipRepository: QuickChipStore
  val chitthiRepository: ChitthiStore
  val commitmentRepository: CommitmentStore
  val preferencesRepository: PreferencesRepository
  val maaMiniModelManager: MaaMiniModelManager
  val isInitialSyncReady: StateFlow<Boolean>
  val isOnboardingCompletedLocally: Boolean
  fun seedDefaultDataOnce()
}

class DefaultRootAppContainer(context: Context) : RootAppContainer {
  private val appContext: Context = context.applicationContext

  override val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val firestore: FirebaseFirestore by lazy {
    val databaseId = appContext.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(databaseId)
  }

  override val preferencesRepository: PreferencesRepository by lazy {
    PreferencesRepository(appContext)
  }

  override val maaMiniModelManager: MaaMiniModelManager by lazy {
    MaaMiniModelManager(appContext)
  }

  internal val guestVault: LocalHouseholdVault by lazy {
    LocalHouseholdVault(appContext, vaultKey = "guest")
  }

  override val guestContainer: AppContainer by lazy {
    GuestAppContainer(
      guestVault = guestVault,
      preferencesRepository = preferencesRepository,
      maaMiniModelManager = maaMiniModelManager,
      appScope = appScope
    )
  }

  private val userContainers = ConcurrentHashMap<String, UserScopedAppContainer>()

  override fun forUser(userId: String, displayName: String?): AppContainer {
    return userContainers.computeIfAbsent(userId) { uid ->
      val userVault = LocalHouseholdVault(
        context = appContext,
        vaultKey = "user_$uid",
        defaultDisplayName = displayName
      )
      UserScopedAppContainer(
        userId = uid,
        userEmailOrDisplayName = displayName,
        firestore = firestore,
        userVault = userVault,
        guestVault = guestVault,
        preferencesRepository = preferencesRepository,
        maaMiniModelManager = maaMiniModelManager,
        appScope = appScope
      )
    }
  }
}

class GuestAppContainer(
  private val guestVault: LocalHouseholdVault,
  override val preferencesRepository: PreferencesRepository,
  override val maaMiniModelManager: MaaMiniModelManager,
  override val appScope: CoroutineScope
) : AppContainer {
  override val userId: String = "guest"
  override val isCloudAuthenticated: Boolean = false
  override val userEmailOrDisplayName: String? = null

  override val transactionRepository: TransactionStore = LocalTransactionStore(guestVault)
  override val budgetRepository: BudgetStore = LocalBudgetStore(guestVault, appScope, defaultName = "Friend")
  override val goalRepository: GoalStore = LocalGoalStore(guestVault)
  override val maaChatRepository: MaaChatStore = LocalMaaChatStore(guestVault)
  override val papaChatRepository: PapaChatStore = LocalPapaChatStore(guestVault)
  override val quickChipRepository: QuickChipStore = LocalQuickChipStore(guestVault)
  override val chitthiRepository: ChitthiStore = LocalChitthiStore(guestVault)
  override val commitmentRepository: CommitmentStore = LocalCommitmentStore(guestVault)

  override val isInitialSyncReady: StateFlow<Boolean> = MutableStateFlow(true)

  override val isOnboardingCompletedLocally: Boolean
    get() = guestVault.isOnboardingCompleted

  override fun seedDefaultDataOnce() {
    seedVaultOnceIfNeeded(
      vault = guestVault,
      transactionStore = transactionRepository,
      goalStore = goalRepository,
      quickChipStore = quickChipRepository,
      preferencesRepository = preferencesRepository,
      userId = userId,
      appScope = appScope
    )
  }
}

class UserScopedAppContainer(
  override val userId: String,
  override val userEmailOrDisplayName: String?,
  firestore: FirebaseFirestore,
  private val userVault: LocalHouseholdVault,
  guestVault: LocalHouseholdVault,
  override val preferencesRepository: PreferencesRepository,
  override val maaMiniModelManager: MaaMiniModelManager,
  override val appScope: CoroutineScope
) : AppContainer {
  override val isCloudAuthenticated: Boolean = true

  private val defaultFirstName: String = userEmailOrDisplayName
    ?.trim()
    ?.substringBefore(" ")
    ?.takeIf { it.isNotBlank() }
    ?: "Friend"

  private val remoteTx = TransactionRepository(firestore, userId)
  private val remoteBudget = BudgetRepository(firestore, userId, userEmailOrDisplayName)
  private val remoteGoal = GoalRepository(firestore, userId)
  private val remoteChat = MaaChatRepository(firestore, userId)
  private val remotePapaChat = PapaChatRepository(firestore, userId)
  private val remoteQuickChip = QuickChipRepository(firestore, userId)
  private val remoteChitthi = ChitthiRepository(firestore, userId)
  private val remoteCommitment = PakkaCommitmentRepository(firestore, userId)

  override val transactionRepository: TransactionStore = CloudSyncedTransactionStore(userVault, remoteTx, appScope)
  override val budgetRepository: BudgetStore = CloudSyncedBudgetStore(userVault, remoteBudget, appScope, defaultFirstName)
  override val goalRepository: GoalStore = CloudSyncedGoalStore(userVault, remoteGoal, appScope)
  override val maaChatRepository: MaaChatStore = CloudSyncedMaaChatStore(userVault, remoteChat, appScope)
  override val papaChatRepository: PapaChatStore = CloudSyncedPapaChatStore(userVault, remotePapaChat, appScope)
  override val quickChipRepository: QuickChipStore = CloudSyncedQuickChipStore(userVault, remoteQuickChip, appScope)
  override val chitthiRepository: ChitthiStore = CloudSyncedChitthiStore(userVault, remoteChitthi, appScope)
  override val commitmentRepository: CommitmentStore = CloudSyncedCommitmentStore(userVault, remoteCommitment, appScope)

  private val syncCoordinator = CloudSyncCoordinator(
    uid = userId,
    userVault = userVault,
    guestVault = guestVault,
    preferencesRepository = preferencesRepository,
    budgetRepository = remoteBudget,
    transactionRepository = remoteTx,
    goalRepository = remoteGoal,
    quickChipRepository = remoteQuickChip,
    maaChatRepository = remoteChat,
    papaChatRepository = remotePapaChat,
    chitthiRepository = remoteChitthi,
    pakkaCommitmentRepository = remoteCommitment,
    appScope = appScope
  )

  override val isInitialSyncReady: StateFlow<Boolean> = syncCoordinator.initialCloudCheckDone

  override val isOnboardingCompletedLocally: Boolean
    get() = userVault.isOnboardingCompleted

  init {
    syncCoordinator.start()
  }

  override fun seedDefaultDataOnce() {
    seedVaultOnceIfNeeded(
      vault = userVault,
      transactionStore = transactionRepository,
      goalStore = goalRepository,
      quickChipStore = quickChipRepository,
      preferencesRepository = preferencesRepository,
      userId = userId,
      appScope = appScope
    )
  }
}

private fun seedVaultOnceIfNeeded(
  vault: LocalHouseholdVault,
  transactionStore: TransactionStore,
  goalStore: GoalStore,
  quickChipStore: QuickChipStore,
  preferencesRepository: PreferencesRepository,
  userId: String,
  appScope: CoroutineScope
) {
  appScope.launch {
    if (!preferencesRepository.isQuickChipsSeeded(userId)) {
      quickChipStore.insertDefaultsIfEmpty()
      preferencesRepository.markQuickChipsSeeded(userId)
    }

    if (vault.isStarterSeeded || vault.transactions.value.isNotEmpty()) {
      vault.markStarterSeeded()
      return@launch
    }

    val now = System.currentTimeMillis()
    val hour = 3600 * 1000L
    val day = 24 * hour

    val starterTransactions = listOf(
      Transaction(
        id = "seed_chai_1",
        amountPaise = 25_00L,
        direction = Direction.DEBIT,
        merchant = "Sharma Tea Corner",
        category = Category.CHAI_SNACKS,
        timestamp = now - (2 * hour),
        source = CaptureSource.SEED
      ),
      Transaction(
        id = "seed_chai_2",
        amountPaise = 40_00L,
        direction = Direction.DEBIT,
        merchant = "Chai Point",
        category = Category.CHAI_SNACKS,
        timestamp = now - (1 * day + 3 * hour),
        source = CaptureSource.SEED
      ),
      Transaction(
        id = "seed_chai_3",
        amountPaise = 30_00L,
        direction = Direction.DEBIT,
        merchant = "Tapri Special Chai",
        category = Category.CHAI_SNACKS,
        timestamp = now - (2 * day + 5 * hour),
        source = CaptureSource.SEED
      ),
      Transaction(
        id = "seed_sabzi_1",
        amountPaise = 450_00L,
        direction = Direction.DEBIT,
        merchant = "Blinkit Daily",
        category = Category.GROCERIES,
        timestamp = now - (3 * day + 2 * hour),
        source = CaptureSource.SEED
      ),
      Transaction(
        id = "seed_tiffin_1",
        amountPaise = 280_00L,
        direction = Direction.DEBIT,
        merchant = "Swiggy Bowl Company",
        category = Category.FOOD_DELIVERY,
        timestamp = now - (4 * day + 1 * hour),
        source = CaptureSource.SEED
      )
    )
    transactionStore.insertAll(starterTransactions, markStarterSeeded = true)

    val currentGoal = goalStore.primaryGoalState.value
    if (currentGoal.targetAmountPaise == 0L && currentGoal.currentAmountPaise == 0L) {
      goalStore.saveGoal(
        SavingsGoal(
          id = "primary_goal",
          name = "Himachal Trip",
          targetAmountPaise = 40_000_00L,
          currentAmountPaise = 0L
        )
      )
    }
  }
}
