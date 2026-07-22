package com.example.warasapp.logic

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MissionType(val key: String, val title: String, val xpReward: Int, val target: Int) {
    SLEEP("tidur_7jam", "Tidur 7+ Jam", 50, 3),
    NO_OVERTIME("tanpa_lembur", "Tanpa Lembur", 80, 5),
    EXERCISE("olahraga", "Olahraga Rutin", 60, 4)
}

data class MissionProgress(
    val title: String,
    val xpReward: Int,
    val currentProgress: Int,
    val targetProgress: Int,
    val percentComplete: Int
)

suspend fun logMissionCheckin(missionType: MissionType): Boolean {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return false

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayKey = dateFormat.format(Date())

    //cek apakah hari ini sudah mission
    val startOfToday = Timestamp(Date(System.currentTimeMillis() - System.currentTimeMillis() % (24L * 60 * 60 * 1000)))
    val existing = firestore.collection("mission_checkins")
        .whereEqualTo("userId", currentUserId)
        .whereEqualTo("missionType", missionType.key)
        .whereGreaterThanOrEqualTo("timestamp", startOfToday)
        .get()
        .await()

    if (existing.documents.isNotEmpty()) {
        return false
    }

    firestore.collection("mission_checkins").add(
        hashMapOf(
            "userId" to currentUserId,
            "missionType" to missionType.key,
            "timestamp" to Timestamp.now()
        )
    ).await()

    return true
}

private suspend fun getCheckinDates(missionType: MissionType, daysBack: Int): List<String> {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

    val since = Timestamp(Date(System.currentTimeMillis() - daysBack.toLong() * 24 * 60 * 60 * 1000))
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val snapshot = firestore.collection("mission_checkins")
        .whereEqualTo("userId", currentUserId)
        .whereEqualTo("missionType", missionType.key)
        .whereGreaterThanOrEqualTo("timestamp", since)
        .get()
        .await()

    return snapshot.documents
        .mapNotNull { it.getTimestamp("timestamp")?.toDate() }
        .map { dateFormat.format(it) }
        .distinct()
        .sorted()
}

suspend fun getSleepStreakMission(): MissionProgress {
    val dates = getCheckinDates(MissionType.SLEEP, daysBack = 7)
    val progress = dates.size.coerceAtMost(MissionType.SLEEP.target)

    return MissionProgress(
        title = MissionType.SLEEP.title,
        xpReward = MissionType.SLEEP.xpReward,
        currentProgress = progress,
        targetProgress = MissionType.SLEEP.target,
        percentComplete = ((progress.toFloat() / MissionType.SLEEP.target) * 100).toInt()
    )
}

suspend fun getNoOvertimeMission(): MissionProgress {
    val dates = getCheckinDates(MissionType.NO_OVERTIME, daysBack = 7)
    val progress = dates.size.coerceAtMost(MissionType.NO_OVERTIME.target)

    return MissionProgress(
        title = MissionType.NO_OVERTIME.title,
        xpReward = MissionType.NO_OVERTIME.xpReward,
        currentProgress = progress,
        targetProgress = MissionType.NO_OVERTIME.target,
        percentComplete = ((progress.toFloat() / MissionType.NO_OVERTIME.target) * 100).toInt()
    )
}

suspend fun getExerciseMission(): MissionProgress {
    val dates = getCheckinDates(MissionType.EXERCISE, daysBack = 7)
    val progress = dates.size.coerceAtMost(MissionType.EXERCISE.target)

    return MissionProgress(
        title = MissionType.EXERCISE.title,
        xpReward = MissionType.EXERCISE.xpReward,
        currentProgress = progress,
        targetProgress = MissionType.EXERCISE.target,
        percentComplete = ((progress.toFloat() / MissionType.EXERCISE.target) * 100).toInt()
    )
}

suspend fun getAllActiveMissions(): List<MissionProgress> {
    return listOf(
        getSleepStreakMission(),
        getNoOvertimeMission(),
        getExerciseMission()
    )
}

//totalxp
suspend fun getTotalXP(): Int {
    return getAllActiveMissions()
        .filter { it.percentComplete >= 100 }
        .sumOf { it.xpReward }
}

fun xpToLevel(totalXp: Int): Pair<Int, Int> {
    var level = 1
    var xpNeeded = 100
    var remainingXp = totalXp
    while (remainingXp >= xpNeeded) {
        remainingXp -= xpNeeded
        level++
        xpNeeded += 100
    }
    return Pair(level, remainingXp)
}