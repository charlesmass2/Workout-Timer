package io.shizen.workouttimer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts
import androidx.compose.foundation.clickable as fClickable

// ── Press-scale clickable ───────────────────────────────────
@Composable
fun Modifier.pressScale(
    enabled: Boolean = true,
    pressed: Float = 0.95f,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (isPressed && enabled) pressed else 1f, label = "press")
    return this
        .scale(s)
        .fClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

// ── Buttons ─────────────────────────────────────────────────
enum class BtnVariant { Primary, Rest, Ghost, Outline, Danger }
enum class BtnSize { Sm, Md, Lg }

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BtnVariant = BtnVariant.Primary,
    size: BtnSize = BtnSize.Md,
    icon: String? = null,
    enabled: Boolean = true,
    fillMaxWidth: Boolean = false,
) {
    val h = when (size) { BtnSize.Lg -> 56.dp; BtnSize.Sm -> 38.dp; BtnSize.Md -> 48.dp }
    val padH = when (size) { BtnSize.Lg -> 24.dp; BtnSize.Sm -> 14.dp; BtnSize.Md -> 18.dp }
    val fontSize = if (size == BtnSize.Lg) 17.sp else 15.sp
    val (bg, fg, borderColor) = when (variant) {
        BtnVariant.Primary -> Triple(WT.Accent, WT.OnAccent, null)
        BtnVariant.Rest -> Triple(WT.Rest, WT.OnRest, null)
        BtnVariant.Ghost -> Triple(WT.Surface2, WT.Text, null)
        BtnVariant.Outline -> Triple(Color.Transparent, WT.Text, WT.Line)
        BtnVariant.Danger -> Triple(Color.Transparent, WT.Danger, WT.Danger.copy(alpha = 0.45f))
    }
    val iconSize = if (size == BtnSize.Lg) 22.dp else 18.dp
    val interaction = remember { MutableInteractionSource() }
    var m = modifier
        .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
        .height(h)
        .then(if (enabled) Modifier else Modifier.alpha(0.4f))
        .clip(RoundedCornerShape(14.dp))
        .background(bg)
    if (borderColor != null) m = m.border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
    Row(
        modifier = m
            .fClickable(
                interactionSource = interaction,
                indication = ripple(color = fg),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = padH),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) WtIcon(icon, size = iconSize, color = fg)
        Text(
            text,
            color = fg,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = WtFonts.Sans,
            maxLines = 2,
            textAlign = TextAlign.Center,
            lineHeight = fontSize * 1.1f,
        )
    }
}

@Composable
fun IconBtn(
    name: String,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    iconSize: Int = 22,
    active: Boolean = false,
    color: Color? = null,
    bg: Color? = null,
    enabled: Boolean = true,
) {
    val background = bg ?: if (active) WT.Accent else WT.Surface2
    val tint = color ?: if (active) WT.OnAccent else WT.Text
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size.dp)
            .then(if (enabled) Modifier else Modifier.alpha(0.35f))
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .fClickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = tint),
                enabled = enabled,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        WtIcon(name, size = iconSize.dp, color = tint)
    }
}

// ── Segmented control ───────────────────────────────────────
@Composable
fun <T> Seg(
    options: List<Pair<T, String>>,
    value: T,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WT.Surface)
            .border(1.dp, WT.Line, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) WT.Accent else Color.Transparent)
                    .pressScale(pressed = 0.97f) { onChange(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) WT.OnAccent else WT.Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Numeric stepper ─────────────────────────────────────────
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 9999,
    step: Int = 1,
    suffix: String? = null,
    width: Int = 116,
) {
    fun set(v: Int) = onValueChange(v.coerceIn(min, max))
    Row(
        modifier = modifier
            .width(width.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WT.Surface2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(40.dp, 44.dp).pressScale { set(value - step) },
            contentAlignment = Alignment.Center,
        ) { WtIcon("minus", size = 18.dp, color = WT.Text) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value.toString(),
                fontFamily = WtFonts.Mono,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = WT.Text,
            )
            if (suffix != null) {
                Text(suffix, fontSize = 11.sp, color = WT.Muted, fontFamily = WtFonts.Mono)
            }
        }
        Box(
            Modifier.size(40.dp, 44.dp).pressScale { set(value + step) },
            contentAlignment = Alignment.Center,
        ) { WtIcon("plus", size = 18.dp, color = WT.Text) }
    }
}

// ── Pill / chip ─────────────────────────────────────────────
enum class PillTone { Default, Accent, Rest }

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.Default,
    leading: String? = null,
) {
    val (bg, fg) = when (tone) {
        PillTone.Default -> WT.Surface2 to WT.Muted
        PillTone.Accent -> WT.Accent.copy(alpha = 0.18f) to WT.Accent
        PillTone.Rest -> WT.Rest.copy(alpha = 0.18f) to WT.Rest
    }
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (leading != null) WtIcon(leading, size = 13.dp, color = fg)
        Text(
            text,
            color = fg,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = WtFonts.Mono,
        )
    }
}

/** A small dot, used as a status indicator in labels. */
@Composable
fun Dot(color: Color, sizeDp: Int = 7) {
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(color))
}
