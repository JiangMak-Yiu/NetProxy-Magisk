package com.fanjv.netproxy.feature.apps.presentation

/** 名单文本一行一项，格式 `包名` 或 `用户 id:包名`（主用户省略前缀）。 */
private val appListPackage = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*")

/** 解析导入文本：忽略空行与 `#` 之后的注释，没有前缀的包名归主用户 0。 */
internal fun parseAppListEntries(text: String): List<String> =
    text.lineSequence()
        .map { it.substringBefore('#').trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator < 0) {
                if (appListPackage.matches(line)) "0:$line" else null
            } else {
                val user = line.take(separator).trim()
                val packageName = line.substring(separator + 1).trim()
                if (user.all(Char::isDigit) && user.isNotEmpty() && appListPackage.matches(packageName)) {
                    "$user:$packageName"
                } else {
                    null
                }
            }
        }
        .distinct()
        .toList()

/** 回写名单文本：主用户省略前缀并按包名排序，和导入格式对称。 */
internal fun serializeAppListEntries(entries: Collection<String>): String =
    entries.map { it.removePrefix("0:") }.sorted().joinToString("\n")
