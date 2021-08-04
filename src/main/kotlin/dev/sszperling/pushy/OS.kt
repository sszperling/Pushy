package dev.sszperling.pushy

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

data class SubcommandResult(val output: String, val error: String, val exitCode: Int) {
	val isSuccess: Boolean
		get() = exitCode == 0
}

@OptIn(ExperimentalTime::class)
fun List<String>.runCommand(workingDir: File = File("."), timeout: Duration = Duration.seconds(5)) = with(
	ProcessBuilder(this)
		.directory(workingDir)
		.redirectOutput(ProcessBuilder.Redirect.PIPE)
		.redirectError(ProcessBuilder.Redirect.PIPE)
		.start()
) {
	waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)
	SubcommandResult(
		inputStream.bufferedReader().readText(),
		errorStream.bufferedReader().readText(),
		exitValue(),
	)
}

fun String.runCommand(workingDir: File = File(".")) = split("\\s".toRegex()).runCommand(workingDir)
