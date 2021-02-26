package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import io.github.ermadmi78.kobby.model.KobbyScope

/**
 * Created on 23.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

internal fun TypeName.nullable(): TypeName = copy(true)

@KobbyScope
internal typealias FileSpecBuilder = FileSpec.Builder

@KobbyScope
internal typealias TypeSpecBuilder = TypeSpec.Builder

@KobbyScope
internal typealias PropertySpecBuilder = PropertySpec.Builder

@KobbyScope
internal typealias FunSpecBuilder = FunSpec.Builder

@KobbyScope
internal typealias ParameterSpecBuilder = ParameterSpec.Builder

@KobbyScope
internal typealias CodeBlockBuilder = CodeBlock.Builder

private val SUPPRESS = AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
    .addMember("%S", "RedundantVisibilityModifier")
    .addMember("%S", "RedundantUnitReturnType")
    .addMember("%S", "FunctionName")
    .addMember("%S", "PropertyName")
    .addMember("%S", "ObjectPropertyName")
    .addMember("%S", "MemberVisibilityCanBePrivate")
    .addMember("%S", "ConstantConditionIf")
    .addMember("%S", "CanBeParameter")
    .addMember("%S", "unused")
    .build()

internal fun buildFile(
    packageName: String,
    fileName: String,
    block: FileSpecBuilder.() -> Unit
): FileSpec = FileSpec.builder(packageName, fileName).apply { addAnnotation(SUPPRESS) }.apply(block).build()

internal fun FileSpecBuilder.buildClass(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.classBuilder(name).apply(block).build().also {
    addType(it)
}

internal fun FileSpecBuilder.buildInterface(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.interfaceBuilder(name).apply(block).build().also {
    addType(it)
}

internal fun FileSpecBuilder.buildEnum(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.enumBuilder(name).apply(block).build().also {
    addType(it)
}

internal fun FileSpecBuilder.buildFunction(
    name: String,
    block: FunSpecBuilder.() -> Unit
): FunSpec = FunSpec.builder(name).apply(block).build().also {
    addFunction(it)
}

internal fun TypeSpecBuilder.buildFunction(
    name: String,
    block: FunSpecBuilder.() -> Unit
): FunSpec = FunSpec.builder(name).apply(block).build().also {
    addFunction(it)
}

internal fun TypeSpecBuilder.buildCompanionObject(
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.companionObjectBuilder().apply(block).build().also {
    this.addType(it)
}

internal fun TypeSpecBuilder.buildPrimaryConstructor(
    block: FunSpecBuilder.() -> Unit
): FunSpec = FunSpec.constructorBuilder().apply(block).build().also {
    primaryConstructor(it)
}

internal fun TypeSpecBuilder.buildProperty(
    arg: Pair<String, TypeName>,
    block: PropertySpecBuilder.() -> Unit = {}
): PropertySpec = buildProperty(arg.first, arg.second, block)

internal fun TypeSpecBuilder.buildProperty(
    name: String,
    type: TypeName,
    block: PropertySpecBuilder.() -> Unit = {}
): PropertySpec = PropertySpec.builder(name, type).apply(block).build().also {
    addProperty(it)
}

internal fun PropertySpecBuilder.buildGetter(
    block: FunSpecBuilder.() -> Unit
): FunSpec = FunSpec.getterBuilder().apply(block).build().also {
    getter(it)
}

internal fun PropertySpecBuilder.buildDelegate(
    block: CodeBlockBuilder.() -> Unit
): CodeBlock = CodeBlock.builder().apply(block).build().also {
    delegate(it)
}

internal fun PropertySpecBuilder.buildLazyDelegate(
    block: CodeBlockBuilder.() -> Unit
): CodeBlock = buildDelegate {
    controlFlow("lazy") {
        apply(block)
    }
}

internal fun CodeBlockBuilder.controlFlow(flow: String, vararg args: Any, block: CodeBlockBuilder.() -> Unit) {
    beginControlFlow(flow, args = args)
    apply(block)
    endControlFlow()
}

internal fun CodeBlockBuilder.ifFlow(condition: String, vararg args: Any, block: CodeBlockBuilder.() -> Unit) {
    beginControlFlow("if ($condition)", args = args)
    apply(block)
    endControlFlow()
}

internal fun CodeBlockBuilder.ifFlowStatement(
    condition: String,
    vararg statementArgs: Any,
    block: () -> String
) = ifFlow(condition) {
    addStatement(block(), args = statementArgs)
}

internal fun CodeBlockBuilder.statement(vararg args: Any, block: () -> String) =
    addStatement(block(), args = args)

internal fun TypeSpecBuilder.buildEnumConstant(
    name: String,
    block: TypeSpecBuilder.() -> Unit
): TypeSpec = TypeSpec.anonymousClassBuilder().apply(block).build().also {
    addEnumConstant(name, it)
}

internal fun FunSpecBuilder.buildParameter(
    arg: Pair<String, TypeName>,
    block: ParameterSpecBuilder.() -> Unit = {}
): ParameterSpec = buildParameter(arg.first, arg.second, block)

internal fun FunSpecBuilder.buildParameter(
    name: String,
    type: TypeName,
    block: ParameterSpecBuilder.() -> Unit = {}
): ParameterSpec = ParameterSpec.builder(name, type).apply(block).build().also {
    addParameter(it)
}

internal fun FunSpecBuilder.suppressUnused() {
    addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
            .addMember("%S, %S", "UNUSED_PARAMETER", "UNUSED_CHANGED_VALUE")
            .build()
    )
}

internal fun FunSpecBuilder.suppressBlocking() {
    addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
            .addMember("%S", "BlockingMethodInNonBlockingContext")
            .build()
    )
}

internal fun FunSpecBuilder.controlFlow(flow: String, vararg args: Any, block: FunSpecBuilder.() -> Unit) {
    beginControlFlow(flow, args = args)
    apply(block)
    endControlFlow()
}

internal fun FunSpecBuilder.ifFlow(condition: String, vararg args: Any, block: FunSpecBuilder.() -> Unit) {
    beginControlFlow("if ($condition)", args = args)
    apply(block)
    endControlFlow()
}

internal fun FunSpecBuilder.ifFlowStatement(
    condition: String,
    vararg statementArgs: Any,
    block: () -> String
) = ifFlow(condition) {
    addStatement(block(), args = statementArgs)
}

internal fun FunSpecBuilder.statement(vararg args: Any, block: () -> String) =
    addStatement(block(), args = args)

internal fun FunSpecBuilder.buildAppendChain(
    variable: String = "",
    block: AppendChainBuilder.() -> Unit
): FunSpecBuilder = AppendChainBuilder(this, variable).apply(block).build()

internal class AppendChainBuilder(private val funBuilder: FunSpecBuilder, variable: String) {
    private val statement: StringBuilder = StringBuilder()
    private val args: MutableList<Any> = mutableListOf()

    init {
        if (variable.isNotBlank()) {
            statement.append(variable.trim())
        }
    }

    fun appendExactly(value: String): AppendChainBuilder {
        if (statement.isNotEmpty()) {
            statement.append('.')
        }

        statement.append("append($value)")

        return this
    }

    fun appendLiteral(value: String): AppendChainBuilder {
        if (statement.isNotEmpty()) {
            statement.append('.')
        }

        statement.append("append(%S)")
        args += value

        return this
    }

    fun appendLiteral(value: Char): AppendChainBuilder {
        if (statement.isNotEmpty()) {
            statement.append('.')
        }

        statement.append("append('$value')")

        return this
    }

    fun spaceAppendExactly(value: String): AppendChainBuilder =
        appendLiteral(' ').appendExactly(value)

    fun spaceAppendLiteral(value: String): AppendChainBuilder =
        appendLiteral(' ').appendLiteral(value)

    fun spaceAppendLiteral(value: Char): AppendChainBuilder =
        appendLiteral(' ').appendLiteral(value)

    fun build(): FunSpecBuilder =
        funBuilder.addStatement(statement.toString(), args = args.toTypedArray())
}

// Primary constructor properties builder
internal fun TypeSpecBuilder.buildPrimaryConstructorProperties(
    block: PrimaryConstructorPropertiesBuilder.() -> Unit
): FunSpec = FunSpec.constructorBuilder()
    .also {
        PrimaryConstructorPropertiesBuilder(this, it).apply(block)
    }
    .build()
    .also {
        primaryConstructor(it)
    }

@KobbyScope
internal class PrimaryConstructorPropertiesBuilder(
    private val classBuilder: TypeSpecBuilder,
    private val primaryConstructorBuilder: FunSpecBuilder
) {
    internal fun buildProperty(
        arg: Pair<String, TypeName>,
        block: PropertySpecBuilder.() -> Unit = {}
    ): PropertySpec = buildProperty(arg.first, arg.second, block)

    internal fun buildProperty(
        name: String,
        type: TypeName,
        block: PropertySpecBuilder.() -> Unit = {}
    ): PropertySpec = PropertySpec.builder(name, type)
        .also {
            primaryConstructorBuilder.addParameter(
                ParameterSpec.builder(name, type).apply {
                    if (type.isNullable) {
                        defaultValue("null")
                    }
                }.build()
            )
            it.initializer(name)
        }
        .apply(block)
        .build()
        .also {
            classBuilder.addProperty(it)
        }

    internal fun buildParameter(
        arg: Pair<String, TypeName>,
        block: ParameterSpecBuilder.() -> Unit = {}
    ): ParameterSpec = buildParameter(arg.first, arg.second, block)

    internal fun buildParameter(
        name: String,
        type: TypeName,
        block: ParameterSpecBuilder.() -> Unit = {}
    ): ParameterSpec = ParameterSpec.builder(name, type)
        .apply {
            if (type.isNullable) {
                defaultValue("null")
            }
        }
        .apply(block)
        .build()
        .also {
            primaryConstructorBuilder.addParameter(it)
        }

    internal fun customizeConstructor(block: FunSpecBuilder.() -> Unit): FunSpecBuilder =
        primaryConstructorBuilder.apply(block)
}

//**********************************************************************************************************************
//                                              Jackson
//**********************************************************************************************************************

internal object JacksonAnnotations {
    val JSON_CREATOR = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonCreator"
    )

    val JSON_TYPE_NAME = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonTypeName"
    )

    val JSON_TYPE_INFO = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonTypeInfo"
    )

    val JSON_INCLUDE = ClassName(
        "com.fasterxml.jackson.annotation",
        "JsonInclude"
    )
}

internal enum class JacksonInclude {
    ALWAYS,
    NON_NULL,
    NON_ABSENT,
    NON_EMPTY,
    NON_DEFAULT,
    CUSTOM,
    USE_DEFAULTS
}