import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.changelog.Changelog
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Year

fun properties(key: String): String = project.findProperty(key).toString()


/// Plugins
plugins {
    // Compilation
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)

    // Tests/coverage
    alias(libs.plugins.kover)

    // Static analysis
    alias(libs.plugins.detekt)

    // Documentation
    alias(libs.plugins.changelog)
    alias(libs.plugins.dokka)

    // To run GitHubScrambler
    application
}


/// Dependencies
val scrambler: Configuration by configurations.creating

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    implementation(libs.uuidGenerator) {
        // Logging API is already provided by IDEA platform
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation(libs.dateparser) {
        // TODO[Workaround]: Remove after https://github.com/sisyphsu/dateparser/issues/30 has been fixed
        exclude(group = "org.projectlombok", module = "lombok")
    }
    implementation(libs.rgxgen)
    implementation(libs.github)
    implementation(libs.nanoid)
    scrambler(libs.kotlin.reflect)

    testImplementation(libs.assertj.swing)
    testImplementation(libs.bundles.kotest)

    detektPlugins(libs.detektFormattingPlugin)
    dokkaHtmlPlugin(libs.dokkaVersioningPlugin)

    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellij.ide.get()) {
            useInstaller = !libs.versions.intellij.ide.get().endsWith("EAP-SNAPSHOT")
        }

        pluginVerifier()
        zipSigner()

        testFramework(TestFrameworkType.Platform)
    }
}

configurations {
    all {
        // See https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#coroutinesLibraries
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
    }
}


/// Configuration
tasks {
    // Toolchain
    java.toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))  // See also https://github.com/gradle/gradle/issues/30499
    withType<JavaCompile> {
        sourceCompatibility = libs.versions.java.get()
        targetCompatibility = libs.versions.java.get()
    }
    withType<KotlinCompile> {
        compilerOptions {
            val kotlinApiVersion = libs.versions.kotlin.get()
                .split(".").take(2).joinToString(".") // Transforms e.g. "2.0.21" to "2.0"
                .let { KotlinVersion.fromVersion(it) }

            jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
            apiVersion = kotlinApiVersion
            languageVersion = kotlinApiVersion
        }
    }
    withType<Detekt> {
        jvmTarget = libs.versions.java.get()
    }


    // Pre-process source files
    processResources {
        val rgxgenVersion = libs.versions.rgxgen.get()
        filter { it.replace("%%RGXGEN_VERSION%%", rgxgenVersion) }
    }


    // Plugin building
    intellijPlatform {
        buildSearchableOptions = !project.hasProperty("build.hotswap")

        pluginConfiguration {
            description = file("src/main/resources/META-INF/description.html").readText()
            changeNotes = provider {
                changelog.renderItem(
                    if (changelog.has(properties("version"))) changelog.get(properties("version"))
                    else changelog.getUnreleased(),
                    Changelog.OutputType.HTML
                )
            }

            ideaVersion {
                sinceBuild = ideVersionToBuildNumber(libs.versions.intellij.ide.get())
                untilBuild = provider { null }
            }
        }

        signing {
            (System.getenv("CERTIFICATE_CHAIN") ?: "").also {
                if (it.startsWith("/")) certificateChainFile = file(it)
                else certificateChain = it
            }
            (System.getenv("PRIVATE_KEY") ?: "").also {
                if (it.startsWith("/")) privateKeyFile = file(it)
                else privateKey = it
            }

            password = System.getenv("PRIVATE_KEY_PASSWORD") ?: ""
        }

        publishing {
            token = System.getenv("PUBLISH_TOKEN") ?: ""

            if (project.hasProperty("publish.beta"))
                channels = listOf("beta")
        }

        pluginVerification {
            ides {
                recommended()
            }
        }
    }

    changelog {
        repositoryUrl = "https://github.com/fwdekker/intellij-randomness"
        itemPrefix = "*"
        unreleasedTerm = "9.9.9-unreleased"
    }


    // Runs GitHubScrambler
    application {
        mainClass = "com.fwdekker.randomness.GitHubScrambler"
    }
    named<JavaExec>("run") {
        classpath += scrambler
        standardInput = System.`in`
    }


    // Tests/coverage
    test {
        systemProperty("java.awt.headless", "false")
        systemProperty("kotest.framework.classpath.scanning.config.disable", "true")
        systemProperty("kotest.framework.classpath.scanning.autoscan.disable", "true")
        systemProperty("kotest.framework.disable.test.nested.jar.scanning", "true")
        systemProperty("kotest.framework.discovery.jar.scan.disable", "true")
        if (project.hasProperty("kotest.tags")) systemProperty("kotest.tags", project.findProperty("kotest.tags")!!)

        useJUnitPlatform()

        testLogging {
            events = setOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }

        finalizedBy(koverXmlReport)
    }

    kover {
        reports {
            total {
                html { onCheck = false }
                xml { onCheck = false }
            }
        }
    }


    // Static analysis
    detekt {
        allRules = true
        config.setFrom(".config/detekt/.detekt.yml")
    }


    // Documentation
    dokka {
        moduleName.set("Randomness")
        moduleVersion.set("v${properties("version")}")

        dokkaSourceSets.main {
            includes.from("packages.md")

            jdkVersion.set(libs.versions.java.get().toInt())
            languageVersion.set(libs.versions.kotlin.get())

            documentedVisibilities.set(
                setOf(
                    VisibilityModifier.Public,
                    VisibilityModifier.Private,
                    VisibilityModifier.Protected,
                    VisibilityModifier.Internal,
                    VisibilityModifier.Package,
                )
            )
            reportUndocumented.set(true)

            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl("https://github.com/fwdekker/intellij-randomness/tree/v${properties("version")}/src/main/kotlin")
                remoteLineSuffix.set("#L")
            }
        }
        dokkaPublications.html {
            suppressInheritedMembers.set(true)
            offlineMode.set(true)
        }
        pluginsConfiguration.html {
            // TODO[Workaround]: Change `logo-icon.svg` back to a symlink after https://github.com/Kotlin/dokka/issues/4369 is fixed
            customAssets.from(file(".config/dokka/logo-icon.svg"))
            footerMessage.set("&copy; ${Year.now().value} Florine&nbsp;W.&nbsp;Dekker")
        }
        pluginsConfiguration.versioning {
            if (project.hasProperty("dokka.pagesDir")) {
                val pagesDir = project.property("dokka.pagesDir")
                olderVersions.setFrom(file("$pagesDir"))
                olderVersionsDir.set(file("$pagesDir/older/"))
            }
        }
    }
}


fun ideVersionToBuildNumber(ideVersion: String): String {
    if (ideVersion.endsWith("-EAP-SNAPSHOT"))
        return ideVersion

    require(ideVersion.matches("[0-9]{4}\\.[0-9]".toRegex())) { "Invalid IDE version number '${ideVersion}'." }
    return "${ideVersion[2]}${ideVersion[3]}${ideVersion[5]}.0"
}
