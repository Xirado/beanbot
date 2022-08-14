package at.xirado.bean.i18n

import at.xirado.simplejson.DataType
import at.xirado.simplejson.JSONObject

class I18n(val name: String, val data: JSONObject) {

    companion object {
        @JvmStatic
        private fun format(layout: String, vararg attributes: Pair<String, Any>): String {
            var output = layout
            attributes.forEach { output = output.replace("{${it.first}}", it.second.toString()) }
            return output
        }
    }

    fun get(path: String, vararg attributes: Pair<String, Any>): String? {
        return try {
            getValue(path, *attributes)
        } catch (ex: Throwable) {
            if (ex is NullPointerException || ex is NoSuchFieldException)
                null
            else
                throw ex
        }
    }

    fun getValue(path: String, vararg attributes: Pair<String, Any>): String {
        val paths = path.split(".")

        var current = data

        paths.forEachIndexed { index, subPath ->
            if (current.isNull(subPath))
                throw NullPointerException("Cannot access path \"$path\" because \"$subPath\" is null!")

            if (index + 1 == paths.size && !current.isType(subPath, DataType.STRING))
                throw IllegalArgumentException("Provided path \"$path\" is not a string!")

            if (current.isType(subPath, DataType.OBJECT)) {
                current = current.getObject(subPath)
                return@forEachIndexed
            }

            if (!current.isType(subPath, DataType.STRING))
                throw IllegalArgumentException("Provided path \"$path\" is not a string!")

            return format(current.getString(subPath), *attributes)
        }
        throw NoSuchFieldException("Path $path does not exist in locale $name!")
    }
}