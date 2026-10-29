const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 500);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validHouseholdPayload(uid, ts) {
  return {
    userId: uid,
    userName: "Harshit",
    onboardingCompleted: true,
    monthlyIncomePaise: 6000000,
    fixedCommitmentsPaise: 2400000,
    savingsTargetPaise: 1200000,
    monthStartDay: 1,
    guiltSensitivity: "MEDIUM",
    habitType: "CHAI_SNACKS",
    habitLeaks: ["CHAI_SNACKS", "FOOD_DELIVERY"],
    painDial: "BALANCED",
    ageBand: "25-34",
    lifeStage: "Salaried",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validTransactionPayload(uid, ts) {
  return {
    userId: uid,
    amountPaise: 2500,
    direction: "DEBIT",
    merchant: "Sharma Tea Corner",
    category: "CHAI_SNACKS",
    timestamp: 1730000000000,
    source: "MANUAL",
    isRecurring: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validGoalPayload(uid, ts) {
  return {
    userId: uid,
    name: "Himachal Trip",
    targetAmountPaise: 4000000,
    currentAmountPaise: 1650000,
    instrumentType: "SIP",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: cannot read or write household or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});

test("Authenticated owner: can create, read, and update their household profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validHouseholdPayload(ALICE_UID, ts))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userName: "Harshit Ji",
      updatedAt: serverTimestamp(),
    })
  );
  // Owner can check existence of not-yet-created subcollection documents (e.g., primary_goal)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("goals").doc("primary_goal").get()
  );
});

test("Shadow Update & Value Poisoning: rejects ghost fields and invalid types on household", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validHouseholdPayload(ALICE_UID, ts))
  );

  // Shadow update with ghost field isVerified
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userName: "Harshit",
      isVerified: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Value poisoning on allowlisted key userName (empty string & wrong type)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userName: "",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userName: 99999,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's household, transactions, or goals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validHouseholdPayload(ALICE_UID, ts))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set(validTransactionPayload(ALICE_UID, ts))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("goals")
      .doc("primary_goal")
      .set(validGoalPayload(ALICE_UID, ts))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("goals").doc("primary_goal").get()
  );
});

test("Client query alignment: Alice can query her transactions and goals with userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = serverTimestamp();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_chai")
      .set(validTransactionPayload(ALICE_UID, ts))
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Identity spoofing on create fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_spoof")
      .set(validTransactionPayload(BOB_UID, ts))
  );
});

test("Maa chat messages: owner can create and list, cannot update, can delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const chatRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("maa_chats")
    .doc("msg_1");

  await assertSucceeds(
    chatRef.set({
      userId: ALICE_UID,
      sender: "USER",
      text: "Maa, can I order dinner tonight?",
      timestamp: 1730000001000,
      createdAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("maa_chats")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Append-only: update must fail
  await assertFails(chatRef.update({ text: "Rewritten!" }));

  // Owner can clear/delete chat message
  await assertSucceeds(chatRef.delete());
});

test("Quick chips: owner can customise, reorder, and delete; rejects ghost fields and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const chipRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("quick_chips")
    .doc("chip_metro");

  // Owner can check existence before creating
  await assertSucceeds(chipRef.get());

  // Owner creates custom expense quick chip
  await assertSucceeds(
    chipRef.set({
      userId: ALICE_UID,
      label: "Metro",
      merchant: "DMRC Smart Card",
      amountPaise: 4500,
      category: "COMMUTE",
      direction: "DEBIT",
      colorKey: "SKY",
      sortOrder: 0,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Owner lists quick chips with userId filter
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quick_chips")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Owner updates chip amount & label
  await assertSucceeds(
    chipRef.update({
      label: "Metro Pass",
      amountPaise: 6000,
      updatedAt: serverTimestamp(),
    })
  );

  // Rejects ghost field injection on update
  await assertFails(
    chipRef.update({
      label: "Metro Pass",
      ghostFlag: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Rejects value poisoning (0 amount or empty label)
  await assertFails(
    chipRef.update({
      amountPaise: 0,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    chipRef.update({
      label: "",
      updatedAt: serverTimestamp(),
    })
  );

  // Bob cannot read or delete Alice's quick chip
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quick_chips")
      .doc("chip_metro")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("quick_chips")
      .doc("chip_metro")
      .delete()
  );

  // Owner deletes quick chip
  await assertSucceeds(chipRef.delete());
});

test("Chitthi notes: owner can stash, list, edit, and unstash; rejects spoofing, ghost fields, and cross-user reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const chitthiRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("chitthis")
    .doc("chitthi_coffee_1");

  await assertSucceeds(chitthiRef.get());

  await assertSucceeds(
    chitthiRef.set({
      userId: ALICE_UID,
      author: "MAA",
      title: "Maa's Rasoi Chitthi",
      body: "Beta, beat coffee with warm water in a steel glass at home instead of ₹280 cafe coffee!",
      tag: "Rasoi Wit",
      savedAt: 1730000500000,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chitthis")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Valid update
  await assertSucceeds(
    chitthiRef.update({
      tag: "Favourite Note",
      updatedAt: serverTimestamp(),
    })
  );

  // Reject ghost field
  await assertFails(
    chitthiRef.update({
      tag: "Favourite Note",
      adminPin: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Reject empty body value poisoning
  await assertFails(
    chitthiRef.update({
      body: "",
      updatedAt: serverTimestamp(),
    })
  );

  // Cross-user Bob cannot read or delete Alice's stashed chitthi
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chitthis")
      .doc("chitthi_coffee_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("chitthis")
      .doc("chitthi_coffee_1")
      .delete()
  );

  await assertSucceeds(chitthiRef.delete());
});

test("Pakke commitments: owner can create, list, edit, pause, and delete bills/subscriptions/receivables; rejects spoofing, ghost fields, invalid bounds, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const billRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("pakke_commitments")
    .doc("pakka_electricity");
  const receivableRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("pakke_commitments")
    .doc("pakka_retainer");

  // Unauthenticated caller rejected
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pakke_commitments")
      .get()
  );

  // Owner checks non-existent doc before upsert
  await assertSucceeds(billRef.get());

  // Owner creates BILL commitment
  await assertSucceeds(
    billRef.set({
      userId: ALICE_UID,
      title: "Electricity Bill",
      amountPaise: 145000,
      type: "BILL",
      category: "BILLS_UTILITIES",
      dueDayOfMonth: 5,
      notes: "BESCOM consumer ID 88412",
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Owner creates RECEIVABLE commitment
  await assertSucceeds(
    receivableRef.set({
      userId: ALICE_UID,
      title: "Flatmate Wi-Fi Share",
      amountPaise: 45000,
      type: "RECEIVABLE",
      category: "ALLOWANCE_INCOME",
      dueDayOfMonth: 10,
      notes: "",
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Aligned list query with userId filter succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pakke_commitments")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Update amount, type, and dueDayOfMonth succeeds
  await assertSucceeds(
    billRef.update({
      title: "BESCOM Power Bill",
      amountPaise: 162000,
      type: "BILL",
      category: "BILLS_UTILITIES",
      dueDayOfMonth: 7,
      notes: "Updated tariff",
      isActive: false,
      updatedAt: serverTimestamp(),
    })
  );

  // Reject ghost field injection on update
  await assertFails(
    billRef.update({
      isActive: true,
      bypassAudit: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Reject value poisoning (dueDayOfMonth out of 1..31, zero amount, invalid type)
  await assertFails(
    billRef.update({
      dueDayOfMonth: 0,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    billRef.update({
      dueDayOfMonth: 32,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    billRef.update({
      amountPaise: 0,
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    billRef.update({
      type: "LOAN_SHARK",
      updatedAt: serverTimestamp(),
    })
  );

  // Cross-user Bob cannot read, list, or delete Alice's commitments
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pakke_commitments")
      .doc("pakka_electricity")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pakke_commitments")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("pakke_commitments")
      .doc("pakka_electricity")
      .delete()
  );

  // Owner deletes commitments cleanly
  await assertSucceeds(billRef.delete());
  await assertSucceeds(receivableRef.delete());
});

test("Papa chats & PAPA chitthis: owner can append grounded messages, list, and clear; rejects updates, spoofing, and cross-user reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const userMsgRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("papa_chats")
    .doc("papa_msg_user_1");

  const papaReplyRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("papa_chats")
    .doc("papa_msg_reply_1");

  // Owner creates USER message without optional sourcesJson
  await assertSucceeds(
    userMsgRef.set({
      userId: ALICE_UID,
      sender: "USER",
      text: "Papa, how should I invest ₹50,000 for 5 years?",
      timestamp: 1730002000000,
      createdAt: serverTimestamp(),
    })
  );

  // Owner creates PAPA reply with GroundingSourceChip JSON payload
  await assertSucceeds(
    papaReplyRef.set({
      userId: ALICE_UID,
      sender: "PAPA",
      text: "Beta, split 40% into a sweep-in FD (~7%) and 40% into a Nifty 50 index fund.",
      timestamp: 1730002005000,
      sourcesJson: JSON.stringify([
        { title: "RBI Monetary Policy", uri: "https://rbi.org.in", domain: "rbi.org.in" },
      ]),
      createdAt: serverTimestamp(),
    })
  );

  // Aligned list query succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("papa_chats")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Chat messages are immutable (updates rejected)
  await assertFails(
    papaReplyRef.update({
      text: "Tampered advice",
    })
  );

  // Rejects invalid sender or ghost field injection
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("papa_chats")
      .doc("papa_msg_invalid_sender")
      .set({
        userId: ALICE_UID,
        sender: "MAA",
        text: "Wrong room sender",
        timestamp: 1730002010000,
        createdAt: serverTimestamp(),
      })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("papa_chats")
      .doc("papa_msg_ghost")
      .set({
        userId: ALICE_UID,
        sender: "PAPA",
        text: "Ghost field test",
        timestamp: 1730002015000,
        isAdmin: true,
        createdAt: serverTimestamp(),
      })
  );

  // Cross-user Bob cannot read, list, or delete Alice's Papa chats
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("papa_chats")
      .doc("papa_msg_reply_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("papa_chats")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("papa_chats")
      .doc("papa_msg_reply_1")
      .delete()
  );

  // PAPA-authored Chitthi note succeeds in /chitthis
  const papaChitthiRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("chitthis")
    .doc("chitthi_papa_1");
  await assertSucceeds(
    papaChitthiRef.set({
      userId: ALICE_UID,
      author: "PAPA",
      title: "Papa's Day-1 Rule",
      body: "Pay your Gullak on Day 1 before discretionary spending starts.",
      tag: "Papa's Gullak",
      savedAt: 1730002020000,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Owner deletes Papa chat messages and Chitthi cleanly
  await assertSucceeds(userMsgRef.delete());
  await assertSucceeds(papaReplyRef.delete());
  await assertSucceeds(papaChitthiRef.delete());
});
