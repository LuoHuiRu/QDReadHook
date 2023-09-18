package cn.xihan.qdds

import android.widget.FrameLayout
import android.widget.LinearLayout
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.android.ViewClass
import com.highcapable.yukihookapi.hook.type.java.CharSequenceClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.UnitType

/**
 * @项目名 : QDReadHook
 * @作者 : MissYang
 * @创建时间 : 2023/9/15 21:47
 * @介绍 :
 */
fun PackageParam.automaticReceiveOptionOption(
    versionCode: Int,
    configurations: List<OptionEntity.SelectedModel>,
) {
    configurations.filter { it.selected }.takeIf { it.isNotEmpty() }?.forEach { selected ->
        when (selected.title) {
            "自动领取阅读积分" -> receiveReadingCreditsAutomatically(versionCode)
        }
    }
}

/**
 * 自动领取阅读积分
 */
fun PackageParam.receiveReadingCreditsAutomatically(versionCode: Int) {
    when (versionCode) {
        in 970..1020 -> {
            /**
             * 自动领取今日阅读时长积分
             */
            findClass("com.qidian.QDReader.ui.view.readtime.ReadTimeTodayCardView").hook {
                injectMember {
                    method {
                        name = "addBubbleView"
                        paramCount(4)
                        returnType = UnitType
                    }
                    afterHook {
                        val bubbleViewMap = instance.getParam<HashMap<*, *>>("bubbleViewMap")
                        bubbleViewMap?.values?.forEach { view ->
                            view.safeCast<LinearLayout>()?.postRandomDelay { performClick() }
                        }
                    }
                }
            }

            /**
             * 自动领取每周阅读时长宝箱
             */
            findClass("com.qidian.QDReader.ui.view.readtime.ReadTimeWeekCardView").hook {
                injectMember {
                    method {
                        name = "bindBox"
                        paramCount(2)
                        returnType = IntType
                    }
                    afterHook {
                        args[1].safeCast<List<*>>() ?: return@afterHook

                        val pBarTagContainerId = when (versionCode) {
                            970 -> 0x7F091391
                            980 -> 0x7F0913D0
                            994 -> 0x7F0913F6
                            1005 -> 0x7F09140D
                            1020 -> 0x7F09140D
                            else -> null
                        }
                        if (pBarTagContainerId == null) {
                            "自动领取每周阅读时长宝箱".printlnNotSupportVersion(HookEntry.versionCode)
                            return@afterHook
                        }
                        val view = instanceClass.method {
                            name = "_\$_findCachedViewById"
                            paramCount(1)
                            returnType = ViewClass
                        }.get(instance).call(pBarTagContainerId).safeCast<FrameLayout>()

                        view?.let {
                            val count = it.childCount
                            for (i in 0..<count) {
                                val child = it.getChildAt(i)
                                if (child is LinearLayout) {
                                    child.postRandomDelay { performClick() }
                                }
                            }
                        }
                    }
                }
            }

            /**
             * 自动领取每日任务奖励
             */
            findClass("com.qidian.QDReader.ui.activity.ReadTimeMainPageActivity").hook {
                injectMember {
                    method {
                        name = "changeSubmitStatus"
                        paramCount(2)
                        returnType = UnitType
                    }
                    afterHook {
                        val button = args[0].safeCast<LinearLayout>() ?: return@afterHook
                        button.current {
                            val text = method {
                                name = "getText"
                                emptyParam()
                                returnType = CharSequenceClass
                            }.call()

                            if ("领取" == text) {
                                button.postRandomDelay { performClick() }
                            }
                        }
                    }
                }
            }

            /**
             * 自动领取(开始)阅读PK场
             */
            findClass("com.qidian.QDReader.ui.view.readtime.ReadTimePKCardView").hook {
                injectMember {
                    method {
                        name = "bindData"
                        paramCount(1)
                        returnType = UnitType
                    }
                    afterHook {
                        val button = instanceClass.method {
                            name = "getQdButtonBottom"
                            emptyParam()
                            returnType = "com.qd.ui.component.widget.QDUIButton".toClass()
                        }.get(instance).call().safeCast<LinearLayout>() ?: return@afterHook

                        button.current {
                            val text = method {
                                name = "getText"
                                emptyParam()
                                returnType = CharSequenceClass
                            }.call()
                            val list = listOf(
                                "领取奖励", "开启新一周PK", "匹配对手"
                            )
                            if (text in list) {
                                button.postRandomDelay { performClick() }
                            }
                        }
                    }
                }
            }

        }

        else -> "自动领取阅读积分".printlnNotSupportVersion(versionCode)
    }
}