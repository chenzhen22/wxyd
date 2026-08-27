---
name: RemoteException
package: java.rmi
order: 380
---

## 介绍

`java.rmi.RemoteException` 是 **RMI 远程异常**类，当 RMI 调用失败时抛出。

## 方法

构造方法：
```java
public RemoteException()
public RemoteException(String s)
public RemoteException(String s, Throwable cause)
```

## 测试

- 描述: 创建远程异常
- 断言: 异常信息正确

```java
// 方法体开始
System.out.println("=== RemoteException ===");
RemoteException re = new RemoteException("远程调用失败");
assertEquals("远程调用失败", re.getMessage());
RemoteException withCause = new RemoteException("连接超时", new java.net.ConnectException());
assertNotNull(withCause.getCause());
System.out.println("RemoteException: " + re.getMessage());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
