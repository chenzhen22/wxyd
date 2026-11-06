package com.cyz.bohe.mapper;

import com.cyz.bohe.model.BoheBill;
import com.cyz.bohe.model.BoheFriend;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 薄荷账 Mapper。friend 关系为有向记录，findFriend/listFriendPair 双向匹配；updateFriendStatus 只按精确方向更新。
 */
public interface BoheMapper {

    List<BoheBill> listBills(@Param("userId") long userId, @Param("month") String month, @Param("period") String period);

    BoheBill findByCloudId(@Param("cloudId") String cloudId);

    int insertBill(BoheBill bill);

    /** 编辑账单（仅属主）：更新金额/分类/备注/日期。 */
    int updateBillById(BoheBill bill);

    int updateBillByCloudId(BoheBill bill);

    int deleteBill(@Param("id") long id, @Param("userId") long userId);

    int clearCloudId(@Param("id") long id, @Param("userId") long userId);

    List<Map<String, Object>> sumByCategory(@Param("userId") long userId, @Param("month") String month, @Param("period") String period);

    Long sumMonth(@Param("userId") long userId, @Param("month") String month, @Param("period") String period);

    BoheFriend findFriend(@Param("a") long a, @Param("b") long b);

    List<BoheFriend> listFriendsByUser(@Param("userId") long userId, @Param("status") Integer status);

    int insertFriend(BoheFriend f);

    int updateFriendStatus(@Param("userId") long userId, @Param("friendId") long friendId, @Param("status") int status);

    List<Map<String, Object>> searchUsers(@Param("keyword") String keyword);

    Long findIdByUsername(@Param("username") String username);

    /** 查 users.bohe_hidden（0=可见 1=隐藏）；列来自 V30 迁移。 */
    Integer boheHidden(@Param("userId") long userId);

    int setBoheHidden(@Param("userId") long userId, @Param("hidden") int hidden);

    /** 好友粒度隐藏：userId（发起方）对 friendId 隐藏自己的薄荷账（V32 hide_my_bills）。 */
    Integer hideMyBills(@Param("userId") long userId, @Param("friendId") long friendId);

    int setHideMyBills(@Param("userId") long userId, @Param("friendId") long friendId, @Param("hidden") int hidden);
}
