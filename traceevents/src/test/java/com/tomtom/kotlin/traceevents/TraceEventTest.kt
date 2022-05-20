package com.tomtom.kotlin.traceevents

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class TraceEventTest {

    @Test
    fun `parameterNames invokes parameterNamesProvider with lazy`() {
        // GIVEN
        val parameterNamesProvider = mockk<() -> Array<String>?>()
        every { parameterNamesProvider.invoke() } returns arrayOf("parameterName1", "parameterName2")

        val sut = createSut(parameterNamesProvider)

        // THEN
        verify(exactly = 0) { parameterNamesProvider.invoke() }

        // WHEN
        val parameterNames = sut.parameterNames

        // THEN
        verify(exactly = 1) { parameterNamesProvider.invoke() }
        assertEquals(arrayOf("parameterName1", "parameterName2").toList(), parameterNames?.toList())
    }

    @Test
    fun `getNamedParametersMap invokes parameterNamesProvider with lazy`() {
        // GIVEN
        val parameterNamesProvider = mockk<() -> Array<String>?>()
        every { parameterNamesProvider.invoke() } returns arrayOf("parameterName1", "parameterName2")

        val sut = createSut(parameterNamesProvider)

        // THEN
        verify(exactly = 0) { parameterNamesProvider.invoke() }

        // WHEN
        val namedParameters = sut.getNamedParametersMap()

        // THEN
        verify(exactly = 1) { parameterNamesProvider.invoke() }
        assertEquals(mapOf("parameterName1" to "value1", "parameterName2" to "value2"), namedParameters)
    }

    @Test
    fun `parameterNames checks number of parameters`() {
        // GIVEN
        val parameterNamesProvider = mockk<() -> Array<String>?>()
        every { parameterNamesProvider.invoke() } returns arrayOf("parameterName1")

        val sut = createSut(parameterNamesProvider)

        // WHEN - THEN
        assertFailsWith<IllegalStateException> {
            sut.parameterNames
        }
    }

    @Test
    fun `getNamedParametersMap checks number of parameters`() {
        // GIVEN
        val parameterNamesProvider = mockk<() -> Array<String>?>()
        every { parameterNamesProvider.invoke() } returns arrayOf("parameterName1")

        val sut = createSut(parameterNamesProvider)

        // WHEN - THEN
        assertFailsWith<IllegalStateException> {
            sut.getNamedParametersMap()
        }
    }

    @Test
    fun `getNamedParametersMap returns null if parameterNamesProvider returns null`() {
        // GIVEN
        val parameterNamesProvider = mockk<() -> Array<String>?>()
        every { parameterNamesProvider.invoke() } returns null

        val sut = createSut(parameterNamesProvider)

        // WHEN
        val namedParameters = sut.getNamedParametersMap()

        // THEN
        assertNull(namedParameters)
    }

    @Test
    fun `equals and hashCode`() {
        EqualsVerifier.forClass(TraceEvent::class.java)
            .withIgnoredFields("${TraceEvent::parameterNames.name}\$delegate")
            .verify()
    }

    private fun createSut(parameterNamesProvider: () -> Array<String>?) =
        TraceEvent(
            dateTime = LocalDateTime.now(),
            logLevel = TraceLog.LogLevel.DEBUG,
            tracerClassName = "TracerClassName",
            taggingClassName = "TaggingClass",
            context = "Context",
            traceThreadLocalContext = emptyMap(),
            interfaceName = "InterfaceName",
            stackTraceHolder = null,
            eventName = "EventName",
            args = arrayOf("value1", "value2"),
            parameterNamesProvider = parameterNamesProvider
        )
}
