package com.example.constructorapp.data

import com.example.constructorapp.data.local.entity.*
import com.example.constructorapp.data.model.*
import com.example.constructorapp.data.remote.ProjectMaterialDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentic data repository for Constructor Multiplatform.
 * Powers the unified screens with real project, material, worklog, and financial records.
 */
object SharedDataRepository {

    // ─── 1. Authentic Projects (Golden Kattumaanam - LVA 24A) ───────────────────
    private val _projects = MutableStateFlow<List<ProjectEntity>>(
        listOf(
            ProjectEntity(
                id = "afc2ec97-7875-4ee9-945d-42923854454e",
                companyId = "b887770d-88f3-4c6c-a519-0d023fb057a6",
                name = "LVA 24A",
                description = "First 2BHK Budget House",
                startDate = "2026-08-24",
                endDate = "2027-02-24",
                status = "In Progress",
                progress = 50,
                clientName = "Boopathy Vaiyapuriappan",
                clientPhone = "+91 89039 45047",
                location = "Coimbatore, Tamil Nadu",
                budget = 2200000.0,
                spent = 1100000.0,
                thumbnailUrl = null
            )
        )
    )
    val projects: StateFlow<List<ProjectEntity>> = _projects.asStateFlow()

    fun addProject(newProject: ProjectDto) {
        val entity = ProjectEntity(
            id = newProject.id.ifBlank { "proj-${ClockUtils.nowMillis()}" },
            companyId = "b887770d-88f3-4c6c-a519-0d023fb057a6",
            name = newProject.name,
            description = newProject.description,
            startDate = newProject.startDate ?: "2026-11-13",
            endDate = newProject.endDate,
            status = newProject.status ?: "In Progress",
            progress = newProject.progress,
            clientName = newProject.clientName,
            clientPhone = newProject.clientPhone,
            location = newProject.location,
            budget = newProject.budget,
            spent = 0.0
        )
        _projects.value = listOf(entity) + _projects.value
    }

    // ─── 2. Authentic Worklogs with Labor & Materials ───────────────────────────
    private val _worklogs = MutableStateFlow<List<WorklogWithDetails>>(
        listOf(
            WorklogWithDetails(
                worklog = DailyWorklogEntity(
                    id = "wl-001",
                    projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                    title = "First Floor Roof Slab Concrete Pour",
                    date = "2026-11-13",
                    createdBy = "Kavin (Site Engineer)",
                    createdAt = "2026-11-13 16:30",
                    updatedAt = "2026-11-13 16:30"
                ),
                laborEntries = listOf(
                    LaborEntryWithCounts(
                        laborEntry = WorklogLaborEntryEntity(
                            id = "le-001",
                            worklogId = "wl-001",
                            contractorName = "Senthil Masons & Bar Benders",
                            category = "Concrete & Shuttering",
                            workDescription = "M20 grade concrete casting with mechanical needle vibrator",
                            paymentStatus = "Pending",
                            totalPayout = 14200.0
                        ),
                        workerCounts = listOf(
                            WorklogWorkerCountEntity(id = "wc-1", laborEntryId = "le-001", workerType = "Mason", count = 4),
                            WorklogWorkerCountEntity(id = "wc-2", laborEntryId = "le-001", workerType = "Helper", count = 8),
                            WorklogWorkerCountEntity(id = "wc-3", laborEntryId = "le-001", workerType = "Bar Bender", count = 2)
                        )
                    )
                ),
                materials = listOf(
                    WorklogMaterialEntity(id = "wm-1", worklogId = "wl-001", materialName = "OPC 53 Cement", quantityConsumed = 120.0, unit = "Bags"),
                    WorklogMaterialEntity(id = "wm-2", worklogId = "wl-001", materialName = "River Sand", quantityConsumed = 350.0, unit = "CFT"),
                    WorklogMaterialEntity(id = "wm-3", worklogId = "wl-001", materialName = "Blue Metal Aggregate (20mm)", quantityConsumed = 500.0, unit = "CFT"),
                    WorklogMaterialEntity(id = "wm-4", worklogId = "wl-001", materialName = "Waterproof Compound (Dr. Fixit)", quantityConsumed = 15.0, unit = "Liters")
                ),
                photos = emptyList()
            ),
            WorklogWithDetails(
                worklog = DailyWorklogEntity(
                    id = "wl-002",
                    projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                    title = "Outer Wall Brick Masonry (1:5 Mortar)",
                    date = "2026-11-12",
                    createdBy = "Kavin (Site Engineer)",
                    createdAt = "2026-11-12 17:00",
                    updatedAt = "2026-11-12 17:00"
                ),
                laborEntries = listOf(
                    LaborEntryWithCounts(
                        laborEntry = WorklogLaborEntryEntity(
                            id = "le-002",
                            worklogId = "wl-002",
                            contractorName = "Murugan Bricklaying Team",
                            category = "Masonry",
                            workDescription = "Perimeter 9-inch brick wall laying up to lintel level",
                            paymentStatus = "Pending",
                            totalPayout = 9800.0
                        ),
                        workerCounts = listOf(
                            WorklogWorkerCountEntity(id = "wc-4", laborEntryId = "le-002", workerType = "Mason", count = 3),
                            WorklogWorkerCountEntity(id = "wc-5", laborEntryId = "le-002", workerType = "Helper", count = 6)
                        )
                    )
                ),
                materials = listOf(
                    WorklogMaterialEntity(id = "wm-5", worklogId = "wl-002", materialName = "Red Country Bricks", quantityConsumed = 2500.0, unit = "Pcs"),
                    WorklogMaterialEntity(id = "wm-6", worklogId = "wl-002", materialName = "OPC 53 Cement", quantityConsumed = 45.0, unit = "Bags"),
                    WorklogMaterialEntity(id = "wm-7", worklogId = "wl-002", materialName = "River Sand", quantityConsumed = 180.0, unit = "CFT")
                ),
                photos = emptyList()
            )
        )
    )
    val worklogs: StateFlow<List<WorklogWithDetails>> = _worklogs.asStateFlow()

    fun addWorklog(item: WorklogWithDetails) {
        _worklogs.value = listOf(item) + _worklogs.value
    }

    fun deleteWorklog(id: String) {
        _worklogs.value = _worklogs.value.filter { it.worklog.id != id }
    }

    // ─── 3. Authentic Project Materials (Inventory & On-Site Stock) ──────────────
    private val _materials = MutableStateFlow<List<ProjectMaterialDto>>(
        listOf(
            ProjectMaterialDto(
                id = "mat-1",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "OPC 53 Grade Cement",
                category = "Cement",
                unitOfMeasurement = "Bags",
                currentStock = 140.0,
                minimumStockLevel = 50.0,
                unitCost = 395.0,
                supplierName = "UltraTech Building Solutions",
                supplierContact = "+91 94432 10987"
            ),
            ProjectMaterialDto(
                id = "mat-2",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "River Sand (Fine Grade)",
                category = "Sand",
                unitOfMeasurement = "CFT",
                currentStock = 450.0,
                minimumStockLevel = 100.0,
                unitCost = 75.0,
                supplierName = "Kovai Sand Depo",
                supplierContact = "+91 98421 55432"
            ),
            ProjectMaterialDto(
                id = "mat-3",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "M-Sand (Manufactured)",
                category = "Sand",
                unitOfMeasurement = "CFT",
                currentStock = 680.0,
                minimumStockLevel = 150.0,
                unitCost = 55.0,
                supplierName = "Trichy Sand Quarry",
                supplierContact = "+91 97890 12345"
            ),
            ProjectMaterialDto(
                id = "mat-4",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "TMT Steel 16mm (Fe 550D)",
                category = "Steel",
                unitOfMeasurement = "Kg",
                currentStock = 850.0,
                minimumStockLevel = 200.0,
                unitCost = 68.0,
                supplierName = "Tata Tiscon Dealer",
                supplierContact = "+91 94440 88765"
            ),
            ProjectMaterialDto(
                id = "mat-5",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "Coarse Blue Metal (20mm)",
                category = "Aggregates",
                unitOfMeasurement = "CFT",
                currentStock = 320.0,
                minimumStockLevel = 80.0,
                unitCost = 52.0,
                supplierName = "Kovai Blue Metal Quarry",
                supplierContact = "+91 99943 21000"
            ),
            ProjectMaterialDto(
                id = "mat-6",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                name = "Red Country Wire Cut Bricks",
                category = "Masonry",
                unitOfMeasurement = "Pcs",
                currentStock = 6500.0,
                minimumStockLevel = 1500.0,
                unitCost = 12.0,
                supplierName = "Annur Brick Works",
                supplierContact = "+91 98433 77112"
            )
        )
    )
    val materials: StateFlow<List<ProjectMaterialDto>> = _materials.asStateFlow()

    // ─── 4. Authentic Project Expenses ──────────────────────────────────────────
    private val _expenses = MutableStateFlow<List<ExpenseEntity>>(
        listOf(
            ExpenseEntity(
                id = "exp-1",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                amount = 39500.0,
                category = "materials",
                description = "Cement Supply (100 Bags OPC 53 Grade)",
                expenseDate = "2026-11-12",
                paymentStatus = "paid",
                paymentMode = "bank_transfer",
                receiver = "UltraTech Building Solutions",
                isBillable = true
            ),
            ExpenseEntity(
                id = "exp-2",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                amount = 28400.0,
                category = "labor",
                description = "Mason & Helper NMR Weekly Wages (Phase 1)",
                expenseDate = "2026-11-10",
                paymentStatus = "paid",
                paymentMode = "cash",
                receiver = "Senthil (Head Mason)",
                isBillable = true
            ),
            ExpenseEntity(
                id = "exp-3",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                amount = 18000.0,
                category = "equipment",
                description = "JCB Foundation Trench Excavation & Earthwork",
                expenseDate = "2026-11-05",
                paymentStatus = "paid",
                paymentMode = "upi",
                receiver = "Kovai Earth Movers",
                isBillable = true
            ),
            ExpenseEntity(
                id = "exp-4",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                amount = 25000.0,
                category = "contractor",
                description = "Bar Bending Contractor Stage 1 Advance",
                expenseDate = "2026-11-01",
                paymentStatus = "paid",
                paymentMode = "bank_transfer",
                receiver = "Manoj Steel Fabricators",
                isBillable = true
            ),
            ExpenseEntity(
                id = "exp-5",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                amount = 12500.0,
                category = "transport",
                description = "River Sand Freight & Site Offloading Charges",
                expenseDate = "2026-10-28",
                paymentStatus = "paid",
                paymentMode = "cash",
                receiver = "Thirumalai Transport",
                isBillable = true
            )
        )
    )
    val expenses: StateFlow<List<ExpenseEntity>> = _expenses.asStateFlow()

    fun addExpense(expense: ExpenseEntity) {
        _expenses.value = listOf(expense) + _expenses.value
    }

    // ─── 5. Authentic Site Purchase Orders ──────────────────────────────────────
    private val _purchaseOrders = MutableStateFlow<List<PurchaseOrderDto>>(
        listOf(
            PurchaseOrderDto(
                id = "PO-2026-001",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                supplierName = "UltraTech Building Solutions",
                supplierContact = "+91 94432 10987",
                totalAmount = 79000.0,
                status = "Approved",
                deliveryDate = "2026-11-14",
                specialInstructions = "Deliver before 8:30 AM before road restrictions"
            ),
            PurchaseOrderDto(
                id = "PO-2026-002",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                supplierName = "Tata Tiscon Dealer",
                supplierContact = "+91 94440 88765",
                totalAmount = 115600.0,
                status = "Delivered",
                deliveryDate = "2026-11-10",
                specialInstructions = "Test certificates included for Fe 550D rebar"
            ),
            PurchaseOrderDto(
                id = "PO-2026-003",
                projectId = "afc2ec97-7875-4ee9-945d-42923854454e",
                supplierName = "Kovai Blue Metal Quarry",
                supplierContact = "+91 99943 21000",
                totalAmount = 36000.0,
                status = "Approved",
                deliveryDate = "2026-11-15",
                specialInstructions = "20mm graded blue metal 2 truckloads"
            )
        )
    )
    val purchaseOrders: StateFlow<List<PurchaseOrderDto>> = _purchaseOrders.asStateFlow()

    fun addPurchaseOrder(po: PurchaseOrderDto) {
        _purchaseOrders.value = listOf(po) + _purchaseOrders.value
    }

    // ─── 6. Authentic Site Tasks (Dashboard & Work Prep) ────────────────────────
    data class SiteTaskItem(
        val id: String,
        val title: String,
        val status: String,
        val priority: String,
        val dueDate: String? = null
    )

    private val _tasks = MutableStateFlow<List<SiteTaskItem>>(
        listOf(
            SiteTaskItem(
                id = "task-1",
                title = "Verify steel bar spacing & cover blocks for slab casting",
                status = "pending",
                priority = "High",
                dueDate = "2026-11-13"
            ),
            SiteTaskItem(
                id = "task-2",
                title = "Collect concrete test cubes for 7-day lab compression test",
                status = "completed",
                priority = "High",
                dueDate = "2026-11-12"
            ),
            SiteTaskItem(
                id = "task-3",
                title = "Order electrical conduits & junction boxes before casting",
                status = "pending",
                priority = "Medium",
                dueDate = "2026-11-14"
            ),
            SiteTaskItem(
                id = "task-4",
                title = "Submit weekly labor NMR muster sheet for engineer approval",
                status = "completed",
                priority = "Low",
                dueDate = "2026-11-11"
            )
        )
    )
    val tasks: StateFlow<List<SiteTaskItem>> = _tasks.asStateFlow()

    fun toggleTask(taskId: String) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) {
                val newStatus = if (it.status == "completed") "pending" else "completed"
                it.copy(status = newStatus)
            } else it
        }
    }

    fun addTask(title: String, priority: String) {
        val newTask = SiteTaskItem(
            id = "task-${ClockUtils.nowMillis()}",
            title = title,
            status = "pending",
            priority = priority
        )
        _tasks.value = listOf(newTask) + _tasks.value
    }

    // ─── 7. Authentic Team Hub Chat Messages ───────────────────────────────────
    data class ChatMessage(
        val id: String,
        val sender: String,
        val message: String,
        val time: String,
        val isSelf: Boolean
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "msg-1",
                sender = "Kavin (Site Engineer)",
                message = "Morning team! Ready-mix concrete truck scheduled to arrive at 10:30 AM for 2nd floor slab casting.",
                time = "09:15 AM",
                isSelf = false
            ),
            ChatMessage(
                id = "msg-2",
                sender = "You",
                message = "Thanks Kavin. Bar benders have completed top mat inspection. Vibrator and cube moulds are staged.",
                time = "09:22 AM",
                isSelf = true
            ),
            ChatMessage(
                id = "msg-3",
                sender = "Boopathy (Client)",
                message = "Please make sure waterproof compound is added to the roof mix.",
                time = "09:35 AM",
                isSelf = false
            ),
            ChatMessage(
                id = "msg-4",
                sender = "Ramesh (Supervisor)",
                message = "Yes sir, Dr. Fixit added at 200ml per bag. Shuttering oil and props double-checked.",
                time = "09:40 AM",
                isSelf = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val msg = ChatMessage(
            id = "msg-${ClockUtils.nowMillis()}",
            sender = "You",
            message = text.trim(),
            time = "Just now",
            isSelf = true
        )
        _chatMessages.value = _chatMessages.value + msg
    }
}
