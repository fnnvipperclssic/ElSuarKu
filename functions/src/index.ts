import * as admin from "firebase-admin";
import * as functions from "firebase-functions/v2";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { onCall } from "firebase-functions/v2/https";

admin.initializeApp();

const auth = admin.auth();
const firestore = admin.firestore();

// ═══════════════════════════════════════════════════════════════════════
// #007: setUserRole — Callable Function for Role Management
// ═══════════════════════════════════════════════════════════════════════
//
// Sets custom claims for a user. Only callable by admins (verified via
// custom claims on the caller's token). Custom claims propagate to the
// user's ID token on next refresh — no need to force sign-out.
//
// Usage from Android:
//   val functions = FirebaseFunctions.getInstance()
//   functions.getHttpsCallable("setUserRole")
//       .call(mapOf("uid" to targetUid, "role" to "MONITOR"))
//       .continueWith { ... }
//

interface SetUserRoleData {
  uid: string;
  role: "ADMIN" | "MONITOR" | "VOTER";
}

interface SetUserRoleResult {
  success: boolean;
  uid: string;
  role: string;
}

export const setUserRole = onCall<SetUserRoleData, Promise<SetUserRoleResult>>(
  {
    region: "asia-southeast2",
    enforceAppCheck: true, // Only verified app instances can call this
  },
  async (request) => {
    const { uid, role } = request.data;
    const callerUid = request.auth?.uid;

    // ── Authorization: only admins can change roles ──
    if (!callerUid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "You must be logged in to manage roles."
      );
    }

    const callerToken = request.auth?.token;
    const callerRole = callerToken?.role;

    if (callerRole !== "ADMIN") {
      throw new functions.https.HttpsError(
        "permission-denied",
        `Only admins can change roles. Caller role: ${callerRole ?? "none"}`
      );
    }

    if (!uid || !role) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Both 'uid' and 'role' are required."
      );
    }

    const validRoles = ["ADMIN", "MONITOR", "VOTER"];
    if (!validRoles.includes(role)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        `Invalid role '${role}'. Must be one of: ${validRoles.join(", ")}`
      );
    }

    // ── Set custom claims ──
    try {
      await auth.setCustomUserClaims(uid, { role });

      // ── Update Firestore user document ──
      const userRef = firestore.collection("users").doc(uid);
      const userDoc = await userRef.get();

      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          `User document not found for uid: ${uid}`
        );
      }

      await userRef.update({
        role: role,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // ── Audit log ──
      await firestore.collection("audit_logs").add({
        actorId: callerUid,
        actorRole: "ADMIN",
        action: role === "ADMIN"
          ? "ROLE_CHANGED_TO_ADMIN"
          : role === "MONITOR"
            ? "ROLE_CHANGED_TO_MONITOR"
            : "ROLE_CHANGED_TO_VOTER",
        target: uid,
        detail: `Role changed to ${role} by admin ${callerUid}`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`✅ Role set: uid=${uid} role=${role} (by admin ${callerUid})`);

      return { success: true, uid, role };
    } catch (error) {
      console.error("❌ setUserRole failed:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to set user role.",
        error instanceof Error ? error.message : String(error)
      );
    }
  }
);

// ═══════════════════════════════════════════════════════════════════════
// #207: onUserRoleChanged — Firestore Trigger for Role Sync
// ═══════════════════════════════════════════════════════════════════════
//
// Automatically syncs Firestore user.role → Firebase Auth custom claims.
// Triggered when a user document's `role` field changes. Ensures custom
// claims are always consistent with Firestore — even if setUserRole wasn't
// used (e.g., direct Firestore console edit, batch import).
//
// Also triggers token refresh notification so the client picks up new claims
// immediately on next `getIdToken(true)` call.
//

export const onUserRoleChanged = onDocumentWritten(
  {
    document: "users/{userId}",
    region: "asia-southeast2",
  },
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();

    if (!beforeData || !afterData) {
      console.log("Skipping: document created or deleted (not modified)");
      return;
    }

    const beforeRole = beforeData.role;
    const afterRole = afterData.role;
    const userId = event.params.userId;

    // Only react to role changes
    if (beforeRole === afterRole) {
      console.log(`No role change for user ${userId}: ${beforeRole}`);
      return;
    }

    console.log(
      `🔄 Role change detected: uid=${userId} ${beforeRole} → ${afterRole}`
    );

    try {
      // Set custom claims to match Firestore
      await auth.setCustomUserClaims(userId, { role: afterRole });

      // Mark user for token refresh (next getIdToken(true) returns fresh claims)
      await firestore.collection("users").doc(userId).update({
        claimsUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Audit log the sync
      await firestore.collection("audit_logs").add({
        actorId: "system",
        actorRole: "SYSTEM",
        action: "CLAIMS_SYNCED",
        target: userId,
        detail: `Custom claims synced: role ${beforeRole} → ${afterRole}`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      console.log(`✅ Custom claims synced for uid=${userId} → role=${afterRole}`);
    } catch (error) {
      console.error(`❌ Failed to sync custom claims for uid=${userId}:`, error);
      // Don't throw — Firestore trigger failures are retried, but we don't want
      // to block the document write
    }
  }
);

// ═══════════════════════════════════════════════════════════════════════
// #379: checkExpiredElections — Scheduled Auto-Close
// ═══════════════════════════════════════════════════════════════════════
//
// Runs every 15 minutes. Finds elections where:
//   - status == "ACTIVE"
//   - endDate < now (timestamp in milliseconds)
// And atomically closes them:
//   1. Update status to "COMPLETED"
//   2. Set closedAt timestamp
//   3. Write audit log
//
// Schedule: "*/15 * * * *" = at minutes 0, 15, 30, 45 of every hour.
//

export const checkExpiredElections = onSchedule(
  {
    schedule: "*/15 * * * *",
    region: "asia-southeast2",
    timeZone: "Asia/Jakarta",
    retryCount: 3,
    minBackoffSeconds: 60,
  },
  async (event) => {
    const now = Date.now();
    console.log(`⏰ checkExpiredElections: running at ${new Date(now).toISOString()}`);

    try {
      // Query: elections that are ACTIVE but have passed their end date
      const snapshot = await firestore
        .collection("elections")
        .where("status", "==", "ACTIVE")
        .where("endDate", "<=", now)
        .get();

      if (snapshot.empty) {
        console.log("No expired elections found.");
        return;
      }

      console.log(`Found ${snapshot.size} expired election(s) to close.`);

      const batch = firestore.batch();
      const auditEntries: Array<Record<string, unknown>> = [];
      const closedElectionIds: string[] = [];

      snapshot.forEach((doc) => {
        const election = doc.data();
        const electionRef = firestore.collection("elections").doc(doc.id);

        batch.update(electionRef, {
          status: "COMPLETED",
          closedAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        closedElectionIds.push(doc.id);

        auditEntries.push({
          actorId: "system",
          actorRole: "SYSTEM",
          action: "ELECTION_AUTO_CLOSED",
          target: doc.id,
          targetName: election.name ?? doc.id,
          detail: `Election auto-closed at ${new Date(now).toISOString()} (endDate was ${new Date(election.endDate).toISOString()})`,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });
      });

      // Atomic batch: all status updates in one write
      await batch.commit();
      console.log(`✅ Closed ${snapshot.size} elections: ${closedElectionIds.join(", ")}`);

      // Write audit logs (separate batch — audit_logs collection is independent)
      const auditBatch = firestore.batch();
      for (const entry of auditEntries) {
        const ref = firestore.collection("audit_logs").doc();
        auditBatch.set(ref, entry);
      }
      await auditBatch.commit();
      console.log(`✅ Audit logs written for ${auditEntries.length} auto-closures.`);
    } catch (error) {
      console.error("❌ checkExpiredElections failed:", error);
      throw error; // Re-throw so Cloud Functions retries
    }
  }
);

// ═══════════════════════════════════════════════════════════════════════
// #380: notifyElectionStatusChange — Notify clients on election close
// ═══════════════════════════════════════════════════════════════════════
//
// When an election's status changes (e.g., ACTIVE → COMPLETED), this
// trigger updates dependent data and logs the event. Primarily triggered
// by checkExpiredElections (auto-close), but also fires for manual closes
// from the admin dashboard.
//

export const onElectionStatusChanged = onDocumentWritten(
  {
    document: "elections/{electionId}",
    region: "asia-southeast2",
  },
  async (event) => {
    const beforeData = event.data?.before?.data();
    const afterData = event.data?.after?.data();

    if (!beforeData || !afterData) {
      return; // Created or deleted — not a status change
    }

    const beforeStatus = beforeData.status;
    const afterStatus = afterData.status;

    if (beforeStatus === afterStatus) {
      return; // No status change
    }

    const electionId = event.params.electionId;
    const electionName = afterData.name ?? electionId;

    console.log(
      `📢 Election status changed: ${electionId} "${electionName}" ${beforeStatus} → ${afterStatus}`
    );

    // Audit log the status change
    try {
      await firestore.collection("audit_logs").add({
        actorId: "system",
        actorRole: "SYSTEM",
        action: "ELECTION_STATUS_CHANGED",
        target: electionId,
        targetName: electionName,
        detail: `Election "${electionName}" status changed: ${beforeStatus} → ${afterStatus}`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
      console.log(`✅ Status change audit logged for election ${electionId}`);
    } catch (error) {
      console.error(`❌ Failed to log status change for ${electionId}:`, error);
    }
  }
);
