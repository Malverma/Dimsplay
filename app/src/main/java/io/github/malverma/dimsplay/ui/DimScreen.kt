package io.github.malverma.dimsplay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.malverma.dimsplay.R
import io.github.malverma.dimsplay.data.MAX_LEVEL
import io.github.malverma.dimsplay.data.MIN_LEVEL
import kotlin.math.roundToInt

private val RingDiameter = 220.dp
private val RingStroke = 12.dp
private val ThumbSize = 28.dp

@Composable
fun DimScreen(
    level: Int,
    dimming: Boolean,
    extraDim: Boolean,
    showPermissionBanner: Boolean,
    onLevelChange: (Int) -> Unit,
    onLevelChangeFinished: () -> Unit,
    onDimmingChange: (Boolean) -> Unit,
    onExtraDimChange: (Boolean) -> Unit,
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controls = @Composable {
        Controls(level, dimming, extraDim, onLevelChange, onLevelChangeFinished, onDimmingChange, onExtraDimChange)
    }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(DimColors.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (maxWidth > maxHeight) {
            // Landscape: ring and controls side by side so the controls stay in view.
            Row(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                ) {
                    Header()
                    Spacer(Modifier.height(24.dp))
                    DimRing(level, Modifier.align(Alignment.CenterHorizontally))
                }
                Spacer(Modifier.width(24.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                ) {
                    if (showPermissionBanner) {
                        PermissionBanner(onGrantPermission)
                        Spacer(Modifier.height(16.dp))
                    }
                    controls()
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
            ) {
                Header()
                if (showPermissionBanner) {
                    Spacer(Modifier.height(23.dp))
                    PermissionBanner(onGrantPermission)
                }
                Spacer(Modifier.height(85.dp))
                DimRing(level, Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(74.dp))
                controls()
            }
        }
    }
}

@Composable
private fun Header() {
    Text(stringResource(R.string.app_name), style = DimType.Title)
    Spacer(Modifier.height(7.dp))
    Text(stringResource(R.string.subtitle), style = DimType.Subtitle)
}

@Composable
private fun Controls(
    level: Int,
    dimming: Boolean,
    extraDim: Boolean,
    onLevelChange: (Int) -> Unit,
    onLevelChangeFinished: () -> Unit,
    onDimmingChange: (Boolean) -> Unit,
    onExtraDimChange: (Boolean) -> Unit,
) {
    SliderCard(level, onLevelChange, onLevelChangeFinished)
    Spacer(Modifier.height(16.dp))
    ToggleCard(
        title = stringResource(R.string.dimming),
        caption = stringResource(if (dimming) R.string.dimming_on else R.string.dimming_off),
        checked = dimming,
        onCheckedChange = onDimmingChange,
        modifier = Modifier.testTag(TAG_DIMMING),
    )
    Spacer(Modifier.height(16.dp))
    ToggleCard(
        title = stringResource(R.string.extra_dim),
        caption = stringResource(R.string.extra_dim_caption),
        checked = extraDim,
        onCheckedChange = onExtraDimChange,
        modifier = Modifier.testTag(TAG_EXTRA_DIM),
    )
}

@Composable
private fun PermissionBanner(onGrantPermission: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(DimColors.Surface, CardShape)
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.permission_title), style = DimType.CardLabel)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.permission_body), style = DimType.CardCaption)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DimColors.Accent)
                .clickable(role = Role.Button, onClick = onGrantPermission),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.permission_button), style = DimType.Button)
        }
    }
}

@Composable
private fun DimRing(level: Int, modifier: Modifier = Modifier) {
    // The stroke is centered on the 220dp circle, so the box is one stroke wider.
    Box(modifier.size(RingDiameter + RingStroke), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(RingDiameter + RingStroke)) {
            val stroke = RingStroke.toPx()
            val diameter = RingDiameter.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawCircle(DimColors.Track, radius = diameter / 2, style = Stroke(stroke))
            if (level > 0) {
                drawArc(
                    color = DimColors.Accent,
                    startAngle = -90f,
                    sweepAngle = 360f * level / MAX_LEVEL,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.percent, level), style = DimType.RingValue)
            Text(stringResource(R.string.dim_level).uppercase(), style = DimType.RingCaption)
        }
    }
}

@Composable
private fun SliderCard(level: Int, onLevelChange: (Int) -> Unit, onLevelChangeFinished: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(DimColors.Surface, CardShape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dim_level), style = DimType.CardLabel, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.percent, level), style = DimType.CardValue, modifier = Modifier.testTag(TAG_LEVEL_VALUE))
        }
        Spacer(Modifier.height(4.dp))
        DimSlider(level, onLevelChange, onLevelChangeFinished)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.percent, MIN_LEVEL), style = DimType.RangeLabel)
            Text(stringResource(R.string.percent, MAX_LEVEL), style = DimType.RangeLabel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DimSlider(level: Int, onLevelChange: (Int) -> Unit, onLevelChangeFinished: () -> Unit) {
    val range = MIN_LEVEL.toFloat()..MAX_LEVEL.toFloat()
    val description = stringResource(R.string.dim_level)
    Slider(
        value = level.toFloat(),
        onValueChange = { onLevelChange(it.roundToInt()) },
        onValueChangeFinished = onLevelChangeFinished,
        valueRange = range,
        steps = MAX_LEVEL - MIN_LEVEL - 1,
        // Material insets its track by half the thumb; bleed out so the track spans the card
        // content and the thumb center lands on the value, as in the design.
        modifier = Modifier
            .bleedHorizontally(ThumbSize / 2)
            .semantics { contentDescription = description }
            .testTag(TAG_SLIDER),
        thumb = {
            Box(
                Modifier
                    .size(ThumbSize)
                    .shadow(4.dp, CircleShape)
                    .background(DimColors.White, CircleShape),
            )
        },
        track = { state ->
            val fraction = (state.value - range.start) / (range.endInclusive - range.start)
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(DimColors.Track, cornerRadius = radius)
                if (fraction > 0f) {
                    drawRoundRect(DimColors.Accent, size = Size(size.width * fraction, size.height), cornerRadius = radius)
                }
            }
        },
    )
}

private fun Modifier.bleedHorizontally(bleed: Dp) = layout { measurable, constraints ->
    val px = bleed.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + 2 * px,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + 2 * px else constraints.maxWidth,
        ),
    )
    layout(placeable.width - 2 * px, placeable.height) { placeable.place(-px, 0) }
}

@Composable
private fun ToggleCard(
    title: String,
    caption: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(DimColors.Surface)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DimType.CardLabel)
            Spacer(Modifier.height(4.dp))
            Text(caption, style = DimType.CardCaption)
        }
        Spacer(Modifier.width(16.dp))
        DimSwitch(checked)
    }
}

@Composable
private fun DimSwitch(checked: Boolean) {
    val knobOffset by animateDpAsState(if (checked) 25.dp else 3.dp, label = "knobOffset")
    val trackColor by animateColorAsState(if (checked) DimColors.Accent else DimColors.Track, label = "trackColor")
    val knobColor by animateColorAsState(if (checked) DimColors.Bg else DimColors.Muted, label = "knobColor")
    Box(Modifier.size(52.dp, 30.dp).background(trackColor, RoundedCornerShape(15.dp))) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = knobOffset)
                .size(24.dp)
                .background(knobColor, CircleShape),
        )
    }
}

const val TAG_SLIDER = "slider"
const val TAG_LEVEL_VALUE = "levelValue"
const val TAG_DIMMING = "dimming"
const val TAG_EXTRA_DIM = "extraDim"

@Preview(widthDp = 360, heightDp = 840)
@Composable
private fun DimScreenPreview() {
    DimsplayTheme {
        DimScreen(
            level = 40,
            dimming = true,
            extraDim = false,
            showPermissionBanner = false,
            onLevelChange = {},
            onLevelChangeFinished = {},
            onDimmingChange = {},
            onExtraDimChange = {},
            onGrantPermission = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 1016)
@Composable
private fun DimScreenPermissionPreview() {
    DimsplayTheme {
        DimScreen(
            level = 40,
            dimming = false,
            extraDim = false,
            showPermissionBanner = true,
            onLevelChange = {},
            onLevelChangeFinished = {},
            onDimmingChange = {},
            onExtraDimChange = {},
            onGrantPermission = {},
        )
    }
}
