#!/usr/bin/env kotlin

@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("eu.jrie.jetbrains:kotlin-shell-core:0.2.1")

import eu.jrie.jetbrains.kotlinshell.shell.*


fun result(command: String): String {
    val builder = StringBuilder()
    shell {
        pipeline {
            command.process() pipe builder
        }
    }
    return builder.toString()
}

val baseApkPath = args.getOrNull(0)
val targetApkPath = args.getOrNull(1)
if (baseApkPath.isNullOrBlank() || targetApkPath.isNullOrBlank()) {
    throw IllegalArgumentException()
}

val baseDownloadSize = result("apkanalyzer -h apk download-size $baseApkPath")
    .trimEnd()
val targetDownloadSize = result("apkanalyzer -h apk download-size $targetApkPath")
    .trimEnd()

val basePermissions = result("apkanalyzer manifest permissions $baseApkPath")
    .trimEnd()
    .replace("\n", "<br>")
val targetPermissions = result("apkanalyzer manifest permissions $targetApkPath")
    .trimEnd()
    .replace("\n", "<br>")

val baseReferences = result("apkanalyzer dex references $baseApkPath")
    .trimEnd()
    .replace("\n", "<br>")
val targetReferences = result("apkanalyzer dex references $targetApkPath")
    .trimEnd()
    .replace("\n", "<br>")

val compare =
    result("apkanalyzer -h apk compare --different-only $baseApkPath $targetApkPath")
        .replace("\t", "|")
        .split("\n")
        .filter { !it.contains("""\dB.+$""".toRegex()) }
        .joinToString("\n")

val diff = """
~ | before | after
--- | --- | ---
Download Size | $baseDownloadSize | $targetDownloadSize
Permissions | $basePermissions | $targetPermissions
References | $baseReferences | $targetReferences
""".trimIndent()

val sizeDetail = """
before | after | diff | path
--- | --- | --- | ---
$compare
""".trimIndent()

// https://github.community/t/set-output-truncates-multiline-strings/16852/3
val outputDiff = diff
    .replace("%", "%25")
    .replace("\n", "%0A")
    .replace("\r", "%0D")
val outputSizeDetail = sizeDetail
    .replace("%", "%25")
    .replace("\n", "%0A")
    .replace("\r", "%0D")

val echoDiffParam = "::set-output name=diff::$outputDiff"
val echoSizeDetailParam = "::set-output name=size-detail::$outputSizeDetail"
shell {
    "echo $echoDiffParam"()
    "echo $echoSizeDetailParam"()
}
