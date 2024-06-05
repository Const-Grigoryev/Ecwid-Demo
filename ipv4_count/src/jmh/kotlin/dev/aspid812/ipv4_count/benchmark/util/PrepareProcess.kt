package dev.aspid812.ipv4_count.benchmark.util

import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KFunction


object PrepareProcess {

	fun java(mainClass: Class<*>, vararg options: String): ProcessBuilder {
		val classPath = System.getProperty("java.class.path")
		return ProcessBuilder("java", "--class-path", classPath, mainClass.canonicalName, *options)
			.redirectError(ProcessBuilder.Redirect.INHERIT)
	}

	fun java(mainMethod: KFunction<Unit>, vararg options: String): ProcessBuilder {
		// See https://stackoverflow.com/questions/70626467
		require(mainMethod.name == "main") { "Main method must be named only 'main'" }
		require(mainMethod is FunctionReference)    // Does it exist a `KFunction` which is not a `FunctionReference` as well?

		val mainClass = when (val mainMethodContainer = mainMethod.owner) {
			// Either `PackageReference` or `ClassReference`
			is ClassBasedDeclarationContainer -> mainMethodContainer.jClass
			else -> throw IllegalArgumentException(
				"""Unsupported type of the main method container: '$mainMethodContainer'.
				Is it still a Java world?"""
			)
		}
		return java(mainClass, *options)
	}
}
