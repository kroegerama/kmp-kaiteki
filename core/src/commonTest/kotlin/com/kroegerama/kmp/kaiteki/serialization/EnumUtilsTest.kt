package com.kroegerama.kmp.kaiteki.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

class EnumUtilsTest {

    @Serializable
    enum class TestEnum {
        @SerialName("Serial Name 1")
        Test1,

        @SerialName("")
        Test2,

        Test3
    }

    @Test
    fun testEnumValueOfSerialName() {
        listOf(
            TestEnum.Test1 to "Serial Name 1",
            TestEnum.Test2 to "",
            TestEnum.Test3 to "Test3"
        ).forEach { (expected, string) ->
            expect(expected) { enumValueOfSerialName<TestEnum>(string) }
        }

        listOf(
            "Test1",
            "Test2"
        ).forEach { string ->
            assertFailsWith(IllegalArgumentException::class) {
                enumValueOfSerialName<TestEnum>(string)
            }
        }
    }

    @Test
    fun testEnumValueOfSerialNameOrNull() {
        listOf(
            TestEnum.Test1 to "Serial Name 1",
            TestEnum.Test2 to "",
            TestEnum.Test3 to "Test3"
        ).forEach { (expected, string) ->
            expect(expected) { enumValueOfSerialName<TestEnum>(string) }
        }

        listOf(
            "Test1",
            "Test2"
        ).forEach { string ->
            expect(null) { enumValueOfSerialNameOrNull<TestEnum>(string) }
        }
    }
}
