---
name: Process
package: java.lang
order: 278
---

## 介绍

`java.lang.Process` 代表**操作系统进程**，由 `ProcessBuilder.start()` 或 `Runtime.exec()` 启动。Java 8 中新增了 `getPid()` 等管理方法。

## 方法

### getInputStream / getOutputStream / getErrorStream

获取子进程的 IO 流。

### waitFor

```java
public int waitFor() throws InterruptedException
```

等待子进程结束。

### exitValue / destroy / destroyForcibly

获取退出值/终止进程。

### getPid

```java
public long getPid()
```

获取进程 ID。

## 测试

- 描述: 启动并等待系统进程
- 断言: 进程正常结束

```java
// 方法体开始
System.out.println("=== Process ===");
ProcessBuilder pb = new ProcessBuilder(
        System.getProperty("java.home") + "/bin/java", "-version");
pb.redirectErrorStream(true);
Process p = pb.start();
int exitCode = p.waitFor();
assertEquals(0, exitCode);
System.out.println("进程退出码: " + exitCode);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
