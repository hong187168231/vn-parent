package com.indo.admin.common.constant;

/**
 * 管理平台常量
 */
public interface SystemConstants {

    Long ROOT_DEPT_ID = 0l; // 根部门ID

    Long ROOT_MENU_ID = 0l; // 根菜单ID

    String ROOT_ROLE_CODE = "ROOT";


    String BTN_PERM_ROLES_KEY = "system:btn_perm_roles:";
    String URL_PERM_ROLES_KEY = "system:url_perm_roles:";


    /**
     * 校验返回结果码
     */
    public final static String UNIQUE = "0";

    public final static String NOT_UNIQUE = "1";

    /**
     * 广告上下架 0 下架  1 上架
     */
    public static final Integer ADE_SOLD_OUT = 0;
    public static final Integer ADE_SHELVES = 1;


    /**
     * 活动上下架 0 下架  1 上架
     */
    public static final Integer ACT_SOLD_OUT = 0;
    public static final Integer ACT_SHELVES = 1;


}
