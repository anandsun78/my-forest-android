package com.anandsundaram.myforest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Tree(growth: Float, modifier: Modifier = Modifier, size: Dp = 180.dp) {
    Canvas(modifier = modifier.size(size)) {
        drawPalmTree(growth)
    }
}

@Composable
fun WitheredTree(modifier: Modifier = Modifier, size: Dp = 180.dp) {
    Canvas(modifier = modifier.size(size)) {
        drawWitheredPalmTree()
    }
}

private fun DrawScope.drawPalmTree(growth: Float) {
    val trunkWidth = 20.dp.toPx() * growth
    val trunkHeight = 150.dp.toPx() * growth

    // Trunk
    val trunkPath = Path().apply {
        moveTo(center.x - trunkWidth / 2, size.height)
        cubicTo(
            center.x - trunkWidth, size.height - trunkHeight / 2,
            center.x + trunkWidth, size.height - trunkHeight / 2,
            center.x + trunkWidth / 2, size.height - trunkHeight
        )
        lineTo(center.x - trunkWidth / 2, size.height - trunkHeight)
        cubicTo(
            center.x - trunkWidth, size.height - trunkHeight / 2,
            center.x + trunkWidth, size.height - trunkHeight / 2,
            center.x - trunkWidth / 2, size.height
        )
    }
    drawPath(trunkPath, color = Color(0xFF8B5A2B))

    // Leaves
    val leafColor = Color(0xFF006400)
    val leafCenter = Offset(center.x, size.height - trunkHeight)
    for (i in 0..5) {
        val angle = i * (360f / 6) + 30f
        val leafLength = 80.dp.toPx() * growth
        val endPoint = Offset(
            leafCenter.x + leafLength * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat(),
            leafCenter.y + leafLength * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        val controlPoint1 = Offset(
            leafCenter.x + leafLength / 2 * kotlin.math.cos(Math.toRadians((angle - 30).toDouble())).toFloat(),
            leafCenter.y + leafLength / 2 * kotlin.math.sin(Math.toRadians((angle - 30).toDouble())).toFloat()
        )
        val controlPoint2 = Offset(
            leafCenter.x + leafLength / 2 * kotlin.math.cos(Math.toRadians((angle + 30).toDouble())).toFloat(),
            leafCenter.y + leafLength / 2 * kotlin.math.sin(Math.toRadians((angle + 30).toDouble())).toFloat()
        )

        val leafPath = Path().apply {
            moveTo(leafCenter.x, leafCenter.y)
            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, endPoint.x, endPoint.y)
            moveTo(leafCenter.x, leafCenter.y)
            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, endPoint.x, endPoint.y + 10.dp.toPx())

        }
        drawPath(leafPath, color = leafColor, style = Stroke(width = 10.dp.toPx() * growth))
    }
}

private fun DrawScope.drawWitheredPalmTree() {
    val trunkWidth = 15.dp.toPx()
    val trunkHeight = 150.dp.toPx()

    // Withered Trunk
    val trunkPath = Path().apply {
        moveTo(center.x - trunkWidth / 2, size.height)
        cubicTo(
            center.x - trunkWidth, size.height - trunkHeight / 2,
            center.x + trunkWidth, size.height - trunkHeight / 2,
            center.x + trunkWidth / 2, size.height - trunkHeight
        )
    }
    drawPath(trunkPath, color = Color(0xFF5A3E2B), style = Stroke(width = 5.dp.toPx()))

    // Withered Leaves
    val leafColor = Color(0xFF8B4513)
    val leafCenter = Offset(center.x, size.height - trunkHeight)
    for (i in 0..3) {
        val angle = i * (360f / 4) + 45f
        val leafLength = 60.dp.toPx()
        val endPoint = Offset(
            leafCenter.x + leafLength * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat(),
            leafCenter.y + leafLength * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() + 30.dp.toPx() // Drooping
        )
        drawLine(leafColor, leafCenter, endPoint, strokeWidth = 5.dp.toPx())
    }
}
