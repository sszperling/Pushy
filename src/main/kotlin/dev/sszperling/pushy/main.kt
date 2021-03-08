package dev.sszperling.pushy

import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

fun main() = Window(title = "Push Notification Sender") {
	MaterialTheme {
		var pushing by remember { mutableStateOf(false) }
		var updateDevices by remember { mutableStateOf(true) }
		var devices by remember { mutableStateOf(listOf<String>()) }
		var receivers by remember { mutableStateOf(mapOf<String, List<String>>()) }
		var selectedDevice by remember { mutableStateOf<String?>(null) }
		var selectedPackage by remember { mutableStateOf<String?>(null) }
		var selectedReceiver by remember { mutableStateOf<String?>(null) }
		var preset by remember { mutableStateOf<Preset?>(null) }
		val extras = remember { mutableStateListOf<Extra>() }
		var customAction by remember { mutableStateOf("") }

		LaunchedEffect(preset) {
			preset?.requiredExtras?.forEach { key ->
				extras.find { extra -> extra.key == key } ?: extras.add(Extra(key))
			}
		}

		LaunchedEffect(devices) {
			selectedDevice = devices.singleOrNull()
		}

		LaunchedEffect(selectedDevice) {
			ensureAdbdRoot(selectedDevice ?: return@LaunchedEffect)
		}

		LaunchedEffect(selectedDevice, preset, customAction) {
			val action = when (val currentPreset = preset) {
				Preset.Custom -> customAction
				null -> return@LaunchedEffect
				else -> currentPreset.action
			}
			getReceivers(selectedDevice ?: return@LaunchedEffect, action).let {
				if (receivers != it) {
					receivers = it
				}
			}
		}

		LaunchedEffect(receivers) {
			selectedPackage = receivers.keys.singleOrNull()
		}

		LaunchedEffect(selectedPackage) {
			selectedReceiver = if (selectedPackage != null) receivers[selectedPackage]?.singleOrNull() else null
		}

		LaunchedEffect(updateDevices) {
			if (updateDevices) {
				devices = getAdbDevices()
				updateDevices = false
			}
		}

		val valid by derivedStateOf {
			selectedDevice != null && selectedPackage != null && selectedReceiver != null && when (val curPreset =
				preset) {
				null -> false
				Preset.Custom -> customAction.isNotBlank()
				else -> curPreset.requiredExtras.all { extras.any { (k, v) -> k == it && v.isNotBlank() } }
			}
		}

		LaunchedEffect(pushing) {
			try {
				if (!pushing || !valid) return@LaunchedEffect

				val deviceId = selectedDevice ?: return@LaunchedEffect
				val packageId = selectedPackage ?: return@LaunchedEffect
				val receiver = selectedReceiver ?: return@LaunchedEffect
				val currentPreset = preset ?: return@LaunchedEffect
				val currentExtras = extras.map { (k, v) -> k to v }.toMap()

				if (currentPreset == Preset.Custom) {
					doBroadcast(deviceId, customAction, packageId, receiver, currentExtras)
				} else {
					doBroadcast(
						deviceId, currentPreset.action, packageId, receiver, currentPreset.defaultExtras + currentExtras
					)
				}
			} finally {
				pushing = false
			}
		}

		val scrollState = rememberScrollState()
		Column(
			modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(16.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			FormRow(title = "Preset") {
				val (menuOpen, toggleMenu) = remember { mutableStateOf(false) }
				DropDownList(
					title = preset?.name ?: "Pick a preset",
					expanded = menuOpen,
					onExpandedChanged = toggleMenu,
				) {
					Preset.values().forEach {
						SimpleDropdownMenuItem(
							text = it.name,
							onClick = {
								toggleMenu(false)
								preset = it
							}
						)
					}
				}
			}

			if (preset == null) return@Column

			FormSpacer()

			Refreshable(
				enabled = !updateDevices,
				onClickUpdate = { updateDevices = true },
				title = "Device",
			) {
				val (menuOpen, toggleMenu) = remember { mutableStateOf(false) }
				DropDownList(
					title = selectedDevice ?: if (devices.isEmpty()) "No devices found" else "Pick a device",
					expanded = menuOpen,
					onExpandedChanged = toggleMenu,
					enabled = devices.isNotEmpty(),
				) {
					devices.forEach { it ->
						SimpleDropdownMenuItem(
							text = it,
							onClick = {
								toggleMenu(false)
								selectedDevice = it
							}
						)
					}
				}
			}

			FormSpacer()

			FormRow(title = "Action") {
				if (preset == Preset.Custom) {
					CustomTextField(
						value = customAction,
						onValueChange = { customAction = it },
						modifier = Modifier.weight(1f),
						fontFamily = FontFamily.Monospace,
					)
				} else {
					Text(
						preset?.action ?: "",
						modifier = Modifier.weight(1f),
						fontFamily = FontFamily.Monospace,
					)
				}
			}

			FormSpacer()

			FormRow(
				title = "Package ID",
			) {
				val (menuOpen, toggleMenu) = remember { mutableStateOf(false) }
				DropDownList(
					title = selectedPackage ?: if (receivers.isEmpty()) "No packages found" else "Pick a package",
					expanded = menuOpen,
					onExpandedChanged = toggleMenu,
					enabled = receivers.isNotEmpty(),
				) {
					receivers.keys.forEach { it ->
						SimpleDropdownMenuItem(
							text = it,
							onClick = {
								toggleMenu(false)
								selectedPackage = it
							}
						)
					}
				}
			}

			FormSpacer()

			FormRow(
				title = "Receiver",
			) {
				val (menuOpen, toggleMenu) = remember { mutableStateOf(false) }
				DropDownList(
					title = selectedReceiver ?: if (receivers.isEmpty()) "No receiver found" else "Pick a receiver",
					expanded = menuOpen,
					onExpandedChanged = toggleMenu,
					enabled = selectedPackage != null && receivers[selectedPackage]?.isNotEmpty() == true,
				) {
					if (selectedPackage != null && selectedPackage in receivers) {
						receivers.getValue(selectedPackage!!).forEach {
							SimpleDropdownMenuItem(
								text = it,
								onClick = {
									toggleMenu(false)
									selectedReceiver = it
								}
							)
						}
					}
				}
			}

			FormSpacer()

			Column(modifier = Modifier.background(Color.White)) {
				ReadOnlyTableRow("Extra Key", "Extra Value")
				preset?.run {
					defaultExtras.forEach { (k, v) ->
						ReadOnlyTableRow(k, v)
					}
					extras.forEachIndexed { idx, (k, v) ->
						EditableTableRow(
							k, { extras[idx] = Extra(it, v) },
							v, { extras[idx] = Extra(k, it) },
							preset?.requiredExtras?.contains(k) == false,
							{ extras.removeAt(idx) }
						)
					}
				}
			}
			IconButton(onClick = { extras.add(Extra("key", "value")) }) {
				Icon(Icons.Filled.Add, contentDescription = null)
			}

			FormSpacer()

			Button(
				onClick = { pushing = true },
				enabled = !pushing,
			) {
				Text("Send push!")
			}
		}
	}
}

@Composable
fun FormSpacer() = Spacer(Modifier.height(16.dp))

@Composable
fun Refreshable(
	enabled: Boolean,
	onClickUpdate: () -> Unit,
	modifier: Modifier = Modifier,
	title: String? = null,
	content: @Composable RowScope.() -> Unit,
) {
	FormRow(modifier, title) {
		FormRow(modifier = Modifier.weight(1f).padding(end = 8.dp), content = content)
		Button(onClick = onClickUpdate, enabled = enabled, contentPadding = PaddingValues(all = 0.dp)) {
			if (enabled) {
				Icon(Icons.Filled.Refresh, contentDescription = null)
			} else {
				CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), strokeWidth = 2.dp)
			}
		}
	}
}

@Composable
fun FormRow(
	modifier: Modifier = Modifier,
	title: String? = null,
	content: @Composable RowScope.() -> Unit
) {
	FormRow(modifier = modifier) {
		Text("$title: ", modifier = Modifier.width(100.dp))
		content()
	}
}

@Composable
fun FormRow(
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit
) {
	Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, content = content)
}
