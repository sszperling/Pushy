package dev.sszperling.pushy

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PushViewModel(private val scope: CoroutineScope) {
	var pushing by mutableStateOf(false)
		private set
	var updating by mutableStateOf(false)
		private set
	var devices by mutableStateOf(listOf<String>())
		private set
	var receivers by mutableStateOf(mapOf<String, List<String>>())
		private set

	var rootedDevice by mutableStateOf(true)
		private set

	private var customAction by mutableStateOf("")
	val extras: MutableList<Extra> = mutableStateListOf()

	private var selectedPreset: Preset? by mutableStateOf(null)
	private var selectedDevice by mutableStateOf<String?>(null)
	private var selectedPackage by mutableStateOf<String?>(null)
	private var selectedReceiver by mutableStateOf<String?>(null)

	var preset: Preset?
		get() = selectedPreset
		set(value) {
			selectedPreset = value
			value?.requiredExtras?.forEach { key ->
				if (extras.none { (k, _) -> k == key }) extras.add(Extra(key, ""))
			}
			updateReceivers()
		}

	var device: String?
		get() = selectedDevice
		set(value) {
			if (value != null && selectedDevice != value) {
				selectedDevice = value
				rootedDevice = ensureAdbdRoot(value)
				updateReceivers()
			}
		}

	var action: String
		get() = if (selectedPreset == Preset.Custom) customAction else selectedPreset?.action ?: ""
		set(value) {
			customAction = value
		}

	var pkg: String?
		get() = selectedPackage
		set(value) {
			if (selectedPackage != value) {
				selectedPackage = value
				receiver = if (value != null) receivers[pkg]?.singleOrNull() else null
			}
		}

	var receiver: String?
		get() = selectedReceiver
		set(value) {
			selectedReceiver = value
		}

	val valid by derivedStateOf {
		device != null
				&& pkg != null
				&& receiver != null
				&& action.isNotBlank()
				&& selectedPreset?.requiredExtras?.all { extras.any { (k, v) -> k == it && v.isNotBlank() } } == true
	}

	fun updateDevices() {
		updating = true
		scope.launch {
			devices = getAdbDevices()
			device = devices.singleOrNull()
			updating = false
		}
	}

	private fun updateReceivers() {
		val preset = this.selectedPreset ?: return
		val device = this.selectedDevice ?: return
		val action = if (preset == Preset.Custom) customAction else preset.action
		scope.launch {
			receivers = getReceivers(device, action)
			pkg = receivers.keys.singleOrNull()
		}
	}

	fun doPush() {
		if (pushing) return
		pushing = true
		scope.launch {
			try {
				if (!valid) return@launch
				val extras = preset!!.defaultExtras + extras.map { (k, v) -> k to v }.toMap()
				doBroadcast(device!!, action, pkg!!, receiver!!, extras)
			} finally {
				pushing = false
			}
		}
	}

	init {
		updateDevices()
	}
}
