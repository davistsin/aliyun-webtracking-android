# aliyun-webtracking-android

## 安装

Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency

```
implementation 'com.github.davistsin:aliyun-webtracking-android:1.0.0'
```


## 使用

### 初始化

```java
Logger.init(this, "your project name", "cn-shenzhen", "Your logstore name",
        new LoggerConfig.Builder()
                .enablePrint(true)
                .setOfflineMode(true)
                .setMaxOfflineNum(100)
                .build()
);
```

### 调用

#### 上传Json文本

```java
JSONObject object = new JSONObject();
object.put("test", "123456789000");
Logger.put(object.toString());
```

#### 上传普通日志，已自动包含设备信息

```java
Logger.log("TAG", "上报一个错误");
```

#### 设置应用信息

```java
Logger.setAppInfo("appName", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
```