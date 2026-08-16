package com.example.model

import kotlin.math.*

enum class BallState {
    IDLE,
    AIMING,
    FLYING,
    SCORED,
    MISSED,
    RESETTING
}

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Long,
    val size: Float,
    val alpha: Float = 1f,
    val life: Float = 1f
)

data class ScorePopup(
    val text: String,
    val x: Float,
    val y: Float,
    val colorHex: Long,
    val alpha: Float = 1f,
    val isCritical: Boolean = false
)

data class Ball(
    val x: Float = 0f,
    val y: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val radius: Float = 24f,
    val rotationAngle: Float = 0f,
    val angularVelocity: Float = 0f,
    val state: BallState = BallState.IDLE,
    val touchedRim: Boolean = false,
    val touchedBackboard: Boolean = false,
    val isMoneyBall: Boolean = false,
    val trail: List<Pair<Float, Float>> = emptyList()
)

data class Hoop(
    val x: Float = 0f, // Center of rim
    val y: Float = 0f, // Height of rim
    val rimWidth: Float = 70f,
    val backboardX: Float = 0f,
    val backboardY: Float = 0f,
    val backboardWidth: Float = 12f,
    val backboardHeight: Float = 140f,
    val vx: Float = 0f,
    val minX: Float = 0f,
    val maxX: Float = 0f,
    val netRippleProgress: Float = 0f
)

data class PhysicsConfig(
    val gravity: Float = 2200f, // px/s^2
    val airResistance: Float = 0.999f,
    val backboardRestitution: Float = 0.65f,
    val rimRestitution: Float = 0.60f,
    val floorRestitution: Float = 0.55f
)

object TrajectoryHelper {
    fun calculateTrajectoryPoints(
        startX: Float,
        startY: Float,
        vx: Float,
        vy: Float,
        gravity: Float = 2200f,
        steps: Int = 24,
        timeStep: Float = 0.045f
    ): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        var currX = startX
        var currY = startY
        var currVx = vx
        var currVy = vy

        for (i in 0 until steps) {
            points.add(Pair(currX, currY))
            currX += currVx * timeStep
            currY += currVy * timeStep + 0.5f * gravity * timeStep * timeStep
            currVy += gravity * timeStep
        }
        return points
    }
}
