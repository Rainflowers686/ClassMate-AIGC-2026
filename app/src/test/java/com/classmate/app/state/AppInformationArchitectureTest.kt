package com.classmate.app.state

import com.classmate.app.data.HistoryRecord
import com.classmate.app.data.InMemoryHistoryStore
import com.classmate.app.importing.OcrImportKind
import com.classmate.app.platform.ConfigRepository
import com.classmate.core.importing.ImportSourceType
import com.classmate.core.sample.SampleCourses
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInformationArchitectureTest {
    private val now = 1_700_000_000_000L

    @Test
    fun initialTabIsHomeAndImportFlowHasLayeredRoutes() {
        val viewModel = vm()

        assertEquals(Tab.HOME, viewModel.currentTab)
        assertEquals(Screen.HOME, viewModel.currentScreen)

        viewModel.navigateTo(Screen.IMPORT)
        assertEquals(Screen.IMPORT, viewModel.currentScreen)
        viewModel.navigateTo(Screen.IMPORT_TRAY)
        assertEquals(Screen.IMPORT_TRAY, viewModel.currentScreen)
        viewModel.navigateTo(Screen.IMPORT_SETTINGS)
        assertEquals(Screen.IMPORT_SETTINGS, viewModel.currentScreen)
    }

    @Test
    fun sampleTextAndOcrAppearInMaterialTrayState() {
        val viewModel = vm()

        assertTrue(viewModel.importTextDraft("级数", "课堂文本。", ImportSourceType.PASTE_TEXT, null))
        assertTrue(viewModel.addOcrImport(OcrImportKind.SLIDE_IMAGE, "课件 OCR 第 1 页", "p 级数 p 大于 1 收敛。"))

        assertEquals("课堂文本。", viewModel.ui.courseText)
        assertEquals(1, viewModel.ui.ocrImports.size)
        assertTrue(viewModel.ui.ocrImports.single().pastedText.contains("p 级数"))
    }

    @Test
    fun courseLibraryOpenCourseNavigatesToCourseDetail() {
        val viewModel = vm(history = listOf(record("a", "高数"), record("b", "高数")))
        val course = viewModel.courseSummaries().single()

        viewModel.openCourse(course.courseKey)

        assertEquals(Screen.COURSE_DETAIL, viewModel.currentScreen)
        assertNotNull(viewModel.ui.session)
        assertNotNull(viewModel.ui.result)
    }

    @Test
    fun openingHistoryLoadsCourseDetailAndTimelineCanStillOpen() {
        val record = record("a", "高数：级数")
        val viewModel = vm(history = listOf(record))

        viewModel.openHistory(record)
        assertEquals(Screen.COURSE_DETAIL, viewModel.currentScreen)

        viewModel.openHistoryTimeline(record)
        assertEquals(Screen.KNOWLEDGE, viewModel.currentScreen)
    }

    @Test
    fun exportCenterArtifactCanBeBuiltFromCourseDetailContext() {
        val record = record("a", "高数：级数")
        val viewModel = vm(history = listOf(record))
        viewModel.openHistory(record)

        val artifact = viewModel.buildCurrentReportArtifact(com.classmate.app.exporting.ExportFileFormat.PDF)

        assertNotNull(artifact)
        assertTrue(artifact!!.bytes.decodeToString().startsWith("%PDF"))
        assertFalse(artifact.containsSensitiveContent)
    }

    @Test
    fun settingsDebugCopyIsNotInHomeSourceAndManifestKeepsFunctionalPermissions() {
        val home = readText("src/main/java/com/classmate/app/ui/screens/home/HomeScreen.kt")
        val manifest = readText("src/main/AndroidManifest.xml")

        assertFalse(home.contains("Debug", ignoreCase = true))
        assertFalse(home.contains("调试"))
        // Functional-first permissions are intentional (on-device model, media import,
        // camera capture, local audio/TTS/ASR UX, review notifications). Bluetooth stays out
        // until there is a real Bluetooth device feature.
        assertTrue(manifest.contains("RECORD_AUDIO"))
        assertTrue(manifest.contains("MANAGE_EXTERNAL_STORAGE"))
        assertTrue(manifest.contains("android.permission.CAMERA"))
        assertFalse(manifest.contains("android.permission.BLUETOOTH_CONNECT"))
        // Legacy storage perms are present but capped at API 32.
        assertTrue(manifest.contains("android:maxSdkVersion=\"32\""))
        // Unrelated dangerous permissions remain out.
        assertFalse(manifest.contains("ACCESS_FINE_LOCATION"))
        assertFalse(manifest.contains("android.permission.READ_CONTACTS"))
    }

    @Test
    fun qwenThinkingGuardStillExists() {
        val vendor = readText("../core/src/main/kotlin/com/classmate/core/provider/VendorIo.kt")
        val diagnostic = readText("../core/src/main/kotlin/com/classmate/core/provider/BlueLMDiagnostic.kt")

        assertTrue(vendor.contains("enable_thinking"))
        assertTrue(vendor.contains("qwen3.5-plus"))
        assertTrue(diagnostic.contains("enable_thinking"))
    }

    private fun vm(history: List<HistoryRecord> = emptyList()): AppViewModel =
        AppViewModel(
            configRepository = ConfigRepository(Files.createTempDirectory("cm-ia").resolve("config.local.json").toFile()),
            historyStore = InMemoryHistoryStore(history),
        )

    private fun record(id: String, title: String): HistoryRecord {
        val session = SampleCourses.seriesSession(now).copy(title = title)
        val result = SampleCourses.seriesAnalysis(now)
        return HistoryRecord(
            id = id,
            title = title,
            createdAtEpochMs = now + id.hashCode(),
            providerName = "BLUELM",
            profileLabel = "official_bluelm",
            model = "qwen3.5-plus",
            knowledgePointCount = result.knowledgePoints.size,
            quizCount = result.quizQuestions.size,
            fallbackUsed = false,
            validationStatus = "PASS",
            session = session,
            result = result,
        )
    }

    private fun readText(path: String): String {
        val direct = Paths.get(path)
        val fromApp = Paths.get("app").resolve(path)
        val resolved = when {
            Files.exists(direct) -> direct
            Files.exists(fromApp) -> fromApp
            else -> direct
        }
        return String(Files.readAllBytes(resolved), Charsets.UTF_8)
    }
}
