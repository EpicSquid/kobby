package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.generator.kotlin.KotlinType
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

/**
 * Created on 18.07.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KotlinConfig {
    @Parameter
    var enabled: Boolean = true

    @Parameter
    var scalars: Map<String, KotlinTypeConfig> = mapOf()

    @Parameter
    var relativePackage: Boolean = true

    @Parameter
    var packageName: String? = "kobby.kotlin"

    /**
     * Target directory where to store generated files, defaults to `target/generated-sources/kobby-kotlin`.
     */
    @Parameter
    var outputDirectory: File? = null

    @Parameter
    var context: KotlinContextConfig = KotlinContextConfig()

    @Parameter
    var dto: KotlinDtoConfig = KotlinDtoConfig()

    @Parameter
    var entity: KotlinEntityConfig = KotlinEntityConfig()

    @Parameter
    var impl: KotlinImplConfig = KotlinImplConfig()

    @Parameter
    var adapter: KotlinAdapterConfig = KotlinAdapterConfig()

    @Parameter
    var resolver: KotlinResolverConfig = KotlinResolverConfig()

    override fun toString(): String {
        return "KotlinConfig(" +
                "\n    enabled=$enabled, " +
                "\n    scalars=${scalars.print(4)}, " +
                "\n    relativePackage=$relativePackage, " +
                "\n    packageName=$packageName, " +
                "\n    outputDirectory= $outputDirectory " +
                "\n    context=$context, " +
                "\n    dto=$dto, " +
                "\n    entity=$entity, " +
                "\n    impl=$impl, " +
                "\n    adapter=$adapter, " +
                "\n    resolver=$resolver" +
                "\n  )"
    }
}

private fun Map<String, KotlinTypeConfig>.print(shift: Int): String = buildString {
    append("{")
    this@print.forEach { (scalar, type) ->
        append('\n').shift(shift + 2).append(scalar).append(": ").append(type.print(shift + 2))
    }
    append('\n').shift(shift).append('}')
}

private fun KotlinTypeConfig.print(shift: Int): String = buildString {
    append("KotlinTypeConfig(")
    append('\n').shift(shift + 2).append("packageName=").append(packageName)
    append('\n').shift(shift + 2).append("className=").append(className)
    append('\n').shift(shift + 2).append("nullable=").append(nullable)
    append('\n').shift(shift + 2).append("generics=")
    if (generics.isEmpty()) {
        append("[]")
    } else {
        append('[')
        var first = true
        for (generic in generics) {
            if (first) {
                first = false
            } else {
                append(',')
            }
            append('\n').shift(shift + 4).append(generic.print(shift + 4))
        }
        append('\n').shift(shift + 2).append(']')
    }
    append('\n').shift(shift).append(')')
}

internal fun StringBuilder.shift(shift: Int): StringBuilder {
    for (i in 1..shift) {
        append(' ')
    }
    return this
}

class KotlinTypeConfig {
    @Parameter(required = true)
    lateinit var packageName: String

    @Parameter(required = true)
    lateinit var className: String

    @Parameter
    var nullable: Boolean = false

    @Parameter
    var generics: List<KotlinTypeConfig> = listOf()

    override fun toString(): String {
        return "KotlinTypeConfig(" +
                "packageName=$packageName, " +
                "className=$className, " +
                "nullable=$nullable, " +
                "generics=$generics" +
                ")"
    }
}

internal fun KotlinTypeConfig.toKotlinType(): KotlinType =
    KotlinType(packageName, className, nullable, generics.map { it.toKotlinType() })

//**********************************************************************************************************************
//                                                 Context Config
//**********************************************************************************************************************

class KotlinContextConfig {
    @Parameter
    var packageName: String? = null

    @Parameter
    var name: String? = null

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = null

    @Parameter
    var query: String = "query"

    @Parameter
    var mutation: String = "mutation"

    @Parameter
    var subscription: String = "subscription"

    override fun toString(): String {
        return "KotlinContextConfig(" +
                "\n      packageName=$packageName, " +
                "\n      name=$name, " +
                "\n      prefix=$prefix, " +
                "\n      postfix=$postfix, " +
                "\n      query=$query, " +
                "\n      mutation=$mutation, " +
                "\n      subscription=$subscription" +
                "\n    )"
    }
}

//**********************************************************************************************************************
//                                                 DTO Config
//**********************************************************************************************************************

class KotlinDtoConfig {
    @Parameter
    var packageName: String? = "dto"

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = "Dto"

    @Parameter
    var enumPrefix: String? = null

    @Parameter
    var enumPostfix: String? = null

    @Parameter
    var inputPrefix: String? = null

    @Parameter
    var inputPostfix: String? = null

    @Parameter
    var applyPrimaryKeys: Boolean = false

    @Parameter
    var jackson: KotlinDtoJacksonConfig = KotlinDtoJacksonConfig()

    @Parameter
    var builder: KotlinDtoBuilderConfig = KotlinDtoBuilderConfig()

    @Parameter
    var graphQL: KotlinDtoGraphQLConfig = KotlinDtoGraphQLConfig()

    override fun toString(): String {
        return "KotlinDtoConfig(" +
                "\n      packageName=$packageName, " +
                "\n      prefix=$prefix, " +
                "\n      postfix=$postfix, " +
                "\n      enumPrefix=$enumPrefix, " +
                "\n      enumPostfix=$enumPostfix, " +
                "\n      inputPrefix=$inputPrefix, " +
                "\n      inputPostfix=$inputPostfix, " +
                "\n      applyPrimaryKeys=$applyPrimaryKeys, " +
                "\n      jackson=$jackson, " +
                "\n      builder=$builder, " +
                "\n      graphQL=$graphQL" +
                "\n    )"
    }
}

class KotlinDtoJacksonConfig {
    @Parameter
    var enabled: Boolean? = null

    @Parameter
    var typeInfoUse: String = "NAME"

    @Parameter
    var typeInfoInclude: String = "PROPERTY"

    @Parameter
    var typeInfoProperty: String = "__typename"

    @Parameter
    var jsonInclude: String = "NON_ABSENT"

    override fun toString(): String {
        return "KotlinDtoJacksonConfig(" +
                "\n        enabled=$enabled," +
                "\n        typeInfoUse=$typeInfoUse," +
                "\n        typeInfoInclude=$typeInfoInclude," +
                "\n        typeInfoProperty=$typeInfoProperty" +
                "\n        jsonInclude=$jsonInclude" +
                "\n      )"
    }
}

class KotlinDtoBuilderConfig {
    @Parameter
    var enabled: Boolean = true

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = "Builder"

    @Parameter
    var copyFun: String = "copy"

    override fun toString(): String {
        return "KotlinDtoBuilderConfig(" +
                "\n        enabled=$enabled, " +
                "\n        prefix=$prefix, " +
                "\n        postfix=$postfix, " +
                "\n        copyFun=$copyFun" +
                "\n      )"
    }
}

class KotlinDtoGraphQLConfig {
    @Parameter
    var enabled: Boolean = true

    @Parameter
    var packageName: String? = "graphql"

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = null

    override fun toString(): String {
        return "KotlinDtoGraphQLConfig(" +
                "\n        enabled=$enabled, " +
                "\n        packageName=$packageName, " +
                "\n        prefix=$prefix, " +
                "\n        postfix=$postfix" +
                "\n      )"
    }
}

//**********************************************************************************************************************
//                                                 Entity Config
//**********************************************************************************************************************

class KotlinEntityConfig {
    @Parameter
    var enabled: Boolean = true

    @Parameter
    var packageName: String? = "entity"

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = null

    @Parameter
    var contextInheritanceEnabled: Boolean = false

    @Parameter
    var contextFunEnabled: Boolean = true

    @Parameter
    var contextFunName: String = "__context"

    @Parameter
    var withCurrentProjectionFun: String = "__withCurrentProjection"

    @Parameter
    var projection: KotlinEntityProjectionConfig = KotlinEntityProjectionConfig()

    @Parameter
    var selection: KotlinEntitySelectionConfig = KotlinEntitySelectionConfig()

    override fun toString(): String {
        return "KotlinEntityConfig(" +
                "\n      enabled=$enabled, " +
                "\n      packageName=$packageName, " +
                "\n      prefix=$prefix, " +
                "\n      postfix=$postfix, " +
                "\n      contextInheritanceEnabled=$contextInheritanceEnabled, " +
                "\n      contextFunEnabled=$contextFunEnabled, " +
                "\n      contextFunName=$contextFunName, " +
                "\n      withCurrentProjectionFun=$withCurrentProjectionFun, " +
                "\n      projection=$projection, " +
                "\n      selection=$selection" +
                "\n    )"
    }
}

class KotlinEntityProjectionConfig {
    @Parameter
    var projectionPrefix: String? = null

    @Parameter
    var projectionPostfix: String? = "Projection"

    @Parameter
    var projectionArgument: String = "__projection"

    @Parameter
    var withPrefix: String? = null

    @Parameter
    var withPostfix: String? = null

    @Parameter
    var withoutPrefix: String? = "__without"

    @Parameter
    var withoutPostfix: String? = null

    @Parameter
    var minimizeFun: String = "__minimize"

    @Parameter
    var qualificationPrefix: String? = null

    @Parameter
    var qualificationPostfix: String? = "Qualification"

    @Parameter
    var qualifiedProjectionPrefix: String? = null

    @Parameter
    var qualifiedProjectionPostfix: String? = "QualifiedProjection"

    @Parameter
    var onPrefix: String? = "__on"

    @Parameter
    var onPostfix: String? = null

    override fun toString(): String {
        return "KotlinEntityProjectionConfig(" +
                "\n        projectionPrefix=$projectionPrefix, " +
                "\n        projectionPostfix=$projectionPostfix, " +
                "\n        projectionArgument=$projectionArgument, " +
                "\n        withPrefix=$withPrefix, " +
                "\n        withPostfix=$withPostfix, " +
                "\n        withoutPrefix=$withoutPrefix, " +
                "\n        withoutPostfix=$withoutPostfix, " +
                "\n        minimizeFun=$minimizeFun, " +
                "\n        qualificationPrefix=$qualificationPrefix, " +
                "\n        qualificationPostfix=$qualificationPostfix, " +
                "\n        qualifiedProjectionPrefix=$qualifiedProjectionPrefix, " +
                "\n        qualifiedProjectionPostfix=$qualifiedProjectionPostfix, " +
                "\n        onPrefix=$onPrefix, " +
                "\n        onPostfix=$onPostfix" +
                "\n      )"
    }
}

class KotlinEntitySelectionConfig {
    @Parameter
    var selectionPrefix: String? = null

    @Parameter
    var selectionPostfix: String? = "Selection"

    @Parameter
    var selectionArgument: String = "__selection"

    @Parameter
    var queryPrefix: String? = null

    @Parameter
    var queryPostfix: String? = "Query"

    @Parameter
    var queryArgument: String = "__query"

    override fun toString(): String {
        return "KotlinEntitySelectionConfig(" +
                "\n        selectionPrefix=$selectionPrefix, " +
                "\n        selectionPostfix=$selectionPostfix, " +
                "\n        selectionArgument=$selectionArgument, " +
                "\n        queryPrefix=$queryPrefix, " +
                "\n        queryPostfix=$queryPostfix, " +
                "\n        queryArgument=$queryArgument" +
                "\n      )"
    }
}

//**********************************************************************************************************************
//                                                 Impl Config
//**********************************************************************************************************************

class KotlinImplConfig {
    @Parameter
    var packageName: String? = "entity.impl"

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = "Impl"

    @Parameter
    var internal: Boolean = true

    @Parameter
    var innerPrefix: String? = "__inner"

    @Parameter
    var innerPostfix: String? = null

    override fun toString(): String {
        return "KotlinImplConfig(" +
                "\n      packageName=$packageName, " +
                "\n      prefix=$prefix, " +
                "\n      postfix=$postfix, " +
                "\n      internal=$internal, " +
                "\n      innerPrefix=$innerPrefix, " +
                "\n      innerPostfix=$innerPostfix" +
                "\n    )"
    }
}

//**********************************************************************************************************************
//                                                 Adapter Config
//**********************************************************************************************************************

class KotlinAdapterConfig {
    @Parameter
    var ktor: KotlinAdapterKtorConfig = KotlinAdapterKtorConfig()

    override fun toString(): String {
        return "KotlinAdapterConfig(" +
                "\n      ktor=$ktor" +
                "\n    )"
    }
}

class KotlinAdapterKtorConfig {
    @Parameter
    var simpleEnabled: Boolean? = null

    @Parameter
    var compositeEnabled: Boolean? = null

    @Parameter
    var packageName: String? = "adapter.ktor"

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = "KtorAdapter"

    @Parameter
    var dynamicHttpHeaders: Boolean = true

    @Parameter
    var receiveTimeoutMillis: Long? = null

    override fun toString(): String {
        return "KotlinAdapterKtorConfig(" +
                "\n        simpleEnabled=$simpleEnabled, " +
                "\n        compositeEnabled=$compositeEnabled, " +
                "\n        packageName=$packageName, " +
                "\n        prefix=$prefix, " +
                "\n        postfix=$postfix, " +
                "\n        dynamicHttpHeaders=$dynamicHttpHeaders, " +
                "\n        receiveTimeoutMillis=$receiveTimeoutMillis" +
                "\n      )"
    }
}

//**********************************************************************************************************************
//                                                 Resolver Config
//**********************************************************************************************************************

class KotlinResolverConfig {
    @Parameter
    var enabled: Boolean? = null

    @Parameter
    var publisherEnabled: Boolean? = null

    @Parameter
    var packageName: String? = "resolver"

    @Parameter
    var prefix: String? = null

    @Parameter
    var postfix: String? = "Resolver"

    @Parameter
    var argument: String? = null

    @Parameter
    var toDoMessage: String? = null

    override fun toString(): String {
        return "KotlinResolverConfig(" +
                "\n      enabled=$enabled, " +
                "\n      publisherEnabled=$publisherEnabled, " +
                "\n      packageName=$packageName, " +
                "\n      prefix=$prefix, " +
                "\n      postfix=$postfix, " +
                "\n      argument=$argument, " +
                "\n      toDoMessage=$toDoMessage" +
                "\n    )"
    }
}