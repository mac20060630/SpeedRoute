const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');

const sourceApp = initializeApp({
  credential: applicationDefault(),
  projectId: 'triprank-in'
}, 'source');

const destApp = initializeApp({
  credential: applicationDefault(),
  projectId: 'speedroute-c2793'
}, 'dest');

const sourceDb = getFirestore(sourceApp);
const destDb = getFirestore(destApp);

async function migrateCollection(collectionName) {
  console.log(`Migrating collection: ${collectionName}`);
  const snapshot = await sourceDb.collection(collectionName).get();
  let count = 0;
  for (const doc of snapshot.docs) {
    await destDb.collection(collectionName).doc(doc.id).set(doc.data());
    count++;
  }
  console.log(`Migrated ${count} documents for collection ${collectionName}`);
}

async function run() {
  try {
    const collections = await sourceDb.listCollections();
    for (const collection of collections) {
      await migrateCollection(collection.id);
    }
    console.log("Migration complete!");
    process.exit(0);
  } catch (error) {
    console.error("Migration failed:", error);
    process.exit(1);
  }
}

run();
