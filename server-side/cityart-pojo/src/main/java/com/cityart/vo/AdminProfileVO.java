package com.cityart.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * B端管理员个人信息返回数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileVO {

    private Long id;
    private String phone;
    private String name;
    private Integer status;
    private LocalDateTime createTime;
}
