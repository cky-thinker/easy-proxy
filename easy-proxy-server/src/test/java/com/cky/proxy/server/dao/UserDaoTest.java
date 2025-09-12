package com.cky.proxy.server.dao;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.User;
import com.cky.proxy.server.util.JsonUtil;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserDaoTest {
    @Test
    public void insertTest() {
        SysUserDao userDao = new SysUserDao();
        for (int i = 0; i < 100; i++) {
            User sysUser = new User();
            sysUser.setUsername("admin" + i);
            sysUser.setPassword("123456");
            userDao.insert(sysUser);
        }
        List<User> users = userDao.selectList(qb -> {
            qb.where().eq("username", "admin1");
        });
        log.info("======selectList======");
        log.info("users:{}", users);
        Page page = new Page(1, 10);
        page.setOrder(new Order("id", Direction.DESC));
        PageResult<User> pageResult = userDao.selectPage(page, (qb) -> {
            qb.like("username", "%admin%");
        });
        log.info("======selectPage======");
        log.info("pageResult:{}", JSONUtil.toJsonStr(pageResult));
    }

    @Test
    public void initUser() {
        SysUserDao userDao = new SysUserDao();
        User sysUser = new User();
        sysUser.setUsername("admin");
        sysUser.setPassword("123456");
        userDao.insert(sysUser);

        User user = userDao.selectList(queryBuilder -> {
            queryBuilder.where().eq("username", "admin");
        }).stream().findFirst().orElse(null);
        System.out.println("------selectList------");
        System.out.println(JsonUtil.toJson(user));
    }
}
