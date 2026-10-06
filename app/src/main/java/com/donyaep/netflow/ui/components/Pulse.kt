@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.donyaep.netflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.donyaep.netflow.R
import kotlin.coroutines.coroutineContext

// ─────────────────────────────────────────────────────────────────────────────
// Forma «pulso»
//
// La cifra principal de cada pantalla va dentro de una forma que pasa de casi
// un círculo a una cookie festoneada según un nivel de 0 a 1.
// ─────────────────────────────────────────────────────────────────────────────

const val CALM_DEGREES_PER_SECOND = 360f / 140f
const val BUSY_DEGREES_PER_SECOND = 360f / 48f

// Casi un círculo, pero con los mismos vértices que la cookie a la que se
// transforma: partiendo de un círculo liso los lóbulos crecían desiguales.
fun calmShape(points: Int): RoundedPolygon =
    RoundedPolygon.star(
        numVerticesPerRadius = points,
        innerRadius = 0.99f,
        rounding = CornerRounding(radius = 0.5f),
    ).normalized()

@Composable
fun rememberCookie12Morph(): Morph = remember { Morph(calmShape(points = 12), MaterialShapes.Cookie12Sided) }

@Composable
fun rememberCookie9Morph(): Morph = remember { Morph(calmShape(points = 9), MaterialShapes.Cookie9Sided) }

/**
 * Dibuja detrás del contenido el [morph] en el punto [level], ajustado al tamaño
 * del composable. [gapColor] añade un trazo que lo separa de lo que tenga debajo.
 */
fun Modifier.pulseShape(
    morph: Morph,
    level: () -> Float,
    color: () -> Color,
    gapColor: Color? = null,
): Modifier = drawWithCache {
    val path = morph.toPath(level())
    path.transform(Matrix().apply { scale(size.width, size.height) })
    path.translate(size.center - path.getBounds().center)
    val gap = Stroke(width = 6.dp.toPx())
    onDrawBehind {
        if (gapColor != null) drawPath(path, gapColor, style = gap)
        drawPath(path, color())
    }
}

/** Ángulo que avanza a [degreesPerSecond], para leerlo dentro de `graphicsLayer`. */
@Composable
fun rememberPulseRotation(degreesPerSecond: Float): FloatState {
    val rotation = remember { mutableFloatStateOf(0f) }
    val speed by rememberUpdatedState(degreesPerSecond)
    LaunchedEffect(Unit) {
        // Con las animaciones del sistema desactivadas la forma se queda quieta.
        if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            rotation.floatValue = (rotation.floatValue + (now - last) / 1e9f * speed) % 360f
            last = now
        }
    }
    return rotation
}

// ─────────────────────────────────────────────────────────────────────────────
// Botones conectados de 72 dp al pie de pantalla
// ─────────────────────────────────────────────────────────────────────────────

val PulseControlHeight = 72.dp
val PulseLeadingShapes = ButtonShapes(
    shape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp, topEnd = 12.dp, bottomEnd = 12.dp),
    pressedShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp),
)
val PulseTrailingShapes = IconButtonShapes(
    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 36.dp, bottomEnd = 36.dp),
    pressedShape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp),
)
val PulseSingleShapes = ButtonShapes(
    shape = RoundedCornerShape(36.dp),
    pressedShape = RoundedCornerShape(24.dp),
)

// ─────────────────────────────────────────────────────────────────────────────
// Reparto WiFi / datos móviles
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Barra de dos tramos con su leyenda. [wifiShare] es la parte de WiFi sobre el
 * total, o `null` si aún no hay consumo y solo se muestra la leyenda.
 */
@Composable
fun NetworkSplit(
    wifiShare: Float?,
    wifiLabel: String,
    mobileLabel: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (wifiShare != null) {
            // Cada tramo conserva un mínimo visible aunque su parte sea casi cero.
            val wifiWeight = wifiShare.coerceIn(0.04f, 0.96f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(wifiWeight)
                        .fillMaxSize()
                        .background(
                            cs.secondary,
                            RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                        ),
                )
                Box(
                    modifier = Modifier
                        .weight(1f - wifiWeight)
                        .fillMaxSize()
                        .background(
                            cs.tertiary,
                            RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 10.dp, bottomEnd = 10.dp),
                        ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "WiFi $wifiLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.secondary,
            )
            Text(
                text = "Móvil $mobileLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.tertiary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fila de lista
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Fila con el icono dentro de una forma. A la derecha va [trailing] o, si no,
 * [value]; sin ninguno de los dos, una flecha. [selected] la marca como la
 * opción elegida dentro de un grupo. Dentro de una hoja inferior conviene un
 * [container] más claro, porque el de por defecto se confunde con el fondo.
 */
@Composable
fun PulseRow(
    onClick: () -> Unit,
    shape: Shape,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    value: String? = null,
    selected: Boolean = false,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    trailing: @Composable (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) cs.secondaryContainer else container,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 68.dp)
                .padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pulseShape(
                        morph = rememberCookie9Morph(),
                        level = { 1f },
                        color = { if (selected) cs.primary else cs.secondaryContainer },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) cs.onPrimary else cs.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                supporting?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            when {
                trailing != null -> trailing()
                value != null -> Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.primary,
                )
                else -> Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = cs.outline,
                )
            }
        }
    }
}
