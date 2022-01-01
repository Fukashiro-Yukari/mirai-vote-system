package io.github.kamishirokalina

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info

object VoteSystem : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.kamishirokalina.votesystem",
        name = "Vote System",
        version = "1.0",
    ) {
        author("Kamishiro Kalina")
        info("""群内的投票系统""")
    }
) {
    private var IsVoting = false
    private val IsVotingQQ = mutableListOf<Member>()
    private var voteYes = 0
    private var voteNo = 0

    private fun CloseVote() {
        IsVoting = false
        IsVotingQQ.clear()
    }

    override fun onEnable() {
        logger.info { "Vote System Loading..." }

        val eventChannel = GlobalEventChannel.parentScope(this)

        eventChannel.subscribeAlways<GroupMessageEvent> {
            val cmd = message.contentToString().split(" ")

            if (cmd[0] == "/vote" || cmd[0] == "/投票") {
                if (group.botPermission.level < 1) {
                    group.sendMessage("权限不足: 需要管理员权限!")

                    return@subscribeAlways
                }

                if (cmd.size < 2) {
                    group.sendMessage("参数不足: 请输入投票类型!")

                    return@subscribeAlways
                } else {
                    when (cmd[1]) {
                        "kick", "踢人" -> {
                            if (cmd.size < 3) {
                                group.sendMessage("参数不足: 请输入QQ号")

                                return@subscribeAlways
                            }

                            if (IsVoting) {
                                group.sendMessage("投票错误: 已经有一场投票在进行中!")

                                return@subscribeAlways
                            }

                            var qq = cmd[2].toLongOrNull()

                            if (qq == null) {
                                qq = cmd[2].substring(1).toLongOrNull()

                                if (qq == null) {
                                    group.sendMessage("参数错误: 请输入正确的QQ号或At对象")

                                    return@subscribeAlways
                                }
                            }

                            val nkick = group[qq]

                            if (nkick != null) {
                                group.sendMessage(
                                    At(sender) + PlainText("发起了踢人投票!\n") +
                                            PlainText("内容: 踢出") + At(nkick) + PlainText("\n") +
                                            PlainText("时间: 120秒")
                                )

                                IsVoting = true
                                voteNo++
                                voteYes++
                                IsVotingQQ.add(nkick)
                                IsVotingQQ.add(sender)

                                async {
                                    delay(120000)

                                    val need = voteYes - voteNo
                                    var kick = false
                                    var text = At(nkick) + PlainText("不会被踢出.")

                                    if (need > 0) {
                                        kick = true
                                        text = At(nkick) + PlainText("将会被踢出.")
                                    }

                                    group.sendMessage(
                                        PlainText("投票结束! 同意: ${voteYes}, 否定: ${voteNo}\n") +
                                                text
                                    )

                                    if (kick) {
                                        if (nkick.permission.level > 0)
                                            group.sendMessage("踢出错误: 踢出人是管理员或群主.")
                                        else {
                                            try {
                                                nkick.kick("投票踢出")
                                            } catch (e: PermissionDeniedException) {
                                                group.sendMessage("踢出错误: 没有权限.")
                                            }
                                        }
                                    }

                                    CloseVote()
                                }

                                group.sendMessage("输入 (yes,同意,支持) 表示同意, 输入 (no,否,不是,反对) 表示否定")
                            } else {
                                group.sendMessage("参数错误: 请输入正确的QQ号或At对象")

                                return@subscribeAlways
                            }
                        }
                        "custom", "自定义" -> {
                            if (cmd.size < 5) {
                                group.sendMessage("参数不足: 请输入投票主题和两个投票选项")

                                return@subscribeAlways
                            }

                            if (IsVoting) {
                                group.sendMessage("投票错误: 已经有一场投票在进行中!")

                                return@subscribeAlways
                            }

                            group.sendMessage(
                                At(sender) + PlainText("发起了自定义投票!\n") +
                                        PlainText("内容: ${cmd[2]}\n") +
                                        PlainText("同意: ${cmd[3]}\n") +
                                        PlainText("否定: ${cmd[4]}\n") +
                                        PlainText("时间: 120秒")
                            )

                            IsVoting = true
                            voteYes++
                            IsVotingQQ.add(sender)

                            async {
                                delay(120000)

                                val need = voteYes - voteNo
                                var text = PlainText("'${cmd[4]}' 投票最多")

                                if (need > 0)
                                    text = PlainText("'${cmd[3]}' 投票最多")

                                group.sendMessage(
                                    PlainText("投票结束! 同意: ${voteYes}, 否定: ${voteNo}\n") +
                                            text
                                )

                                CloseVote()
                            }

                            group.sendMessage("输入 (yes,同意,支持) 表示同意, 输入 (no,否,不是,反对) 表示否定")
                        }
                        "progress", "进度" -> {
                            if (!IsVoting) {
                                group.sendMessage("投票错误: 目前没有任何投票正在进行!")

                                return@subscribeAlways
                            }

                            group.sendMessage("当前投票数: 同意: ${voteYes}, 否定: ${voteNo}")
                        }
                        else -> {
                            group.sendMessage("参数错误: 请输入正确的投票类型")

                            return@subscribeAlways
                        }
                    }
                }
            }

            if (cmd[0] == "yes" || cmd[0] == "同意" || cmd[0] == "支持") {
                if (!IsVoting)
                    return@subscribeAlways

                if (IsVotingQQ.contains(sender)) {
                    group.sendMessage("投票错误: 你已经进行过投票!")

                    return@subscribeAlways
                }

                voteYes++
                IsVotingQQ.add(sender)
                group.sendMessage("当前投票数: 同意: ${voteYes}, 否定: ${voteNo}")
            }

            if (cmd[0] == "no" || cmd[0] == "否" || cmd[0] == "不是" || cmd[0] == "反对") {
                if (!IsVoting)
                    return@subscribeAlways

                if (IsVotingQQ.contains(sender)) {
                    group.sendMessage("投票错误: 你已经进行过投票!")

                    return@subscribeAlways
                }

                voteNo++
                IsVotingQQ.add(sender)
                group.sendMessage("当前投票数: 同意: ${voteYes}, 否定: ${voteNo}")
            }
        }
    }
}