package com.readtrack.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.readtrack.domain.model.BookStatus
import com.readtrack.presentation.ui.theme.AbandonedRed
import com.readtrack.presentation.ui.theme.FinishedBlue
import com.readtrack.presentation.ui.theme.OnHoldGray
import com.readtrack.presentation.ui.theme.ReadingOrange
import com.readtrack.presentation.ui.theme.WantToReadGreen

@Composable
fun BookStatusChip(
    status: BookStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        BookStatus.WANT_TO_READ -> WantToReadGreen to "想读"
        BookStatus.READING -> ReadingOrange to "在读"
        BookStatus.FINISHED -> FinishedBlue to "已读"
        BookStatus.ON_HOLD -> OnHoldGray to "闲置"
        BookStatus.ABANDONED -> AbandonedRed to "放弃"
    }

    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun getStatusColor(status: BookStatus): Color {
    return statusColorOf(status)
}

fun statusColorOf(status: BookStatus): Color {
    return when (status) {
        BookStatus.WANT_TO_READ -> WantToReadGreen
        BookStatus.READING -> ReadingOrange
        BookStatus.FINISHED -> FinishedBlue
        BookStatus.ON_HOLD -> OnHoldGray
        BookStatus.ABANDONED -> AbandonedRed
    }
}

fun statusLabelOf(status: BookStatus): String {
    return when (status) {
        BookStatus.WANT_TO_READ -> "想读"
        BookStatus.READING -> "在读"
        BookStatus.FINISHED -> "已读"
        BookStatus.ON_HOLD -> "闲置"
        BookStatus.ABANDONED -> "放弃"
    }
}
