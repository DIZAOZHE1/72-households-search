# 《七十二家房客》·《外来媳妇本地郎》剧集查询 · Android

[72-households-search](https://github.com/DIZAOZHE1/72-households-search) 的原生 Android 客户端。
**一个应用、三个站点、两部剧**：

| 底部导航 | 剧集 | 数据 | 主题 |
| --- | --- | --- | --- |
| 传统 | 《七十二家房客》 | 1552 个故事 / 19 季 | 岭南传统：青砖灰瓦 + 旧木框 + 满洲窗四色 |
| 现代 | 《七十二家房客》（同一份数据） | 同上 | 现代简洁：白底圆角 + 极轻投影 + 青绿强调 |
| 外来 | 《外来媳妇本地郎》 | 2573 个故事 / 14 部 | 朱红 / 描金 / 宣纸 |

三个站点各自独立的封面、配色、排版与搜索状态，切换站点不会互相污染。完全离线，不申请网络权限。

## 功能

《七十二家房客》站点（对齐网页版 v2.20 的四个模式）：

- **剧集查询** —— 关键词搜索（标题 → 梗概 → 角色名/别称 三级排序）、纯数字按集数区间定位、19 个季标签、12 个主题标签、★仅看主线 / ☆只看想看 / ✓隐藏已看、每批 60 条「显示更多」、命中关键词高亮。
- **主线剧情** —— 16 条故事线按 5 组展开，每条含若干剧情节点，节点可展开查看相关剧集卡。
- **角色查询** —— 34 位角色按 5 个阵营分类，含别称、人物志、出场数，可按季筛选相关剧集。
- **相似抽取** —— 多选角色 + 多选主题 + 多关键词（空格分隔，命中其一）+ 只抽没看过的，随机抽取 1/3/5/10 集。
- **追剧进度** —— 卡片上 ☆想看 / ✓看过 标记，顶部进度条显示「追到第几季 · 已看 x/y 集」与全剧进度，本地持久化。
- **🎲 随便看一集 / 📅 今日推荐** —— 今日推荐按日期哈希，同一天始终推荐同一集，并跳过已看。
- **🖼 分享卡片** —— 1080×1520 海报：宣纸底 + 描金格纹 + 木框满洲窗 + 牌匾 + 印章集数 + 梗概 + 瓦檐页脚，可保存到相册或系统分享。

《外来媳妇本地郎》站点保留原有的搜索 + 分部标签 + 每页 60 条分页；该数据集不含角色、标签与主线，因此不显示对应模式。

**封面折叠**：向下滚动时封面平滑折叠——装饰行与副标题淡出、标题降到紧凑字号、纹样淡出，同时保留各站自己的底部满洲窗色条与描边。

## 数据来源与可复现构建

`data/sources/` 下三份页面是数据来源，逐字保留：

| 文件 | 用途 |
| --- | --- |
| `qiershi_traditional_v2.20.html` | 《七十二家房客》，取自本仓库 `v2.20/傳統風格.html`（同修订版） |
| `qiershi_modern_v2.20.html` | 同上现代风格版，仅 CSS 不同，**数据与上面逐字节相同**，作为对拍留档 |
| `wailai_episodes.html` | 《外来媳妇本地郎》剧集查询页 |

构建链路完全自动，全新克隆只需 `assembleDebug`：

```
data/sources/*.html --extract_*--> data/extracted/*.json --build_asset--> app/src/main/assets/*
```

`data/extracted/` 也一并提交（含 `provenance.json`，记录每份源文件的 SHA256 与抽取条数），因此常规构建只跑最后一步。删掉 `data/extracted/` 与 `app/src/main/assets/` 即可验证全链路可复现——本仓库提交前正是这样验证的：重新生成的 `qiershi.json` 与在模拟器上验证过的版本 SHA256 一致。

抽取脚本复刻了源页面运行时的两处预处理，否则渲染会出错：

1. **角色 ID 兼容层** —— 剧集 `c[]` 使用旧版角色顺序，需经 `LEGACY_CHAR_TO_CURRENT` 映射到当前顺序。
2. **主线索引预计算** —— 把每个节点的 `[季, 起, 止]` 区间展开成剧集下标，并预先算好「某集属于哪些主线节点」。

`tools/verify_qiershi.js` 用独立实现交叉校验：数量与区间、角色映射是否为双射且无重复、主线节点的剧集集合是否恰好等于其区间并集、搜索排序语义（标题 → 梗概 → 角色，别称不做前缀匹配）、季合计与集数跨度。

角色的「出场数」直接来自源数据：34 位角色中有若干位在 `chars` 里有条目与人物志，但没有任何剧集的 `c[]` 引用，因此显示 0，这是源数据的缺口而非抽取错误。

## 构建

需要 JDK 17–21（Gradle 8.14.3 无法在 JDK 25 上运行脚本编译器）与 Android SDK（platform-tools、platforms;android-36、build-tools;36.0.0）。

**任意平台**（推荐，Linux / macOS / Windows 通用；数据由构建自动生成，无需预置任何东西）：

```bash
cd android
chmod +x gradlew      # 仓库未提交 gradlew 的执行位，Linux / macOS 上首次需要
./gradlew assembleDebug testDebugUnitTest
```

Windows 上还有个便利脚本，会自动探测 JDK / SDK 并把 APK 复制到仓库根的 `release-dist\`：

```powershell
.\android\tools\build.ps1 assembleDebug testDebugUnitTest
```

技术栈：Kotlin 2.2.20、Jetpack Compose（BOM 2025.09.00）、Material 3、AGP 8.13.0、Gradle 8.14.3、compileSdk 36 / minSdk 24。

CI（工作流 `Android`）会对每次改动 `android/` 的推送与 PR 执行同样的三步：校验数据可复现、跑单测、构建 APK。

### 关于 `app/build.gradle.kts` 的注意事项

这个脚本里几乎不写注释，是有原因的：Kotlin 脚本的整个正文会编译进单个方法，注释与装饰性文本会消耗方法字节码配额。曾经因为注释过多触及上限，导致脚本在 `android { }` 之后被**静默截断**——`dependencies { }` 不再执行，而 AGP 报出的却是一个误导性的
`project ':app' does not specify compileSdk` 错误。排查了很久才定位。若要加注释，请同时保持文件精简，并留意该症状。

## 工程结构

```
android/
├── app/src/main/java/com/jordan/wailaixifu/
│   ├── MainActivity.kt                # edge-to-edge 入口
│   ├── data/                          # 站点、文案、剧集/角色/主线模型、资源解析、进度存储
│   └── ui/                            # 三站点外壳、四个模式、分享卡片、三套主题
├── app/src/test/                      # 22 项单测
├── data/sources/                      # 原始页面（数据来源）
├── data/extracted/                    # 抽取结果 + provenance.json
└── tools/                             # 抽取、校验、资源生成、构建、模拟器端到端验证
```

## 测试与验证

22 项单元测试覆盖卡片区间与合集标记、进度 key、大小写不敏感匹配、搜索三级排序、别称前缀不匹配、分页页数、站点装配、文案单位、Catalog 聚合量。

`tools/verify_flows.ps1` 在 Android 16（API 36）模拟器上驱动完整流程并逐屏截图，同时把每步断言打进日志：三站点切换、主线、角色、分享卡片弹窗、集数定位（输入 `45` 断言只剩覆盖该集的「炳哥醉酒」且「停水风波」已被过滤）、深色模式、logcat 崩溃扫描。三个站点、四个模式、分享卡片与深色模式均已实测通过，全程无 `FATAL EXCEPTION`。

模拟器上曾发现一个静态检查无法察觉的真实崩溃并已修复：自写的 `ChipFlow` 换行布局被父级 `IntrinsicSize.Min` 触发固有尺寸测量，`layout()` 收到 `2147483647 x 47` 被 Compose 拒绝。现改用内置 `FlowRow`，卡片左侧竖条改为内部 `drawBehind` 绘制。

## 许可与致谢

本 Android 客户端按其上游仓库的 **CC BY-NC 4.0**（署名 — 非商业性使用）发布，仅用于剧集查询、个人学习与爱好者交流，不得用于商业用途。

- 内容、数据与视觉设计：小红书 **@Jordan**（GitHub [@DIZAOZHE1](https://github.com/DIZAOZHE1)）
- 数据整理自百度百科与哔哩哔哩用户 hahahhhhahha 的相关专栏
- 上游项目：<https://github.com/DIZAOZHE1/72-households-search>
- Android 客户端开发：[Kastorice](https://github.com/Kastorice)

《七十二家房客》《外来媳妇本地郎》及相关影视作品版权归相应权利方所有。本项目为非官方爱好者工具，不提供原剧影视资源，也不主张拥有相关知识产权。
