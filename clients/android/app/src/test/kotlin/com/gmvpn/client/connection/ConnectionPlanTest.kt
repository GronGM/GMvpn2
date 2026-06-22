package com.gmvpn.client.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ConnectionPlanTest {

    @Test
    fun `default plan preserves current direct Xray path`() {
        val plan = ConnectionPlan(profileRef = ProfileRef("profile-1"))

        assertEquals(EngineKind.XRAY, plan.engine)
        assertEquals(RoutingMode.AllApps, plan.routingMode)
        assertEquals(TransportMode.Direct, plan.transportMode)
        assertEquals(DnsPolicy.Default, plan.dnsPolicy)
        assertEquals(DiagnosticsPolicy.Default, plan.diagnosticsPolicy)
        assertEquals(RedactionPolicy.Strict, plan.redactionPolicy)
    }

    @Test
    fun `profile reference cannot be blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileRef(" ")
        }
    }

    @Test
    fun `routing mode carries package names without Android dependencies`() {
        val routing = RoutingMode.SelectedAppsOnly(
            packageNames = setOf("com.example.browser", "org.example.mail"),
        )
        val plan = ConnectionPlan(
            profileRef = ProfileRef("profile-2"),
            routingMode = routing,
        )

        assertEquals(routing, plan.routingMode)
    }
}
