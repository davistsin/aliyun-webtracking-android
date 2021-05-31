# aliyun-webtracking-android

## 安装

```
implementation 'davis.tsin:aliyun-webtracking:1.0.0'
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


```java
JSONObject object = new JSONObject();
object.put("test", "123456789000");
Logger.put(object.toString());
```

