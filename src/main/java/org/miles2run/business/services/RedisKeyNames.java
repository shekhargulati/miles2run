package org.miles2run.business.services;

/**
 * Created by shekhargulati on 03/07/14.
 */
public interface RedisKeyNames {

    public static final String PROFILE_S_TIMELINE = "profile:%s:timeline";
    public static final String HOME_S_TIMELINE = "home:%s:timeline";
    public static final String PROFILE_S_GOAL_S_TIMELINE = "profile:%s:goal:%s:timeline";
    public static final String PROFILE_S_TIMELINE_LATEST = "profile:%s:timeline:latest";
    public static final String ACTIVITY_S = "activity:%s";
    public static final String COMMUNITY_RUN_TIMELINE = "community_run:%s:timeline";
    public static final String COMMUNITY_RUNS = "community_runs";
}
