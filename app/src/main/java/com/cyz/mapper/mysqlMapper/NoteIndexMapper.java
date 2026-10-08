package com.cyz.mapper.mysqlMapper;

import com.cyz.note.model.NoteIndex;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 笔记检索索引 mapper（note_index 表）。
 * <p>
 * 🔒 所有查询都必须在 SQL 层强制可见性条件，
 * 调用方（NoteDataService）传 currentUserId，禁止拼接用户输入到 SQL。
 */
public interface NoteIndexMapper {

    /** 按 (owner_id, note_name) 唯一键 upsert 一行索引 */
    int upsert(NoteIndex row);

    /** 删除某用户某篇笔记的索引（改名/删除笔记时调用） */
    int deleteByOwnerAndName(@Param("ownerId") Long ownerId, @Param("noteName") String noteName);

    /** 清空索引（rebuildIndex 前置） */
    int truncateAll();

    /**
     * 全库检索。
     * <p>
     * 可见性强制：{@code (is_public = 1 OR owner_id = #{currentUserId})} —— 私有笔记对他人永不可见。
     * 正文命中强制：仅 {@code content_indexed = 1} 的行参与正文 LIKE（Q1 决策：
     * 他人 public 笔记正文不入库，因此只能按标题命中）。
     *
     * @param currentUserId 当前登录用户 id
     * @param keyword       关键词（大小写不敏感子串匹配）；为空时只按可见性列出全部
     * @param scope         mine=仅自己的笔记；public=全部可见（含他人 public）；空=全部可见
     * @param field         all=标题+正文；title=仅标题；content=仅正文
     * @param limit         返回条数上限
     */
    List<NoteIndex> search(@Param("currentUserId") Long currentUserId,
                           @Param("keyword") String keyword,
                           @Param("scope") String scope,
                           @Param("field") String field,
                           @Param("limit") Integer limit);

    /** 反查引用某来源条目的【公开】笔记（资料页底部「关于此条目的笔记」用） */
    List<NoteIndex> listPublicBySource(@Param("sourceKey") String sourceKey,
                                       @Param("limit") Integer limit);

    /** 某用户用过的全部分类（含次数），供分类联想 */
    List<Map<String, Object>> listCategories(@Param("ownerId") Long ownerId);

    /** 某用户用过的全部标签（含次数），供标签筛选与管理 */
    List<Map<String, Object>> listTags(@Param("ownerId") Long ownerId);

    /**
     * 🔒 检索越权自检（Q1 验收）：以 currentUserId 身份实际执行一次「仅正文」检索，
     * 返回结果条数。正常必须为 0 —— 若大于 0，说明 search 的正文匹配条件漏了 owner_id 限定，
     * 他人能搜到我的 public 笔记正文，属严重越权。
     */
    int countContentSearchHits(@Param("currentUserId") Long currentUserId,
                                @Param("keyword") String keyword);

    /**
     * 公开笔记中带正文的行数 —— 数据暴露面（信息性指标，非红线）。
     * public 笔记正文必须入库，否则属主本人无法全文检索自己的笔记。
     */
    int countPublicWithContent();

    /** 索引总行数（rebuildIndex 进度/日志用） */
    int countAll();

    /** 列出全部用户目录 id（rebuildIndex 遍历用） */
    List<Long> listOwnerIds();
}
