package cc.tomko.outify.utils

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import kotlin.random.Random

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun getRandomMaterialShape(): androidx.compose.ui.graphics.Shape {
    val shapes = listOf(
        MaterialShapes.Slanted.toShape(),
        MaterialShapes.Arch.toShape(),
        MaterialShapes.Gem.toShape(),
        MaterialShapes.Pentagon.toShape(),
        MaterialShapes.Sunny.toShape(),
    )

    return shapes[Random.nextInt(shapes.size)]
}
