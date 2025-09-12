package com.cky.proxy.server.dao;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import com.cky.proxy.server.bean.dto.PageResult;
import com.cky.proxy.server.config.DatabaseProperty;
import com.cky.proxy.server.config.ConfigProperty;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.SneakyThrows;

import java.sql.SQLException;
import java.util.List;

public abstract class BaseDao<T> {
    protected Dao<T, Integer> dao = null;

    @SneakyThrows
    public void insert(T t) {
        getDaoTemplate().create(t);
    }

    @SneakyThrows
    public void updateById(T t) {
        getDaoTemplate().update(t);
    }

    @SneakyThrows
    public void deleteById(Integer id) {
        getDaoTemplate().deleteById(id);
    }

    @SneakyThrows
    public T selectById(Integer id) {
        return getDaoTemplate().queryForId(id);
    }

    @SneakyThrows
    public List<T> selectList(ThrowingConsumer<QueryBuilder<T, Integer>> consumer) {
        QueryBuilder<T, Integer> queryBuilder = getDaoTemplate().queryBuilder();
        consumer.accept(queryBuilder);
        return queryBuilder.query();
    }

    @SneakyThrows
    public PageResult<T> selectPage(Page page, ThrowingConsumer<Where<T, Integer>> consumer) {
        // 查询总数
        QueryBuilder<T, Integer> countQuery = getDaoTemplate().queryBuilder();
        Where<T, Integer> countWhere = countQuery.where();
        consumer.accept(countWhere);
        int totle = (int) countQuery.countOf();
        int totlePage = (int) (totle % page.getPageSize() == 0 ? totle / page.getPageSize()
                : totle / page.getPageSize() + 1);
        // 查询列表
        QueryBuilder<T, Integer> queryBuilder = getDaoTemplate().queryBuilder();
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
    }

    @SneakyThrows
    public Dao<T, Integer> getDaoTemplate() {
        if (dao == null) {
            ConnectionSource connectionSource = getConnectionSource();
            Class<T> entityClass = getEntityClass();
            TableUtils.createTableIfNotExists(connectionSource, entityClass);
            dao = DaoManager.createDao(connectionSource, entityClass);
        }

        return dao;
    }

    public ConnectionSource getConnectionSource() throws SQLException {
        DatabaseProperty db = ConfigProperty.getInstance().getDb();
        return new JdbcConnectionSource(db.getUrl(), db.getUsername(), db.getPassword());
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
    public interface ThrowingConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
