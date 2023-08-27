package com.osrsprofile.tracker;

public enum AccountType {
    /**
     * Normal account type.
     */
    NORMAL,
    /**
     * Ironman account type.
     */
    IRONMAN,
    /**
     * Ultimate ironman account type.
     */
    ULTIMATE_IRONMAN,
    /**
     * Hardcore ironman account type.
     */
    HARDCORE_IRONMAN,
    /**
     * Group ironman account type
     */
    GROUP_IRONMAN,
    /**
     * Hardcore group ironman account type
     */
    HARDCORE_GROUP_IRONMAN,
    /**
     * Unranked group ironman account type
     */
    UNRANKED_GROUP_IRONMAN;

    public static AccountType getType(int value) {
        switch(value) {
            case 1: return IRONMAN;
            case 2: return ULTIMATE_IRONMAN;
            case 3: return HARDCORE_IRONMAN;
            case 4: return GROUP_IRONMAN;
            case 5: return HARDCORE_GROUP_IRONMAN;
            case 6: return UNRANKED_GROUP_IRONMAN;
            default: return NORMAL;
        }
    }
}