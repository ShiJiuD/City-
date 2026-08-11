package com.cityart.constant;

/**
 * 登录、注册业务提示文案常量
 */
public class AuthMessageConstant {

    /**
     * 手机号不存在
     */
    public static final String PHONE_NOT_EXIST = "手机号不存在";

    /**
     * 密码错误
     */
    public static final String PASSWORD_ERROR = "密码错误";

    /**
     * 该手机号已注册
     */
    public static final String PHONE_ALREADY_REGISTER = "该手机号已注册";

    /**
     * 该手机号未注册
     */
    public static final String PHONE_NOT_REGISTER = "该手机号未注册";

    /**
     * 手机号不能为空
     */
    public static final String PHONE_EMPTY = "手机号不能为空";

    /**
     * 密码不能为空
     */
    public static final String PASSWORD_EMPTY = "密码不能为空";

    /**
     * 登录成功
     */
    public static final String LOGIN_SUCCESS = "登录成功";

    /**
     * 注册成功
     */
    public static final String REGISTER_SUCCESS = "注册成功";

    /**
     * 账号或密码错误（登录统一错误提示）
     */
    public static final String ACCOUNT_OR_PASSWORD_ERROR = "账号或密码错误";

    /**
     * 账号已被禁用
     */
    public static final String ACCOUNT_DISABLED = "账号已被禁用，请联系管理员";

    /**
     * 验证码已发送
     */
    public static final String CODE_SENT = "验证码已发送";

    /**
     * 验证码错误
     */
    public static final String CODE_ERROR = "验证码错误";

    /**
     * 当前账号不存在
     */
    public static final String ACCOUNT_NOT_EXIST = "当前账号不存在";

    /**
     * 校验通过
     */
    public static final String VERIFY_SUCCESS = "校验通过";

    /**
     * 密码修改成功
     */
    public static final String PASSWORD_RESET_SUCCESS = "密码修改成功";

    /**
     * 重置密码前必须先通过验证码校验（凭证不存在或已过期）
     */
    public static final String VERIFY_CODE_REQUIRED = "请先完成验证码校验";

    /**
     * 昵称不能为空
     */
    public static final String NICKNAME_EMPTY = "昵称不能为空";

    /**
     * 姓名不能为空
     */
    public static final String NAME_EMPTY = "姓名不能为空";

    /**
     * 角色参数错误
     */
    public static final String ROLE_INVALID = "角色参数错误";

    /**
     * 修改成功
     */
    public static final String UPDATE_PROFILE_SUCCESS = "修改成功";

    // ==================== 订单模块 ====================

    /** 无权查看该订单 */
    public static final String ORDER_NO_PERMISSION = "无权查看该订单";
    /** 无权操作该订单 */
    public static final String ORDER_OP_NO_PERMISSION = "无权操作该订单";
    /** 订单不存在 */
    public static final String ORDER_NOT_EXIST = "订单不存在";
    /** 当前状态不可取消 */
    public static final String ORDER_CANNOT_CANCEL = "当前状态不可取消";
    /** 仅已支付订单可申请退款 */
    public static final String ORDER_REFUND_ONLY_PAID = "仅已支付订单可申请退款";
    /** 展览库存不足 */
    public static final String ORDER_STOCK_INSUFFICIENT = "展览库存不足，剩余票数已不够";
    /** 展览不存在或未配置库存 */
    public static final String ORDER_EXHIBITION_NO_STOCK = "展览不存在或未配置库存";
    /** 展览不存在或未设置票价 */
    public static final String ORDER_EXHIBITION_NO_PRICE = "该展览暂未设置票价";
    /** 观展日期格式不正确 */
    public static final String ORDER_VISIT_DATE_INVALID = "观展日期格式不正确，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss";
    /** 不支持的票种 */
    public static final String ORDER_TICKET_TYPE_UNSUPPORTED = "不支持的票种";
    /** 下单成功 */
    public static final String ORDER_CREATE_SUCCESS = "下单成功";
    /** 取消成功 */
    public static final String ORDER_CANCEL_SUCCESS = "取消成功";
    /** 退款申请已提交 */
    public static final String ORDER_REFUND_SUCCESS = "退款申请已提交";

    // ==================== 收藏模块 ====================

    /** 收藏成功 */
    public static final String FAVORITE_ADD_SUCCESS = "收藏成功";
    /** 取消收藏成功 */
    public static final String FAVORITE_CANCEL_SUCCESS = "取消收藏成功";
    /** 已收藏（重复收藏幂等提示，不报错） */
    public static final String FAVORITE_ALREADY = "已收藏";
    /** 未收藏（重复取消幂等提示，不报错） */
    public static final String FAVORITE_NOT_EXIST = "未收藏";
    /** 收藏目标不存在（展览/美术馆记录不存在） */
    public static final String FAVORITE_TARGET_NOT_EXIST = "收藏目标不存在";
    /** 收藏类型不合法（仅允许 1-展览 2-美术馆） */
    public static final String FAVORITE_TYPE_INVALID = "收藏类型不合法";

    // ==================== 详情模块 ====================

    /** 展馆不存在 */
    public static final String GALLERY_NOT_EXIST = "展馆不存在";
    /** 展览不存在 */
    public static final String EXHIBITION_NOT_EXIST = "展览不存在";

}