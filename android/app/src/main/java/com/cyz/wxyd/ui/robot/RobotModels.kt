package com.cyz.wxyd.ui.robot

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 钉钉机器人（`GET /robot/list` 的一行；服务端 `DingRobot` 字段）。
 *
 * 首页之外的模块也会用到（羊毛雷达的钉钉通道要选机器人），所以独立成文件。
 */
data class RobotItem(
    val id: Long,
    val name: String,
    /** 群机器人 webhook 的 access_token；首版不做编辑回显的脱敏处理（服务端已按用户隔离） */
    val accessToken: String,
    val secret: String,
)

internal fun parseRobots(array: JsonArray): List<RobotItem> = array.mapNotNull { el ->
    val obj = el as? JsonObject ?: return@mapNotNull null
    val id = obj["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
    RobotItem(
        id = id,
        name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifEmpty { "机器人#$id" },
        accessToken = obj["accessToken"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        secret = obj["secret"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}
