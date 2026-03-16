# 🐾 NekoProbe

<img src="icon.png" width="256" height="256" alt="NekoProbe 图标">

**赋予脚本感知力：NekoJS 的类型提示与补全之魂**

NekoProbe 是 **NekoJS** 的官方核心附属模组。它的使命只有一个：扫描 Minecraft 环境中庞大的 Java 类库，并将其转化为前端开发者熟悉的 `.d.ts` 类型声明文件，为你的脚本开发插上 IDE 智能补全的翅膀。

**前置需要 [NekoJS](https://github.com/Tki-sor/NekoJS)**

---

## ✨ 核心特性

* 🧠 **全自动化类型扫描**: 自动识别并解析 Minecraft 原版、NeoForge 以及所有已安装模组的 Java 类、方法、字段。
* ⚡ **极速生成**: 基于精心优化的反射扫描机制，在游戏启动时即可瞬间生成数万行精准的类型声明。2w 个类只需要 2.2s 左右。
* 📝 **完美的 TypeScript 契约**: 配合 VS Code，让你在编写脚本时享受实时报错、参数提醒和文档注释。
* 🔗 **无缝集成**: 专门针对 NekoJS 的 `Wrapper` 体系（如 `ItemStackWrapper`, `IngredientWrapper`）进行了深度适配。
* 📂 **标准化输出**: 声明文件统一生成在游戏根目录的 `.probe` 文件夹下，方便多模组环境管理。

---

## 🛠️ 它是如何工作的？

当你启动游戏后，NekoProbe 会介入类加载流程，并执行以下操作：

1. **扫描**: 遍历注册表、事件总线和 NekoJS 注入的全局 API。
2. **转译**: 将 Java 的泛型、多态、重载等复杂特性映射为对应的 TypeScript 声明。
3. **输出**: 在 `.minecraft/.probe` 目录下生成 `generated.d.ts` 及其关联文件。
4. **绑定**: 自动通过 NekoJS 生成的 `tsconfig.json` 引导 IDE 加载这些声明。

---

## 📂 目录结构

NekoProbe 主要操作以下路径：
```
.minecraft/
├── .probe/                # NekoProbe 核心产物目录
│   ├── client/            # 仅客户端环境可见的类型声明
│   ├── common/            # 客户端与服务端共用的通用类型声明
│   ├── server/            # 仅服务端环境可见的类型声明
│   ├── startup/           # 物品/方块注册等启动阶段专属类型
│   │    ├── globals/      # 对应环境下的全局变量 (如 ServerEvents, Ingredient)
│   │    └── packages/     # 按 Java 包路径映射的原始类声明 (如 net.minecraft)
│   └── probe_cache.md5    # 扫描缓存校验文件，用于加速启动时的增量扫描
└── nekojs/
    └── tsconfig.json      # 核心配置文件，桥接 IDE 补全与 .probe 类型库
```

---

## 💻 快速开始

1. 安装 NekoJS 和 **NekoProbe**。
2. 启动游戏进入主界面（此时 `.probe` 目录已生成）。
3. 使用 **VS Code** 打开游戏根目录下的 `nekojs` 文件夹。
4. **享受魔法**：在脚本中输入 `ServerEvents.` 或 `Ingredient.`， IDE 将瞬间弹出精准的补全提示。

---

## 🎯 补全示例

当你有了 NekoProbe，在 VS Code 中写代码会变成这样：

```typescript
// IDE 会告诉你 event 的类型是 RecipeEventJS
ServerEvents.recipes(event => {
    // 输入 event. 之后，smelting, shaped 等方法一览无余
    // 连参数类型（ItemStackWrapper, Ingredient）都会清晰显示
    event.smelting("minecraft:diamond", "minecraft:dirt");
});
```

---

## 🤝 参与贡献
NekoProbe 的扫描算法仍在持续进化中！如果你发现某些特殊的 Java 类无法被正确转译，欢迎反馈。

* **API 文档**: [即将到来]

* **QQ 群**: 1158525822 [点击加入群聊【NekoJS 魔改交流群（？】](https://qm.qq.com/q/rbryak0K6k)

---
### License

本项目采用 [LGPL-3.0 License](LICENSE) 开源。