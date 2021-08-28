package io.github.ermadmi78.kobby.generator.kotlin

import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.ANY
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.MAP
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.STRING
import io.github.ermadmi78.kobby.model.Decoration
import io.github.ermadmi78.kobby.model.KobbyInvalidSchemaException
import io.github.ermadmi78.kobby.model.parseSchema
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Created on 28.08.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class SchemaValidationTest {
    private val layout = KotlinLayout(
        KotlinTypes.PREDEFINED_SCALARS + mapOf(
            "DateTime" to KotlinType("java.time", "OffsetDateTime"),
            "JSON" to MAP.parameterize(STRING, ANY.nullable())
        ),
        KotlinContextLayout(
            "kobby",
            "kobby",
            Decoration("Kobby", null),
            "query",
            "mutation",
            "subscription"
        ),
        KotlinDtoLayout(
            "kobby.dto",
            Decoration(null, "Dto"),
            Decoration(null, null),
            Decoration(null, null),
            true,
            KotlinDtoJacksonLayout(true),
            KotlinDtoBuilderLayout(
                true,
                Decoration(null, "Builder"),
                "copy"
            ),
            KotlinDtoGraphQLLayout(
                true,
                "kobby.dto.graphql",
                Decoration("GraphQL", null)
            )
        ),
        KotlinEntityLayout(
            true,
            "kobby.entity",
            Decoration(null, null),
            "__withCurrentProjection",
            KotlinEntityProjectionLayout(
                Decoration(null, "Projection"),
                "__projection",
                Decoration(null, null),
                Decoration("__without", null),
                "__minimize",
                Decoration(null, "Qualification"),
                Decoration(null, "QualifiedProjection"),
                Decoration("__on", null)
            ),
            KotlinEntitySelectionLayout(
                Decoration(null, "Selection"),
                "__selection",
                Decoration(null, "Query"),
                "__query"
            )
        ),
        KotlinImplLayout(
            "kobby.entity.impl",
            Decoration(null, "Impl"),
            true,
            Decoration("__inner", null)
        ),
        KotlinAdapterLayout(
            KotlinAdapterKtorLayout(
                true,
                true,
                "kobby.adapter.ktor",
                Decoration("Kobby", "Adapter")
            )
        ),
        KotlinResolverLayout(
            true,
            true,
            "resolvers",
            Decoration("IGraphQL", "Resolver"),
            null,
            "Not implemented yet"
        )
    )

    //@Test
    fun temp() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls")!!)
        )
        val files = generateKotlin(schema, layout)

        files.forEach {
            println()
            it.writeTo(System.out)
            println("---------")
        }
    }

    @Test
    fun testUnknownScalar() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_scalar.graphqls.txt")!!)
        )

        val expected = "Kotlin data type for scalar 'DummyScalar' not found. " +
                "Please, configure it by means of 'kobby' extension. https://github.com/ermadmi78/kobby"
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: IllegalStateException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownType() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_type.graphqls.txt")!!)
        )

        val expected = "Unknown type \"DummyType\""
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownArgType() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_arg_type.graphqls.txt")!!)
        )

        val expected = "Unknown type \"DummyArg\""
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownParent() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_parent.graphqls.txt")!!)
        )

        val expected = "Unknown type \"DummyParent\""
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }
}