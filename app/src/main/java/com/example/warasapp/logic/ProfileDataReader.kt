package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProfileStats(
    val hariAktif: Int,
    val missionOkCount: Int,
    val avgBurnout: Int,
    val totalXp: Int,
    val level: Int,
    val levelTitle: String
)

suspend fun checkAndAwardMissionCompletion(missionType: MissionType) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val progress = when (missionType) {
        MissionType.SLEEP -> getSleepStreakMission()
        MissionType.NO_OVERTIME -> getNoOvertimeMission()
        MissionType.EXERCISE -> getExerciseMission()
    }

    if (progress.percentComplete < 100) return

    //cek apakah minggu ini udah pernah dicatat selesai (biar ga doouble xp)
    val sevenDaysAgo = Timestamp(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
    val existing = firestore.collection("mission_completions")
        .whereEqualTo("userId", currentUserId)
        .whereEqualTo("missionType", missionType.key)
        .whereGreaterThanOrEqualTo("timestamp", sevenDaysAgo)
        .get()
        .await()

    if (existing.documents.isNotEmpty()) return

    firestore.collection("mission_completions").add(
        hashMapOf(
            "userId" to currentUserId,
            "missionType" to missionType.key,
            "xpAwarded" to missionType.xpReward,
            "timestamp" to Timestamp.now()
        )
    ).await()
}

//totalxp sepanjang waktu ga terbatas seminggu
suspend fun getTotalXpAllTime(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0

    val snapshot = firestore.collection("mission_completions")
        .whereEqualTo("userId", currentUserId)
        .get()
        .await()

    return snapshot.documents.sumOf { (it.get("xpAwarded") as? Number)?.toInt() ?: 0 }
}

//total mission sepanjang waktu ga terbatas minggu ini
suspend fun getMissionOkCount(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0

    val snapshot = firestore.collection("mission_completions")
        .whereEqualTo("userId", currentUserId)
        .get()
        .await()

    return snapshot.documents.size
}

//total hari udah check in mood
suspend fun getHariAktif(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 0

    val snapshot = firestore.collection("mood_logs")
        .whereEqualTo("userId", currentUserId)
        .get()
        .await()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return snapshot.documents
        .mapNotNull { it.getTimestamp("timestamp")?.toDate() }
        .map { dateFormat.format(it) }
        .distinct()
        .size
}

//avg skor burnout 7 hari terakhir
suspend fun getAvgBurnoutScore(): Int {
    val trend = getWeeklyTrend()
    return if (trend.isNotEmpty()) trend.map { it.score }.average().toInt() else 0
}

fun levelTitle(level: Int): String {
    return when {
        level < 3 -> "Pemula"
        level < 6 -> "Burnout Warrior"
        level < 10 -> "Mindful Master"
        else -> "Zen Legend"
    }
}

//statistik
suspend fun getProfileStats(): ProfileStats {
    val totalXp = getTotalXpAllTime()
    val (level, _) = xpToLevel(totalXp)

    return ProfileStats(
        hariAktif = getHariAktif(),
        missionOkCount = getMissionOkCount(),
        avgBurnout = getAvgBurnoutScore(),
        totalXp = totalXp,
        level = level,
        levelTitle = levelTitle(level)
    )
}

//settings
suspend fun saveTargetBurnout(target: Int) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    firestore.collection("user_settings").document(currentUserId)
        .set(hashMapOf("targetBurnout" to target), com.google.firebase.firestore.SetOptions.merge())
        .await()
}

suspend fun getTargetBurnout(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return 60

    val doc = firestore.collection("user_settings").document(currentUserId).get().await()
    return (doc.get("targetBurnout") as? Number)?.toInt() ?: 60
}

//ekspor pdf/csv belum