package com.cky.proxy.server.dao;

import java.util.List;
import java.util.function.Consumer;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.cky.proxy.server.config.DatabaseConnectionManager;
import com.cky.proxy.server.domain.dto.PageResult;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import org.apache.ibatis.session.SqlSession;

public abstract class BaseDao<T, M extends BaseMapper<T>> {

    public BaseDao() {
    }

    protected abstract Class<M> getMapperClass();

    protected M getMapper(SqlSession session) {
        return session.getMapper(getMapperClass());
    }

    public void insert(T t) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession(true)) {
            getMapper(session).insert(t);
        }
    }

    public void updateById(T t) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession(true)) {
            getMapper(session).updateById(t);
        }
    }

    public void deleteById(Integer id) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession(true)) {
            getMapper(session).deleteById(id);
        }
    }

    public T selectById(Integer id) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession()) {
            return getMapper(session).selectById(id);
        }
    }

    public List<T> selectList(Consumer<QueryWrapper<T>> consumer) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession()) {
            QueryWrapper<T> wrapper = new QueryWrapper<>();
            consumer.accept(wrapper);
            return getMapper(session).selectList(wrapper);
        }
    }

    public PageResult<T> selectPage(Page page, Consumer<QueryWrapper<T>> consumer) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession()) {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> mybatisPage =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getPageNumber(), page.getPageSize());

            if (page.getOrders() != null) {
                for (Order order : page.getOrders()) {
                    mybatisPage.addOrder(new OrderItem(order.getField(), order.getDirection() == Direction.ASC));
                }
            }

            QueryWrapper<T> wrapper = new QueryWrapper<>();
            consumer.accept(wrapper);

            M mapper = getMapper(session);
            mapper.selectPage(mybatisPage, wrapper);

            return new PageResult<>(page.getPageNumber(), page.getPageSize(), (int) mybatisPage.getPages(), (int) mybatisPage.getTotal(), mybatisPage.getRecords());
        }
    }
    
    public <R> R execute(SqlFunction<M, R> function) {
        try (SqlSession session = DatabaseConnectionManager.getInstance().getSqlSessionFactory().openSession(true)) {
            return function.apply(getMapper(session));
        } catch (Exception e) {
             throw new RuntimeException("Execute failed", e);
        }
    }
    
    // Legacy method to support getDao().countOf() in UserService.checkInit()
    // We can replace it with execute(mapper -> mapper.selectCount(null))
    
    @FunctionalInterface
    public interface SqlFunction<M, R> {
        R apply(M mapper);
    }
}
