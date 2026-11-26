# WebWrap — 网址转 App

把一个网址包成安卓 App，装到手机上点开就是全屏网页，看起来跟原生软件一样。

## 功能

- 启动即打开预设网址（默认 `https://www.pix.ai/en/`）
- 全屏无地址栏
- 返回键回退网页历史，退到底才退出
- **长按屏幕** → 弹出菜单 → 可改起始网址、刷新
- 支持 JS、Cookie、文件上传、相机/麦克风授权
- 支持 http 明文站点
- 网址保存在本地，改完即时生效

## 怎么编译出 APK

不需要在自己电脑装 Android SDK，用 GitHub Actions 云端编译：

### 1. 建仓库

登录 GitHub → 右上 `+` → **New repository** → 名字随意（比如 `webwrap`）→ 选 **Public** 或 Private 都行 → **Create repository**。

### 2. 上传这些文件

把本目录所有文件传上去。最简单的办法是网页端：

- 在新仓库页面点 **uploading an existing file**
- 把整个文件夹拖进去（注意 `.github` 目录也要传，那是编译脚本）
- 点 **Commit changes**

### 3. 触发编译

- 点仓库顶部 **Actions** 标签页
- 左侧选 **Build APK**
- 右侧点 **Run workflow** → 绿色按钮
- 等 3~5 分钟，出现绿勾 ✅

### 4. 下载 APK

- 点进那次运行记录
- 页面底部 **Artifacts** 区域
- 下载 `WebWrap-APK.zip`
- 解压得到 `app-release.apk`

### 5. 装到手机

- 把 APK 传到手机
- 点开 → 允许「安装未知应用」→ 安装

> 安装包是自签名的，如果之前装过别的版本，先卸载再装。

## 改网址不用重新编译

装上后，**长按页面任意处** → 点「设置起始网址」→ 输入新网址 → 保存，立即生效。

## 改默认网址 / 改 App 名字

- 默认网址：`app/src/main/java/com/subara/webwrap/MainActivity.java` 里的 `DEFAULT_URL`
- App 名称：`app/src/main/res/values/strings.xml` 里的 `app_name`
- 包名/图标：`app/build.gradle` 的 `applicationId`、`res/mipmap-*/ic_launcher.png`

改完 push 一下，Actions 会自动重新编译。

## 签名说明

- 密钥在编译时自动生成，密码 `webwrap123`（仅示例用途）
- 每次编译都是一个新密钥，所以**新版本要先卸载旧的再装**
- 想固定密钥：把 `keystore.jks` 提交进仓库，删掉 workflow 里的 Generate keystore 步骤
