package com.cyz.note.model;

import lombok.Data;

/**
 * 笔记检索索引行，对应 note_index 表。
 * <p>
 * md 文件仍是唯一真相源，本对象仅用于检索加速，由 NoteDataService 在
 * saveNote / deleteNote / migrateOld / rebuildIndex 时同步维护。
 * <p>
 * 🔒 contentIndexed 语义（产品决策 Q1）：
 * 1 = content 已入库（仅属主自己的笔记）；0 = 仅元数据（他人 public 笔记，正文不入库）。
 */
@Data
public class NoteIndex {

    private Long id;
    /** 笔记属主用户 id */
    private Long ownerId;
    /** 净化后的标题，与文件名一致 */
    private String noteName;
    /** frontmatter.name 原始标题 */
    private String title;
    private String category;
    /** 标签，逗号分隔 */
    private String tags;
    /** 来源条目 key（javaapi/x、shell/x、dubbo/x），空=非摘录 */
    private String sourceKey;
    /** 1=公开 0=私有 */
    private Integer isPublic;
    /** 1=content 已入库 0=仅元数据（Q1 决策） */
    private Integer contentIndexed;
    /** 正文全文，仅 contentIndexed=1 时有值 */
    private String content;
    private String preview;
    private String ownerName;
    private String updateTime;
}
