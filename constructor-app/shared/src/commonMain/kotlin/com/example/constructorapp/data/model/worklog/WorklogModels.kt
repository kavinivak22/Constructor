package com.example.constructorapp.data.model.worklog

enum class WorklogCreationTab(val title: String) {
    DETAILS("Details"),
    LABOR("Labor"),
    MATERIALS("Materials"),
    PHOTOS("Photos")
}

data class WorkerCountInput(
    val workerType: String, // "Mason", "MC", "FC", "Helper", "Carpenter", "Bar Bender", "Painter", etc.
    val count: Int = 0
)

data class WorklogLaborInput(
    val id: String = "labor-in-${WorklogCounter.next()}",
    val contractorId: String? = null,
    val contractorName: String,
    val category: String = "Masonry",
    val workType: String = "nmr",         // "nmr" or "rate"
    val payoutClass: String = "NMR",      // "NMR" or "Rate"
    val workDescription: String = "",
    val paymentStatus: String = "Pending", // "Paid", "On Payday", "Pending"
    val workDoneQuantity: Double? = null,
    val workDoneUnit: String? = null,     // "sq.ft", "cu.ft", "rft", "nos"
    val payoutRate: Double? = null,       // Rate per unit in INR
    val totalPayout: Double? = null,      // workDoneQuantity * payoutRate
    val workers: List<WorkerCountInput> = emptyList()
)

data class WorklogMaterialInput(
    val id: String = "mat-in-${WorklogCounter.next()}",
    val projectMaterialId: String? = null,
    val materialName: String,
    val quantityConsumed: Double,
    val unit: String,                     // "Bags", "CFT", "MT", "Nos", "Kg"
    val unitCost: Double? = null
)

data class WorklogPhotoInput(
    val id: String = "photo-in-${WorklogCounter.next()}",
    val localUri: String? = null,
    val localFilePath: String? = null,
    val remoteUrl: String = "",
    val caption: String = ""
)

data class CreateWorklogRequest(
    val id: String = "req-in-${WorklogCounter.next()}",
    val projectId: String,
    val title: String,
    val date: String,                     // YYYY-MM-DD
    val labor: List<WorklogLaborInput> = emptyList(),
    val materials: List<WorklogMaterialInput> = emptyList(),
    val photos: List<WorklogPhotoInput> = emptyList()
)

private object WorklogCounter {
    private var count = 100L
    fun next(): Long = ++count
}
