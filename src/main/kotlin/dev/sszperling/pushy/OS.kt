package dev.sszperling.pushy

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

sealed class Result<T, E>
data class Success<T>(val value: T) : Result<T, Nothing>()
data class Failure<E>(val error: E) : Result<Nothing, E>()

data class SubcommandResult(val output: String, val error: String, val exitCode: Int) {
	val isSuccess: Boolean
		get() = exitCode == 0
}

@OptIn(ExperimentalTime::class)
fun List<String>.runCommand(workingDir: File = File("."), timeoutSecs: Duration = 5.seconds) = with(
	ProcessBuilder(this)
		.directory(workingDir)
		.redirectOutput(ProcessBuilder.Redirect.PIPE)
		.redirectError(ProcessBuilder.Redirect.PIPE)
		.start()
) {
	waitFor(timeoutSecs.inSeconds.toLong(), TimeUnit.SECONDS)
	SubcommandResult(
		inputStream.bufferedReader().readText(),
		errorStream.bufferedReader().readText(),
		exitValue(),
	)
}

fun String.runCommand(workingDir: File = File(".")) = split("\\s".toRegex()).runCommand(workingDir)
