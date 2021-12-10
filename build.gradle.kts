import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0"
}

group = "dev.sszperling"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    implementation(compose.desktop.currentOs)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
		freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
		jvmTarget = "11"
	}
}

compose.desktop {
    application {
        mainClass = "dev.sszperling.pushy.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Pushy"
        }
    }
}
