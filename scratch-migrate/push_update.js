const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');

const app = initializeApp({
  credential: applicationDefault(),
  projectId: 'speedroute-c2793'
});

const db = getFirestore(app);

async function run() {
  try {
    console.log("Pushing update notification to all users...");
    await db.collection("app_metadata").doc("version").set({
        latest_version: "1.9",
        minimum_version_code: 0, // Not forcing an update, just showing the notification
        apk_url: "https://github.com/mac20060630/SpeedRoute/releases/latest"
    });
    console.log("Successfully pushed update notification for all users!");
    process.exit(0);
  } catch (e) {
    console.error("Error:", e);
    process.exit(1);
  }
}

run();
