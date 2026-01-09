package tech.titans.runwars.views

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.repo.UserRepo
import java.util.Locale

/**
 * ViewModel for RunHistoryScreen
 * Manages run session data and statistics
 */
class RunHistoryViewModel : ViewModel() {

    private val _runSessions = MutableStateFlow<List<RunSession>>(emptyList())
    val runSessions = _runSessions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Statistics
    private val _totalDistance = MutableStateFlow(0.0) // in meters
    val totalDistance = _totalDistance.asStateFlow()

    private val _totalRuns = MutableStateFlow(0)
    val totalRuns = _totalRuns.asStateFlow()

    private val _totalTerritoriesCount = MutableStateFlow(0)
    val totalTerritoriesCount = _totalTerritoriesCount.asStateFlow()

    private val _totalCapturedArea = MutableStateFlow(0.0) // in m²
    val totalCapturedArea = _totalCapturedArea.asStateFlow()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadRunHistory()
    }

    fun loadRunHistory() {
        currentUserId?.let { uid ->
            _isLoading.value = true
            _error.value = null

            UserRepo.getUserWithRunSessions(uid) { user, runSessions, errorMsg ->
                if (errorMsg != null) {
                    _error.value = errorMsg
                    _isLoading.value = false
                    println("❌ RunHistoryViewModel: Error loading runs: $errorMsg")
                    return@getUserWithRunSessions
                }

                // Sort by startTime descending (newest first)
                val sortedRuns = runSessions.sortedByDescending { it.startTime }
                _runSessions.value = sortedRuns

                // Calculate statistics
                _totalRuns.value = sortedRuns.size
                _totalDistance.value = sortedRuns.sumOf { it.distance }
                _totalTerritoriesCount.value = sortedRuns.count { it.hasTerritory() }
                _totalCapturedArea.value = sortedRuns.sumOf { it.capturedArea }

                _isLoading.value = false
                println("✅ RunHistoryViewModel: Loaded ${sortedRuns.size} runs")
            }
        } ?: run {
            _error.value = "User not logged in"
            _isLoading.value = false
        }
    }

    /**
     * Get formatted total distance in km
     */
    fun getTotalDistanceKm(): String {
        return String.format(Locale.US, "%.1f km", _totalDistance.value / 1000)
    }

    /**
     * Get formatted total captured area in hectares
     */
    fun getTotalAreaHectares(): String {
        return String.format(Locale.US, "%.2f ha", _totalCapturedArea.value / 10_000)
    }
}

