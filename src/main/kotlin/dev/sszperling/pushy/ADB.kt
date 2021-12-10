package dev.sszperling.pushy

import java.util.UUID.randomUUID

private const val EXCLUDED_PKGS = "^(android|com\\.android|com\\.google|org\\.chromium)"
private val ExcludedRegex = Regex(EXCLUDED_PKGS)
private val ADBUnsafeRegex = Regex("([?&=])")
private val Whitespace = Regex("\\s")

fun ensureAdb() = "adb --version".runCommand().isSuccess

fun getAdbDevices(): List<String> = with("adb devices".runCommand()) {
	if (isSuccess) {
		val devicesList = output.substringAfter("List of devices attached").trim()
		devicesList.lineSequence()
			.filter(String::isNotEmpty)
			.map(Whitespace::split)
			.map(List<String>::first)
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
			.filter(String::isNotBlank)
			.filterNot(ExcludedRegex::containsMatchIn)
			.map { it.split("/") }
			.filter { it.size == 2 }
			.groupBy({ it[0] }, { it[1] })
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
		.map { (k, v) -> k to (if (v == UUID_PLACEHOLDER) randomUUID().toString() else v.escaped()) }
		.map { (k, v) -> "-e $k \"$v\"" }
		.joinToString(" ")

	val base = listOf("adb", "-s", deviceId, "shell", "am broadcast -a $action -n $packageId/$receiver $extrasList")
	with((base + extrasList).runCommand()) {
		// TODO
	}
}

private fun String.escaped() = replace(ADBUnsafeRegex) {
	val (char) = it.destructured
	"\\$char"
}
