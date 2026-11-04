package com.ait.mall.core.mapper;

import com.ait.common.mapper.CoreMapper;
import com.ait.mall.core.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends CoreMapper<ProductSku> {

    /**
     * Atomic SKU stock decrement; returns 0 when stock is insufficient.
     */
    @Update("update shop_product_sku set stock = stock - #{quantity}, sales = sales + #{quantity} "
            + "where id = #{skuId} and stock >= #{quantity}")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Update("update shop_product_sku set stock = stock + #{quantity}, "
            + "sales = greatest(sales - #{quantity}, 0) where id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
