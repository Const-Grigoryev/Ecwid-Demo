package dev.aspid812.ipv4_count.benchmark.util

import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.jvm.internal.FunctionReference
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.KFunction


object PrepareProcess {

	private fun requireClassBasedContainer(container: KDeclarationContainer) =
		requireNotNull(container as? ClassBasedDeclarationContainer) {
			"Unsupported type of the main method container: '$container'. Is it still a Java world?"
		}

	fun java(mainClass: Class<*>, vararg options: String): ProcessBuilder {
		val classPath = System.getProperty("java.class.path")
		return ProcessBuilder("java", "--class-path", classPath, mainClass.canonicalName, *options)
			.redirectError(ProcessBuilder.Redirect.INHERIT)
	}

	fun java(mainMethod: KFunction<Unit>, vararg options: String): ProcessBuilder {
		// See https://stackoverflow.com/questions/70626467
		require(mainMethod.name == "main") { "Main method must be named only 'main'" }
		require(mainMethod is FunctionReference)    // Does it exist a `KFunction` which is not a `FunctionReference` as well?
		val mainClass = mainMethod.owner.let(::requireClassBasedContainer).jClass
		return java(mainClass, *options)
	}
}
