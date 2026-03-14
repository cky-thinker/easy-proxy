package com.cky.proxy.server.util;

import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cky.proxy.server.domain.dto.PageResult;

/**
 * 分页工具类
 */
public class PageUtil {

    /**
     * 将 Hutool Page 转换为 MyBatis-Plus Page
     */
    public static <T> Page<T> toMybatisPage(cn.hutool.db.Page page) {
        Page<T> mybatisPage = new Page<>(page.getPageNumber(), page.getPageSize());
        
        if (page.getOrders() != null) {
            for (Order order : page.getOrders()) {
                mybatisPage.addOrder(new OrderItem(order.getField(), order.getDirection() == Direction.ASC));
            }
        }
        return mybatisPage;
    }

    /**
     * 将 MyBatis-Plus 分页结果转换为 PageResult
     */
    public static <T> PageResult<T> toPageResult(cn.hutool.db.Page hutoolPage, IPage<T> resultPage) {
        return new PageResult<>(
            hutoolPage.getPageNumber(),
            hutoolPage.getPageSize(),
            (int) resultPage.getPages(),
            (int) resultPage.getTotal(),
            resultPage.getRecords()
        );
    }
}
