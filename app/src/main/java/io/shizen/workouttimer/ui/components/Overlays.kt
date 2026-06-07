package io.shizen.workouttimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.shizen.workouttimer.ui.theme.WT
import io.shizen.workouttimer.ui.theme.WtFonts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WtBottomSheet(
    onDismiss: () -> Unit,
    title: String?,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WT.Surface,
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(WT.Line)
                        .width(38.dp)
                        .height(4.dp),
                )
            }
        },
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            if (title != null) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WT.Text,
                    modifier = Modifier.padding(bottom = 14.dp, start = 2.dp),
                )
            }
            content()
        }
    }
}

@Composable
fun MenuRow(
    icon: String,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressScale(pressed = 0.98f, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WtIcon(icon, size = 20.dp, color = if (danger) WT.Danger else WT.Muted)
        Text(
            label,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) WT.Danger else WT.Text,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String?,
    confirmLabel: String,
    danger: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(WT.Surface)
                .border(1.dp, WT.Line, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = WT.Text)
            if (body != null) {
                Text(
                    body,
                    fontSize = 14.sp,
                    color = WT.Muted,
                    fontFamily = WtFonts.Sans,
                    modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
                )
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 9.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn(
                    "Cancel",
                    onClick = onDismiss,
                    variant = BtnVariant.Ghost,
                    fillMaxWidth = true,
                    modifier = Modifier.weight(1f),
                )
                Btn(
                    confirmLabel,
                    onClick = onConfirm,
                    variant = if (danger) BtnVariant.Danger else BtnVariant.Primary,
                    fillMaxWidth = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
