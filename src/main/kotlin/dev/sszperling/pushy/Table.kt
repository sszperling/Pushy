package dev.sszperling.pushy

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ReadOnlyTableRow(key: String, value: String, rowModifier: Modifier = Modifier, cellModifier: Modifier = Modifier) {
    TableRow(
        keyContent = { Text(key, modifier = it) },
        valueContent = { Text(value, modifier = it) },
        rowModifier,
        cellModifier
    )
}

@Composable
fun EditableTableRow(
    key: String,
    onKeyChange: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit,
    rowModifier: Modifier = Modifier,
    cellModifier: Modifier = Modifier
) {
    TableRow(
        keyContent = { CustomTextField(key, onKeyChange, modifier = it) },
        valueContent = {
			CustomTextField(value, onValueChange, modifier = it)
			if (canDelete)
				Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.clickable(onClick = onDelete).padding(4.dp))
		},
        rowModifier,
        cellModifier
    )
}

@Composable
fun TableRow(
    keyContent: @Composable (Modifier) -> Unit,
    valueContent: @Composable (Modifier) -> Unit,
    rowModifier: Modifier = Modifier,
    cellModifier: Modifier = Modifier,
) {
    Row(modifier = rowModifier) {
        val actualCellModifier = cellModifier.weight(1f).padding(8.dp)
		Row(modifier = Modifier.weight(1f).border(1.dp, Color.Black)) {
			keyContent(actualCellModifier)
		}
		Row(modifier = Modifier.weight(1f).border(1.dp, Color.Black)) {
			valueContent(actualCellModifier)
		}
    }
}
