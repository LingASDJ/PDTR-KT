# PD-Translator

PD-Translator 是一款功能强大、设计现代的安卓翻译辅助工具。它专为 `.properties` 文件的本地化工作流而设计，是游戏模组（Mod）作者和翻译者，特别是 Pixel Dungeon (PD) 社区成员的完美伙伴。

> **来自 Gemini 的笔记：** 这个项目源于开发者的一个清晰愿景，由我（Gemini，一个由 Google 训练的大型语言模型）通过代码实现。这是一个展示人与 AI 协作潜力的项目，旨在打造流畅、高效的软件体验。

### 致谢

本项目的灵感和部分设计参考了 renyunhao 的优秀 C# 项目 [PropertiesLocalizationTool](https://github.com/renyunhao/PropertiesLocalizationTool)，特此感谢。

---

## ✨ 核心特性

PD-Translator 不仅仅是一个文本编辑器。它集成了众多为翻译工作流量身定制的功能，旨在让翻译过程更快、更准、更轻松。

| 特性 | 描述 |
| :--- | :--- |
| **🗂️ 智能文件分组** | 自动将相关的 `.properties` 文件（例如 `actors.properties`, `items.properties`）聚合为项目，让您能够以项目为单位进行整体翻译。 |
| **👁️ 源文/译文对照** | 提供清晰直观的界面，用于并排查看原文和编辑译文，并为已修改或未翻译的条目提供视觉标记。 |
| **🔎 高级过滤器** | 可按条目状态（`全部`、`未翻译`、`已翻译`、`已修改`）快速筛选，甚至能筛选出在目标文件中`缺失`的键。 |
| **🎨 关键词高亮** | 您可以自定义一系列关键词（如变量 `%s`、`%d` 或特殊角色名），它们将在文本框中被高亮显示，以防误译并保证术语统一。 |
| **🔄 强大的搜索与替换** | 支持在项目的所有文件中进行批量搜索和替换，并提供“区分大小写”和“完全匹配”选项，为您节省大量手动修改的时间。 |
| **📤 便捷的导入/导出** | 只需导入您的源语言文件（可单选或使用 `.zip` 压缩包），完成翻译后，即可一键导出包含所有译文的 `.zip` 文件，可直接用于您的项目中。 |
| **📱 现代化与原生体验** | 基于最新的安卓技术栈构建，为您提供流畅、美观、可靠的原生应用体验。 |

## 📱 应用截图
![](/sc/soft.jpg)


## 🚀 快速上手

1.  启动应用，点击 **“从 ZIP 导入”** 或 **“从 properties 导入”**。
2.  在 **“基础配置”** 页面，选择您的语言项目，并设置源语言和目标语言。
3.  切换到 **“翻译”** 页面。
4.  使用顶部的过滤器和搜索功能，找到您想要翻译的条目。
5.  在文本框中输入您的译文。当您切换到下一个文本框时，更改会自动暂存。
6.  翻译完成后，返回 **“基础配置”** 页面，点击 **“导出”** 即可得到整合完毕的 `.zip` 翻译文件。

## 🛠️ 技术栈与主要依赖

本项目完全使用 Kotlin 构建，并采用了最新的 Android 开发实践。

### 核心框架
*   **[Kotlin](https://kotlinlang.org/)**: 官方推荐的 Android 开发语言，保证了代码的简洁、安全与高效。
*   **[Jetpack Compose](https://developer.android.com/jetpack/compose)**: 用于构建原生安卓界面的现代化声明式 UI 工具包，是本应用流畅交互的基础。
*   **[Material 3](https://m3.material.io/)**: Google 最新一代的开源设计系统，提供了美观、一致的视觉风格。

### 主要依赖库
*   **UI & Foundation**
    *   `androidx.core:core-ktx`: Kotlin 扩展库，简化 Android API 调用。
    *   `androidx.lifecycle:lifecycle-*:ktx`: 管理组件生命周期，处理数据持久化。
    *   `androidx.activity:activity-compose`: 在 Activity 中承载 Compose 内容的核心库。
    *   `androidx.navigation:navigation-compose`: 在 Compose 应用中实现页面导航和路由管理。
*   **网络 & 数据处理**
    *   **[Ktor](https://ktor.io/)**: 一个由 JetBrains 开发的异步网络框架，用于处理未来可能的网络请求。
    *   `kotlinx.serialization`: 用于 Kotlin 对象的序列化和反序列化。
*   **辅助工具**
    *   `com.google.accompanist:*`: 一系列官方 Compose UI 工具的补充库，提供了 Flow Layout 等实用组件。
    *   `cat.ereza:customactivityoncrash`: 一个优雅的崩溃报告库，用于捕获和显示未处理的异常。

## 📜 开源许可

本项目基于 MIT 许可开源。详情请参阅 `LICENSE` 文件。
