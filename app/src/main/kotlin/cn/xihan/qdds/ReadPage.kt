package cn.xihan.qdds

import android.content.Context
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.alibaba.fastjson2.parseObject
import com.alibaba.fastjson2.toJSONString
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.ListClass
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.type.java.UnitType
import org.luckypray.dexkit.DexKitBridge
import java.io.File
import java.lang.reflect.Modifier

/**
 * 重定向阅读页背景路径
 * 重定向至: /storage/emulated/0/Download/QDReader/ReaderTheme
 * @since 7.9.306-1030 ~ 1099
 * @param [versionCode] 版本代码
 */
fun PackageParam.redirectReadingPageBackgroundPath(versionCode: Int) {
    when (versionCode) {
        in 1030..1099 -> {
            DexKitBridge.create(appInfo.sourceDir)?.use { bridge ->
                bridge.findClass {
                    excludePackages = listOf("com")
                    matcher {
                        usingStrings =
                            listOf("QDReaderAndroidUpdateNew.xml", "QDReader.apk", "ReaderTheme")
                    }
                }.firstNotNullOfOrNull { classData ->
                    classData.getMethods().findMethod {
                        matcher {
                            returnType = "java.lang.String"
                            paramTypes = listOf("long")
                            usingStrings = listOf("ReaderTheme")
                        }
                    }.firstNotNullOfOrNull { methodData ->
                        methodData.className.toClass().method {
                            name = methodData.methodName
                            paramCount(methodData.paramTypeNames.size)
                            returnType = StringClass
                        }.hook().replaceTo(redirectThemePath)
                    }
                }
            }
        }

        else -> "重定向阅读页背景路径".printlnNotSupportVersion(versionCode)
    }
}

/**
 * 阅读页面章节相关
 * @since 7.9.306-1030 ~ 1099
 * @param [versionCode] 版本代码
 * @param [enableShowReaderPageChapterSaveRawPictures] 启用显示阅读器页面章节保存原始图片
 * @param [enableShowReaderPageChapterSavePictureDialog] 启用显示阅读器页面章节保存图片对话框
 * @param [enableShowReaderPageChapterSaveAudioDialog] 启用“显示阅读器页面”章节“保存音频”对话框
 * @param [enableCopyReaderPageChapterComment] 启用副本阅读器页面章节注释
 */
fun PackageParam.readingPageChapterCorrelation(
    versionCode: Int,
    enableShowReaderPageChapterSaveRawPictures: Boolean = false,
    enableShowReaderPageChapterSavePictureDialog: Boolean = false,
    enableShowReaderPageChapterSaveAudioDialog: Boolean = false,
    enableCopyReaderPageChapterComment: Boolean = false
) {
    when (versionCode) {
        in 1030..1099 -> {
            if (enableShowReaderPageChapterSaveRawPictures) {
                "com.qd.ui.component.modules.imagepreivew.QDUIGalleryActivity".toClass().method {
                    name = "initView"
                    emptyParam()
                    returnType = UnitType
                }.hook().after {
                    instance.setParam("mMoreIconStyle", 1)
                }
            }

            if (enableShowReaderPageChapterSavePictureDialog || enableCopyReaderPageChapterComment) {
                DexKitBridge.create(appInfo.sourceDir)?.use { bridge ->
                    bridge.findClass {
                        searchPackages = listOf("com.qidian.QDReader.ui.viewholder.chaptercomment")
                        matcher {
                            usingStrings = listOf("%s楼 · %s")
                        }
                    }.forEach { classData ->
                        classData.getMethods().findMethod {
                            matcher {
                                returnType = "void"
                                paramTypes = listOf(
                                    "com.qidian.QDReader.repository.entity.chaptercomment.NewParagraphCommentListBean\$DataListBean",
                                    "com.qidian.QDReader.repository.entity.chaptercomment.NewParagraphCommentListBean\$BookInfoBean"
                                )
                                usingStrings = listOf(" · %s")
                            }
                        }.firstNotNullOfOrNull { methodData ->
                            methodData.className.toClass().method {
                                name = methodData.methodName
                                paramCount(methodData.paramTypeNames.size)
                                returnType = UnitType
                            }.hook().after {
                                if (enableShowReaderPageChapterSavePictureDialog) {
                                    val rawImgUrl = args[0]?.toJSONString().parseObject()
                                        .getString("imageDetail")
                                    val imageViews = instance.getViews<ImageView>()
                                    if (!rawImgUrl.isNullOrBlank() || imageViews.isNotEmpty()) {
                                        imageViews.filter { "app:id/image" in it.toString() }
                                            .takeIf { it.isNotEmpty() }?.first()
                                            ?.setOnLongClickListener { imageView ->
                                                imageView.context.alertDialog {
                                                    title = "图片地址"
                                                    message = rawImgUrl
                                                    positiveButton("复制") {
                                                        imageView.context.copyToClipboard(rawImgUrl)
                                                    }
                                                    negativeButton("取消") {
                                                        it.dismiss()
                                                    }
                                                    build()
                                                    show()
                                                }
                                                true
                                            }
                                    }
                                }

                                if (enableCopyReaderPageChapterComment) {
                                    val messageTextView =
                                        "com.qd.ui.component.widget.textview.MessageTextView".toClass()
                                    val textViews = instance.getViews(messageTextView)
                                    if (textViews.isNotEmpty()) {
                                        textViews.forEach { any ->
                                            val textView = any.safeCast<TextView>()
                                            textView?.setOnLongClickListener {
                                                textView.context.alertDialog {
                                                    title = "评论内容"
                                                    message = textView.text.toString()
                                                    positiveButton("复制") {
                                                        textView.context.copyToClipboard(textView.text.toString())
                                                    }
                                                    negativeButton("取消") {
                                                        it.dismiss()
                                                    }
                                                    build()
                                                    show()
                                                }
                                                true
                                            }

                                        }
                                    }
                                }

                            }
                        }
                    }
                }

            }

            if (enableShowReaderPageChapterSaveAudioDialog) {
                DexKitBridge.create(appInfo.sourceDir)?.use { bridge ->
                    bridge.findClass {
                        searchPackages = listOf("com.qidian.QDReader.ui.view.chapter_review")
                        matcher {
                            usingStrings = listOf("temp_audio", "temp", "audio")
                        }
                    }.firstNotNullOfOrNull { classData ->
                        classData.getMethods().findMethod {
                            matcher {
                                modifiers = Modifier.PUBLIC
                                paramCount = 6
                                returnType = "void"
                                usingStrings = listOf("temp")
                            }
                        }.firstNotNullOfOrNull { methodData ->
                            methodData.className.toClass().method {
                                name = methodData.methodName
                                paramCount(methodData.paramTypeNames.size)
                                returnType = UnitType
                            }.hook().after {
                                val relativeLayouts = instance.getViews(
                                    "com.qd.ui.component.widget.roundwidget.QDUIRoundRelativeLayout".toClass(),
                                    true
                                ).filterIsInstance<RelativeLayout>()
                                val strings =
                                    instance.getParamList<String>().filter { it.isNotBlank() }
                                relativeLayouts.forEach { relativeLayout ->
                                    relativeLayout.setOnLongClickListener { view ->
                                        view.context.audioExportDialog(strings[0], strings[1])
                                        true
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        else -> "阅读页面章节相关".printlnNotSupportVersion(versionCode)
    }
}

/**
 * 音频文件导出对话框
 */
private fun Context.audioExportDialog(networkUrl: String, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        toast("音频文件不存在")
        return
    }
    val linearLayout = CustomLinearLayout(context = this)
    val textView = CustomTextView(
        context = this,
        text = "音频文件网络地址: $networkUrl\n音频文件本地地址: $filePath",
    )
    val editText = CustomEditText(
        context = this,
        hint = "输入要保存的文件名",
        value = file.name,
    )
    linearLayout.apply {
        addView(editText)
        addView(textView)
    }
    alertDialog {
        title = "导出文件\n输入要保存的文件名"
        customView = linearLayout
        positiveButton("本地导出") {
            val fileName = "${editText.editText.text}.m4a"
            if (fileName.isBlank()) {
                toast("文件名不能为空")
                return@positiveButton
            }
            val saveFile = File(
                audioPath, fileName
            ).apply {
                parentFile?.mkdirs()
            }
            if (saveFile.exists()) {
                toast("文件已存在")
                return@positiveButton
            }
            file.copyTo(saveFile)
            toast("导出成功")
            it.dismiss()
        }
        negativeButton("复制网络地址") {
            copyToClipboard(networkUrl)
        }
        build()
        show()
    }

}

/**
 * 阅读时间加倍
 * 随缘生效
 * @since 7.9.306-1030 ~ 1099
 * @param [versionCode] 版本代码
 * @param [speedFactor] 速度系数
 */
fun PackageParam.readingTimeSpeedFactor(
    versionCode: Int,
    speedFactor: Int = 5
) {
    when (versionCode) {
        in 1030..1099 -> {
            DexKitBridge.create(appInfo.sourceDir)?.use { bridge ->
                bridge.findClass {
                    excludePackages = listOf("com")
                    matcher {
                        usingStrings =
                            listOf("xys", "自动保存后重新开始新的Session", "user_book_read_time")
                    }
                }.firstNotNullOfOrNull { classData ->
                    classData.getMethods().findMethod {
                        matcher {
                            returnType = "java.util.List"
                            paramCount = 2
                            usingStrings = listOf("user_book_read_time")
                        }
                    }.firstNotNullOfOrNull { methodData ->
                        methodData.className.toClass().method {
                            name = methodData.methodName
                            paramCount(methodData.paramTypeNames.size)
                            returnType = ListClass
                        }.hook().after {
                            val list = result.safeCast<MutableList<*>>()
                            if (list.isNullOrEmpty()) return@after
                            list.forEach { item ->
                                item?.let {
                                    val totalTime = it.getParam<Long>("totalTime")
                                    val currentTime = System.currentTimeMillis()
                                    val startTime2 =
                                        currentTime - ((totalTime ?: 10000) * speedFactor)
                                    it.setParams(
                                        "startTime" to startTime2,
                                        "endTime" to currentTime,
                                        "totalTime" to (currentTime - startTime2),
                                        "chapterVIP" to 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        else -> "阅读时间加倍".printlnNotSupportVersion(versionCode)
    }

}

/**
 * 阅读页最后一页
 * @since 7.9.306-1030
 * @param [versionCode] 版本代码
 * @param [shieldAlsoRead] 屏蔽推荐
 * @param [shieldRecommendation] 屏蔽推荐
 * @param [shieldSimilarRecommend] 屏蔽相似推荐
 * @param [hideAlsoRead] 隐藏读过的书
 * @param [hideRecommendation] 隐藏推荐
 * @param [hideBookList] 隐藏书单
 * @param [hideSimilarRecommend] 隐藏相似推荐
 * @param [hideTryRead] 隐藏试读
 * @param [hideCircle] 隐藏圈子
 * @param [hideAdView] 隐藏广告
 */
fun PackageParam.readBookLastPage(
    versionCode: Int,
    shieldAlsoRead: Boolean = false,
    shieldRecommendation: Boolean = false,
    shieldSimilarRecommend: Boolean = false,
    hideAlsoRead: Boolean = false,
    hideRecommendation: Boolean = false,
    hideBookList: Boolean = false,
    hideSimilarRecommend: Boolean = false,
    hideTryRead: Boolean = false,
    hideCircle: Boolean = false,
    hideAdView: Boolean = false
) {
    when (versionCode) {
        in 1030..1099 -> {
            "com.qidian.QDReader.ui.view.lastpage.LastPageRoleView".toClass().method {
                param("com.qidian.QDReader.repository.entity.BookLastPage".toClass())
                returnType = UnitType
            }.hook().after {
                setBookLastPage(
                    obj = args[0],
                    shieldAlsoRead = shieldAlsoRead,
                    shieldRecommendation = shieldRecommendation,
                    shieldSimilarRecommend = shieldSimilarRecommend,
                    hideAlsoRead = hideAlsoRead,
                    hideRecommendation = hideRecommendation,
                    hideBookList = hideBookList,
                    hideSimilarRecommend = hideSimilarRecommend,
                    hideTryRead = hideTryRead,
                    hideCircle = hideCircle
                )
            }

            "com.qidian.QDReader.ui.view.lastpage.LastPageCircleView".toClass().method {
                param("com.qidian.QDReader.repository.entity.BookLastPage".toClass())
                returnType = UnitType
            }.hook().after {
                setBookLastPage(
                    obj = args[0],
                    shieldAlsoRead = shieldAlsoRead,
                    shieldRecommendation = shieldRecommendation,
                    shieldSimilarRecommend = shieldSimilarRecommend,
                    hideAlsoRead = hideAlsoRead,
                    hideRecommendation = hideRecommendation,
                    hideBookList = hideBookList,
                    hideSimilarRecommend = hideSimilarRecommend,
                    hideTryRead = hideTryRead,
                    hideCircle = hideCircle
                )
            }

            "com.qidian.QDReader.ui.view.lastpage.LastPageTryReadViewWrap".toClass().method {
                param("com.qidian.QDReader.repository.entity.BookLastPage".toClass())
                returnType = UnitType
            }.hook().after {
                setBookLastPage(
                    obj = args[0],
                    shieldAlsoRead = shieldAlsoRead,
                    shieldRecommendation = shieldRecommendation,
                    shieldSimilarRecommend = shieldSimilarRecommend,
                    hideAlsoRead = hideAlsoRead,
                    hideRecommendation = hideRecommendation,
                    hideBookList = hideBookList,
                    hideSimilarRecommend = hideSimilarRecommend,
                    hideTryRead = hideTryRead,
                    hideCircle = hideCircle
                )
            }

            if (hideAdView) {
                intercept(
                    className = "com.qidian.QDReader.ui.activity.BookLastPageNewActivity",
                    methodName = "updateADView",
                    paramCount = 1
                )
            }
        }

        else -> "阅读页最后一页".printlnNotSupportVersion(versionCode)
    }
}

/**
 * 处理传入的对象
 * @param obj 传入的对象
 * @param shieldAlsoRead 是否屏蔽读过的书
 * @param shieldRecommendation 是否屏蔽推荐
 * @param hideAlsoRead 是否隐藏读过的书
 * @param hideRecommendation 是否隐藏推荐
 * @param hideBookList 是否隐藏书单
 * @param shieldSimilarRecommend 是否屏蔽相似推荐
 * @param hideSimilarRecommend 是否隐藏相似推荐
 * @param hideTryRead 是否隐藏试读
 * @param hideCircle 是否隐藏圈子
 */
private fun setBookLastPage(
    obj: Any?,
    shieldAlsoRead: Boolean = false,
    shieldRecommendation: Boolean = false,
    shieldSimilarRecommend: Boolean = false,
    hideAlsoRead: Boolean = true,
    hideRecommendation: Boolean = true,
    hideBookList: Boolean = true,
    hideSimilarRecommend: Boolean = true,
    hideTryRead: Boolean = true,
    hideCircle: Boolean = true,
) {

    if (shieldAlsoRead) {
        val alsoReadList = obj?.getParam<MutableList<*>>("alsoReadList")
        alsoReadList?.let {
            obj.setParam("alsoReadList", HookEntry.parseNeedShieldList(alsoReadList))
        }
    }
    if (hideAlsoRead) {
        obj?.setParam("alsoReadList", null)
    }
    if (shieldRecommendation) {
        val recommendList = obj?.getParam<MutableList<*>>("recommendList")
        recommendList?.let {
            obj.setParam("recommendList", HookEntry.parseNeedShieldList(recommendList))
        }
    }
    if (hideRecommendation) {
        obj?.setParam("recommendList", null)
    }
    if (shieldSimilarRecommend) {
        val similarRecommend = obj?.getParam<Any>("similarRecommend")
        val bookList = similarRecommend?.getParam<MutableList<*>>("bookList")
        bookList?.let {
            similarRecommend.setParam("bookList", HookEntry.parseNeedShieldList(bookList))
        }
    }
    if (hideSimilarRecommend) {
        obj?.setParam("similarRecommend", null)
    }
    if (hideBookList) {
        obj?.setParam("relatedBookList", null)
    }
    if (hideTryRead) {
        obj?.setParam("tryReadInfo", null)
    }
    if (hideCircle) {
        val bookCircleInfo = obj?.getParam<Any>("bookCircleInfo")
        bookCircleInfo?.setParam("enableCircle", 0)
    }


}