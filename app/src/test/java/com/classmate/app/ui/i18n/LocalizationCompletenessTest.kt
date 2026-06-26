package com.classmate.app.ui.i18n

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The localization CONTRACT for the in-app [Strings] system (the single source of UI copy): every
 * product string must have a non-blank Chinese AND English value, and — except for deliberate
 * language-neutral values (app name, format menus) — the two must actually differ. This prevents the
 * "switched to English but the page is still Chinese" drift the team hit on device.
 *
 * Technical short codes (BlueLM, qwen3.5-plus, JSON), model names, course content and file paths are
 * NOT covered here on purpose — they stay in English in both packs (see Strings.kt docs).
 */
class LocalizationCompletenessTest {

    private val zh = appStrings(AppLanguage.ZH)
    private val en = appStrings(AppLanguage.EN)

    /** Representative product strings spanning every wired section. (name -> zh value -> en value) */
    private fun pairs(): List<Triple<String, String, String>> = listOf(
        Triple("tabHome", zh.tabHome, en.tabHome),
        Triple("tabImport", zh.tabImport, en.tabImport),
        Triple("tabReview", zh.tabReview, en.tabReview),
        Triple("tabHistory", zh.tabHistory, en.tabHistory),
        Triple("tabSettings", zh.tabSettings, en.tabSettings),
        Triple("back", zh.back, en.back),
        Triple("emptyNoReviewTasks", zh.emptyNoReviewTasks, en.emptyNoReviewTasks),
        Triple("emptyNoCourses", zh.emptyNoCourses, en.emptyNoCourses),
        Triple("homeImport", zh.homeImport, en.homeImport),
        Triple("homeLive", zh.homeLive, en.homeLive),
        Triple("homeReviewToday", zh.homeReviewToday, en.homeReviewToday),
        Triple("homeLibrary", zh.homeLibrary, en.homeLibrary),
        Triple("homeModelSettings", zh.homeModelSettings, en.homeModelSettings),
        Triple("homeExport", zh.homeExport, en.homeExport),
        Triple("historyTitle", zh.historyTitle, en.historyTitle),
        Triple("courseLibrary", zh.courseLibrary, en.courseLibrary),
        Triple("recentLessons", zh.recentLessons, en.recentLessons),
        Triple("backToLibrary", zh.backToLibrary, en.backToLibrary),
        Triple("lessonRecords", zh.lessonRecords, en.lessonRecords),
        Triple("relatedTasks", zh.relatedTasks, en.relatedTasks),
        Triple("localFallback", zh.localFallback, en.localFallback),
        Triple("importTitle", zh.importTitle, en.importTitle),
        Triple("importIntroTitle", zh.importIntroTitle, en.importIntroTitle),
        Triple("importIntro", zh.importIntro, en.importIntro),
        Triple("importCourseTitle", zh.importCourseTitle, en.importCourseTitle),
        Triple("importClassroomText", zh.importClassroomText, en.importClassroomText),
        Triple("importGenerate", zh.importGenerate, en.importGenerate),
        Triple("importSample", zh.importSample, en.importSample),
        Triple("settingsTitle", zh.settingsTitle, en.settingsTitle),
        Triple("settingsLanguageDesc", zh.settingsLanguageDesc, en.settingsLanguageDesc),
        Triple("quizLabel", zh.quizLabel, en.quizLabel),
        Triple("quizEmpty", zh.quizEmpty, en.quizEmpty),
        Triple("quizPrev", zh.quizPrev, en.quizPrev),
        Triple("quizNext", zh.quizNext, en.quizNext),
        Triple("quizToReview", zh.quizToReview, en.quizToReview),
        Triple("quizExplanation", zh.quizExplanation, en.quizExplanation),
        Triple("quizTestedKp", zh.quizTestedKp, en.quizTestedKp),
        Triple("quizTooHard", zh.quizTooHard, en.quizTooHard),
        Triple("quizNeedMore", zh.quizNeedMore, en.quizNeedMore),
        Triple("quizCorrect", zh.quizCorrect, en.quizCorrect),
        Triple("quizSelected", zh.quizSelected, en.quizSelected),
    )

    /** Values that are intentionally identical across languages (proper nouns / format menus). */
    private val sameByDesign = setOf("appName", "homeExportDesc")

    @Test
    fun everyWiredStringHasNonBlankZhAndEn() {
        pairs().forEach { (name, z, e) ->
            assertTrue("$name zh value is blank", z.isNotBlank())
            assertTrue("$name en value is blank", e.isNotBlank())
        }
    }

    @Test
    fun productStringsAreActuallyLocalized() {
        pairs().filter { it.first !in sameByDesign }.forEach { (name, z, e) ->
            assertNotEquals("$name is not localized (zh == en)", z, e)
        }
    }

    @Test
    fun parameterizedStringsLocalizeToo() {
        assertNotEquals(zh.homeDueToday(3), en.homeDueToday(3))
        assertNotEquals(zh.lessonsUnit(2), en.lessonsUnit(2))
        assertTrue(zh.importChars(10).contains("10"))
        assertTrue(en.importChars(10).contains("10"))
    }

    @Test
    fun languageNeutralValuesStayIdentical() {
        // App name and the export-format menu are deliberately the same in both packs.
        org.junit.Assert.assertEquals(zh.appName, en.appName)
        org.junit.Assert.assertEquals(zh.homeExportDesc, en.homeExportDesc)
    }
}
