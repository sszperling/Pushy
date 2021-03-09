package dev.sszperling.pushy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun DropDownList(
    title: String,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
	buttonContent: @Composable RowScope.() -> Unit = {},
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val widthModifier = Modifier.width(maxWidth)
        OutlinedButton(
            onClick = { onExpandedChanged(!expanded) },
            modifier = widthModifier,
            enabled = enabled,
        ) {
            Text(title, modifier = Modifier.weight(1f))
			buttonContent()
            // TODO ArrowDropUp
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChanged(false) },
            modifier = widthModifier.heightIn(max = 200.dp),
            content = dropdownContent,
        )
    }
}

@Composable
fun SimpleDropdownMenuItem(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    textModifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    DropdownMenuItem(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(text, modifier = textModifier)
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null
) {
	val localStyle = LocalTextStyle.current
	val style = if (fontFamily == null) localStyle else localStyle.copy(fontFamily = fontFamily)
    BasicTextField(value, onValueChange, Modifier.background(Color.LightGray).then(modifier), textStyle = style)
}
