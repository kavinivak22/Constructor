package com.example.constructorapp.util

import kotlin.math.round

enum class ConstructionActivity {
    CONCRETE, MASONRY, PLASTERING
}

enum class MixGrade(val label: String, val sandRatio: Double, val gravelRatio: Double) {
    M20("M20 (1 : 1.5 : 3)", 1.5, 3.0),
    M25("M25 (1 : 1 : 2)", 1.0, 2.0),
    M15("M15 (1 : 2 : 4)", 2.0, 4.0),
    M10("M10 (1 : 3 : 6)", 3.0, 6.0),
    M7_5("M7.5 (1 : 4 : 8)", 4.0, 8.0),
    MORTAR_1_3("1 : 3 Mortar", 3.0, 0.0),
    MORTAR_1_4("1 : 4 Mortar", 4.0, 0.0),
    MORTAR_1_5("1 : 5 Mortar", 5.0, 0.0),
    MORTAR_1_6("1 : 6 Mortar", 6.0, 0.0),
    CUSTOM("Custom", 0.0, 0.0)
}

data class CalculatedMixMaterial(
    val materialName: String,
    val quantity: Double,
    val unit: String
)

object ConcreteMixCalculator {
    const val CFT_PER_CEMENT_BAG = 1.25 // 50kg cement bag volume in CFT (Indian standard IS 456)

    fun calculateMaterials(
        activity: ConstructionActivity,
        mixGrade: MixGrade,
        cementBags: Double,
        customSandRatio: Double = 0.0,
        customGravelRatio: Double = 0.0
    ): List<CalculatedMixMaterial> {
        if (cementBags <= 0) return emptyList()

        val sandRatio = if (mixGrade == MixGrade.CUSTOM) customSandRatio else mixGrade.sandRatio
        val gravelRatio = if (mixGrade == MixGrade.CUSTOM) customGravelRatio else mixGrade.gravelRatio

        val (sandName, gravelName) = when (activity) {
            ConstructionActivity.CONCRETE -> Pair("M-Sand (Fine Aggregate)", "20mm Blue Metal (Coarse Aggregate)")
            ConstructionActivity.MASONRY -> Pair("P-Sand (Masonry Sand)", "")
            ConstructionActivity.PLASTERING -> Pair("Plastering Sand (Fine)", "")
        }

        val calculatedSandCFT = round(cementBags * sandRatio * CFT_PER_CEMENT_BAG * 10.0) / 10.0
        val calculatedGravelCFT = round(cementBags * gravelRatio * CFT_PER_CEMENT_BAG * 10.0) / 10.0

        val result = mutableListOf(
            CalculatedMixMaterial("53 Grade Cement", cementBags, "Bags"),
            CalculatedMixMaterial(sandName, calculatedSandCFT, "CFT")
        )

        if (gravelRatio > 0.0) {
            result.add(CalculatedMixMaterial(gravelName, calculatedGravelCFT, "CFT"))
        }

        return result
    }
}
