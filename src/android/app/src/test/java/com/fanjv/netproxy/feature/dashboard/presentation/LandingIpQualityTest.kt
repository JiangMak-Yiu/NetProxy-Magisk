package com.fanjv.netproxy.feature.dashboard.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class LandingIpQualityTest {
    @Test
    fun datacenterIpIsNormalNotClean() {
        assertEquals(
            IpNetworkType.DATACENTER to IpRiskLevel.NORMAL,
            evaluateIpQuality(hosting = true, tor = false, mobile = false, business = false),
        )
    }

    @Test
    fun torAlwaysWinsAsRisky() {
        assertEquals(
            IpNetworkType.DATACENTER to IpRiskLevel.RISKY,
            evaluateIpQuality(hosting = true, tor = true, mobile = false, business = false),
        )
    }

    @Test
    fun residentialAndMobileAreClean() {
        assertEquals(
            IpNetworkType.RESIDENTIAL to IpRiskLevel.CLEAN,
            evaluateIpQuality(hosting = false, tor = false, mobile = false, business = false),
        )
        assertEquals(
            IpNetworkType.MOBILE to IpRiskLevel.CLEAN,
            evaluateIpQuality(hosting = false, tor = false, mobile = true, business = false),
        )
        assertEquals(
            IpNetworkType.BUSINESS to IpRiskLevel.CLEAN,
            evaluateIpQuality(hosting = false, tor = false, mobile = false, business = true),
        )
    }

    @Test
    fun flagFollowsCountryCodeOrFallsBackToGlobe() {
        assertEquals("🇺🇸", countryFlag("us"))
        assertEquals("🇭🇰", countryFlag("HK"))
        assertEquals("🌐", countryFlag(null))
        assertEquals("🌐", countryFlag("USA"))
        assertEquals("🌐", countryFlag("1"))
    }
}
