package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.model.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable
fun BasketballCourtCanvas(
    modifier: Modifier = Modifier,
    gameMode: GameMode,
    shotZone: ShotZone,
    ballSkin: BallSkin,
    playerAvatar: Avatar = PRESET_AVATARS.first(),
    jerseyNumber: Int = 23,
    playerName: String = "Player",
    isMovingHoop: Boolean = false,
    hoopSpeedFactor: Float = 1.0f,
    isPaused: Boolean = false,
    onScore: (points: Int, isSwish: Boolean, isBankShot: Boolean) -> Unit,
    onShotTaken: () -> Unit,
    onSoundEvent: (type: String) -> Unit
) {
    val density = LocalDensity.current

    // Physics State
    var ball by remember { mutableStateOf(Ball()) }
    var hoop by remember { mutableStateOf(Hoop()) }
    var particles by remember { mutableStateOf(listOf<Particle>()) }
    var scorePopups by remember { mutableStateOf(listOf<ScorePopup>()) }

    // Aiming State
    var isAiming by remember { mutableStateOf(false) }
    var aimStart by remember { mutableStateOf(Offset.Zero) }
    var aimCurrent by remember { mutableStateOf(Offset.Zero) }
    var trajectoryDots by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }

    // Moving hoop direction
    var hoopDirection by remember { mutableFloatStateOf(1f) }
    var canvasWidth by remember { mutableFloatStateOf(1000f) }
    var canvasHeight by remember { mutableFloatStateOf(1600f) }

    // Money ball (for 3pt contest)
    var shotCounter by remember { mutableIntStateOf(0) }
    var resetCountdown by remember { mutableFloatStateOf(0f) }
    var isShotEnding by remember { mutableStateOf(false) }

    // Natural Court Ground Level
    val groundYRatio = 0.77f

    // Calculate player launch position on court based on ShotZone
    fun getPlayerPosition(zone: ShotZone, width: Float, height: Float): Offset {
        val groundY = height * groundYRatio
        val startX = when (zone) {
            ShotZone.TWO_POINT_CLOSE -> width * 0.38f
            ShotZone.TWO_POINT_MID -> width * 0.26f
            ShotZone.THREE_POINT -> width * 0.15f
            ShotZone.DEEP_THREE -> width * 0.07f
        }
        return Offset(startX, groundY)
    }

    // Calculate ball position in player's hands at shooting pocket
    fun getBallHandPosition(playerPos: Offset, state: BallState, isAimingMode: Boolean): Offset {
        return when {
            isAimingMode -> Offset(playerPos.x + 22f, playerPos.y - 128f) // Loaded above forehead
            state == BallState.FLYING || state == BallState.SCORED || state == BallState.MISSED -> Offset(playerPos.x + 36f, playerPos.y - 148f) // High release point
            else -> Offset(playerPos.x + 18f, playerPos.y - 110f) // Set pocket at chest/face height
        }
    }

    // Reset ball to player's hands
    fun resetBall(zone: ShotZone, width: Float, height: Float) {
        val playerPos = getPlayerPosition(zone, width, height)
        val ballPos = getBallHandPosition(playerPos, BallState.IDLE, false)
        val isMoney = (gameMode == GameMode.SOLO_3PT_CONTEST && (shotCounter + 1) % 5 == 0)
        ball = Ball(
            x = ballPos.x,
            y = ballPos.y,
            vx = 0f,
            vy = 0f,
            radius = with(density) { 17.dp.toPx() },
            state = BallState.IDLE,
            isMoneyBall = isMoney
        )
    }

    // Setup canvas dimensions, hoop and initial positions
    LaunchedEffect(canvasWidth, canvasHeight, shotZone) {
        if (canvasWidth > 0 && canvasHeight > 0) {
            val hoopY = canvasHeight * 0.35f
            val hoopX = canvasWidth * 0.77f
            val bbX = hoopX + with(density) { 34.dp.toPx() }
            val bbY = hoopY - with(density) { 52.dp.toPx() }

            hoop = Hoop(
                x = hoopX,
                y = hoopY,
                rimWidth = with(density) { 52.dp.toPx() },
                backboardX = bbX,
                backboardY = bbY,
                backboardWidth = with(density) { 10.dp.toPx() },
                backboardHeight = with(density) { 110.dp.toPx() },
                minX = canvasWidth * 0.62f,
                maxX = canvasWidth * 0.84f,
                vx = if (isMovingHoop) 140f * hoopSpeedFactor else 0f
            )

            if (ball.state == BallState.IDLE || ball.state == BallState.RESETTING) {
                resetBall(shotZone, canvasWidth, canvasHeight)
            }
        }
    }

    // High Performance Smooth Physics Simulation Loop (Synced with Display Frame Rate)
    LaunchedEffect(isPaused, canvasWidth, canvasHeight) {
        val gravity = 2200f
        var lastFrameTime = System.nanoTime()

        while (true) {
            withFrameNanos { nowNanos ->
                val elapsedSeconds = ((nowNanos - lastFrameTime) / 1_000_000_000f).coerceIn(0.008f, 0.033f)
                lastFrameTime = nowNanos
                val dt = elapsedSeconds

                if (!isPaused && canvasWidth > 0 && canvasHeight > 0) {
                    // 1. Update Moving Hoop
                    if (isMovingHoop) {
                        var newHoopX = hoop.x + hoop.vx * hoopDirection * dt
                        if (newHoopX > hoop.maxX) {
                            newHoopX = hoop.maxX
                            hoopDirection = -1f
                        } else if (newHoopX < hoop.minX) {
                            newHoopX = hoop.minX
                            hoopDirection = 1f
                        }
                        val deltaX = newHoopX - hoop.x
                        hoop = hoop.copy(
                            x = newHoopX,
                            backboardX = hoop.backboardX + deltaX
                        )
                    }

                    // 2. Net ripple decay
                    if (hoop.netRippleProgress > 0f) {
                        hoop = hoop.copy(netRippleProgress = max(0f, hoop.netRippleProgress - dt * 2.5f))
                    }

                    // 3. Update Ball Physics when Flying or Bouncing
                    if (ball.state == BallState.FLYING || ball.state == BallState.SCORED || ball.state == BallState.MISSED) {
                        var bx = ball.x + ball.vx * dt
                        var by = ball.y + ball.vy * dt + 0.5f * gravity * dt * dt
                        var bvx = ball.vx
                        var bvy = ball.vy + gravity * dt
                        var bRot = ball.rotationAngle + ball.angularVelocity * dt
                        var bTouchedRim = ball.touchedRim
                        var bTouchedBb = ball.touchedBackboard
                        var newState = ball.state

                        val rimFrontX = hoop.x - hoop.rimWidth / 2f
                        val rimBackX = hoop.x + hoop.rimWidth / 2f
                        val rimY = hoop.y
                        val pegRadius = with(density) { 5.dp.toPx() }
                        val ballR = ball.radius

                        // Collisions with Rim Pegs (Circle-to-Circle Collision)
                        // Front Peg:
                        val distFront = hypot(bx - rimFrontX, by - rimY)
                        if (distFront < (ballR + pegRadius)) {
                            bTouchedRim = true
                            onSoundEvent("rim")
                            val nx = (bx - rimFrontX) / (distFront + 0.001f)
                            val ny = (by - rimY) / (distFront + 0.001f)
                            val dot = bvx * nx + bvy * ny
                            if (dot < 0) {
                                bvx -= 1.65f * dot * nx
                                bvy -= 1.65f * dot * ny
                            }
                        }

                        // Back Peg:
                        val distBack = hypot(bx - rimBackX, by - rimY)
                        if (distBack < (ballR + pegRadius)) {
                            bTouchedRim = true
                            onSoundEvent("rim")
                            val nx = (bx - rimBackX) / (distBack + 0.001f)
                            val ny = (by - rimY) / (distBack + 0.001f)
                            val dot = bvx * nx + bvy * ny
                            if (dot < 0) {
                                bvx -= 1.65f * dot * nx
                                bvy -= 1.65f * dot * ny
                            }
                        }

                        // Collision with Backboard
                        val bbX = hoop.backboardX
                        val bbTop = hoop.backboardY
                        val bbBottom = bbTop + hoop.backboardHeight
                        if (bx + ballR >= bbX && bx - ballR <= bbX + 16f && by >= bbTop && by <= bbBottom) {
                            if (bvx > 0) {
                                bTouchedBb = true
                                bvx = -bvx * 0.70f
                                bvy *= 0.85f
                                bx = bbX - ballR
                                onSoundEvent("backboard")
                            }
                        }

                        // Score Detection (Passing downward through rim plane)
                        val prevY = ball.y
                        val currY = by
                        val isInsideRimHorizontally = bx > (rimFrontX + 6f) && bx < (rimBackX - 6f)
                        val crossedRimPlane = prevY <= rimY && currY >= rimY

                        if (crossedRimPlane && isInsideRimHorizontally && bvy > 0 && newState == BallState.FLYING) {
                            newState = BallState.SCORED
                            val isSwish = !bTouchedRim && !bTouchedBb
                            val isBank = bTouchedBb
                            val pts = shotZone.points * (if (ball.isMoneyBall) 2 else 1)

                            // Sound & Net Ripple
                            if (isSwish) {
                                onSoundEvent("swish")
                            } else {
                                onSoundEvent("score")
                            }
                            hoop = hoop.copy(netRippleProgress = 1f)

                            // Particle explosions
                            val newParticles = (0..22).map {
                                Particle(
                                    x = hoop.x + Random.nextFloat() * 20 - 10,
                                    y = hoop.y + 10f,
                                    vx = Random.nextFloat() * 320 - 160,
                                    vy = Random.nextFloat() * -220 - 60,
                                    color = if (isSwish) 0xFF38BDF8 else 0xFFF59E0B,
                                    size = Random.nextFloat() * 6 + 4
                                )
                            }
                            particles = particles + newParticles

                            // Score Popup
                            val popupText = when {
                                ball.isMoneyBall -> "MONEY BALL! +$pts"
                                isSwish -> "SWISH! +$pts"
                                isBank -> "BANK SHOT! +$pts"
                                else -> "GOAL! +$pts"
                            }
                            scorePopups = scorePopups + ScorePopup(
                                text = popupText,
                                x = hoop.x,
                                y = hoop.y - 45f,
                                colorHex = if (isSwish) 0xFF38BDF8 else 0xFFF59E0B,
                                isCritical = isSwish || ball.isMoneyBall
                            )

                            onScore(pts, isSwish, isBank)
                        }

                        // Floor Collision
                        val floorY = canvasHeight * groundYRatio
                        if (by + ballR >= floorY) {
                            by = floorY - ballR
                            if (abs(bvy) > 100f) {
                                bvy = -bvy * 0.58f
                                bvx *= 0.82f
                                onSoundEvent("bounce")
                            } else {
                                bvy = 0f
                                bvx *= 0.5f
                            }

                            if (newState == BallState.FLYING) {
                                newState = BallState.MISSED
                            }
                        }

                        // Out of screen reset
                        if (bx > canvasWidth + 100f || bx < -100f || by > canvasHeight + 100f || (by + ballR >= floorY - 5f && abs(bvx) < 20f && abs(bvy) < 20f)) {
                            newState = BallState.RESETTING
                        }

                        // Trail updating
                        val newTrail = (ball.trail + Pair(bx, by)).takeLast(8)

                        ball = ball.copy(
                            x = bx,
                            y = by,
                            vx = bvx,
                            vy = bvy,
                            rotationAngle = bRot,
                            touchedRim = bTouchedRim,
                            touchedBackboard = bTouchedBb,
                            state = newState,
                            trail = newTrail
                        )

                        // Smooth Non-blocking Reset countdown
                        if (newState == BallState.RESETTING || newState == BallState.SCORED || newState == BallState.MISSED) {
                            if (!isShotEnding) {
                                isShotEnding = true
                                resetCountdown = 0.50f
                            }
                        }
                    }

                    if (isShotEnding) {
                        resetCountdown -= dt
                        if (resetCountdown <= 0f) {
                            isShotEnding = false
                            shotCounter++
                            resetBall(shotZone, canvasWidth, canvasHeight)
                        }
                    }

                    // 4. Update Particles
                    if (particles.isNotEmpty()) {
                        particles = particles.mapNotNull { p ->
                            val life = p.life - dt * 2.0f
                            if (life <= 0f) null
                            else p.copy(
                                x = p.x + p.vx * dt,
                                y = p.y + p.vy * dt + 500f * dt * dt,
                                alpha = life,
                                life = life
                            )
                        }
                    }

                    // 5. Update Score Popups
                    if (scorePopups.isNotEmpty()) {
                        scorePopups = scorePopups.mapNotNull { pop ->
                            val alpha = pop.alpha - dt * 1.5f
                            if (alpha <= 0f) null
                            else pop.copy(
                                y = pop.y - 60f * dt,
                                alpha = alpha
                            )
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isPaused, shotZone) {
                    if (isPaused) return@pointerInput
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            if (ball.state == BallState.IDLE) {
                                isAiming = true
                                aimStart = startOffset
                                aimCurrent = startOffset
                            }
                        },
                        onDrag = { change, _ ->
                            if (isAiming) {
                                change.consume()
                                aimCurrent = change.position

                                // UNRESTRICTED BALL PULLING & FREE HEIGHT CONTROL
                                // Pull backward (left) generates forward vx
                                // Pull downward generates vertical vy (lift/height)
                                val pullDx = aimStart.x - aimCurrent.x
                                val pullDy = aimStart.y - aimCurrent.y

                                // Sensitivity scaling allowing gentle floaters to high rainbow arcs & full court bombs
                                val sensitivity = 4.6f
                                val vx = if (pullDx >= 0f) pullDx * sensitivity else (-pullDx) * sensitivity
                                val vy = pullDy * sensitivity

                                val playerPos = getPlayerPosition(shotZone, canvasWidth, canvasHeight)
                                val releasePos = getBallHandPosition(playerPos, BallState.FLYING, true)

                                // Trajectory limited to initial 8 dots (short launch angle indicator)
                                // so it never reaches the rim, challenging player skill and feel!
                                trajectoryDots = TrajectoryHelper.calculateTrajectoryPoints(
                                    startX = releasePos.x,
                                    startY = releasePos.y,
                                    vx = vx.coerceAtLeast(60f),
                                    vy = vy,
                                    gravity = 2200f,
                                    steps = 8,
                                    timeStep = 0.030f
                                )
                            }
                        },
                        onDragEnd = {
                            if (isAiming) {
                                isAiming = false
                                val pullDx = aimStart.x - aimCurrent.x
                                val pullDy = aimStart.y - aimCurrent.y

                                val sensitivity = 4.6f
                                val vx = if (pullDx >= 0f) pullDx * sensitivity else (-pullDx) * sensitivity
                                val vy = pullDy * sensitivity

                                val playerPos = getPlayerPosition(shotZone, canvasWidth, canvasHeight)
                                val releasePos = getBallHandPosition(playerPos, BallState.FLYING, true)

                                val power = hypot(vx, vy)
                                if (power >= 90f) {
                                    ball = ball.copy(
                                        x = releasePos.x,
                                        y = releasePos.y,
                                        vx = vx.coerceAtLeast(60f),
                                        vy = vy,
                                        angularVelocity = (8f + (power / 300f)).coerceIn(6f, 15f),
                                        state = BallState.FLYING,
                                        touchedRim = false,
                                        touchedBackboard = false
                                    )
                                    trajectoryDots = emptyList()
                                    onSoundEvent("whoosh")
                                    onShotTaken()
                                } else {
                                    trajectoryDots = emptyList()
                                }
                            }
                        },
                        onDragCancel = {
                            isAiming = false
                            trajectoryDots = emptyList()
                        }
                    )
                }
        ) {
            canvasWidth = size.width
            canvasHeight = size.height
            val groundY = size.height * groundYRatio
            val playerPos = getPlayerPosition(shotZone, size.width, size.height)

            // 1. Draw School Basketball Gymnasium Background with Creator Banners
            drawSchoolGymnasiumBackground(size, groundY, shotZone)

            // 2. Draw Court Floor Lines & Key Area
            drawSchoolCourtMarkings(size, groundY, shotZone)

            // 3. Draw Trajectory Line (Garis Putus-Putus Parabola) when Aiming
            if (isAiming && trajectoryDots.isNotEmpty()) {
                drawTrajectoryDottedLine(trajectoryDots, ballSkin)
            }

            // 4. Draw Ball Motion Trail
            if (ball.trail.size > 1 && ball.state == BallState.FLYING) {
                drawBallTrail(ball.trail, ballSkin, ball.radius)
            }

            // 5. Draw Realistic Side-View Basketball Hoop Post & Structure
            drawSchoolHoopStructure(hoop, groundY)

            // 6. Draw Realistic Side-View Basketball Player Athlete
            drawSideViewPlayer(
                playerPos = playerPos,
                avatar = playerAvatar,
                jerseyNumber = jerseyNumber,
                ballState = ball.state,
                isAiming = isAiming,
                aimCurrent = aimCurrent,
                aimStart = aimStart,
                density = density
            )

            // 7. Draw Basketball with 3D Shading, Seams & Rotation
            drawBasketball(ball, ballSkin)

            // 8. Draw Front Rim Edge (layering on top of ball so ball goes *inside* the rim)
            drawFrontRimLip(hoop)

            // 9. Draw Particle FX
            particles.forEach { p ->
                drawCircle(
                    color = Color(p.color).copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(p.x, p.y)
                )
            }

            // 10. Draw Floating Score Popups
            scorePopups.forEach { pop ->
                drawScorePopup(pop, density)
            }

            // 11. Draw Aiming Drag Indicator with Height & Power Info
            if (isAiming) {
                drawAimGuide(aimStart, aimCurrent, ballSkin)
            }

            // 12. Draw Elegant Watermark: "Created By : Cikgu Dasril Amri"
            drawCourtWatermark(size, groundY)
        }
    }
}

// -------------------------------------------------------------
// DRAWING EXTENSION FUNCTIONS: SCHOOL BASKETBALL COURT & GYMNASIUM
// -------------------------------------------------------------

private fun DrawScope.drawSchoolGymnasiumBackground(size: Size, groundY: Float, zone: ShotZone) {
    // 1. Upper Gymnasium Wall with Athletic School Atmosphere
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090D16),
                Color(0xFF0F172A),
                Color(0xFF1E293B)
            ),
            startY = 0f,
            endY = groundY
        ),
        size = Size(size.width, groundY)
    )

    // 2. High Gym Windows / Clerestory Skylights with Natural Light Beams
    val windowWidth = size.width * 0.18f
    val windowHeight = size.height * 0.08f
    for (i in 0..3) {
        val winX = size.width * (0.06f + i * 0.24f)
        val winY = size.height * 0.06f

        // Window Frame
        drawRoundRect(
            color = Color(0x3338BDF8),
            topLeft = Offset(winX, winY),
            size = Size(windowWidth, windowHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = Color(0x6694A3B8),
            topLeft = Offset(winX, winY),
            size = Size(windowWidth, windowHeight),
            style = Stroke(width = 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        // Window mullions
        drawLine(
            color = Color(0x6694A3B8),
            start = Offset(winX + windowWidth / 2f, winY),
            end = Offset(winX + windowWidth / 2f, winY + windowHeight),
            strokeWidth = 1.5f
        )

        // Soft daylight slant beam shining onto the court
        val lightBeam = Path().apply {
            moveTo(winX, winY + windowHeight)
            lineTo(winX + windowWidth, winY + windowHeight)
            lineTo(winX + windowWidth + size.width * 0.25f, groundY)
            lineTo(winX + size.width * 0.12f, groundY)
            close()
        }
        drawPath(
            path = lightBeam,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x1838BDF8), Color(0x0638BDF8), Color.Transparent),
                startY = winY + windowHeight,
                endY = groundY
            )
        )
    }

    // 3. Gymnasium Ceiling Steel Trusses & Industrial Rafters
    val trussY = size.height * 0.035f
    drawLine(
        color = Color(0xFF334155),
        start = Offset(0f, trussY),
        end = Offset(size.width, trussY),
        strokeWidth = 5f
    )
    for (step in 0..12) {
        val sx = step * (size.width / 12f)
        drawLine(
            color = Color(0x55475569),
            start = Offset(sx, 0f),
            end = Offset(sx + size.width / 24f, trussY),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0x55475569),
            start = Offset(sx + size.width / 24f, trussY),
            end = Offset(sx + size.width / 12f, 0f),
            strokeWidth = 2f
        )
    }

    // 4. Stadium Hanging Floodlights with Ambient Spotlights
    val lights = listOf(size.width * 0.25f, size.width * 0.50f, size.width * 0.78f)
    lights.forEach { lx ->
        // Cable & lamp fixture
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(lx, trussY),
            end = Offset(lx, trussY + 22f),
            strokeWidth = 2f
        )
        drawCircle(
            color = Color(0xFFF1F5F9),
            radius = 6f,
            center = Offset(lx, trussY + 24f)
        )
        drawCircle(
            color = Color(0x88FEF08A),
            radius = 12f,
            center = Offset(lx, trussY + 24f)
        )

        // Downward spotlight glow
        val cone = Path().apply {
            moveTo(lx - 8f, trussY + 24f)
            lineTo(lx + 8f, trussY + 24f)
            lineTo(lx + size.width * 0.22f, groundY)
            lineTo(lx - size.width * 0.22f, groundY)
            close()
        }
        drawPath(
            path = cone,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x24FEF08A), Color(0x08FEF08A), Color.Transparent),
                startY = trussY + 24f,
                endY = groundY
            )
        )
    }

    // 5. School Championship Banners hanging on Wall
    // Banner 1: SMA Championship
    drawSchoolBanner(
        topLeft = Offset(size.width * 0.06f, size.height * 0.16f),
        width = size.width * 0.23f,
        height = size.height * 0.10f,
        mainColor = Color(0xFF1E3A8A),
        accentColor = Color(0xFFF59E0B),
        title = "SMA CHAMPIONS",
        subtitle = "🏆 2026 BASKETBALL CUP"
    )

    // Banner 2: Creator Tribute Banner (Created By : Cikgu Dasril Amri)
    drawSchoolBanner(
        topLeft = Offset(size.width * 0.32f, size.height * 0.15f),
        width = size.width * 0.29f,
        height = size.height * 0.11f,
        mainColor = Color(0xFF831843),
        accentColor = Color(0xFFFDE047),
        title = "CREATED BY : CIKGU DASRIL AMRI",
        subtitle = "🏀 GURU PJOK & DEVELOPER ⭐"
    )

    // 6. Wall Scoreboard in Background
    drawGymScoreboard(
        topLeft = Offset(size.width * 0.63f, size.height * 0.16f),
        width = size.width * 0.28f,
        height = size.height * 0.11f
    )

    // 7. School Bleachers / Spectator Benches (Tribun Penonton Sekolah)
    val bleacherTop = groundY - size.height * 0.14f
    for (tier in 0..2) {
        val ty = bleacherTop + tier * (size.height * 0.045f)
        drawRect(
            color = if (tier % 2 == 0) Color(0xFF1E293B) else Color(0xFF334155),
            topLeft = Offset(0f, ty),
            size = Size(size.width * 0.55f, size.height * 0.045f)
        )
        // Bleacher plank edge
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(0f, ty),
            end = Offset(size.width * 0.55f, ty),
            strokeWidth = 2f
        )
    }

    // Silhouettes of cheering students on the bleachers
    for (i in 0..9) {
        val sx = size.width * (0.04f + i * 0.052f)
        val sy = bleacherTop + (i % 3) * (size.height * 0.04f) + 4f
        // Head
        drawCircle(
            color = Color(0x88475569),
            radius = 7f,
            center = Offset(sx, sy)
        )
        // Torso
        drawRoundRect(
            color = Color(0x88334155),
            topLeft = Offset(sx - 7f, sy + 7f),
            size = Size(14f, 16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
    }

    // 8. Gym Wall Protective Crash Pads (Busa Pengaman Dinding Basket)
    val padTop = groundY - size.height * 0.09f
    drawRect(
        color = Color(0xFF0369A1),
        topLeft = Offset(size.width * 0.56f, padTop),
        size = Size(size.width * 0.44f, size.height * 0.09f)
    )
    // Pad seams and accents
    for (px in 1..4) {
        val seamX = size.width * (0.56f + px * 0.088f)
        drawLine(
            color = Color(0xFF0284C7),
            start = Offset(seamX, padTop),
            end = Offset(seamX, groundY),
            strokeWidth = 2f
        )
    }
    // Pad yellow warning stripe
    drawRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(size.width * 0.56f, padTop + 4f),
        size = Size(size.width * 0.44f, 4f)
    )

    // 9. Hardwood Polished Maple Floor (Lantai Parket Basket Sekolah)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF59E0B), // Golden maple wood gloss
                Color(0xFFD97706),
                Color(0xFFB45309),
                Color(0xFF78350F)
            ),
            startY = groundY,
            endY = size.height
        ),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, size.height - groundY)
    )

    // Hardwood Floor Plank Lines (Horizontal planks)
    val plankHeight = 16f
    var currY = groundY
    while (currY < size.height) {
        drawLine(
            color = Color(0x2A000000),
            start = Offset(0f, currY),
            end = Offset(size.width, currY),
            strokeWidth = 1.5f
        )
        currY += plankHeight
    }

    // Floor Gym Light Reflections
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x22FFFFFF),
                Color(0x35FFFFFF),
                Color(0x15FFFFFF),
                Color.Transparent
            ),
            startX = size.width * 0.2f,
            endX = size.width * 0.85f
        ),
        topLeft = Offset(size.width * 0.2f, groundY),
        size = Size(size.width * 0.65f, size.height - groundY)
    )
}

private fun DrawScope.drawSchoolCourtMarkings(size: Size, groundY: Float, currentZone: ShotZone) {
    val floorHeight = size.height - groundY

    // 1. Painted Key Area (Daerah Bersyarat / Paint Zone) in Navy/Teal
    val keyLeft = size.width * 0.56f
    val keyWidth = size.width * 0.44f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x380369A1), Color(0x550284C7)),
            startY = groundY,
            endY = size.height
        ),
        topLeft = Offset(keyLeft, groundY),
        size = Size(keyWidth, floorHeight)
    )

    // Key Border Line
    drawLine(
        color = Color(0xDDFFFFFF),
        start = Offset(keyLeft, groundY),
        end = Offset(keyLeft, size.height),
        strokeWidth = 3.5f
    )

    // Free Throw Line
    val ftX = size.width * 0.56f
    drawLine(
        color = Color(0xEEFFFFFF),
        start = Offset(ftX, groundY + floorHeight * 0.25f),
        end = Offset(ftX, groundY + floorHeight * 0.75f),
        strokeWidth = 4f
    )

    // Free Throw Circle
    drawArc(
        color = Color(0xAAFFFFFF),
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(ftX - floorHeight * 0.25f, groundY + floorHeight * 0.25f),
        size = Size(floorHeight * 0.5f, floorHeight * 0.5f),
        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
    )

    // 2. Official 3-Point Arc Line
    val hoopX = size.width * 0.77f
    val arcRadius = size.width * 0.54f
    drawArc(
        color = if (currentZone == ShotZone.THREE_POINT || currentZone == ShotZone.DEEP_THREE) Color(0xFF38BDF8) else Color(0xCCFFFFFF),
        startAngle = 105f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(hoopX - arcRadius, groundY - arcRadius * 0.35f),
        size = Size(arcRadius * 2f, floorHeight * 1.6f),
        style = Stroke(
            width = if (currentZone.points == 3) 5f else 3.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
        )
    )

    // 3. Active Shooting Spot Badge on Floor
    val spotX = when (currentZone) {
        ShotZone.TWO_POINT_CLOSE -> size.width * 0.38f
        ShotZone.TWO_POINT_MID -> size.width * 0.26f
        ShotZone.THREE_POINT -> size.width * 0.15f
        ShotZone.DEEP_THREE -> size.width * 0.07f
    }
    val spotY = groundY + floorHeight * 0.35f

    // Concentric glowing target ring on floor
    drawCircle(
        color = if (currentZone.points == 3) Color(0x6638BDF8) else Color(0x66F59E0B),
        radius = 32f,
        center = Offset(spotX, spotY)
    )
    drawCircle(
        color = Color.White,
        radius = 32f,
        center = Offset(spotX, spotY),
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = if (currentZone.points == 3) Color(0xFF38BDF8) else Color(0xFFF59E0B),
        radius = 8f,
        center = Offset(spotX, spotY)
    )
}

// -------------------------------------------------------------
// DRAWING EXTENSION FUNCTIONS: SIDE-VIEW BASKETBALL PLAYER
// -------------------------------------------------------------

private fun DrawScope.drawSideViewPlayer(
    playerPos: Offset,
    avatar: Avatar,
    jerseyNumber: Int,
    ballState: BallState,
    isAiming: Boolean,
    aimCurrent: Offset,
    aimStart: Offset,
    density: androidx.compose.ui.unit.Density
) {
    val px = playerPos.x
    val groundY = playerPos.y

    // Jump & crouch state offsets
    val isFlying = (ballState == BallState.FLYING || ballState == BallState.SCORED || ballState == BallState.MISSED)
    val jumpElevation = if (isFlying) 24f else 0f
    val crouchDrop = if (isAiming) 14f else 0f
    val playerBaseY = groundY - jumpElevation + crouchDrop

    val jerseyPrimary = Color(avatar.jerseyColorHex)
    val jerseyAccent = Color(avatar.accentColorHex)
    val skinTone = Color(0xFFE0AC69)
    val skinShadow = Color(0xFFC68642)
    val shoesWhite = Color(0xFFF8FAFC)
    val shoeAccent = jerseyAccent

    // 1. Dynamic Floor Shadow
    val shadowWidth = if (isFlying) 36f else 54f
    val shadowAlpha = if (isFlying) 0.3f else 0.55f
    drawOval(
        color = Color.Black.copy(alpha = shadowAlpha),
        topLeft = Offset(px - shadowWidth / 2f, groundY - 6f),
        size = Size(shadowWidth, 12f)
    )

    // Player anatomical anchors
    val hipX = px
    val hipY = playerBaseY - 60f

    val torsoX = px + (if (isAiming) 4f else if (isFlying) 6f else 2f)
    val shoulderY = playerBaseY - 116f

    val headX = px + (if (isAiming) 8f else if (isFlying) 8f else 4f)
    val headY = shoulderY - 22f

    // 2. LEGS & SNEAKERS (Sepatu Basket Samping)
    // Left Leg (Back leg)
    val leftKneeX = px - 6f + (if (isAiming) -8f else 0f)
    val leftKneeY = playerBaseY - 32f
    val leftAnkleX = px - 12f + (if (isFlying) -4f else 0f)
    val leftAnkleY = playerBaseY - 8f

    // Thigh
    drawLine(color = skinShadow, start = Offset(hipX, hipY), end = Offset(leftKneeX, leftKneeY), strokeWidth = 14f, cap = StrokeCap.Round)
    // Calf
    drawLine(color = skinShadow, start = Offset(leftKneeX, leftKneeY), end = Offset(leftAnkleX, leftAnkleY), strokeWidth = 11f, cap = StrokeCap.Round)
    // Left Shoe (Back)
    drawSneakerSide(leftAnkleX, leftAnkleY, shoesWhite.copy(alpha = 0.8f), shoeAccent.copy(alpha = 0.8f))

    // Right Leg (Front leg)
    val rightKneeX = px + 8f + (if (isAiming) 6f else 0f)
    val rightKneeY = playerBaseY - 30f
    val rightAnkleX = px + 10f
    val rightAnkleY = playerBaseY - 8f

    // Thigh
    drawLine(color = skinTone, start = Offset(hipX + 2f, hipY), end = Offset(rightKneeX, rightKneeY), strokeWidth = 15f, cap = StrokeCap.Round)
    // Black Knee Compression Sleeve
    drawCircle(color = Color(0xFF1E293B), radius = 8f, center = Offset(rightKneeX, rightKneeY))
    // Calf
    drawLine(color = skinTone, start = Offset(rightKneeX, rightKneeY), end = Offset(rightAnkleX, rightAnkleY), strokeWidth = 12f, cap = StrokeCap.Round)
    // Right Shoe (Front)
    drawSneakerSide(rightAnkleX, rightAnkleY, shoesWhite, shoeAccent)

    // 3. BASKETBALL SHORTS (Celana Pendek Basket)
    val shortsPath = Path().apply {
        moveTo(hipX - 12f, hipY - 8f)
        lineTo(hipX + 14f, hipY - 8f)
        lineTo(hipX + 16f, playerBaseY - 38f)
        lineTo(hipX - 10f, playerBaseY - 38f)
        close()
    }
    drawPath(path = shortsPath, color = jerseyPrimary)
    // Shorts Trim Stripe
    drawLine(
        color = jerseyAccent,
        start = Offset(hipX + 14f, hipY - 6f),
        end = Offset(hipX + 16f, playerBaseY - 38f),
        strokeWidth = 3.5f
    )

    // 4. ATHLETIC TORSO & JERSEY (Jersey Basket Tampak Samping)
    val jerseyPath = Path().apply {
        moveTo(torsoX - 12f, hipY - 8f)
        lineTo(torsoX + 12f, hipY - 8f)
        lineTo(torsoX + 14f, shoulderY + 8f)
        lineTo(torsoX - 8f, shoulderY)
        close()
    }
    drawPath(path = jerseyPath, color = jerseyPrimary)
    // Jersey Side Accent Band
    drawLine(
        color = jerseyAccent,
        start = Offset(torsoX + 12f, hipY - 8f),
        end = Offset(torsoX + 14f, shoulderY + 8f),
        strokeWidth = 4f
    )

    // Jersey Number on Side/Back
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        canvas.nativeCanvas.drawText("$jerseyNumber", torsoX - 1f, hipY - 26f, paint)
    }

    // 5. HEAD, HAIR & HEADBAND (Kepala & Wajah Tampak Samping)
    // Neck
    drawLine(color = skinTone, start = Offset(torsoX + 2f, shoulderY), end = Offset(headX, headY + 12f), strokeWidth = 10f, cap = StrokeCap.Round)

    // Head profile
    drawCircle(color = skinTone, radius = 13f, center = Offset(headX, headY))
    // Nose / Jaw profile facing right towards hoop
    val nosePath = Path().apply {
        moveTo(headX + 9f, headY - 4f)
        lineTo(headX + 16f, headY + 2f)
        lineTo(headX + 9f, headY + 6f)
        close()
    }
    drawPath(path = nosePath, color = skinTone)

    // Hair / Fade Cut
    drawArc(
        color = Color(0xFF1E1B18),
        startAngle = 150f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headX - 14f, headY - 15f),
        size = Size(26f, 26f)
    )

    // Headband (Bando Olahraga)
    drawRoundRect(
        color = jerseyAccent,
        topLeft = Offset(headX - 12f, headY - 8f),
        size = Size(26f, 6f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
    )

    // Eye looking forward at the basket
    drawCircle(color = Color(0xFF0F172A), radius = 2f, center = Offset(headX + 7f, headY - 1f))

    // 6. ARMS & SHOOTING MECHANICS (Lengan & Tangan Menembak)
    when {
        isAiming -> {
            // AIMING POSE: Elbow cocked, arm lifted into shooting pocket
            val elbowX = torsoX + 16f
            val elbowY = shoulderY - 8f
            val handX = headX + 14f
            val handY = headY - 14f

            // Left Guide Arm
            drawLine(color = skinShadow, start = Offset(torsoX + 4f, shoulderY + 4f), end = Offset(elbowX - 6f, elbowY + 6f), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(color = skinShadow, start = Offset(elbowX - 6f, elbowY + 6f), end = Offset(handX - 4f, handY + 6f), strokeWidth = 7f, cap = StrokeCap.Round)

            // Right Shooting Arm (Cocked)
            drawLine(color = skinTone, start = Offset(torsoX + 10f, shoulderY + 2f), end = Offset(elbowX, elbowY), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(color = skinTone, start = Offset(elbowX, elbowY), end = Offset(handX, handY), strokeWidth = 8f, cap = StrokeCap.Round)
            // Wrist wristband
            drawCircle(color = jerseyAccent, radius = 5f, center = Offset(handX - 4f, handY + 2f))
        }

        isFlying -> {
            // RELEASE / FOLLOW-THROUGH POSE: Iconic "Swan Neck" follow through wrist snap!
            val shoulderAnchor = Offset(torsoX + 10f, shoulderY)
            val elbowAnchor = Offset(torsoX + 26f, shoulderY - 26f)
            val wristAnchor = Offset(torsoX + 38f, shoulderY - 48f)
            val fingerTip = Offset(torsoX + 42f, shoulderY - 42f) // Bent wrist downward!

            // Shooting Arm
            drawLine(color = skinTone, start = shoulderAnchor, end = elbowAnchor, strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(color = skinTone, start = elbowAnchor, end = wristAnchor, strokeWidth = 8f, cap = StrokeCap.Round)
            // Snapped wrist / fingers
            drawLine(color = skinTone, start = wristAnchor, end = fingerTip, strokeWidth = 6f, cap = StrokeCap.Round)
            // Wristband
            drawCircle(color = jerseyAccent, radius = 5f, center = wristAnchor)

            // Left Guide Hand (Held steady)
            drawLine(color = skinShadow, start = Offset(torsoX + 4f, shoulderY + 4f), end = Offset(torsoX + 18f, shoulderY - 20f), strokeWidth = 7f, cap = StrokeCap.Round)
        }

        ballState == BallState.SCORED -> {
            // CELEBRATION POSE: Fist pump high in the air!
            val fistY = shoulderY - 46f
            drawLine(color = skinTone, start = Offset(torsoX + 10f, shoulderY), end = Offset(torsoX + 20f, shoulderY - 24f), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(color = skinTone, start = Offset(torsoX + 20f, shoulderY - 24f), end = Offset(torsoX + 22f, fistY), strokeWidth = 8f, cap = StrokeCap.Round)
            drawCircle(color = skinTone, radius = 7f, center = Offset(torsoX + 22f, fistY)) // Fist
        }

        else -> {
            // IDLE POSE: Holding the ball in front ready to shoot
            val elbowX = torsoX + 14f
            val elbowY = shoulderY + 12f
            val handX = torsoX + 22f
            val handY = shoulderY - 4f

            drawLine(color = skinTone, start = Offset(torsoX + 8f, shoulderY + 2f), end = Offset(elbowX, elbowY), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(color = skinTone, start = Offset(elbowX, elbowY), end = Offset(handX, handY), strokeWidth = 8f, cap = StrokeCap.Round)
            drawCircle(color = jerseyAccent, radius = 5f, center = Offset(handX - 4f, handY))
        }
    }
}

private fun DrawScope.drawSneakerSide(ankleX: Float, ankleY: Float, mainColor: Color, accentColor: Color) {
    // High-top basketball shoe facing right
    val shoePath = Path().apply {
        moveTo(ankleX - 6f, ankleY)
        lineTo(ankleX + 6f, ankleY)
        lineTo(ankleX + 8f, ankleY + 8f)
        lineTo(ankleX + 20f, ankleY + 8f) // Toe
        lineTo(ankleX + 20f, ankleY + 14f) // Sole front
        lineTo(ankleX - 8f, ankleY + 14f) // Sole heel
        lineTo(ankleX - 8f, ankleY + 4f)
        close()
    }
    drawPath(path = shoePath, color = mainColor)

    // Rubber Grip Outsole
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(ankleX - 8f, ankleY + 12f),
        size = Size(28f, 3f)
    )

    // Sneaker Accent Swoosh
    drawLine(
        color = accentColor,
        start = Offset(ankleX - 2f, ankleY + 7f),
        end = Offset(ankleX + 12f, ankleY + 8f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

// -------------------------------------------------------------
// DRAWING EXTENSION FUNCTIONS: REALISTIC SCHOOL HOOP & HARDWOOD GOAL
// -------------------------------------------------------------

private fun DrawScope.drawSchoolHoopStructure(hoop: Hoop, groundY: Float) {
    val bbX = hoop.backboardX
    val bbY = hoop.backboardY
    val bbW = hoop.backboardWidth
    val bbH = hoop.backboardHeight

    // 1. Heavy-Duty Steel Cantilever Arm & Support Post (Tiang Basket Hidrolik Sekolah)
    val postFloorX = bbX + bbW + 65f
    // Floor anchor base padding
    drawRoundRect(
        color = Color(0xFF1E3A8A), // Navy heavy padded base
        topLeft = Offset(postFloorX - 16f, groundY - 45f),
        size = Size(48f, 45f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(postFloorX - 16f, groundY - 45f),
        size = Size(48f, 45f),
        style = Stroke(width = 2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )

    // Main steel angled post
    drawLine(
        color = Color(0xFF334155),
        start = Offset(postFloorX, groundY - 40f),
        end = Offset(bbX + bbW + 28f, bbY + bbH * 0.45f),
        strokeWidth = 16f,
        cap = StrokeCap.Round
    )
    // Hydraulic cylinder brace
    drawLine(
        color = Color(0xFF64748B),
        start = Offset(postFloorX - 8f, groundY - 30f),
        end = Offset(bbX + bbW + 18f, bbY + bbH * 0.70f),
        strokeWidth = 7f,
        cap = StrokeCap.Round
    )
    // Upper backboard extension neck
    drawLine(
        color = Color(0xFF475569),
        start = Offset(bbX + bbW + 28f, bbY + bbH * 0.45f),
        end = Offset(bbX + bbW, bbY + bbH * 0.45f),
        strokeWidth = 12f,
        cap = StrokeCap.Square
    )

    // 2. Crystal Clear Tempered Glass Backboard (Papan Pantul Transparan)
    drawRoundRect(
        color = Color(0xAAFFFFFF),
        topLeft = Offset(bbX, bbY),
        size = Size(bbW, bbH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(bbX, bbY),
        size = Size(bbW, bbH),
        style = Stroke(width = 2.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
    )

    // Backboard High-Density Bottom Crash Pad (Busa Pelindung Bawah Papan)
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(bbX - 2f, bbY + bbH - 6f),
        size = Size(bbW + 4f, 8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
    )

    // Official Backboard Orange Target Rectangle (Kotak Target Pantul)
    val sqTop = hoop.y - 28f
    val sqH = 42f
    drawRect(
        color = Color(0xFFEA580C),
        topLeft = Offset(bbX - 2f, sqTop),
        size = Size(4f, sqH)
    )

    // 3. Steel Bracket & Breakaway Spring Mechanism
    drawRect(
        color = Color(0xFFDC2626),
        topLeft = Offset(hoop.x + hoop.rimWidth / 2f - 4f, hoop.y - 4f),
        size = Size(bbX - (hoop.x + hoop.rimWidth / 2f) + 4f, 9f)
    )
    // Spring Cover Box
    drawRoundRect(
        color = Color(0xFFB91C1C),
        topLeft = Offset(bbX - 12f, hoop.y - 7f),
        size = Size(14f, 14f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
    )

    // 4. Realistic Basketball Net with Swish Physics & Ripple
    val rimLeft = hoop.x - hoop.rimWidth / 2f
    val rimRight = hoop.x + hoop.rimWidth / 2f
    val netHeight = 52f + hoop.netRippleProgress * 16f
    val netBottomLeft = rimLeft + 12f + (hoop.netRippleProgress * 10f)
    val netBottomRight = rimRight - 12f + (hoop.netRippleProgress * 10f)

    val netSegments = 5
    for (i in 0..netSegments) {
        val frac = i.toFloat() / netSegments
        val topX = rimLeft + frac * (rimRight - rimLeft)
        val botX = netBottomLeft + frac * (netBottomRight - netBottomLeft)

        // Diagonal criss-cross strands
        drawLine(
            color = Color(0xEEFFFFFF),
            start = Offset(topX, hoop.y),
            end = Offset(netBottomRight - frac * (netBottomRight - netBottomLeft), hoop.y + netHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xEEFFFFFF),
            start = Offset(topX, hoop.y),
            end = Offset(botX, hoop.y + netHeight),
            strokeWidth = 2f
        )
    }

    // Horizontal net loops
    for (j in 1..3) {
        val fracY = j.toFloat() / 4f
        val leftX = rimLeft + fracY * (netBottomLeft - rimLeft)
        val rightX = rimRight + fracY * (netBottomRight - rimRight)
        val y = hoop.y + fracY * netHeight
        drawLine(
            color = Color(0xDDFFFFFF),
            start = Offset(leftX, y),
            end = Offset(rightX, y),
            strokeWidth = 1.6f
        )
    }

    // 5. Back Rim Bar (Back section of official orange ring)
    drawLine(
        color = Color(0xFFB91C1C),
        start = Offset(rimLeft, hoop.y),
        end = Offset(rimRight, hoop.y),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawFrontRimLip(hoop: Hoop) {
    val rimLeft = hoop.x - hoop.rimWidth / 2f
    val rimRight = hoop.x + hoop.rimWidth / 2f

    // Front rim steel peg lips
    drawCircle(color = Color(0xFFDC2626), radius = 5.5f, center = Offset(rimLeft, hoop.y))
    drawCircle(color = Color(0xFFDC2626), radius = 5.5f, center = Offset(rimRight, hoop.y))
}

// -------------------------------------------------------------
// DRAWING EXTENSION FUNCTIONS: BASKETBALL, TRAJECTORY & UI
// -------------------------------------------------------------

private fun DrawScope.drawBasketball(ball: Ball, skin: BallSkin) {
    val r = ball.radius
    val center = Offset(ball.x, ball.y)

    // Ball Glow (if Money Ball or special skin)
    if (ball.isMoneyBall) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x99F59E0B), Color(0x44EF4444), Color.Transparent),
                center = center,
                radius = r * 1.6f
            ),
            radius = r * 1.6f,
            center = center
        )
    }

    // Base Sphere Gradient (3D volumetric light source from top-left)
    val lightOffset = Offset(center.x - r * 0.35f, center.y - r * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(skin.glowColorHex),
                Color(skin.primaryColorHex),
                Color(0xFF7C2D12)
            ),
            center = lightOffset,
            radius = r * 1.2f
        ),
        radius = r,
        center = center
    )

    // Basketball Seams & Grooves with Rotation
    rotate(degrees = ball.rotationAngle, pivot = center) {
        // Horizontal seam
        drawLine(
            color = Color(skin.lineHex),
            start = Offset(center.x - r * 0.95f, center.y),
            end = Offset(center.x + r * 0.95f, center.y),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        // Vertical seam
        drawLine(
            color = Color(skin.lineHex),
            start = Offset(center.x, center.y - r * 0.95f),
            end = Offset(center.x, center.y + r * 0.95f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        // Curved side seam
        drawArc(
            color = Color(skin.lineHex),
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(center.x - r * 0.7f, center.y - r * 0.85f),
            size = Size(r * 1.4f, r * 1.7f),
            style = Stroke(width = 2.2f)
        )
    }

    // Specular Highlight
    drawCircle(
        color = Color(0x66FFFFFF),
        radius = r * 0.22f,
        center = lightOffset
    )
}

private fun DrawScope.drawTrajectoryDottedLine(
    points: List<Pair<Float, Float>>,
    skin: BallSkin
) {
    val dotColor = Color(skin.glowColorHex)
    points.forEachIndexed { index, point ->
        val progress = index.toFloat() / points.size
        val alpha = (1f - progress * 0.45f).coerceIn(0.2f, 0.95f)
        val radius = (5.5f - progress * 2.2f).coerceAtLeast(2.5f)

        // Outer glow dot
        drawCircle(
            color = dotColor.copy(alpha = alpha * 0.45f),
            radius = radius * 1.8f,
            center = Offset(point.first, point.second)
        )
        // Solid center dot
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(point.first, point.second)
        )
    }
}

private fun DrawScope.drawBallTrail(
    trail: List<Pair<Float, Float>>,
    skin: BallSkin,
    ballRadius: Float
) {
    val path = Path()
    trail.forEachIndexed { i, pt ->
        if (i == 0) path.moveTo(pt.first, pt.second)
        else path.lineTo(pt.first, pt.second)
    }
    drawPath(
        path = path,
        color = Color(skin.trailColorHex),
        style = Stroke(
            width = ballRadius * 0.85f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawAimGuide(start: Offset, current: Offset, skin: BallSkin) {
    // 1. Dotted pull-back guideline
    drawLine(
        color = Color(skin.glowColorHex).copy(alpha = 0.7f),
        start = start,
        end = current,
        strokeWidth = 3.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )

    // 2. Drag control ring & crosshair
    drawCircle(
        color = Color(skin.glowColorHex).copy(alpha = 0.3f),
        radius = 16f,
        center = current
    )
    drawCircle(
        color = Color(skin.glowColorHex),
        radius = 8f,
        center = current,
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = current
    )

    // 3. Dynamic Height & Power Live Indicator Pill
    val pullDx = start.x - current.x
    val pullDy = start.y - current.y
    val rawPower = hypot(pullDx, pullDy) * 4.6f
    val powerPercent = (rawPower / 14f).toInt().coerceIn(10, 100)

    val angleDeg = (Math.toDegrees(atan2(pullDy.toDouble(), if (pullDx >= 0f) pullDx.toDouble() else -pullDx.toDouble()))).toInt()
    val arcType = when {
        pullDy > 140f -> "🚀 Arc Super Tinggi"
        pullDy > 80f -> "🎯 Arc Tinggi Optimal"
        pullDy > 30f -> "⚡ Arc Sedang"
        else -> "🏹 Arc Datar"
    }

    // Pill background
    val pillW = 190f
    val pillH = 46f
    val pillX = (current.x - pillW / 2f).coerceIn(10f, size.width - pillW - 10f)
    val pillY = (current.y - pillH - 24f).coerceIn(10f, size.height - pillH - 10f)

    drawRoundRect(
        color = Color(0xDD0F172A),
        topLeft = Offset(pillX, pillY),
        size = Size(pillW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(skin.glowColorHex),
        topLeft = Offset(pillX, pillY),
        size = Size(pillW, pillH),
        style = Stroke(width = 1.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
    )

    drawIntoCanvas { canvas ->
        CourtPaintCache.aimTextWhite.textSize = 15f
        CourtPaintCache.aimTextGold.textSize = 13f
        canvas.nativeCanvas.drawText(arcType, pillX + pillW / 2f, pillY + 18f, CourtPaintCache.aimTextWhite)
        canvas.nativeCanvas.drawText("Tenaga: $powerPercent% | Sudut: ${-angleDeg}°", pillX + pillW / 2f, pillY + 36f, CourtPaintCache.aimTextGold)
    }
}

private fun DrawScope.drawCourtWatermark(size: Size, groundY: Float) {
    // Watermark at the bottom court floor: "Created By : Cikgu Dasril Amri"
    val wmX = size.width * 0.5f
    val wmY = size.height - 18f

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("🏀 Created By : Cikgu Dasril Amri", wmX, wmY - 14f, CourtPaintCache.watermarkMain)
        canvas.nativeCanvas.drawText("Bebas Atur Tarikan, Sudut & Ketinggian Bola Sesuai Keinginan", wmX, wmY + 4f, CourtPaintCache.watermarkSub)
    }
}

private fun DrawScope.drawSchoolBanner(
    topLeft: Offset,
    width: Float,
    height: Float,
    mainColor: Color,
    accentColor: Color,
    title: String,
    subtitle: String
) {
    // Banner body
    val bannerPath = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topLeft.x + width, topLeft.y)
        lineTo(topLeft.x + width, topLeft.y + height)
        lineTo(topLeft.x + width / 2f, topLeft.y + height - 12f) // V-cut bottom
        lineTo(topLeft.x, topLeft.y + height)
        close()
    }
    drawPath(path = bannerPath, color = mainColor)
    drawPath(path = bannerPath, color = accentColor, style = Stroke(width = 2f))

    // Banner texts
    drawIntoCanvas { canvas ->
        CourtPaintCache.bannerSub.color = accentColor.toArgb()
        canvas.nativeCanvas.drawText(title, topLeft.x + width / 2f, topLeft.y + height * 0.42f, CourtPaintCache.bannerTitle)
        canvas.nativeCanvas.drawText(subtitle, topLeft.x + width / 2f, topLeft.y + height * 0.72f, CourtPaintCache.bannerSub)
    }
}

private fun DrawScope.drawGymScoreboard(
    topLeft: Offset,
    width: Float,
    height: Float
) {
    // Scoreboard outer frame
    drawRoundRect(
        color = Color(0xFF090D16),
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = topLeft,
        size = Size(width, height),
        style = Stroke(width = 2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )

    // Scoreboard header
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText("SMA 1   PERIOD 2   SMA 2", topLeft.x + width / 2f, topLeft.y + height * 0.38f, CourtPaintCache.scoreboardHeader)
        canvas.nativeCanvas.drawText("58  ⏱️ 24s  52", topLeft.x + width / 2f, topLeft.y + height * 0.78f, CourtPaintCache.scoreboardScores)
    }
}

private fun DrawScope.drawScorePopup(popup: ScorePopup, density: androidx.compose.ui.unit.Density) {
    val paint = CourtPaintCache.scorePopupPaint
    paint.textSize = if (popup.isCritical) 54f else 42f
    paint.alpha = (popup.alpha * 255).toInt().coerceIn(0, 255)

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            popup.text,
            popup.x,
            popup.y,
            paint
        )
    }
}

private object CourtPaintCache {
    val aimTextWhite = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val aimTextGold = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#F59E0B")
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val watermarkMain = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E2E8F0")
        textSize = 21f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        setShadowLayer(6f, 0f, 2f, android.graphics.Color.parseColor("#0F172A"))
        alpha = 190
    }
    val watermarkSub = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FDE047")
        textSize = 14f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.parseColor("#0F172A"))
        alpha = 210
    }
    val bannerTitle = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 20f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val bannerSub = android.graphics.Paint().apply {
        textSize = 15f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val scoreboardHeader = android.graphics.Paint().apply {
        color = android.graphics.Color.YELLOW
        textSize = 18f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val scoreboardScores = android.graphics.Paint().apply {
        color = android.graphics.Color.RED
        textSize = 24f
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val scorePopupPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
        setShadowLayer(8f, 0f, 4f, android.graphics.Color.BLACK)
    }
}
