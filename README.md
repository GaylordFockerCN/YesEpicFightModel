# Epic Fight Mesh Model Mod

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-success)

为史诗战斗模组（Epic Fight Mod）添加动态玩家模型替换功能的扩展模组，支持通过Blender导出的自定义模型实时替换玩家角色。

## ✨ 功能特性
- **动态模型替换** - 游戏内实时切换玩家骨骼模型
- **Blender原生支持** - 直接使用Blender + [史诗战斗插件](https://epicfight-docs.readthedocs.io/zh/Guides/page1/) 导出的Armature和Mesh
- **参数化配置** - 通过config.json自定义缩放比例和盔甲显示
- **兼容性** - 依赖原版史诗战斗模组系统，兼容性与史诗战斗兼容性大致相同

## 📥 安装要求
- Minecraft 1.20.1
- Forge 47.3.0+
- [Epic Fight Mod 20.9.x](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod)

## 🛠 使用指南

### 模型文件结构
可参考 [model_example/Anon Chihaya](https://github.com/GaylordFockerCN/YesEpicFightModel/tree/master/model_example/Anon%20Chihaya)
```bash
your_model_name/
├── main.json       # Blender导出的骨骼和网格数据
├── config.json     # 模型配置文件
├── texture.png     # 模型贴图文件
├── texture_n.png   # pbr nomal 模型贴图文件（可选）
└── texture_s.png   # pbr special 模型贴图文件（可选）
