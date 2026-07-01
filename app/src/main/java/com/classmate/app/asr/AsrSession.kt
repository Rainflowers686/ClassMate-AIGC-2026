package com.classmate.app.asr

import com.classmate.core.material.SpeakerLabel
import com.classmate.core.transcript.TranscriptSegmentDraft

/**
 * State of the EXPERIMENTAL live speech-to-text mode. This is the pure, Android-free state machine so
 * it is fully unit-testable; the real [SpeechRecognizerEngine] (system SpeechRecognizer) is a seam that
 * drives it via callbacks. ClassMate uses ONLY the system recognizer: it does not save raw audio, does
 * not upload audio, does not record in the background, and does not do voiceprint identification.
 */
enum class AsrState {
    IDLE,
    PERMISSION_REQUIRED,
    LISTENING,
    PROCESSING,
    PAUSED,
    ERROR,
    UNSUPPORTED,
}

data class AsrSession(
    val state: AsrState = AsrState.IDLE,
    val partialText: String = "",
    val segments: List<TranscriptSegmentDraft> = emptyList(),
    val errorMessage: String? = null,
    val startedAtMs: Long = 0L,
    val accumulatedMs: Long = 0L,
    val lastSegmentEndMs: Long = 0L,
) {
    val isActive: Boolean get() = state == AsrState.LISTENING || state == AsrState.PROCESSING
    val confirmedCount: Int get() = segments.size

    /** Approximate elapsed wall-clock since start (used only for coarse sentence timing, not audio). */
    fun elapsedMs(now: Long): Long =
        accumulatedMs + if (isActive) (now - startedAtMs).coerceAtLeast(0L) else 0L
}

/**
 * Pure transitions for [AsrSession]. Partial results NEVER become segments (they are display-only);
 * only final results are appended. Errors/stops keep already-confirmed segments so nothing is lost.
 */
object AsrSessionController {

    fun begin(session: AsrSession, available: Boolean, permissionGranted: Boolean, now: Long): AsrSession = when {
        !available -> session.copy(state = AsrState.UNSUPPORTED, partialText = "", errorMessage = null)
        !permissionGranted -> session.copy(state = AsrState.PERMISSION_REQUIRED, partialText = "")
        else -> session.copy(
            state = AsrState.LISTENING,
            startedAtMs = now,
            accumulatedMs = 0L,
            lastSegmentEndMs = 0L,
            partialText = "",
            errorMessage = null,
            segments = emptyList(),
        )
    }

    /** Recognizer is actively listening again (e.g. after restarting for continuous dictation). */
    fun onListening(session: AsrSession): AsrSession =
        if (session.state == AsrState.PROCESSING || session.state == AsrState.LISTENING) {
            session.copy(state = AsrState.LISTENING)
        } else {
            session
        }

    /** System reports end-of-speech; we are waiting for the final result. */
    fun onEndOfSpeech(session: AsrSession): AsrSession =
        if (session.state == AsrState.LISTENING) session.copy(state = AsrState.PROCESSING) else session

    /** Partial (interim) text — display only, never stored as a segment. */
    fun onPartial(session: AsrSession, text: String): AsrSession =
        if (session.isActive) session.copy(partialText = text) else session

    /** A FINAL recognized utterance becomes one confirmed [TranscriptSegmentDraft]. */
    fun onFinal(session: AsrSession, text: String, confidence: Double?, speaker: SpeakerLabel, now: Long): AsrSession {
        val clean = text.trim()
        if (!session.isActive || clean.isEmpty()) return session.copy(partialText = "", state = AsrState.LISTENING)
        val lastText = session.segments.lastOrNull()?.text?.trim()
        if (lastText == clean) {
            return session.copy(state = AsrState.LISTENING, partialText = "")
        }
        val end = session.elapsedMs(now)
        val start = session.lastSegmentEndMs.coerceAtMost(end)
        val index = session.segments.size + 1
        val segment = TranscriptSegmentDraft(
            id = "asr_$index",
            startMs = start,
            endMs = end,
            speaker = speaker,
            text = clean,
            sourceLine = index,
            confidence = confidence,
        )
        return session.copy(
            state = AsrState.LISTENING,
            segments = session.segments + segment,
            partialText = "",
            lastSegmentEndMs = end,
        )
    }

    fun onError(session: AsrSession, message: String): AsrSession =
        session.copy(state = AsrState.ERROR, errorMessage = message, partialText = "")

    fun pause(session: AsrSession, now: Long): AsrSession =
        if (session.isActive) session.copy(state = AsrState.PAUSED, accumulatedMs = session.elapsedMs(now), partialText = "") else session

    fun resume(session: AsrSession, now: Long): AsrSession =
        if (session.state == AsrState.PAUSED) session.copy(state = AsrState.LISTENING, startedAtMs = now) else session

    /** Stop listening but KEEP confirmed segments (for editing / analysis). */
    fun stop(session: AsrSession, now: Long): AsrSession =
        session.copy(state = AsrState.IDLE, accumulatedMs = session.elapsedMs(now), partialText = "")
}
