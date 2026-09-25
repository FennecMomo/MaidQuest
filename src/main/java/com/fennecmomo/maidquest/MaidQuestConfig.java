package com.fennecmomo.maidquest;

// 模块可调常量。后期接入玩家配置文件时从此类读取。
public final class MaidQuestConfig
{
    // ======== 行为权重 ========

    public static final int CLAIM_WEIGHT = 30;
    public static final int EXECUTE_WEIGHT = 60;
    public static final int SUBMIT_WEIGHT = 90;
    // 女仆当前工作类型与委托类型匹配时的加权
    public static final int WORK_TYPE_MATCH_BONUS = 100;
    // 注入女仆 WORK 活动的行为优先级
    public static final int QUEST_BEHAVIOR_PRIORITY = 5;
    // 行为最长持续 tick（超时自动结束，交给 Picker 重选）
    public static final int EXECUTOR_MAX_DURATION = 20 * 60 * 10;

    // ======== 搜索 ========

    // 女仆寻找任务板的半径（格）
    public static final int BOARD_SEARCH_RADIUS = 32;
    // 搜索失败后的重试间隔（tick）
    public static final int SEARCH_RETRY_TICKS = 100;
    // 到达判定距离平方
    public static final double ARRIVE_DIST_SQ = 4.0;

    // ======== 伐木 ========

    public static final int TREE_SEARCH_HALF_XZ = 15;
    public static final int TREE_SEARCH_Y_DOWN = 1;
    public static final int TREE_SEARCH_Y_UP = 14;
    public static final int CHOP_REACH = 3;
    public static final int CHOP_INTERVAL_TICKS = 20;

    private MaidQuestConfig()
    {
    }
}
