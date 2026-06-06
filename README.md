# 商海浮沉 — 中文商业模拟放置游戏

一款基于 WebView 的中文商业模拟放置游戏，支持 Android APK 独立运行。

## 功能特色

- 🏢 **创业经营**：从4种出身开始，经营零售/科技/餐饮等业务
- 📈 **股票投资**：8支A股风格股票，支持买卖、持仓市值、盈亏统计
- 🔬 **科技研发**：数字化/AI/区块链三条研发路线
- 💎 **资产中心**：资产市场、拍卖行、典当行
- 🗺 **世界地图**：10座城市（国内5+国际5），跨城协同加成
- 🤝 **NPC社交**：10+NPC，好感度系统，送礼/谈判/并购
- 🤖 **AI叙事**：接入OpenAI兼容API，动态事件叙事/商业新闻/竞争情报
- 🏆 **成就系统**：里程碑/排行榜/技能树
- 📱 **液态玻璃UI**：iOS风格底部导航、滚动隐藏、backdrop-filter毛玻璃效果

## 移动端特性

- 顶部栏紧凑化 (42px)
- 底部液态玻璃导航栏 (7个快捷入口)
- 下滚隐藏导航、上滑浮现
- 仪表盘始终显示股票/贷款摘要卡片
- 所有面板完整可访问

## 快速开始

### 浏览器运行
```bash
cd app/src/main/assets/game
python3 -m http.server 8765
# 打开 http://localhost:8765
```

### 安装APK
从 [Releases](../../releases) 下载最新 APK 安装到 Android 设备。

## 技术架构

- 前端：纯HTML/CSS/JS，单页应用
- 后端：Android WebView + Java Bridge (GameNative)
- LLM：OpenAI兼容API（可选）
- 存储：SharedPreferences（APK）/ localStorage（浏览器）

## 文件结构

```
app/src/main/
├── assets/game/          # 游戏前端代码
│   ├── index.html        # 主页面 + 液态玻璃CSS
│   ├── config.js         # 配置常量（股票/城市/业务/技能树）
│   ├── data.js           # 游戏数据
│   ├── core.js           # 核心游戏逻辑
│   ├── core-stock.js     # 股票投资系统
│   ├── core-research.js  # 科技研发系统
│   ├── core-rival.js     # 竞争对手系统
│   ├── ui.js             # 界面渲染
│   ├── events.js         # 事件系统
│   ├── npc.js            # NPC交互
│   ├── llm.js            # LLM集成
│   ├── settings.js       # 设置面板
│   ├── storage.js        # 存储抽象层
│   ├── main.js           # 入口初始化
│   └── audio.js          # 音效
├── java/.../MainActivity.java  # Android WebView宿主
└── AndroidManifest.xml
```

## 构建APK

APK基于WebView，主要步骤：
1. 编译 `MainActivity.java` → `classes.dex`
2. 打包assets + manifest → unsigned APK
3. zipalign + apksigner签名

详细构建脚本见 `dist21/` 目录。

## 版本历史

- **v21** (2026-06-07): 液态玻璃UI改版，股票初始化修复
- **v19** (2026-06-07): API修复版
- **v18** (2026-06-06): 移动端适配初版

## License

MIT
