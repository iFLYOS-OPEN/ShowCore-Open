### 带屏设备开源项目 ShowCore Open
==========

## 带屏设备开源项目是什么

本项目是使用 iFLYOS 的 [EVS协议](https://doc.iflyos.cn/device/evs/) 开发的、运行在 Android 平台的通用语音交互 Launcher App。项目的根本目的是为了减少开发者集成到自有硬件中的开发成本，可以快速开发一款使用 iFLYOS 的智能音箱。

==========

## 运行项目

### 依赖项

* 要求 cmake 3.6 或更高的版本。（用于编译唤醒的 JNI 模块）
* [Android SDK tools](https://developer.android.com/studio/#comand-tools)。（Android Studio 中已内置）

### 集成开发环境 

项目开发使用的是 Android Studio 3.5 正式版。

### EVS SDK

项目中引入了 [SDK-EVS-Android](https://github.com/iFLYOS-OPEN/SDK-EVS-Android) 作为 `evs_sdk` 模块，其中代码更新将与 [SDK-EVS-Android](https://github.com/iFLYOS-OPEN/SDK-EVS-Android) 同步。

## App 提供能力概述

整个项目的前提是，为智能音箱开发语音交互应用。在此前提下，App 声明作为 Launcher，期望作为设备开机后的第一入口，比较符合智能音箱的交互。

初次打开应用时，App 提供了简易的网络配置引导和授权绑定引导。

授权完成后，App 提供了一个会提供部分交互推荐的桌面、音频播放界面、视频播放界面、闹钟界面、包含部分简单配置项的设置界面。在主界面中，提供会在桌面轮播的推荐项，左上角可以进入提供有声内容和视频内容推荐的「内容发现」和语音交互使用概览的「技能中心」。

## App 依赖权限概述

为了实现智能音箱的大部分交互，App 依赖若干权限，期望效果中 ROM 在集成应用后应当能在开机后授予应用所有的权限。以下为各个权限及其相关用途说明，如果你开发过程中并不需要，亦可以酌情移除部分权限。

> 以下罗列的主要是我们认为较为重要的需要介绍的权限，针对一些特殊设备的适配时可能有额外的工作要做。例如，VIVO 一些机型上调节音量会导致自动开关勿扰模式，因此你会发现项目中也声明了 `android.permission.ACCESS_NOTIFICATION_POLICY` 权限来解决这种特殊情况。

### 录音权限（必须）

用于发送语音请求和语音唤醒模块

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### 网络连接权限（必须）

用于各种网络请求

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 网络状态获取与修改权限

用于在引导页面、设置页面中的 WLAN 引导。Manifest 包含以下内容。

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<!-- 在更高 Android API 版本中还需要以下权限才可以搜索 WLAN -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### 获取当前前台应用状态

服务端可以根据当前前台应用，将语义分配给不同的技能做处理，提供更人性化的交互体验。

```xml
<!-- Android 5.1 及以下 -->
<uses-permission android:name="android.permission.GET_TASKS" />
<!-- Android 6.0 以上 -->
<uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />
```

### 悬浮窗

App 通过悬浮窗接口实现了全局可显示的 **语音唤醒按钮**、**语音识别界面**、**左边缘右滑返回手势**、**上边缘下拉控制栏**、**静态模板渲染界面**。

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

> 请注意，返回手势默认只在 App 内才可以有效，全局返回手势需要将 App 作为系统应用或通过 adb 手动授予 `android.permission.INJECT_EVENTS` 权限。

### 修改系统设置

通过这些权限，App 可以做到识别你调节音量、亮度等系统设置相关的语音请求，并对 Android 设备做出相应的更改。

```xml
<uses-permission
        android:name="android.permission.WRITE_SETTINGS"
        tools:ignore="ProtectedPermissions" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### 请求安装应用

项目中包含了一个通用的 OTA 模块，可以通过 iFLYOS 的 [自动更新API](https://doc.iflyos.cn/device/ota.html) 更新 App。下载上传到设备控制台的 APK 后，应用提示安装。

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## 其他

为了拥有更好的体验，我们推荐使用 adb 对设备执行以下命令进入全屏模式。

```
adb shell settings put secure user_setup_complete 0
adb shell settings put global policy_control "immersive.full=*"
```

如果你可以授予设备 `android.permission.WRITE_SECURE_SETTINGS` 权限的话，也可以通过 Android 的 Settings API 执行上述授权。

```kotlin
Settings.Secure.putInt(contentResolver, "user_setup_complete", 0)
Settings.Global.putString(contentResolver, "policy_control", "immersive.full=*")
```

恢复原有状态则执行以下命令

```
adb shell settings put secure user_setup_complete 1
adb shell settings put global policy_control "immersive.full="
```

## License

[Apache License, Version 2.0](LICENSE)
