package com.cityart.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * C端普通观展用户表
 * </p>
 *
 * @author 
 * @since 2026-07-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("buyer_user")
public class BuyerUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号（登录账号），11位数字
     */
    @TableField("phone")
    private String phone;

    /**
     * BCrypt 加密后的密码密文
     */
    @TableField("password")
    private String password;

    /**
     * 用户昵称，注册时默认取手机号脱敏（如138****1234）
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 账号状态：0-正常  1-禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 注册时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 最近更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;


}
