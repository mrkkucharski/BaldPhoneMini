package com.bald.uriah.baldphone.resources

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class RecentStringResourcesTest {
    @Test
    fun recentFeatureStringsExistInBaseResources() {
        val requiredNames = allRecentStringNames()
        val baseStrings = parseStringNames(projectFile("src/main/res/values/strings.xml"))

        val missing = requiredNames - baseStrings
        assertTrue("Missing recent feature strings in base resources: $missing", missing.isEmpty())
    }

    @Test
    fun stringsAddedToLocalizedResourcesExistInEveryLocale() {
        val requiredNames = setOf(
            "pills_settings_subtext",
            "internet_settings_subtext",
            "maps_settings_subtext",
            "alarms_settings_subtext",
            "photos_settings_subtext",
            "top_bar_controls_settings",
            "top_bar_controls_settings_subtext",
            "top_bar_controls_full",
            "top_bar_controls_simple",
            "add_to_speed_dial",
            "remove_from_speed_dial"
        )

        localizedStringResourceFiles().forEach { file ->
            val actualNames = parseStringNames(file)
            val missing = requiredNames - actualNames
            assertTrue(
                "Missing localized recent feature strings in ${file.parentFile!!.name}: $missing",
                missing.isEmpty()
            )
        }
    }

    private fun allRecentStringNames(): Set<String> =
        setOf(
            "pills_settings_subtext",
            "internet_settings_subtext",
            "maps_settings_subtext",
            "alarms_settings_subtext",
            "photos_settings_subtext",
            "top_bar_controls_settings",
            "top_bar_controls_settings_subtext",
            "top_bar_controls_full",
            "top_bar_controls_simple",
            "add_to_speed_dial",
            "remove_from_speed_dial",
            "call_label",
            "speed_dial_full",
            "select_phone_number",
            "speed_dial_added",
            "speed_dial_removed"
        )

    private fun localizedStringResourceFiles(): List<File> {
        val resDir = projectFile("src/main/res")
        return resDir.listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .map { File(it, "strings.xml") }
            .filter { it.exists() }
            .sortedBy { it.parentFile!!.name }
    }

    private fun parseStringNames(file: File): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = document.getElementsByTagName("string")
        return (0 until strings.length)
            .map { strings.item(it) as Element }
            .map { it.getAttribute("name") }
            .toSet()
    }

    private fun projectFile(path: String): File {
        val fromAppModule = File(path)
        if (fromAppModule.exists()) return fromAppModule
        return File("app/$path")
    }
}
