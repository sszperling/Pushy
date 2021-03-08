package dev.sszperling.pushy

import java.io.File
import java.util.concurrent.TimeUnit

fun List<String>.runCommand(workingDir: File = File(".")): String {
	val proc = ProcessBuilder(this)
		.directory(workingDir)
		.redirectOutput(ProcessBuilder.Redirect.PIPE)
		.redirectError(ProcessBuilder.Redirect.PIPE)
		.start()

	proc.waitFor(5, TimeUnit.SECONDS)
	return proc.inputStream.bufferedReader().readText()
}

fun String.runCommand(workingDir: File = File(".")) = split("\\s".toRegex()).runCommand(workingDir)
