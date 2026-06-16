package com.classmate.core.tools

import com.classmate.core.ai.AiExecutionSource
import com.classmate.core.capture.CaptureProviderConfig
import com.classmate.core.learning.ReviewEventType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

enum class FunctionCallingStatus {
    TOOL_PROPOSED,
    CONFIG_MISSING,
    INVALID_TOOL,
    PARSE_FAILED,
}

data class OfficialToolCallProposal(
    val toolName: String,
    val arguments: Map<String, String>,
    val source: AiExecutionSource = AiExecutionSource.CLOUD,
)

data class FunctionCallingProviderResult(
    val status: FunctionCallingStatus,
    val proposal: OfficialToolCallProposal? = null,
    val message: String = "",
)

fun interface FunctionCallingProvider {
    fun propose(userIntent: String, availableTools: List<InternalToolName>): FunctionCallingProviderResult
}

class ConfigMissingFunctionCallingProvider : FunctionCallingProvider {
    override fun propose(userIntent: String, availableTools: List<InternalToolName>): FunctionCallingProviderResult =
        FunctionCallingProviderResult(
            status = FunctionCallingStatus.CONFIG_MISSING,
            message = "Official function calling is not configured; use explicit UI actions.",
        )
}

class VivoFunctionCallingProvider(
    private val config: CaptureProviderConfig = CaptureProviderConfig.ABSENT,
    private val modelOutput: (String, List<InternalToolName>) -> String? = { _, _ -> null },
) : FunctionCallingProvider {
    override fun propose(userIntent: String, availableTools: List<InternalToolName>): FunctionCallingProviderResult {
        if (!config.isConfigured) return ConfigMissingFunctionCallingProvider().propose(userIntent, availableTools)
        val raw = modelOutput(userIntent, availableTools)
            ?: return FunctionCallingProviderResult(FunctionCallingStatus.PARSE_FAILED, message = "No tool proposal returned.")
        return OfficialFunctionCallParser.parse(raw, availableTools)
    }
}

object OfficialFunctionCallParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String, availableTools: List<InternalToolName> = InternalToolName.entries): FunctionCallingProviderResult {
        val root = try { json.parseToJsonElement(raw).jsonObject } catch (_: Exception) {
            return FunctionCallingProviderResult(FunctionCallingStatus.PARSE_FAILED, message = "Tool proposal JSON could not be parsed.")
        }
        val call = extractCall(root) ?: return FunctionCallingProviderResult(FunctionCallingStatus.PARSE_FAILED, message = "No tool call found.")
        val tool = OfficialToolAdapter.toInternalName(call.toolName)
            ?: return FunctionCallingProviderResult(FunctionCallingStatus.INVALID_TOOL, message = "Unknown tool rejected.")
        if (tool !in availableTools) {
            return FunctionCallingProviderResult(FunctionCallingStatus.INVALID_TOOL, message = "Tool not in allow-list.")
        }
        return FunctionCallingProviderResult(FunctionCallingStatus.TOOL_PROPOSED, call)
    }

    private fun extractCall(root: JsonObject): OfficialToolCallProposal? {
        val directName = root.str("tool") ?: root.str("name") ?: root.str("toolName")
        if (directName != null) {
            return OfficialToolCallProposal(directName, argsFrom(root["arguments"] ?: root["args"]))
        }
        val toolCalls = root["tool_calls"] as? JsonArray
        val first = toolCalls?.firstOrNull() as? JsonObject
        val function = first?.get("function") as? JsonObject
        val name = function?.str("name") ?: return null
        return OfficialToolCallProposal(name, argsFrom(function["arguments"]))
    }

    private fun argsFrom(element: JsonElement?): Map<String, String> {
        val obj = when (element) {
            is JsonObject -> element
            is JsonPrimitive -> element.contentOrNull?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
            else -> null
        } ?: return emptyMap()
        return obj.mapValues { (_, value) ->
            when (value) {
                is JsonPrimitive -> value.contentOrNull.orEmpty()
                else -> value.toString()
            }
        }
    }
}

object OfficialToolAdapter {
    fun toInternalName(name: String): InternalToolName? = when (name.trim()) {
        "searchEvidence" -> InternalToolName.SEARCH_EVIDENCE
        "createPractice" -> InternalToolName.CREATE_PRACTICE
        "updateMastery" -> InternalToolName.UPDATE_MASTERY
        "createReviewTask" -> InternalToolName.CREATE_REVIEW_TASK
        "exportStudyReport" -> InternalToolName.EXPORT_STUDY_REPORT
        "createEssenceAudioScript" -> InternalToolName.CREATE_ESSENCE_AUDIO_SCRIPT
        else -> null
    }

    fun adapt(proposal: OfficialToolCallProposal): InternalToolCall? {
        val name = toInternalName(proposal.toolName) ?: return null
        val args = proposal.arguments
        return InternalToolCall(
            name = name,
            courseTitle = args["courseTitle"].orEmpty(),
            query = args["query"].orEmpty(),
            knowledgePointId = args["knowledgePointId"].orEmpty(),
            eventType = args["eventType"]?.let { runCatching { ReviewEventType.valueOf(it) }.getOrNull() },
            now = args["now"]?.toLongOrNull() ?: 0L,
        )
    }

    fun requiresConfirmation(call: InternalToolCall): Boolean =
        call.name != InternalToolName.SEARCH_EVIDENCE
}

private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
