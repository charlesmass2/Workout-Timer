package io.shizen.workouttimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.shizen.workouttimer.ui.components.WtIcon
import io.shizen.workouttimer.ui.theme.WT

@Composable
fun StatChip(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        WtIcon(icon, size = 15.dp, color = WT.Faint)
        Text(text, color = WT.Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionLabel(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = 2.dp, top = 4.dp)) {
        Text(
            title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = WT.Faint,
        )
        if (sub != null) {
            Text(sub, fontSize = 12.sp, color = WT.Faint, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** A screen header: small uppercase eyebrow + large title. */
@Composable
fun ScreenHeader(eyebrow: String, title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                eyebrow.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = WT.Faint,
            )
            Text(
                title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                color = WT.Text,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (trailing != null) trailing()
    }
}
