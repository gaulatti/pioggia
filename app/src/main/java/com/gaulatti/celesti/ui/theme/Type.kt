package com.gaulatti.celesti.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.gaulatti.celesti.R

// Encode Sans - for code and titles
val EncodeSans = FontFamily(
    Font(R.font.encode_sans_regular, FontWeight.Normal),
    Font(R.font.encode_sans_semibold, FontWeight.SemiBold)
)

// Libre Franklin - for body text
val LibreFranklin = FontFamily(
    Font(R.font.libre_franklin_regular, FontWeight.Normal),
    Font(R.font.libre_franklin_medium, FontWeight.Medium)
)

// Set of Material typography styles to start with
@OptIn(ExperimentalTvMaterial3Api::class)
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = LibreFranklin,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)