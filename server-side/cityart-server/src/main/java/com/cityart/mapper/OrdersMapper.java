package com.cityart.mapper;

import com.cityart.entity.Orders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * C端用户订单主表 Mapper
 * <p>
 * 继承 MyBatis-Plus BaseMapper，提供基础 CRUD，
 * 复杂查询在 Service 层用 LambdaQueryWrapper 组装。
 *
 * @author shijiu
 * @since 2026-08-04
 */
public interface OrdersMapper extends BaseMapper<Orders> {

}
