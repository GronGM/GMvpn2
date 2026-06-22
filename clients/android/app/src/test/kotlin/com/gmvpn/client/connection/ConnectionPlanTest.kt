package com.gmvpn.client.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionPlanTest {

    @Test
    fun `connection plan defaults to Xray Direct and strict redaction`() {
        val plan = ConnectionPlan(
            profileRef = ProfileRef("profile-1"),
        )

        assertEquals(
            EngineKind.XRAY,
            plan.engine,
        )
        assertEquals(
            RoutingMode.AllApps,
            plan.routingMode,
        )
        assertEquals(
            TransportMode.Direct,
            plan.transportMode,
        )
        assertEquals(
            DnsPolicy.Default,
            plan.dnsPolicy,
        )
        assertEquals(
            DiagnosticsPolicy.Default,
            plan.diagnosticsPolicy,
        )
        assertEquals(
            RedactionPolicy.Strict,
            plan.redactionPolicy,
        )
    }

    @Test
    fun `profile reference cannot be blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProfileRef(" ")
        }
    }

    @Test
    fun `routing mode cannot represent allowed and disallowed at same time`() {
        val selected: RoutingMode = RoutingMode.SelectedAppsOnly(
            packageNames = setOf("com.example.browser"),
        )
        val excluded: RoutingMode = RoutingMode.AllExceptSelected(
            packageNames = setOf("com.example.banking"),
        )

        assertTrue(selected is RoutingMode.SelectedAppsOnly)
        assertFalse(selected is RoutingMode.AllExceptSelected)
        assertTrue(excluded is RoutingMode.AllExceptSelected)
        assertFalse(excluded is RoutingMode.SelectedAppsOnly)
    }

    @Test
    fun `selected apps only empty selection is invalid`() {
        val routing = RoutingMode.SelectedAppsOnly(
            packageNames = emptySet(),
        )

        assertFalse(routing.isValid)
    }

    @Test
    fun `all except selected can be empty`() {
        val routing = RoutingMode.AllExceptSelected(
            packageNames = emptySet(),
        )

        assertTrue(routing.isValid)
    }

    @Test
    fun `routing mode carries package names without Android dependencies`() {
        val routing = RoutingMode.SelectedAppsOnly(
            packageNames = setOf(
                "com.example.browser",
                "org.example.mail",
            ),
        )
        val plan = ConnectionPlan(
            profileRef = ProfileRef("profile-2"),
            routingMode = routing,
        )

        assertEquals(
            routing,
            plan.routingMode,
        )
        assertTrue(plan.routingMode.isValid)
    }

    @Test
    fun `connection plan does not expose endpoint secret fields`() {
        val fieldNames = ConnectionPlan::class.java.declaredFields
            .map { it.name }
            .toSet()

        assertForbiddenFieldNamesAbsent(fieldNames)
    }

    @Test
    fun `connection evidence does not expose endpoint secret fields`() {
        val fieldNames = ConnectionEvidence::class.java.declaredFields
            .map { it.name }
            .toSet()

        assertForbiddenFieldNamesAbsent(fieldNames)
    }

    private fun assertForbiddenFieldNamesAbsent(
        fieldNames: Set<String>,
    ) {
        val forbidden = setOf(
            "server",

            "host",

            "domain",

            "port",

            "rawUri",

            "uuid",

            "token",

            "password",

            "subscriptionUrl",
        )

        assertEquals(
            emptySet<String>(),
            fieldNames.intersect(forbidden),
        )
    }
}
