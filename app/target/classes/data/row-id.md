---
name: RowId
package: java.sql
order: 333
---

## 介绍

`java.sql.RowId` 是 **SQL ROWID 类型**的 Java 表示，用于表示数据库表中的行标识。

## 方法

### getBytes / toString

## 测试

- 描述: 创建 RowId 模拟
- 断言: 测试通过

```java
// 方法体开始
System.out.println("=== RowId ===");
// RowId 通常由数据库生成，这里仅演示接口
RowId rowId = new RowId() {
    public byte[] getBytes() { return new byte[]{1, 2, 3}; }
    public String toString() { return "000102"; }
};
assertArrayEquals(new byte[]{1, 2, 3}, rowId.getBytes());
System.out.println("RowId: " + rowId);
System.out.println("=== 测试通过 ===");
// 方法体结束
```
