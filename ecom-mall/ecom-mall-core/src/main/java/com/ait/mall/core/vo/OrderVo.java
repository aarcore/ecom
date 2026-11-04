package com.ait.mall.core.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Order header with lines")
public class OrderVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long memberId;

    private String memberNickname;

    private Integer status;

    @Schema(description = "Localised status text")
    private String statusText;

    private BigDecimal totalAmount;

    private BigDecimal freightAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private String payType;

    private LocalDateTime paidAt;

    private String receiver;

    private String receiverMobile;

    @Schema(description = "Province + city + district + detail")
    private String receiverAddress;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverDetail;

    private String remark;

    private String platform;

    private String expressCompany;

    private String expressNo;

    private LocalDateTime shippedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime closedAt;

    private String adminRemark;

    private LocalDateTime createdAt;

    private List<OrderItemVo> items = new ArrayList<>();

    @Schema(description = "Seconds left before an unpaid order is cancelled")
    private Long payDeadlineSeconds;
}
