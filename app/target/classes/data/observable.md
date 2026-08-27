---
name: Observable
package: java.util
order: 251
---

## 介绍

`java.util.Observable` 是**观察者模式**中的被观察者类（Java 9+ 中已弃用）。配合 `Observer` 接口使用，Java 8 中可用 Lambda 风格的 Consumer 替代。

## 方法

### addObserver / deleteObserver

添加/删除观察者。

### notifyObservers

```java
public void notifyObservers(Object arg)
```

通知所有观察者。

### setChanged / clearChanged

设置/清除状态变化标记。

### countObservers

观察者数量。

## 测试

- 描述: 使用 Observable 观察数据变化
- 断言: 观察者接收通知

```java
// 方法体开始
System.out.println("=== Observable ===");
Observable observable = new Observable() {
    public void setData(Object data) {
        setChanged();
        notifyObservers(data);
    }
};
List<Object> received = new ArrayList<>();
Observer observer = (o, arg) -> received.add(arg);
observable.addObserver(observer);
observable.setData("hello");
assertEquals(1, received.size());
assertEquals("hello", received.get(0));
System.out.println("观察者收到: " + received.get(0));
System.out.println("=== 测试通过 ===");
// 方法体结束
```
