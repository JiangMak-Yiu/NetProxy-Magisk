package com.fanjv.netproxy.feature.dashboard.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/** 落地 IP 的网络类型，判定顺序与 OwnBox / FlClash 一致。 */
internal enum class IpNetworkType { RESIDENTIAL, DATACENTER, MOBILE, BUSINESS, UNKNOWN }

internal enum class IpRiskLevel { CLEAN, NORMAL, RISKY }

internal enum class IpRiskTag { PROXY, VPN, TOR }

internal data class LandingIpInfo(
    val ip: String,
    val countryCode: String,
    val country: String,
    val city: String,
    val isp: String,
    val asn: String,
    val networkType: IpNetworkType,
    val level: IpRiskLevel,
    val tags: List<IpRiskTag>,
    val source: String,
    val elapsedMillis: Long,
)

/** 模块 mixed 入站：探测请求显式走它，出来的才是节点落地 IP。 */
internal const val MIXED_INBOUND_PORT = 7080

private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

/** 等级与类型判定：Tor → 风险；机房 → 普通；其余 → 优。 */
internal fun evaluateIpQuality(
    hosting: Boolean,
    tor: Boolean,
    mobile: Boolean,
    business: Boolean,
): Pair<IpNetworkType, IpRiskLevel> {
    val type = when {
        hosting -> IpNetworkType.DATACENTER
        business -> IpNetworkType.BUSINESS
        mobile -> IpNetworkType.MOBILE
        else -> IpNetworkType.RESIDENTIAL
    }
    val level = when {
        tor -> IpRiskLevel.RISKY
        type == IpNetworkType.DATACENTER -> IpRiskLevel.NORMAL
        else -> IpRiskLevel.CLEAN
    }
    return type to level
}

/** 国家码转旗帜；非法输入回落地球图标。 */
internal fun countryFlag(code: String?): String {
    val normalized = code?.trim()?.uppercase().orEmpty()
    if (normalized.length != 2 || normalized.any { it !in 'A'..'Z' }) return "🌐"
    val base = 0x1F1E6
    return buildString(2) {
        appendCodePoint(base + (normalized[0] - 'A'))
        appendCodePoint(base + (normalized[1] - 'A'))
    }
}

private fun landingClient(): OkHttpClient = OkHttpClient.Builder()
    .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", MIXED_INBOUND_PORT)))
    .connectTimeout(4, TimeUnit.SECONDS)
    .readTimeout(4, TimeUnit.SECONDS)
    .callTimeout(6, TimeUnit.SECONDS)
    .retryOnConnectionFailure(false)
    .build()

private fun OkHttpClient.readUrl(url: String): String? = runCatching {
    newCall(Request.Builder().url(url.toHttpUrl()).header("User-Agent", USER_AGENT).build())
        .execute()
        .use { response -> if (response.isSuccessful) response.body.string() else null }
}.getOrNull()

/** 通过 mixed 入站探测当前节点的落地 IP 与质量；拿不到任何数据时返回 null，不伪造。 */
internal suspend fun probeLandingIp(): LandingIpInfo? = withContext(Dispatchers.IO) {
    val client = landingClient()
    val started = System.currentTimeMillis()
    val egress = fetchEgressIp(client) ?: return@withContext null
    val ip = egress.first
    val fallbackCountryCode = egress.second
    ((lookupIpLocate(client, ip, fallbackCountryCode, started)
        ?: lookupIpApiCom(client, ip, fallbackCountryCode, started)
        ?: lookupIpQueryIo(client, ip, fallbackCountryCode, started))
        ?: LandingIpInfo(
            ip = ip,
            countryCode = fallbackCountryCode,
            country = fallbackCountryCode,
            city = "",
            isp = "",
            asn = "",
            networkType = IpNetworkType.UNKNOWN,
            level = IpRiskLevel.NORMAL,
            tags = emptyList(),
            source = "",
            elapsedMillis = System.currentTimeMillis() - started,
        ))
}

private fun fetchEgressIp(client: OkHttpClient): Pair<String, String>? {
    client.readUrl("https://cloudflare.com/cdn-cgi/trace")?.let { body ->
        val ip = body.lineSequence().firstOrNull { it.startsWith("ip=") }?.substringAfter("ip=")?.trim().orEmpty()
        val loc = body.lineSequence().firstOrNull { it.startsWith("loc=") }?.substringAfter("loc=")?.trim().orEmpty()
        if (ip.isNotEmpty()) return ip to loc
    }
    client.readUrl("https://api.ip.sb/geoip")?.let { body ->
        val json = runCatching { JSONObject(body) }.getOrNull()
        val ip = json?.optString("ip").orEmpty().trim()
        if (ip.isNotEmpty()) return ip to json?.optString("country_code").orEmpty().uppercase()
    }
    client.readUrl("https://ident.me/json")?.let { body ->
        val json = runCatching { JSONObject(body) }.getOrNull()
        val ip = json?.optString("ip").orEmpty().trim()
        if (ip.isNotEmpty()) return ip to json?.optString("country_code").orEmpty().uppercase()
    }
    return null
}

private fun lookupIpLocate(
    client: OkHttpClient,
    ip: String,
    fallbackCountryCode: String,
    started: Long,
): LandingIpInfo? {
    val json = runCatching { JSONObject(client.readUrl("https://iplocate.io/api/lookup/$ip") ?: return null) }
        .getOrNull() ?: return null
    val privacy = json.optJSONObject("privacy")
    val company = json.optJSONObject("company")
    val asnObject = json.optJSONObject("asn")
    val hosting = privacy?.optBoolean("is_hosting", false) ?: false
    val tor = privacy?.optBoolean("is_tor", false) ?: false
    val proxy = privacy?.optBoolean("is_proxy", false) ?: false
    val vpn = privacy?.optBoolean("is_vpn", false) ?: false
    val mobile = company?.optString("type").equals("mobile", ignoreCase = true)
    val business = company?.optString("type").equals("business", ignoreCase = true)
    val (type, level) = evaluateIpQuality(hosting, tor, mobile, business)
    val asnNumber = asnObject?.optInt("asn", 0) ?: 0
    val countryCode = json.optString("country_code").uppercase().ifBlank { fallbackCountryCode }
    return LandingIpInfo(
        ip = json.optString("ip", ip).trim().ifBlank { ip },
        countryCode = countryCode,
        country = json.optString("country").ifBlank { countryCode },
        city = json.optString("city"),
        isp = company?.optString("name").orEmpty().ifBlank { asnObject?.optString("name").orEmpty() },
        asn = if (asnNumber > 0) "AS$asnNumber" else asnObject?.optString("asn").orEmpty(),
        networkType = type,
        level = level,
        tags = buildList {
            if (proxy) add(IpRiskTag.PROXY)
            if (vpn) add(IpRiskTag.VPN)
            if (tor) add(IpRiskTag.TOR)
        },
        source = "iplocate.io",
        elapsedMillis = System.currentTimeMillis() - started,
    )
}

private fun lookupIpApiCom(
    client: OkHttpClient,
    ip: String,
    fallbackCountryCode: String,
    started: Long,
): LandingIpInfo? {
    val body = client.readUrl(
        "http://ip-api.com/json/$ip?fields=status,country,countryCode,regionName,city,isp,org,as,mobile,proxy,hosting"
    ) ?: return null
    val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
    if (json.optString("status") != "success") return null
    val hosting = json.optBoolean("hosting", false)
    val mobile = json.optBoolean("mobile", false)
    val proxy = json.optBoolean("proxy", false)
    val (type, level) = evaluateIpQuality(hosting = hosting, tor = false, mobile = mobile, business = false)
    val countryCode = json.optString("countryCode").uppercase().ifBlank { fallbackCountryCode }
    return LandingIpInfo(
        ip = ip,
        countryCode = countryCode,
        country = json.optString("country").ifBlank { countryCode },
        city = json.optString("city"),
        isp = json.optString("org").ifBlank { json.optString("isp") },
        asn = json.optString("as").substringBefore(' '),
        networkType = type,
        level = level,
        tags = if (proxy) listOf(IpRiskTag.PROXY) else emptyList(),
        source = "ip-api.com",
        elapsedMillis = System.currentTimeMillis() - started,
    )
}

private fun lookupIpQueryIo(
    client: OkHttpClient,
    ip: String,
    fallbackCountryCode: String,
    started: Long,
): LandingIpInfo? {
    val body = client.readUrl("https://api.ipquery.io/$ip?format=json") ?: return null
    val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
    val ispObject = json.optJSONObject("isp")
    val location = json.optJSONObject("location")
    val risk = json.optJSONObject("risk")
    val datacenter = risk?.optBoolean("is_datacenter", false) ?: false
    val tor = risk?.optBoolean("is_tor", false) ?: false
    val proxy = risk?.optBoolean("is_proxy", false) ?: false
    val vpn = risk?.optBoolean("is_vpn", false) ?: false
    val mobile = risk?.optBoolean("is_mobile", false) ?: false
    val (type, level) = evaluateIpQuality(hosting = datacenter, tor = tor, mobile = mobile, business = false)
    val countryCode = location?.optString("country_code").orEmpty().uppercase().ifBlank { fallbackCountryCode }
    return LandingIpInfo(
        ip = json.optString("ip", ip).trim().ifBlank { ip },
        countryCode = countryCode,
        country = location?.optString("country").orEmpty().ifBlank { countryCode },
        city = location?.optString("city").orEmpty(),
        isp = ispObject?.optString("org").orEmpty().ifBlank { ispObject?.optString("isp").orEmpty() },
        asn = ispObject?.optString("asn").orEmpty(),
        networkType = type,
        level = level,
        tags = buildList {
            if (proxy) add(IpRiskTag.PROXY)
            if (vpn) add(IpRiskTag.VPN)
            if (tor) add(IpRiskTag.TOR)
        },
        source = "ipquery.io",
        elapsedMillis = System.currentTimeMillis() - started,
    )
}
