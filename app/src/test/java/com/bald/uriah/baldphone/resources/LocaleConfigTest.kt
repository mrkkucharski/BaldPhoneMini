package com.bald.uriah.baldphone.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.SortedSet
import javax.xml.parsers.DocumentBuilderFactory

class LocaleConfigTest {
    @Test
    fun localeConfigCoversEveryLocalizedValuesDirectory() {
        val resDir = projectFile("src/main/res")
        val expectedLocales = (resDir.listFiles()
            .orEmpty()
            .mapNotNull { it.valuesDirectoryLocale() }
            + "en")
            .toSortedSet()

        val actualLocales = parseLocaleConfig(projectFile("src/main/res/xml/locales_config.xml"))

        assertFalse("Expected localized resource directories", expectedLocales.isEmpty())
        assertEquals(expectedLocales, actualLocales)
    }

    private fun File.valuesDirectoryLocale(): String? {
        if (!isDirectory || !name.startsWith("values-")) return null
        val qualifier = name.removePrefix("values-")
        if (qualifier == "night") return null
        return qualifier
            .removePrefix("b+")
            .replace("-r", "-")
            .replace("iw", "he")
    }

    private fun parseLocaleConfig(file: File): SortedSet<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val locales = document.getElementsByTagName("locale")
        return (0 until locales.length)
            .map { locales.item(it) as Element }
            .map { it.getAttribute("android:name") }
            .toSortedSet()
    }

    private fun projectFile(path: String): File {
        val fromAppModule = File(path)
        if (fromAppModule.exists()) return fromAppModule
        return File("app/$path")
    }
}
