package com.ait.mall.core.mapper;

import com.ait.common.mapper.CoreMapper;
import com.ait.mall.core.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends CoreMapper<Product> {

    /**
     * Atomic stock decrement. The {@code stock >= #{quantity}} predicate makes
     * concurrent overselling impossible without a row lock held by the caller.
     *
     * @return 1 when the deduction succeeded, 0 when stock was insufficient
     */
    @Update("update shop_product set stock = stock - #{quantity}, sales = sales + #{quantity} "
            + "where id = #{productId} and stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("update shop_product set stock = stock + #{quantity}, sales = greatest(sales - #{quantity}, 0) "
            + "where id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Update("update shop_product set views = views + 1 where id = #{productId}")
    int increaseViews(@Param("productId") Long productId);

    /** Sales ranking used by the admin dashboard. */
    @Select("select name, sales, price, cover from shop_product "
            + "where deleted = 0 order by sales desc limit #{limit}")
    List<Map<String, Object>> topSelling(@Param("limit") int limit);

    /**
     * Category distribution for the dashboard pie. Products live in level-2
     * categories, so sales are rolled up to their level-1 parent.
     */
    @Select("select root.name as name, coalesce(sum(p.sales), 0) as value "
            + "from shop_category root "
            + "left join shop_category child on child.parent_id = root.id and child.deleted = 0 "
            + "left join shop_product p on p.category_id in (root.id, child.id) and p.deleted = 0 "
            + "where root.parent_id = 0 and root.deleted = 0 "
            + "group by root.id, root.name order by value desc limit 8")
    List<Map<String, Object>> salesByCategory();

    /** Recomputes the denormalised price/stock aggregates for one product. */
    @Update("update shop_product p set "
            + " price = coalesce((select min(s.price) from shop_product_sku s where s.product_id = p.id and s.deleted = 0), p.price),"
            + " stock = coalesce((select sum(s.stock) from shop_product_sku s where s.product_id = p.id and s.deleted = 0), 0) "
            + "where p.id = #{productId}")
    int refreshAggregates(@Param("productId") Long productId);
}
