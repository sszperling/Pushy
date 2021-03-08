package dev.sszperling.pushy

private const val EXCLUDED_PKGS = "^(android|com\\.android|com\\.google|org\\.chromium)"
private val ExcludedRegex = Regex(EXCLUDED_PKGS)

fun getAdbDevices(): List<String> = try {
	val result = "adb devices".runCommand()
	val devicesList = result.substringAfter("List of devices attached").trim()
	devicesList.lineSequence()
		.filter { it.isNotEmpty() }
		.map { it.split(Regex("\\s")).first() }
		.toList()
} catch (e: Exception) {
	listOf()
}

fun getReceivers(deviceId: String, action: String): Map<String, List<String>> {
	val receivers = "adb -s $deviceId shell cmd package query-receivers --components -a $action".runCommand()
	return receivers.lineSequence()
		.filter { it.isNotBlank() }
		.filterNot { ExcludedRegex.containsMatchIn(it) }
		.map { it.split("/") }
		.filter { it.size == 2 }
		.map { it[0] to it[1] }
		.groupBy({ (k, _) -> k }, { (_, v) -> v })
}

fun ensureAdbdRoot(deviceId: String) {
	println("adb -s $deviceId root".runCommand())
}

fun doBroadcast(deviceId: String, action: String, packageId: String, receiver: String, extras: Map<String, String>) {
	val extrasList = extras
		.asSequence()
		.map { (k, v) -> k to (if (v == UUID) java.util.UUID.randomUUID().toString() else v) }
		.map { (k, v) -> "-e $k \"$v\"" }
		.joinToString(" ")

	val base = listOf("adb", "-s", deviceId, "shell", "am broadcast -a $action -n $packageId/$receiver $extrasList")
	println((base + extrasList).runCommand())
}
