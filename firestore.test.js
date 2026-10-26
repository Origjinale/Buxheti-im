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

// Security bounds tests
test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: cannot read another user's transactions", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: can create and read own transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_001");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      amount: 45.5,
      type: "EXPENSE",
      category: "food",
      note: "Dreka në restorant",
      date: "2026-10-25",
      paymentMethod: "CARD",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(txRef.get());
});

test("Authenticated user: cannot create transaction with invalid type", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_002");
  await assertFails(
    txRef.set({
      userId: ALICE_UID,
      amount: 45.5,
      type: "INVALID_TYPE",
      category: "food",
      date: "2026-10-25",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create transaction for another user", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const txRef = bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_003");
  await assertFails(
    txRef.set({
      userId: BOB_UID,
      amount: 10,
      type: "EXPENSE",
      category: "food",
      date: "2026-10-25",
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: can create and update budget goal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const budgetRef = aliceDb.collection("users").doc(ALICE_UID).collection("budget").doc("current");
  await assertSucceeds(
    budgetRef.set({
      userId: ALICE_UID,
      monthlyBudget: 600,
      dailyBudget: 20,
      currency: "EUR",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(
    budgetRef.update({
      monthlyBudget: 750,
      updatedAt: new Date(),
    })
  );
});
