package com.cky.proxy.server.dao;

import java.util.List;

import com.cky.proxy.server.domain.entity.SysUser;
import org.junit.jupiter.api.Test;

import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.util.JsonUtil;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SysUserDaoTest {
    @Test
    public void insertTest() {
        UserDao userDao = new UserDao();
        for (int i = 0; i < 100; i++) {
            SysUser sysUser = new SysUser();
            sysUser.setUsername("admin" + i);
            sysUser.setPassword("123456");
            userDao.insert(sysUser);
        }
        List<SysUser> sysUsers = userDao.selectList(qb -> {
            qb.eq("username", "admin1");
        });
        log.info("======selectList======");
        log.info("users:{}", sysUsers);
        Page page = new Page(1, 10);
        page.setOrder(new Order("id", Direction.DESC));
        PageResult<SysUser> pageResult = userDao.selectPage(page, (qb) -> {
            qb.like("username", "admin");
        });
        log.info("======selectPage======");
        log.info("pageResult:{}", JSONUtil.toJsonStr(pageResult));
    }

    @Test
    public void initUser() {
        UserDao userDao = new UserDao();
        SysUser sysUser = new SysUser();
        sysUser.setUsername("admin");
        sysUser.setPassword("123456");
        userDao.insert(sysUser);

        SysUser user = userDao.selectList(queryBuilder -> {
            queryBuilder.eq("username", "admin");
        }).stream().findFirst().orElse(null);
        System.out.println("------selectList------");
        System.out.println(JsonUtil.toJson(user));
    }
}
