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

    List<BoheBill> listBills(@Param("userId") long userId, @Param("month") String month);

    BoheBill findByCloudId(@Param("cloudId") String cloudId);

    int insertBill(BoheBill bill);

    int updateBillByCloudId(BoheBill bill);

    int deleteBill(@Param("id") long id, @Param("userId") long userId);

    int clearCloudId(@Param("id") long id, @Param("userId") long userId);

    List<Map<String, Object>> sumByCategory(@Param("userId") long userId, @Param("month") String month);

    Long sumMonth(@Param("userId") long userId, @Param("month") String month);

    BoheFriend findFriend(@Param("a") long a, @Param("b") long b);

    List<BoheFriend> listFriendsByUser(@Param("userId") long userId, @Param("status") Integer status);

    int insertFriend(BoheFriend f);

    int updateFriendStatus(@Param("userId") long userId, @Param("friendId") long friendId, @Param("status") int status);

    List<Map<String, Object>> searchUsers(@Param("keyword") String keyword);

    Long findIdByUsername(@Param("username") String username);
}
