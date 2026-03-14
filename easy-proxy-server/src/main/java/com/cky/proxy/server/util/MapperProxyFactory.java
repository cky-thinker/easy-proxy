package com.cky.proxy.server.util;

import com.cky.proxy.server.config.DatabaseConnectionManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Mapper 动态代理工厂
 * 用于生成自动管理 SqlSession 的 Mapper 代理对象
 */
public class MapperProxyFactory {
    private static final Logger log = LoggerFactory.getLogger(MapperProxyFactory.class);

    @SuppressWarnings("unchecked")
    public static <T> T getMapper(Class<T> mapperInterface) {
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                new MapperInvocationHandler<>(mapperInterface)
        );
    }

    static class MapperInvocationHandler<T> implements InvocationHandler {
        private final Class<T> mapperInterface;

        public MapperInvocationHandler(Class<T> mapperInterface) {
            this.mapperInterface = mapperInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 如果是 Object 类的方法，直接调用
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);
            }

            SqlSessionFactory sqlSessionFactory = DatabaseConnectionManager.getInstance().getSqlSessionFactory();
            // true 表示自动提交事务
            try (SqlSession session = sqlSessionFactory.openSession(true)) {
                T mapper = session.getMapper(mapperInterface);
                return method.invoke(mapper, args);
            } catch (Exception e) {
                // 如果是反射调用的异常，解包获取原始异常
                if (e instanceof java.lang.reflect.InvocationTargetException) {
                    throw ((java.lang.reflect.InvocationTargetException) e).getTargetException();
                }
                throw e;
            }
        }
    }
}
