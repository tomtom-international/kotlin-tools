package com.tomtom.kotlin.traceevents

import org.junit.Test
import kotlin.test.assertEquals

internal class TraceEventConsumerCollectionTest {

    @Test
    fun `TAG matches class name`() {
        assertEquals(
            TraceEventConsumerCollection::class.simpleName,
            TraceEventConsumerCollection.TAG
        )
    }
}
