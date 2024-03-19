# common

✨ 星社 Project 服务器的通用组件。

## 📦 模块说明

所有模块在本服务器的开发术语内都被称作为 “组件”。

标有 * 的组件可以在 Bukkit 或 Velocity 平台作为插件加载。

- `common-dependency-loader*`：在 Spigot 平台上通过自带的 Maven Dependency 功能下载外部依赖。
- `common-dependency-loader-velocity*`：由于 Velocity 平台没有类似的功能，因此单独区分一个组件通过 `shadowJar` 加载所有的依赖库。
- `common-library-member-api`：成员管理系统 API。
- `common-library-misc-api`：杂项功能 API。
- `common-library-server-api`：跨端操作框架。
- `common-library-utils`：工具类集合。
- `common-member*`：成员系统逻辑实现。
- `common-misc*`：杂项功能逻辑实现。
- `common-utils*`：用于初始化工具类中部分涉及到服务端平台的内容。

## 🔧 构建

> [!NOTE]
>
> 此处以 Linux 系统上的步骤举例。
>
> 如果您使用的是 Windows，可能需要修改部分命令。
>

1. 将本项目拉取到你的设备：`git clone https://github.com/PlutoProject/common.git`
2. 进入项目目录：`cd ./common`
3. 打包构建：`./gradlew shadowJar`

## 👨‍💻 贡献

目前我们还没有制定明确的贡献指南。

如果你是社区中的一位玩家，你可以直接提交 Pull Request，前提是你认为你的修改是有意义且正确的。

## 📄️ 许可

PlutoProject/common 在 [GNU Lesser General Public License v3.0](https://www.gnu.org/licenses/lgpl-3.0.html) 下许可。

![license](lgpl-v3.png)