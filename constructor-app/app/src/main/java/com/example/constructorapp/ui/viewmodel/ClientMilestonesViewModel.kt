package com.example.constructorapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.AppDatabase
import com.example.constructorapp.data.local.dao.FinancialsDao
import com.example.constructorapp.data.local.entity.ClientMilestoneEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ClientMilestonesUiState(
    val selectedProjectId: String = "",
    val milestones: List<ClientMilestoneEntity> = emptyList(),
    val totalContractValue: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val overallProgressPct: Int = 0,
    val isRecordPaymentDialogOpen: Boolean = false,
    val selectedMilestoneForPayment: ClientMilestoneEntity? = null,
    val isLoading: Boolean = false
)

class ClientMilestonesViewModel(
    private val financialsDao: FinancialsDao
) : ViewModel() {

    constructor(database: AppDatabase) : this(database.financialsDao)

    private val _selectedProjectId = MutableStateFlow("")
    private val _isRecordPaymentDialogOpen = MutableStateFlow(false)
    private val _selectedMilestoneForPayment = MutableStateFlow<ClientMilestoneEntity?>(null)

    val uiState: StateFlow<ClientMilestonesUiState> = combine(
        financialsDao.getAllMilestones(),
        _selectedProjectId,
        _isRecordPaymentDialogOpen,
        _selectedMilestoneForPayment
    ) { allMilestones, projectId, isPaymentOpen, selectedMilestone ->
        val filtered = if (projectId.isBlank()) {
            allMilestones
        } else {
            allMilestones.filter { it.projectId == projectId }
        }

        val totalExpected = filtered.sumOf { it.expectedAmount }
        val totalCollected = filtered.sumOf { it.collectedAmount }
        val totalPending = (totalExpected - totalCollected).coerceAtLeast(0.0)
        val progress = if (totalExpected > 0.0) {
            ((totalCollected / totalExpected) * 100.0).toInt().coerceIn(0, 100)
        } else 0

        ClientMilestonesUiState(
            selectedProjectId = projectId,
            milestones = filtered,
            totalContractValue = totalExpected,
            totalCollected = totalCollected,
            totalPending = totalPending,
            overallProgressPct = progress,
            isRecordPaymentDialogOpen = isPaymentOpen,
            selectedMilestoneForPayment = selectedMilestone,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClientMilestonesUiState(isLoading = true)
    )

    fun filterByProject(projectId: String) {
        _selectedProjectId.value = projectId
    }

    fun setRecordPaymentDialogOpen(open: Boolean, milestone: ClientMilestoneEntity? = null) {
        _selectedMilestoneForPayment.value = milestone
        _isRecordPaymentDialogOpen.value = open
    }

    fun recordPayment(
        milestoneId: String,
        amount: Double,
        paymentMode: String = "bank_transfer",
        paymentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ) {
        viewModelScope.launch {
            val milestone = uiState.value.milestones.find { it.id == milestoneId }
            if (milestone != null) {
                val newCollected = milestone.collectedAmount + amount
                val newStatus = if (newCollected >= milestone.expectedAmount) "collected" else "pending"
                financialsDao.recordMilestonePayment(
                    id = milestoneId,
                    collectedAmount = newCollected,
                    status = newStatus,
                    paymentDate = paymentDate,
                    paymentMode = paymentMode
                )
            }
            _isRecordPaymentDialogOpen.value = false
            _selectedMilestoneForPayment.value = null
        }
    }

    fun initializeDefaultMilestones(projectId: String, totalBudget: Double) {
        viewModelScope.launch {
            val stages = listOf(
                StageTemplate(1, "Booking Advance", "முன்பதிவு முன்பணம்", 10.0),
                StageTemplate(2, "Foundation Complete", "அடித்தளம் நிறைவு", 15.0),
                StageTemplate(3, "Ground Floor Slab", "தரைத்தள கான்கிரீட்", 20.0),
                StageTemplate(4, "First Floor Slab & Lintel", "முதல் தளம் கான்கிரீட் & லிண்டல்", 20.0),
                StageTemplate(5, "Brickwork & Internal Plastering", "செங்கல் பூச்சு வேலைகள்", 20.0),
                StageTemplate(6, "Flooring, Painting & Handover", "தரை, பெயிண்டிங் & ஒப்படைப்பு", 15.0)
            )

            val entities = stages.map { stage ->
                val expected = (totalBudget * stage.pct) / 100.0
                ClientMilestoneEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    stageNumber = stage.num,
                    stageName = stage.name,
                    stageNameTa = stage.nameTa,
                    pctShare = stage.pct,
                    expectedAmount = expected,
                    collectedAmount = 0.0,
                    status = "upcoming",
                    syncStatus = 1
                )
            }
            financialsDao.upsertMilestones(entities)
        }
    }

    private data class StageTemplate(
        val num: Int,
        val name: String,
        val nameTa: String,
        val pct: Double
    )
}
