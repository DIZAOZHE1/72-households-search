# 72-households-search

## 《七十二家房客》剧集查询 · Episode Search

一个面向《七十二家房客》爱好者的剧集搜索与回忆工具。

**QQ交流群：1126888489，欢迎加入。**

Search and rediscover episodes, storylines, characters, and memorable moments from 72 Households.

---

## 📺 项目简介 · About

《七十二家房客》自 2008 年开播以来，剧集数量庞大。很多观众只记得一个角色、一段剧情或一句台词，却想不起具体是哪一集。

本项目将剧集资料结构化，帮助用户通过模糊记忆快速找到对应故事。

72 Households has a large number of episodes since its debut in 2008. This project organizes episode information to help users find stories through characters, plots, titles, and keywords.

---

## 📊 内容 · Content

- 第 1–19 季 · Seasons 1–19
- 1,552 个故事主线 · 1,552 Storylines
- 34 位经典角色 · 34 Characters

---

## 🔍 搜索 · Search

支持按以下内容查询：

- 季数 · Season
- 集数 · Episode
- 集名 · Title
- 剧情 · Plot
- 角色 · Character
- 关键词 · Keyword

支持多条件组合搜索，即使只记得一部分剧情，也可以尝试找回对应剧集。

Search by season, episode, title, plot, character, or keyword, with support for combined filters.

---

## 🎯 功能 · Features

- 🔎 剧集搜索 · Episode Search
- 📚 分集目录 · Episode Directory
- 👤 角色筛选 · Character Filter
- 🎲 随机选集 · Random Episode
- 🔗 相似剧情 · Similar Stories
- ⭐ 每日推荐 · Daily Recommendations
- 📋 想看 / 已看 · Watch Tracking
- 📱 响应式布局 · Responsive Design

---

## 📱 Android 客户端 · Android App

除了网页版，本项目还提供**原生 Android 应用**：一个应用、三个站点、两部剧，**完全离线、不申请网络权限**。

Besides the web pages, this project ships a native Android app: three sites in one app, fully offline, requesting no network permission.

| 底部导航 | 剧集 | 数据 | 主题 |
| --- | --- | --- | --- |
| 传统 | 《七十二家房客》 | 1552 个故事 / 19 季 | 岭南传统：青砖灰瓦 + 旧木框 + 满洲窗四色 |
| 现代 | 《七十二家房客》（同一份数据） | 同上 | 现代简洁：白底圆角 + 极轻投影 + 青绿强调 |
| 外来 | 《外来媳妇本地郎》 | 2573 个故事 / 14 部 | 朱红 / 描金 / 宣纸 |

三个站点各自独立的封面、配色、排版与搜索状态，切换站点不会互相污染。

- **剧集查询** —— 标题 → 梗概 → 角色三级排序搜索，纯数字按集数区间定位，季与主题标签，★仅看主线 / ☆只看想看 / ✓隐藏已看
- **主线剧情** —— 16 条故事线、104 个节点，节点可展开相关剧集
- **角色查询** —— 34 位角色分 5 个阵营，含别称、人物志与出场数，可按季筛选
- **相似抽取** —— 多选角色 + 主题 + 多关键词，随机抽取 1/3/5/10 集
- **追剧进度** —— ☆想看 / ✓看过 本地持久化，顶部显示「追到第几季 · 已看 x/y 集」
- **🎲 随便看一集 / 📅 今日推荐** —— 今日推荐按日期哈希，同日同推荐并跳过已看
- **🖼 分享卡片** —— 1080×1520 海报，可保存到相册或系统分享
- **深色模式** —— 三套主题各有夜间配色

### 下载 · Download

前往 [Releases](https://github.com/DIZAOZHE1/72-households-search/releases) 下载 `qiershi-android-*.apk`。

> ⚠️ **这是独立的原生版，不是网页版的升级包。** 它的包名是 `com.jordan.wailaixifu.debug`，与 `chaju-*.apk`（网页封装版，包名 `com.w2a.i4gy`）**并存**安装，可放心试用后单独卸载。当前为 debug 签名测试版，Android 7.0（API 24）及以上。

### 自行构建 · Build

源码与完整说明见 [`android/`](android/README.md)。数据由仓库内的原始网页自动生成，全新克隆无需任何额外步骤：

```bash
cd android && ./gradlew assembleDebug
```

技术栈：Kotlin 2.2.20、Jetpack Compose（BOM 2025.09.00）、Material 3、AGP 8.13.0、Gradle 8.14.3、compileSdk 36 / minSdk 24。CI（工作流 `Android`）会在每次改动 `android/` 时重建全部数据并比对，确保应用与仓库内容严格对应。

### 数据管线 · Data pipeline

源页面逐字存入 `android/data/sources/`，构建链路全自动：

```
data/sources/*.html --extract_*--> data/extracted/*.json --build_asset--> app/src/main/assets/*
```

`android/data/extracted/provenance.json` 记录每份源页面的 SHA256 与抽取条数，`android/tools/verify_qiershi.js` 用独立实现交叉校验抽取结果。

---

## 🎨 设计 · Design

视觉设计灵感来自：

**广州西关大屋 · 岭南建筑 · 民国广州 · 旧时市井生活**

Inspired by Guangzhou Xiguan Mansions, Lingnan architecture, Republican-era Guangzhou, and nostalgic neighborhood life.

希望让剧集查询不仅是一个数据库，也成为重新找回岭南市井记忆的入口。

The goal is not just to build an episode database, but to recreate the nostalgic atmosphere of old Guangzhou.

---

## 📚 数据来源 · Sources

主要整理自：

- 百度百科 · Baidu Baike
- 哔哩哔哩用户 hahahhhhahha 发布的相关专栏 · Bilibili user hahahhhhahha's related articles

数据经过整理和结构化，可能存在遗漏或错误，欢迎提交修正。

Data has been organized and structured from publicly available sources. Errors or omissions may exist; corrections are welcome.

---

## 🤖 AI 辅助创作 · AI-Assisted

本项目网页由 AI 辅助创作，作者负责创意、产品设计、内容整理、筛选及最终定稿。

The project was created with AI assistance, while the author handled the concept, product design, content curation, review, and final decisions.

---

## ⚠️ 版权声明 · Disclaimer

非官方项目，仅供剧集查询、个人学习与爱好者交流。

This is an unofficial fan-made project for episode search, personal learning, and community use.

《七十二家房客》及相关影视作品的版权归相应权利方所有。本项目不提供原剧影视资源，也不主张拥有相关知识产权。

All rights to 72 Households and related media belong to their respective rights holders. This project does not provide or claim ownership of the original media.

如有版权或内容问题，请联系项目维护者。

For copyright or content concerns, please contact the project maintainer.

---

## ❤️ 项目初衷 · Why

> “我记得有一集……”

> 从一个角色、一段剧情、一句台词，重新找到那个熟悉的故事。

> “I remember an episode…”

> Start with a character, a plot, or a line — and rediscover the story you remember.

---

## ⭐ 支持项目 · Support

如果项目对你有帮助，欢迎：

⭐ Star · 🐛 Issue · 💡 Suggestions · 🔧 Data Corrections · 📢 Share

---

## 🔖 Tags

"七十二家房客" "72-households" "episode-search" "粤语情景喜剧"  
"广东电视剧" "广州" "岭南文化" "西关" "Episode Search"