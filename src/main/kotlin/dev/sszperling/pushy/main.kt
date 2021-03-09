package dev.sszperling.pushy

import androidx.compose.desktop.Window
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*

fun main() {
	Window(title = "Push Notification Sender") {
		val adbFound = remember { ensureAdb() }
		MaterialTheme {
			if (!adbFound) {
				Column(
					modifier = Modifier.fillMaxSize(),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Text("ADB could not be found", style = MaterialTheme.typography.h5)
					Spacer(Modifier.height(8.dp))
					Text("Please make sure you have adb properly installed and in the system path")
				}
				return@MaterialTheme
			}

			val scope = rememberCoroutineScope()
			val vm = remember(scope) { PushViewModel(scope) }

			Row(modifier = Modifier.fillMaxSize()) {
				val scrollState = rememberScrollState()
				Column(
					modifier = Modifier
						.weight(1f).verticalScroll(scrollState)
						.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					FormRow(title = "Preset") {
						val (menuOpen, toggleMenu) = remember { mutableStateOf(false) }
						DropDownList(
							title = vm.preset?.name ?: "Pick a preset",
							expanded = menuOpen,
							onExpandedChanged = toggleMenu,
						) {
							Preset.values().forEach {
								SimpleDropdownMenuItem(
									text = it.name,
									onClick = {
										toggleMenu(false)
										vm.preset = it
									}
								)
							}
						}
					}

					val preset = vm.preset ?: return@Column

					FormSpacer()

					Refreshable(
						enabled = !vm.updating,
						onClickUpdate = { vm.updateDevices() },
						title = "Device",
					) {
						val (menuOpen, toggleMenu) = remember { mutableStateOf(false) }
						DropDownList(
							title = vm.device ?: if (vm.devices.isEmpty()) "No devices found" else "Pick a device",
							expanded = menuOpen,
							onExpandedChanged = toggleMenu,
							enabled = vm.devices.isNotEmpty(),
							buttonContent = {
								var tooltipVisible by remember { mutableStateOf(false) }
								if (!vm.rootedDevice) {
									Box {
										Icon(
											Icons.Filled.Warning,
											contentDescription = null,
											modifier = Modifier.clickable { tooltipVisible = true },
											tint = Color(0xffffcc00)
										)
										if (tooltipVisible) {
											Tooltip(onDismissRequest = { tooltipVisible = false}) {
												Text("This device is not rooted.", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
												Spacer(Modifier.height(8.dp))
												Text("The push notification broadcast\nmay not be properly sent.", color = Color.White)
											}
										}
									}
								}
							}
						) {
							vm.devices.forEach {
								SimpleDropdownMenuItem(
									text = it,
									onClick = {
										toggleMenu(false)
										vm.device = it
									}
								)
							}
						}
					}

					FormSpacer()

					FormRow(title = "Action") {
						if (preset == Preset.Custom) {
							CustomTextField(
								value = vm.action,
								onValueChange = { vm.action = it },
								modifier = Modifier.weight(1f),
								fontFamily = FontFamily.Monospace,
							)
						} else {
							Text(
								vm.action,
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
							title = vm.pkg ?: if (vm.receivers.isEmpty()) "No packages found" else "Pick a package",
							expanded = menuOpen,
							onExpandedChanged = toggleMenu,
							enabled = vm.receivers.isNotEmpty(),
						) {
							vm.receivers.keys.forEach {
								SimpleDropdownMenuItem(
									text = it,
									onClick = {
										toggleMenu(false)
										vm.pkg = it
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
						val selectedPackage = vm.pkg
						DropDownList(
							title = vm.receiver
								?: if (vm.receivers.isEmpty()) "No receiver found" else "Pick a receiver",
							expanded = menuOpen,
							onExpandedChanged = toggleMenu,
							enabled = selectedPackage != null && vm.receivers[selectedPackage]?.isNotEmpty() == true,
						) {
							if (selectedPackage != null && selectedPackage in vm.receivers) {
								vm.receivers.getValue(selectedPackage).forEach {
									SimpleDropdownMenuItem(
										text = it,
										onClick = {
											toggleMenu(false)
											vm.receiver = it
										}
									)
								}
							}
						}
					}

					FormSpacer()

					Column(modifier = Modifier.background(Color.White)) {
						ReadOnlyTableRow("Extra Key", "Extra Value")
						preset.defaultExtras.forEach { (k, v) ->
							ReadOnlyTableRow(k, v)
						}
						vm.extras.forEachIndexed { idx, (k, v) ->
							EditableTableRow(
								k, { vm.extras[idx] = Extra(it, v) },
								v, { vm.extras[idx] = Extra(k, it) },
								k !in preset.requiredExtras,
								{ vm.extras.removeAt(idx) }
							)
						}
					}
					IconButton(onClick = { vm.extras.add(Extra()) }) {
						Icon(Icons.Filled.Add, contentDescription = null)
					}

					FormSpacer()

					Button(
						onClick = { vm.doPush() },
						enabled = !vm.pushing && vm.valid,
					) {
						Text("Send push!")
					}
				}

				VerticalScrollbar(
					modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp, vertical = 16.dp),
					adapter = rememberScrollbarAdapter(scrollState)
				)
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

@Composable
fun Tooltip(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
	val yOffset = with(LocalDensity.current) { 36.dp.roundToPx() }
	Popup(
		Alignment.TopCenter,
		IntOffset(0, yOffset),
		isFocusable = true,
		onDismissRequest = onDismissRequest) {
		Column(
			modifier = Modifier
				.clip(MaterialTheme.shapes.medium)
				.background(Color.Black.copy(alpha = 0.8f))
				.padding(8.dp),
			content = content
		)
	}
}
