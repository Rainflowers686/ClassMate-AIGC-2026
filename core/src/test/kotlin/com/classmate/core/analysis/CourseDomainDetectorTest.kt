package com.classmate.core.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseDomainDetectorTest {

    @Test
    fun mechanicalContentIsNotMisreadAsPhysics() {
        val result = CourseDomainDetector.detect(
            title = "机械设计基础",
            documentText = "本节课讲解齿轮、轴承、连杆、扭矩，以及机械原理中的自由度与机构分析。",
        )
        assertEquals(CourseDomain.MECHANICAL.id, result.domainId)
        assertNotEquals(CourseDomain.PHYSICS.id, result.domainId)
        assertFalse("strong signal should not require confirmation", result.requiresUserConfirmation)
        assertTrue(result.evidenceTerms.contains("齿轮"))
    }

    @Test
    fun materialMechanicsTermsStayMechanicalNotPhysics() {
        val result = CourseDomainDetector.detect(
            title = "材料力学",
            documentText = "梁的弯矩与剪力，应力应变关系，材料力学的强度校核。",
        )
        assertEquals(CourseDomain.MECHANICAL.id, result.domainId)
        assertNotEquals(CourseDomain.PHYSICS.id, result.domainId)
    }

    @Test
    fun pythonContentIsComputerScienceNotGeneral() {
        val result = CourseDomainDetector.detect(
            title = "编程入门",
            documentText = "学习 Python 的函数、列表、循环、字典、类和异常处理。",
        )
        assertEquals(CourseDomain.COMPUTER_SCIENCE.id, result.domainId)
        assertNotEquals(CourseDomain.PHYSICS.id, result.domainId)
    }

    @Test
    fun realPhysicsContentIsDetectedAsPhysics() {
        val result = CourseDomainDetector.detect(
            title = "大学物理 电磁学",
            documentText = "洛伦兹力、电磁感应、楞次定律与磁通量变化产生的感应电动势。",
        )
        assertEquals(CourseDomain.PHYSICS.id, result.domainId)
    }

    @Test
    fun lowSignalContentFallsBackToGeneralAndAsksForConfirmation() {
        val result = CourseDomainDetector.detect(
            title = "今天的课",
            documentText = "请大家认真听讲，做好笔记，下课前完成练习。",
        )
        assertEquals(CourseDomain.GENERAL.id, result.domainId)
        assertTrue(result.requiresUserConfirmation)
    }

    @Test
    fun emptyInputIsGeneralAndNeedsConfirmation() {
        val result = CourseDomainDetector.detect()
        assertEquals(CourseDomain.GENERAL.id, result.domainId)
        assertTrue(result.requiresUserConfirmation)
        assertEquals(0.0, result.confidence, 0.0001)
    }

    @Test
    fun seedTermsAreExposedPerDomain() {
        assertTrue(CourseDomainDetector.seedTermsFor(CourseDomain.MECHANICAL).contains("齿轮"))
        assertTrue(CourseDomainDetector.seedTermsFor(CourseDomain.COMPUTER_SCIENCE).contains("python"))
        assertTrue(CourseDomainDetector.seedTermsFor(CourseDomain.GENERAL).isEmpty())
    }
}
