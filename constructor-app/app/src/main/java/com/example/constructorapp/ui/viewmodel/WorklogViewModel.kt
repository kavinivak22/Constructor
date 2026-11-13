package com.example.constructorapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.constructorapp.data.local.entity.WorklogWithDetails
import com.example.constructorapp.data.model.worklog.*
import com.example.constructorapp.data.remote.ContractorDto
import com.example.constructorapp.data.remote.ProjectDto
import com.example.constructorapp.data.remote.ProjectMaterialDto
import com.example.constructorapp.data.remote.SupabaseClient
import com.example.constructorapp.data.repository.ProjectRepository
import com.example.constructorapp.data.repository.WorklogRepository
import com.example.constructorapp.util.ConcreteMixCalculator
import com.example.constructorapp.util.ConstructionActivity
import com.example.constructorapp.util.ImageCompressor
import com.example.constructorapp.util.MixGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WorklogUiState(
    val selectedProjectId: String = "all",
    val selectedDateFilter: String = "",
    val worklogs: List<WorklogWithDetails> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val editingWorklogId: String? = null,
    val activeCreationTab: WorklogCreationTab = WorklogCreationTab.DETAILS,
    val selectedWorklogForModal: WorklogWithDetails? = null,
    val usersMap: Map<String, String> = emptyMap(),
    val contractors: List<ContractorDto> = emptyList(),
    val projectMaterials: List<ProjectMaterialDto> = emptyList(),
    // Draft form state
    val draftTitle: String = "",
    val draftDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val draftLabor: List<WorklogLaborInput> = emptyList(),
    val draftMaterials: List<WorklogMaterialInput> = emptyList(),
    val draftPhotos: List<WorklogPhotoInput> = emptyList(),
    val isSubmitting: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorklogViewModel(
    private val worklogRepository: WorklogRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow("all")
    private val _selectedDateFilter = MutableStateFlow("")
    private val _isCreateDialogOpen = MutableStateFlow(false)
    private val _editingWorklogId = MutableStateFlow<String?>(null)
    private val _activeTab = MutableStateFlow(WorklogCreationTab.DETAILS)
    private val _selectedWorklogForModal = MutableStateFlow<WorklogWithDetails?>(null)
    private val _usersMap = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _contractors = MutableStateFlow<List<ContractorDto>>(emptyList())
    private val _projectMaterials = MutableStateFlow<List<ProjectMaterialDto>>(emptyList())
    private val _draftTitle = MutableStateFlow("")
    private val _draftDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    private val _draftLabor = MutableStateFlow<List<WorklogLaborInput>>(emptyList())
    private val _draftMaterials = MutableStateFlow<List<WorklogMaterialInput>>(emptyList())
    private val _draftPhotos = MutableStateFlow<List<WorklogPhotoInput>>(emptyList())
    private val _isSubmitting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WorklogUiState> = combine(
        _selectedProjectId.flatMapLatest { pId ->
            _selectedDateFilter.flatMapLatest { dFilter ->
                worklogRepository.getWorklogs(pId, dFilter)
            }
        },
        projectRepository.projects,
        _selectedProjectId,
        _selectedDateFilter,
        _isCreateDialogOpen,
        _editingWorklogId,
        _activeTab,
        _selectedWorklogForModal,
        _usersMap,
        _contractors,
        _projectMaterials,
        _draftTitle,
        _draftDate,
        _draftLabor,
        _draftMaterials,
        _draftPhotos,
        _isSubmitting,
        _errorMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val worklogs = args[0] as List<WorklogWithDetails>
        @Suppress("UNCHECKED_CAST")
        val projects = args[1] as List<ProjectDto>
        val pId = args[2] as String
        val dFilter = args[3] as String
        val createOpen = args[4] as Boolean
        val editingId = args[5] as String?
        val tab = args[6] as WorklogCreationTab
        val detailModal = args[7] as WorklogWithDetails?
        @Suppress("UNCHECKED_CAST")
        val users = args[8] as Map<String, String>
        @Suppress("UNCHECKED_CAST")
        val contractorsList = args[9] as List<ContractorDto>
        @Suppress("UNCHECKED_CAST")
        val materialsList = args[10] as List<ProjectMaterialDto>
        val title = args[11] as String
        val date = args[12] as String
        @Suppress("UNCHECKED_CAST")
        val labor = args[13] as List<WorklogLaborInput>
        @Suppress("UNCHECKED_CAST")
        val materials = args[14] as List<WorklogMaterialInput>
        @Suppress("UNCHECKED_CAST")
        val photos = args[15] as List<WorklogPhotoInput>
        val submitting = args[16] as Boolean
        val error = args[17] as String?

        WorklogUiState(
            selectedProjectId = pId,
            selectedDateFilter = dFilter,
            worklogs = worklogs,
            projects = projects,
            isCreateDialogOpen = createOpen,
            editingWorklogId = editingId,
            activeCreationTab = tab,
            selectedWorklogForModal = detailModal,
            usersMap = users,
            contractors = contractorsList,
            projectMaterials = materialsList,
            draftTitle = title,
            draftDate = date,
            draftLabor = labor,
            draftMaterials = materials,
            draftPhotos = photos,
            isSubmitting = submitting,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WorklogUiState(isLoading = true)
    )

    init {
        syncWorklogs()
        loadTeamUsers()
        loadContractors()
    }

    private fun loadTeamUsers() {
        viewModelScope.launch {
            try {
                val usersRes = projectRepository.getCompanyUsers()
                if (usersRes.isSuccess) {
                    val map = usersRes.getOrNull()?.mapNotNull { u ->
                        val id = u.id
                        val name = (u.displayName ?: u.fullName ?: u.email?.substringBefore("@"))?.trim() ?: ""
                        if (id.isNotBlank() && name.isNotBlank()) id to name else null
                    }?.toMap() ?: emptyMap()
                    _usersMap.value = _usersMap.value + map
                }
            } catch (_: Exception) {}
        }
    }

    fun getCreatorName(createdBy: String?): String {
        val id = createdBy?.trim() ?: ""

        // 1. Matched in usersMap (from company team/employees)
        if (id.isNotBlank() && _usersMap.value.containsKey(id)) {
            val name = _usersMap.value[id]
            if (!name.isNullOrBlank()) return name
        }

        // 2. Logged-in current user match or offline user
        val currentUserId = SupabaseClient.userId
        val currentEmail = SupabaseClient.userEmail
        if (id.isBlank() || id == "offline-user" || (currentUserId != null && id == currentUserId)) {
            val formatted = currentEmail?.substringBefore("@")
                ?.replace(".", " ")
                ?.replace("_", " ")
                ?.split(" ")
                ?.filter { it.isNotBlank() }
                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            if (!formatted.isNullOrBlank()) return formatted
            return "You"
        }

        // 3. Email string in createdBy
        if (id.contains("@")) {
            return id.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }

        // 4. Human-readable name string already stored in createdBy
        val isUuid = id.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
        if (!isUuid && id.isNotBlank()) {
            return id
        }

        // 5. Fallback to current user if logged in
        val fallback = currentEmail?.substringBefore("@")
            ?.replace(".", " ")
            ?.replace("_", " ")
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return fallback ?: "Site User"
    }

    fun syncWorklogs(projectId: String? = null) {
        viewModelScope.launch {
            worklogRepository.syncWorklogsFromRemote(projectId)
        }
    }

    fun loadContractors() {
        viewModelScope.launch {
            try {
                val res = projectRepository.getContractors()
                if (res.isSuccess) {
                    _contractors.value = res.getOrNull() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun createContractor(
        name: String,
        category: String?,
        contactPerson: String?,
        phone: String,
        onSuccess: (ContractorDto) -> Unit
    ) {
        viewModelScope.launch {
            val res = projectRepository.createContractor(name, category, contactPerson, phone)
            val contractor = res.getOrNull() ?: ContractorDto(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                trade = category
            )
            if (_contractors.value.none { it.id == contractor.id || it.name.equals(contractor.name, ignoreCase = true) }) {
                _contractors.value = _contractors.value + contractor
            }
            onSuccess(contractor)
        }
    }

    fun loadProjectMaterials(projectId: String) {
        if (projectId.isBlank() || projectId == "all") return
        viewModelScope.launch {
            try {
                val res = projectRepository.getProjectMaterials(projectId)
                if (res.isSuccess) {
                    _projectMaterials.value = res.getOrNull() ?: emptyList()
                }
            } catch (_: Exception) {}
        }
    }

    fun filterByProject(projectId: String) {
        _selectedProjectId.value = projectId
        syncWorklogs(projectId)
        loadProjectMaterials(projectId)
    }

    fun filterByDate(date: String) {
        _selectedDateFilter.value = date
    }

    fun openCreateDialog(initialProjectId: String? = null, initialTitle: String? = null) {
        if (!initialProjectId.isNullOrBlank() && initialProjectId != "all") {
            _selectedProjectId.value = initialProjectId
        }
        _editingWorklogId.value = null
        _draftTitle.value = initialTitle ?: ""
        _draftDate.value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _draftLabor.value = emptyList()
        _draftMaterials.value = emptyList()
        _draftPhotos.value = emptyList()
        _activeTab.value = WorklogCreationTab.DETAILS
        _errorMessage.value = null
        _isCreateDialogOpen.value = true
        loadContractors()
        val currentPId = _selectedProjectId.value
        val effectivePId = if (currentPId.isNotBlank() && currentPId != "all") {
            currentPId
        } else {
            uiState.value.projects.firstOrNull()?.id ?: ""
        }
        if (effectivePId.isNotBlank() && effectivePId != "all") {
            _selectedProjectId.value = effectivePId
            loadProjectMaterials(effectivePId)
        }
    }

    fun openEditDialog(worklogWithDetails: WorklogWithDetails) {
        _editingWorklogId.value = worklogWithDetails.worklog.id
        _selectedProjectId.value = worklogWithDetails.worklog.projectId
        _draftTitle.value = worklogWithDetails.worklog.title
        _draftDate.value = worklogWithDetails.worklog.date.substringBefore("T")
        _draftLabor.value = worklogWithDetails.laborEntries.map { l ->
            WorklogLaborInput(
                id = l.laborEntry.id,
                contractorName = l.laborEntry.contractorName,
                category = l.laborEntry.category ?: "Masonry",
                workType = l.laborEntry.workType,
                payoutClass = if (l.laborEntry.payoutClass.equals("rate", ignoreCase = true) || l.laborEntry.workType.equals("rate", ignoreCase = true)) "Rate" else "NMR",
                workDescription = l.laborEntry.workDescription ?: "",
                paymentStatus = l.laborEntry.paymentStatus,
                workDoneQuantity = l.laborEntry.workDoneQuantity,
                workDoneUnit = l.laborEntry.workDoneUnit,
                payoutRate = l.laborEntry.payoutRate,
                totalPayout = l.laborEntry.totalPayout,
                workers = l.workerCounts.map { wc ->
                    WorkerCountInput(workerType = wc.workerType, count = wc.count)
                }
            )
        }
        _draftMaterials.value = worklogWithDetails.materials.map { m ->
            WorklogMaterialInput(
                id = m.id,
                projectMaterialId = m.projectMaterialId,
                materialName = m.materialName,
                quantityConsumed = m.quantityConsumed,
                unit = m.unit ?: "Bags"
            )
        }
        _draftPhotos.value = worklogWithDetails.photos.map { p ->
            WorklogPhotoInput(
                id = p.id,
                localFilePath = p.localFilePath,
                remoteUrl = p.remotePhotoUrl,
                caption = p.caption ?: ""
            )
        }
        _activeTab.value = WorklogCreationTab.DETAILS
        _errorMessage.value = null
        _isCreateDialogOpen.value = true
        loadContractors()
        loadProjectMaterials(worklogWithDetails.worklog.projectId)
    }

    fun closeCreateDialog() {
        _editingWorklogId.value = null
        _isCreateDialogOpen.value = false
    }

    fun setCreationTab(tab: WorklogCreationTab) {
        _activeTab.value = tab
    }

    fun updateDraftDetails(title: String, date: String) {
        _draftTitle.value = title
        _draftDate.value = date
    }

    fun addLaborTeam(entry: WorklogLaborInput) {
        _draftLabor.value = _draftLabor.value + entry
    }

    fun updateLaborTeam(index: Int, entry: WorklogLaborInput) {
        _draftLabor.value = _draftLabor.value.mapIndexed { i, old -> if (i == index) entry else old }
    }

    fun removeLaborTeam(index: Int) {
        _draftLabor.value = _draftLabor.value.filterIndexed { i, _ -> i != index }
    }

    fun addMaterial(material: WorklogMaterialInput) {
        _draftMaterials.value = _draftMaterials.value + material
    }

    fun updateMaterial(index: Int, material: WorklogMaterialInput) {
        _draftMaterials.value = _draftMaterials.value.mapIndexed { i, old -> if (i == index) material else old }
    }

    fun removeMaterial(index: Int) {
        _draftMaterials.value = _draftMaterials.value.filterIndexed { i, _ -> i != index }
    }

    fun updatePhotoCaption(index: Int, caption: String) {
        _draftPhotos.value = _draftPhotos.value.mapIndexed { i, old -> if (i == index) old.copy(caption = caption) else old }
    }

    fun applyMixRatio(
        activity: ConstructionActivity,
        mixGrade: MixGrade,
        cementBags: Double,
        customSand: Double = 0.0,
        customGravel: Double = 0.0
    ) {
        val calculated = ConcreteMixCalculator.calculateMaterials(activity, mixGrade, cementBags, customSand, customGravel)
        val newItems = calculated.map {
            WorklogMaterialInput(
                materialName = it.materialName,
                quantityConsumed = it.quantity,
                unit = it.unit
            )
        }
        _draftMaterials.value = _draftMaterials.value + newItems
    }

    fun addPhotos(uris: List<Uri>, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val compressedList = mutableListOf<WorklogPhotoInput>()
            for (uri in uris) {
                try {
                    val file = ImageCompressor.compressImage(context, uri)
                    compressedList.add(
                        WorklogPhotoInput(
                            localUri = uri,
                            localFilePath = file.absolutePath
                        )
                    )
                } catch (_: Exception) {
                    compressedList.add(
                        WorklogPhotoInput(
                            localUri = uri,
                            localFilePath = uri.toString()
                        )
                    )
                }
            }
            _draftPhotos.value = _draftPhotos.value + compressedList
        }
    }

    fun removePhoto(index: Int) {
        _draftPhotos.value = _draftPhotos.value.filterIndexed { i, _ -> i != index }
    }

    fun submitWorklog(context: Context, onSuccess: () -> Unit = {}) {
        val state = uiState.value
        val projectId = if (state.selectedProjectId == "all" || state.selectedProjectId.isBlank()) {
            state.projects.firstOrNull()?.id ?: ""
        } else {
            state.selectedProjectId
        }

        if (projectId.isBlank() || state.draftTitle.isBlank()) {
            _errorMessage.value = "Project and Title are required."
            return
        }

        _isSubmitting.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val req = CreateWorklogRequest(
                projectId = projectId,
                title = state.draftTitle.trim(),
                date = state.draftDate,
                labor = state.draftLabor,
                materials = state.draftMaterials,
                photos = state.draftPhotos
            )
            val editingId = state.editingWorklogId
            val result = if (editingId != null) {
                worklogRepository.updateWorklog(editingId, req, context)
            } else {
                worklogRepository.createWorklog(req, context)
            }
            _isSubmitting.value = false
            if (result.isSuccess) {
                // Deduct material consumption from inventory & record in material stock history log
                for (mat in req.materials) {
                    val matId = mat.projectMaterialId
                    if (!matId.isNullOrBlank() && mat.quantityConsumed > 0) {
                        try {
                            val invItem = state.projectMaterials.find { it.id == matId }
                            val currentQty = invItem?.effectiveQuantity ?: 0.0
                            val newStock = (currentQty - mat.quantityConsumed).coerceAtLeast(0.0)

                            // 1. Update stock in inventory
                            projectRepository.updateMaterialStock(
                                materialId = matId,
                                newStock = newStock
                            )

                            // 2. Add entry to material stock history log
                            projectRepository.createMaterialLog(
                                materialId = matId,
                                changeAmount = -mat.quantityConsumed,
                                purpose = "Consumed in Daily Worklog: ${req.title} (${req.date})"
                            )
                        } catch (_: Exception) {}
                    }
                }
                loadProjectMaterials(projectId)

                closeCreateDialog()
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save worklog"
            }
        }
    }

    fun deleteWorklog(worklogId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            worklogRepository.deleteWorklog(worklogId)
            closeDetailModal()
            onDone()
        }
    }

    fun openDetailModal(worklog: WorklogWithDetails) {
        _selectedWorklogForModal.value = worklog
    }

    fun closeDetailModal() {
        _selectedWorklogForModal.value = null
    }
}
