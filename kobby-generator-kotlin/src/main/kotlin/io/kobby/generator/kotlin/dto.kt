package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import graphql.language.*
import graphql.schema.idl.TypeDefinitionRegistry

internal data class GenerateDtoResult(
    val dslAnnotation: ClassName,
    val types: Map<String, TypeName>,
    val files: List<FileSpec>
)

/**
 * Created on 19.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateDto(layout: KotlinGeneratorLayout, graphQLSchema: TypeDefinitionRegistry): GenerateDtoResult {
    val dtoLayout = layout.dto
    val builderLayout = layout.dto.builder
    val graphqlLayout = layout.dto.graphql

    val types = mutableMapOf<String, TypeName>().apply {
        layout.scalars.forEach { (scalar, type) ->
            put(scalar, type.toTypeName())
        }
    }
    val interfaces = mutableMapOf<String, MutableSet<String>>()
    for (type in graphQLSchema.types().values) {
        when (type) {
            is ObjectTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)
            )
            is InputObjectTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name
            )
            is InterfaceTypeDefinition -> {
                types[type.name] = ClassName(
                    dtoLayout.packageName,
                    type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)
                )
                interfaces.computeIfAbsent(type.name) { mutableSetOf() }.also {
                    for (field in type.fieldDefinitions) {
                        it += field.name
                    }
                }
            }
            is EnumTypeDefinition -> types[type.name] = ClassName(
                dtoLayout.packageName,
                type.name
            )
            is UnionTypeDefinition -> TODO()
        }
    }
    val files = mutableMapOf<String, FileSpec.Builder>()

    files[dtoLayout.dslAnnotation] = FileSpec.builder(dtoLayout.packageName, dtoLayout.dslAnnotation).addType(
        TypeSpec.classBuilder(dtoLayout.dslAnnotation)
            .addModifiers(KModifier.ANNOTATION)
            .addAnnotation(DslMarker::class)
            .build()
    )
    val dslAnnotation = ClassName(dtoLayout.packageName, dtoLayout.dslAnnotation)

    for (type in graphQLSchema.types().values) {
        when (type) {
            is ObjectTypeDefinition -> {
                val className = type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)
                val classBuilder = TypeSpec.classBuilder(className).apply {
                    addModifiers(KModifier.DATA)
                    type.implements.asSequence().map { it.resolve(types, true) }.forEach {
                        addSuperinterface(it)
                    }
                }
                val constructorBuilder = FunSpec.constructorBuilder()
                for (field in type.fieldDefinitions) {
                    val fieldType = field.type.resolve(types).copy(true)
                    classBuilder.addProperty(constructorBuilder, field.name, fieldType) {
                        for (ancestorType in type.implements) {
                            if (field.name in interfaces[ancestorType.extractName()]!!) {
                                addModifiers(KModifier.OVERRIDE)
                                break
                            }
                        }
                    }
                }

                classBuilder
                    .primaryConstructor(constructorBuilder.jacksonize(dtoLayout).build())
                    .jacksonize(dtoLayout, type.name, className)
                    .build()
            }
            is InputObjectTypeDefinition -> {
                val classBuilder = TypeSpec.classBuilder(type.name).addModifiers(KModifier.DATA)
                val constructorBuilder = FunSpec.constructorBuilder()
                for (field in type.inputValueDefinitions) {
                    classBuilder.addProperty(
                        constructorBuilder,
                        field.name,
                        field.type.resolve(types).copy(true)
                    )
                }

                classBuilder
                    .primaryConstructor(constructorBuilder.jacksonize(dtoLayout).build())
                    .jacksonize(dtoLayout, type.name, type.name)
                    .build()
            }
            is InterfaceTypeDefinition ->
                TypeSpec.interfaceBuilder(type.name.decorate(dtoLayout.prefix, dtoLayout.postfix)).apply {
                    type.implements.asSequence().map { it.resolve(types, true) }.forEach {
                        addSuperinterface(it)
                    }
                    for (field in type.fieldDefinitions) {
                        val fieldType = field.type.resolve(types).copy(true)
                        addProperty(PropertySpec.builder(field.name, fieldType).apply {
                            for (ancestorType in type.implements) {
                                if (field.name in interfaces[ancestorType.extractName()]!!) {
                                    addModifiers(KModifier.OVERRIDE)
                                    break
                                }
                            }
                        }.build())
                    }
                }.build()
            is EnumTypeDefinition -> TypeSpec.enumBuilder(type.name).apply {
                for (enumValue in type.enumValueDefinitions) {
                    addEnumConstant(enumValue.name)
                }
            }.build()
            is UnionTypeDefinition -> TODO("Union support is not implemented yet")
            else -> null
        }?.also {
            require(files[type.name] == null) {
                "DTO type name conflict - there are several types with name: ${type.name}"
            }
            files[type.name] = FileSpec.builder(dtoLayout.packageName, it.name!!).addType(it)
        }
    }

    if (layout.dto.builder.enabled) {
        for (type in graphQLSchema.types().values) {
            // Create builder functions
            when (type) {
                is ObjectTypeDefinition -> type.fieldDefinitions.map { it.name }.joinToString { it }
                is InputObjectTypeDefinition -> type.inputValueDefinitions.map { it.name }.joinToString { it }
                else -> null
            }?.let { arguments ->
                val dtoType: TypeName = types[type.name]!!
                val dtoName: String = (dtoType as ClassName).simpleName
                val builderType: TypeName = ClassName(
                    dtoLayout.packageName,
                    dtoName.decorate(builderLayout.prefix, builderLayout.postfix)
                )
                FunSpec.builder(dtoName)
                    .addParameter("block", LambdaTypeName.get(builderType, emptyList(), UNIT))
                    .returns(dtoType)
                    .addStatement("return %T().apply(block).run·{ %T($arguments) }", builderType, dtoType)
                    .build()
            }?.also {
                files[type.name]!!.addFunction(it)
            }

            // Create builders
            when (type) {
                is ObjectTypeDefinition -> type.fieldDefinitions.map {
                    PropertySpec.builder(it.name, it.type.resolve(types).copy(true))
                        .mutable()
                        .initializer("null")
                        .build()
                }
                is InputObjectTypeDefinition -> type.inputValueDefinitions.map {
                    PropertySpec.builder(it.name, it.type.resolve(types).copy(true))
                        .mutable()
                        .initializer("null")
                        .build()
                }
                else -> null
            }?.let { properties ->
                val dtoName: String = (types[type.name] as ClassName).simpleName
                TypeSpec.classBuilder(dtoName.decorate(builderLayout.prefix, builderLayout.postfix)).apply {
                    addAnnotation(dslAnnotation)
                    properties.forEach {
                        addProperty(it)
                    }
                }.build()
            }?.also {
                files[type.name]!!.addType(it)
            }
        }
    }

    if (graphqlLayout.enabled) {
        // GraphQL Request
        val requestName = "Request".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
        val requestConstructorBuilder = FunSpec.constructorBuilder()
        val requestType = TypeSpec.classBuilder(requestName)
            .addModifiers(KModifier.DATA)
            .addProperty(
                requestConstructorBuilder,
                "query",
                STRING
            )
            .addProperty(
                requestConstructorBuilder,
                "variables",
                MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .addProperty(
                requestConstructorBuilder,
                "operationName",
                STRING.nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
            .primaryConstructor(requestConstructorBuilder.build())
            .build()
        files["graphql.$requestName"] =
            FileSpec.builder(graphqlLayout.packageName, requestName).addType(requestType)

        // GraphQL ErrorType
        val errorTypeName = "ErrorType".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
        val errorTypeType = TypeSpec.enumBuilder(errorTypeName)
            .addEnumConstant("InvalidSyntax")
            .addEnumConstant("ValidationError")
            .addEnumConstant("DataFetchingException")
            .addEnumConstant("OperationNotSupported")
            .addEnumConstant("ExecutionAborted")
            .build()
        files["graphql.$errorTypeName"] =
            FileSpec.builder(graphqlLayout.packageName, errorTypeName).addType(errorTypeType)

        // GraphQLErrorSourceLocation
        val errorSourceLocationName = "ErrorSourceLocation".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
        val errorSourceLocationConstructorBuilder = FunSpec.constructorBuilder()
        val errorSourceLocationType = TypeSpec.classBuilder(errorSourceLocationName)
            .addModifiers(KModifier.DATA)
            .addProperty(
                errorSourceLocationConstructorBuilder,
                "line",
                INT
            )
            .addProperty(
                errorSourceLocationConstructorBuilder,
                "column",
                INT
            )
            .addProperty(
                errorSourceLocationConstructorBuilder,
                "sourceName",
                STRING.nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
            .primaryConstructor(errorSourceLocationConstructorBuilder.build())
            .build()
        files["graphql.$errorSourceLocationName"] =
            FileSpec.builder(graphqlLayout.packageName, errorSourceLocationName).addType(errorSourceLocationType)

        // GraphQLError
        val errorName = "Error".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
        val errorConstructorBuilder = FunSpec.constructorBuilder()
        val errorType = TypeSpec.classBuilder(errorName)
            .addModifiers(KModifier.DATA)
            .addProperty(
                errorConstructorBuilder,
                "message",
                STRING
            )
            .addProperty(
                errorConstructorBuilder,
                "locations",
                LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorSourceLocationName)).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .addProperty(
                errorConstructorBuilder,
                "errorType",
                ClassName(graphqlLayout.packageName, errorTypeName).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
            .addProperty(
                errorConstructorBuilder,
                "path",
                LIST.parameterizedBy(ANY).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .addProperty(
                errorConstructorBuilder,
                "extensions",
                MAP.parameterizedBy(STRING, ANY.nullable()).nullable()
            ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
            .primaryConstructor(errorConstructorBuilder.build())
            .build()
        files["graphql.$errorName"] =
            FileSpec.builder(graphqlLayout.packageName, errorName).addType(errorType)

        // GraphQLException
        val exceptionName = "Exception".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
        val exceptionConstructorBuilder = FunSpec.constructorBuilder()
            .addParameter("message", STRING)
        val exceptionType = TypeSpec.classBuilder(exceptionName)
            .addProperty(
                exceptionConstructorBuilder,
                "request",
                ClassName(graphqlLayout.packageName, requestName)
            )
            .addProperty(
                exceptionConstructorBuilder,
                "errors",
                LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorName)).nullable()
            )
            .primaryConstructor(exceptionConstructorBuilder.build())
            .superclass(ClassName("kotlin", "RuntimeException"))
            .addSuperclassConstructorParameter(
                "message + (errors?.joinToString(\",\\n  \", \"\\n  \", \"\\n\")·{ it.toString() } ?: \"\")"
            )
            .build()
        files["graphql.$exceptionName"] =
            FileSpec.builder(graphqlLayout.packageName, exceptionName).addType(exceptionType)

        // GraphQLQueryResult
        types["Query"]?.also {
            val queryName = "QueryResult".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
            val queryConstructorBuilder = FunSpec.constructorBuilder()
            val queryType = TypeSpec.classBuilder(queryName)
                .addModifiers(KModifier.DATA)
                .addProperty(
                    queryConstructorBuilder,
                    "data",
                    it.nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
                .addProperty(
                    queryConstructorBuilder,
                    "errors",
                    LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorName)).nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
                .primaryConstructor(queryConstructorBuilder.build())
                .build()
            files["graphql.$queryName"] =
                FileSpec.builder(graphqlLayout.packageName, queryName).addType(queryType)
        }

        // GraphQLMutationResult
        types["Mutation"]?.also {
            val mutationName = "MutationResult".decorate(graphqlLayout.prefix, graphqlLayout.postfix)
            val mutationConstructorBuilder = FunSpec.constructorBuilder()
            val mutationType = TypeSpec.classBuilder(mutationName)
                .addModifiers(KModifier.DATA)
                .addProperty(
                    mutationConstructorBuilder,
                    "data",
                    it.nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_ABSENT) }
                .addProperty(
                    mutationConstructorBuilder,
                    "errors",
                    LIST.parameterizedBy(ClassName(graphqlLayout.packageName, errorName)).nullable()
                ) { jacksonInclude(dtoLayout, JacksonInclude.NON_EMPTY) }
                .primaryConstructor(mutationConstructorBuilder.build())
                .build()
            files["graphql.$mutationName"] =
                FileSpec.builder(graphqlLayout.packageName, mutationName).addType(mutationType)
        }
    }

    return GenerateDtoResult(dslAnnotation, types, files.values.asSequence().map { it.build() }.toList())
}

internal fun String.decorate(prefix: String?, postfix: String?): String {
    return if (prefix.isNullOrBlank() && postfix.isNullOrBlank()) {
        this
    } else if (prefix.isNullOrBlank()) {
        this + postfix
    } else if (postfix.isNullOrBlank()) {
        prefix + this
    } else {
        prefix + this + postfix
    }
}

internal fun Type<*>.resolve(types: Map<String, TypeName>, nonNull: Boolean = false): TypeName = when (this) {
    is NonNullType -> type.resolve(types, true)
    is ListType -> LIST.parameterizedBy(type.resolve(types)).run {
        if (nonNull) this else copy(true)
    }
    is graphql.language.TypeName -> types[name]?.run {
        if (nonNull) this else copy(true)
    } ?: error("Scalar type is not configured: $name")
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun Type<*>.extractName(): String = when (this) {
    is NonNullType -> type.extractName()
    is ListType -> type.extractName()
    is graphql.language.TypeName -> name
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}

internal fun TypeName.nullable(): TypeName = copy(true)

internal fun FunSpec.Builder.jacksonize(layout: KotlinDtoLayout): FunSpec.Builder {
    if (layout.jackson.enabled && parameters.size == 1) {
        addAnnotation(JacksonAnnotations.JSON_CREATOR)
    }

    return this
}

internal fun TypeSpec.Builder.jacksonize(
    layout: KotlinDtoLayout,
    typeName: String,
    className: String
): TypeSpec.Builder {
    if (!layout.jackson.enabled) {
        return this
    }

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_TYPE_NAME)
            .addMember("value = %S", typeName)
            .build()
    )

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_TYPE_INFO)
            .addMember("use = %T.Id.NAME", JacksonAnnotations.JSON_TYPE_INFO)
            .addMember("include = %T.As.PROPERTY", JacksonAnnotations.JSON_TYPE_INFO)
            .addMember("property = %S", "__typename")
            .addMember("defaultImpl = $className::class")
            .build()
    )

    addAnnotation(
        AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
            .addMember("value = %T.Include.NON_ABSENT", JacksonAnnotations.JSON_INCLUDE)
            .build()
    )

    return this
}

internal fun TypeSpec.Builder.addProperty(
    constructorBuilder: FunSpec.Builder,
    name: String,
    type: TypeName,
    block: PropertySpec.Builder.() -> Unit = {}
): TypeSpec.Builder = addProperty(PropertySpec.builder(name, type).also {
    constructorBuilder.addParameter(
        ParameterSpec.builder(name, type).apply {
            if (type.isNullable) {
                defaultValue("null")
            }
        }.build()
    )
    it.initializer(name)
}.apply(block).build())

internal fun PropertySpec.Builder.jacksonInclude(
    layout: KotlinDtoLayout,
    include: JacksonInclude
): PropertySpec.Builder {
    if (layout.jackson.enabled) {
        addAnnotation(
            AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
                .addMember("value = %T.Include.${include.name}", JacksonAnnotations.JSON_INCLUDE)
                .build()
        )
    }
    return this
}

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