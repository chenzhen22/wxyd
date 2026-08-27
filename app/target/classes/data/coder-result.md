---
name: CoderResult
package: java.nio.charset
order: 374
---

## 介绍

`java.nio.charset.CoderResult` 是**编解码结果类**，表示编码器或解码器的操作结果。

## 常量

- `UNDERFLOW` — 输入缓冲区数据不足
- `OVERFLOW` — 输出缓冲区空间不足

## 方法

### isUnderflow / isOverflow / isError / isMalformed / isUnmappable

### length / malformedForLength / unmappableForLength

## 测试

- 描述: CoderResult 常量
- 断言: 常量正确

```java
// 方法体开始
System.out.println("=== CoderResult ===");
assertTrue(CoderResult.UNDERFLOW.isUnderflow());
assertFalse(CoderResult.UNDERFLOW.isError());
assertTrue(CoderResult.OVERFLOW.isOverflow());
assertFalse(CoderResult.OVERFLOW.isError());
System.out.println("UNDERFLOW: " + CoderResult.UNDERFLOW);
System.out.println("OVERFLOW: " + CoderResult.OVERFLOW);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
