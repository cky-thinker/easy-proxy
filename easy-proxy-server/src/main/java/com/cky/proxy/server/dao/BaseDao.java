package com.cky.proxy.server.dao;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import com.cky.proxy.server.config.DatabaseConnectionManager;
import com.cky.proxy.server.domain.dto.PageResult;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import lombok.SneakyThrows;

import java.sql.SQLException;
import java.util.List;

public abstract class BaseDao<T> {
    protected volatile Dao<T, Integer> dao = null;
    private final Object daoLock = new Object();

    @SneakyThrows
    public void insert(T t) {
        try {
            getDao().create(t);
        } catch (SQLException e) {
            throw new RuntimeException("插入数据失败", e);
        }
    }

    @SneakyThrows
    public void updateById(T t) {
        try {
            getDao().update(t);
        } catch (SQLException e) {
            throw new RuntimeException("更新数据失败", e);
        }
    }

    @SneakyThrows
    public void deleteById(Integer id) {
        try {
            getDao().deleteById(id);
        } catch (SQLException e) {
            throw new RuntimeException("删除数据失败, ID: " + id, e);
        }
    }

    @SneakyThrows
    public T selectById(Integer id) {
        try {
            return getDao().queryForId(id);
        } catch (SQLException e) {
            throw new RuntimeException("查询数据失败, ID: " + id, e);
        }
    }

    @SneakyThrows
    public List<T> selectList(QueryConsumer<QueryBuilder<T, Integer>> consumer) {
        try {
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            consumer.accept(queryBuilder);
            return queryBuilder.query();
        } catch (SQLException e) {
            throw new RuntimeException("查询列表数据失败", e);
        }
    }

    @SneakyThrows
    public PageResult<T> selectPage(Page page, QueryConsumer<Where<T, Integer>> consumer) {
        try {
            // 查询总数
            QueryBuilder<T, Integer> countQuery = getDao().queryBuilder();
            Where<T, Integer> countWhere = countQuery.where();
            consumer.accept(countWhere);
            int totle = (int) countQuery.countOf();
            int totlePage = (int) (totle % page.getPageSize() == 0 ? totle / page.getPageSize()
                : totle / page.getPageSize() + 1);
            // 查询列表
            QueryBuilder<T, Integer> queryBuilder = getDao().queryBuilder();
            Where<T, Integer> where = queryBuilder.where();
            consumer.accept(where);
            if (page.getOrders() != null) {
                for (Order order : page.getOrders()) {
                    if (order.getDirection() == Direction.ASC) {
                        queryBuilder.orderBy(order.getField(), true);
                    } else {
                        queryBuilder.orderBy(order.getField(), false);
                    }
                }
            }

            List<T> list = queryBuilder.offset((long) page.getStartPosition()).limit((long) page.getPageSize()).query();
            PageResult<T> result = new PageResult<>(page.getPageNumber(), page.getPageSize(), totlePage, totle, list);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("分页查询数据失败", e);
        }
    }

    @SneakyThrows
    public Dao<T, Integer> getDao() {
        // 双重检查锁定模式，确保线程安全
        if (dao == null) {
            synchronized (daoLock) {
                if (dao == null) {
                    try {
                        ConnectionSource connectionSource = getConnectionSource();
                        Class<T> entityClass = getEntityClass();
                        dao = DaoManager.createDao(connectionSource, entityClass);
                    } catch (SQLException e) {
                        throw new RuntimeException("创建DAO失败: " + getEntityClass().getSimpleName(), e);
                    } catch (Exception e) {
                        throw new RuntimeException("创建DAO时发生未知错误: " + getEntityClass().getSimpleName(), e);
                    }
                }
            }
        }
        return dao;
    }

    @SneakyThrows
    public ConnectionSource getConnectionSource() {
        return DatabaseConnectionManager.getInstance().createConnectionSource();
    }

    /**
     * 获取泛型类型
     *
     * @return 实体类类型
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getEntityClass() {
        // 获取当前类的泛型超类
        java.lang.reflect.Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof java.lang.reflect.ParameterizedType) {
            // 获取泛型参数类型数组
            java.lang.reflect.Type[] actualTypeArguments = ((java.lang.reflect.ParameterizedType) genericSuperclass)
                .getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                // 返回第一个泛型参数类型
                return (Class<T>) actualTypeArguments[0];
            }
        }
        throw new RuntimeException("无法获取泛型类型");
    }

    @FunctionalInterface
    public interface QueryConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
