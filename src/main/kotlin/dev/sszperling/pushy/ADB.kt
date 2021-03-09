package dev.sszperling.pushy

import java.util.UUID.randomUUID

private const val EXCLUDED_PKGS = "^(android|com\\.android|com\\.google|org\\.chromium)"
private val ExcludedRegex = Regex(EXCLUDED_PKGS)

fun ensureAdb() = "adb --version".runCommand().isSuccess

fun getAdbDevices(): List<String> = with("adb devices".runCommand()) {
	if (isSuccess) {
		val devicesList = output.substringAfter("List of devices attached").trim()
		devicesList.lineSequence()
			.filter { it.isNotEmpty() }
			.map { it.split(Regex("\\s")).first() }
			.toList()
	} else {
		listOf()
	}
}

fun getReceivers(deviceId: String, action: String): Map<String, List<String>> = with(
	"adb -s $deviceId shell cmd package query-receivers --components -a $action".runCommand()
) {
	if (isSuccess)
		output.lineSequence()
			.filter { it.isNotBlank() }
			.filterNot { ExcludedRegex.containsMatchIn(it) }
			.map { it.split("/") }
			.filter { it.size == 2 }
			.map { it[0] to it[1] }
			.groupBy({ (k, _) -> k }, { (_, v) -> v })
	else
		emptyMap()
}

fun ensureAdbdRoot(deviceId: String): Boolean {
	"adb -s $deviceId root".runCommand()
	return "adb -s $deviceId shell which su".runCommand().isSuccess
}

fun doBroadcast(deviceId: String, action: String, packageId: String, receiver: String, extras: Map<String, String>) {
	val extrasList = extras
		.asSequence()
		.map { (k, v) -> k to (if (v == UUID_PLACEHOLDER) randomUUID().toString() else v) }
		.map { (k, v) -> "-e $k \"$v\"" }
		.joinToString(" ")

	val base = listOf("adb", "-s", deviceId, "shell", "am broadcast -a $action -n $packageId/$receiver $extrasList")
	with((base + extrasList).runCommand()) {
		// TODO
	}
}
