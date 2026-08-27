---
name: ProcessBuilder
package: java.lang
order: 137
---

## 介绍

`java.lang.ProcessBuilder` 用于创建和启动操作系统进程。Java 8 新增了 `inheritIO()` 便捷方法和 `redirectErrorStream` 设置的流式接口。

Java 8 新增的核心方法：
- `inheritIO()` — 将子进程的标准 IO 重定向到当前进程
- `redirectInput(ProcessBuilder.Redirect)` — 重定向输入
- `redirectOutput(ProcessBuilder.Redirect)` — 重定向输出
- `redirectError(ProcessBuilder.Redirect)` — 重定向错误
- `redirectErrorStream(boolean)` — 合并错误流到输出流
- `redirectInput(File)` / `redirectOutput(File)` / `redirectError(File)` — 文件重定向便捷方法

`ProcessBuilder.Redirect` 枚举 / 类型：
- `INHERIT` — 继承当前进程的 IO
- `PIPE` — 管道连接（默认）
- `DISCARD` — 丢弃输出（Java 9）
- `to(File)` / `from(File)` — 重定向到/从文件

## 方法

### command

```java
public ProcessBuilder command(List<String> command)
public ProcessBuilder command(String... command)
```

设置要执行的命令和参数。

### inheritIO

```java
public ProcessBuilder inheritIO()
```

将子进程的标准 IO 重定向到当前进程（Java 8 新增）。

### redirectOutput / redirectError

```java
public ProcessBuilder redirectOutput(File file)
public ProcessBuilder redirectError(File file)
```

将输出/错误重定向到文件（Java 8 新增）。

### start

```java
public Process start()
```

启动进程。

## 测试

### 执行命令

- 描述: 使用 ProcessBuilder 执行系统命令
- 断言: 进程正常启动

```java
// 方法体开始
System.out.println("=== 执行命令 ===");
// 测试 Java 版本命令
ProcessBuilder pb = new ProcessBuilder(
        System.getProperty("java.home") + "/bin/java", "-version");
pb.redirectErrorStream(true);
Process p = pb.start();
String output = new java.util.Scanner(p.getInputStream()).useDelimiter("\\A").next();
int exitCode = p.waitFor();
assertTrue(output.contains("java"));
System.out.println("Java 版本输出: " + output.trim());
System.out.println("=== 测试通过 ===");
// 方法体结束
```
