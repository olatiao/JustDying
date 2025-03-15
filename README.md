# JustDying - Minecraft 属性系统模组

![模组图标](src/main/resources/assets/justdying/icon.png)

这是一个完全由 AI 工具辅助设计和开发的 Minecraft 模组！它为游戏添加了一个全新的属性系统，让玩家可以通过升级和分配属性点来增强自己的能力。

## ✨ 功能特性

### 🎮 属性系统
- **五大基础属性**
  - 体质：提升最大生命值
  - 力量：增加攻击伤害
  - 防御：提升伤害抗性
  - 速度：提升移动速度
  - 幸运：增加幸运效果

### 🎯 属性点系统
- 玩家可以通过升级获得属性点
- 自由分配属性点到不同属性上
- 支持等级兑换属性点功能
- 可配置的属性上限系统

### 💫 词缀系统
- 为物品添加随机词缀
- 多种词缀效果
- 可配置的词缀生成规则
- 支持自定义词缀

### ⚙️ 高度可配置
- 完整的配置系统
- 可调整所有属性的最大/最小值
- 可配置属性点获取规则
- 支持禁用/启用特定功能

## 📦 安装说明

### 要求
- Minecraft 1.20.1
- Fabric Loader
- Fabric API

### 安装步骤
1. 下载并安装 [Fabric Loader](https://fabricmc.net/use/)
2. 下载 [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. 下载本模组
4. 将模组文件放入游戏的 `mods` 文件夹
5. 启动游戏

## 🎮 使用指南

### 基础命令
- `/attribute points` - 查看可用属性点
- `/attribute get <属性>` - 查看指定属性的值
- `/attribute set <属性> <值>` - 设置属性值（创造模式）
- `/attribute reset` - 重置所有属性

### 快捷键
- `K`（默认）- 打开属性面板

### 属性界面
- 显示所有属性的当前值和上限
- 使用 `+/-` 按钮分配属性点
- 支持等级兑换属性点
- 显示详细的属性效果说明

## 🛠️ 配置说明

配置文件位于：`.minecraft/config/justdying.json`

主要配置项：
```json
{
  "attributes": {
    "enableSystem": true,
    "showDecreaseButtons": true,
    "defaultPoints": 0
  },
  "levelExchange": {
    "enableLevelExchange": true,
    "baseLevel": 5,
    "levelIncrement": 5
  }
}
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建您的特性分支：`git checkout -b feature/AmazingFeature`
3. 提交您的更改：`git commit -m '添加一些特性'`
4. 推送到分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

## 📝 开发计划

- [ ] 添加更多属性类型
- [ ] 实现属性技能系统
- [ ] 添加属性装备系统
- [ ] 优化词缀生成算法
- [ ] 添加更多配置选项
- [ ] 实现属性效果可视化
- [ ] 添加成就系统

## 📜 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 鸣谢

- 感谢 [Fabric](https://fabricmc.net/) 提供的模组加载器
- 感谢所有为项目提供反馈和建议的玩家
- 特别感谢 Claude 和 Cursor 在开发过程中提供的 AI 辅助

## 📞 联系方式

- GitHub Issues: [提交问题](https://github.com/olatiao/JustDying/issues)
- Email: a@rmb.sh

---

<div align="center">

**用❤️制作**

[报告Bug](https://github.com/olatiao/JustDying/issues) · [请求功能](https://github.com/olatiao/JustDying/issues) · [Fork](https://github.com/olatiao/JustDying/fork)

</div>

## 开发分支

这是开发分支(dev)，用于开发新功能和修复bug。

## 功能

- 属性系统
- 属性点分配
- 等级兑换
- 自定义物品 