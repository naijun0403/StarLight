package dev.mooner.starlight.plugincore.api

import dev.mooner.starlight.plugincore.project.Project

abstract class Api <T> {

    protected class ApiFunctionBuilder {

        var name: String? = null
        var args: Array<Class<*>> = emptyArray()
        var returns: Class<*> = Unit::class.java

        @Suppress("SameParameterValue")
        private fun required(fieldName: String, value: Any?) {
            if (value == null) {
                throw IllegalArgumentException("Required field '$fieldName' is null")
            }
        }

        fun build(): ApiFunction {
            required("name", name)

            return ApiFunction(
                name = name!!,
                args = args,
                returns = returns
            )
        }
    }

    protected class ApiValueBuilder {

        var name: String? = null
        var returns: Class<*> = Unit::class.java

        @Suppress("SameParameterValue")
        private fun required(fieldName: String, value: Any?) {
            if (value == null) {
                throw IllegalArgumentException("Required field '$fieldName' is null")
            }
        }

        fun build(): ApiValue {
            required("name", name)

            return ApiValue(
                name = name!!,
                returns = returns
            )
        }
    }

    protected fun function(block: ApiFunctionBuilder.() -> Unit): ApiFunction {
        val builder = ApiFunctionBuilder().apply(block)
        return builder.build()
    }

    protected fun value(block: ApiValueBuilder.() -> Unit): ApiValue {
        val builder = ApiValueBuilder().apply(block)
        return builder.build()
    }

    abstract val name: String

    abstract val objects: List<ApiObject>

    abstract val instanceClass: Class<T>

    abstract val instanceType: InstanceType

    abstract fun getInstance(project: Project): Any

    protected inline fun <reified T> getApiObjects(): List<ApiObject> {
        val values: List<ApiValue> = T::class.java.declaredFields.map {
            value {
                name = it.name
                returns = it.type
            }
        }
        val functions: List<ApiFunction> = T::class.java.declaredMethods.map {
            function {
                name = it.name
                args = it.parameterTypes
                returns = it.returnType
            }
        }

        return (values + functions)
    }

    override fun equals(other: Any?): Boolean {
        return when(other) {
            null -> false
            !is Api<*> -> false
            else -> other.name == name
        }
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}