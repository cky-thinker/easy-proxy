package com.cky.proxy.server.dao;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cky.proxy.server.bo.PageResult;
import com.cky.proxy.server.domain.SysUser;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserDaoTest {
    @Test
    public void test() throws SQLException {
        SysUserDao userDao = new SysUserDao();
        TableUtils.dropTable(userDao.getDaoTemplate(), false);
        for (int i = 0; i < 100; i++) {
            SysUser sysUser = new SysUser();
            sysUser.setUsername("admin" + i);
            sysUser.setPassword("123456");
            userDao.insert(sysUser);
        }
        List<SysUser> users = userDao.selectList(qb -> {
            qb.where().eq("username", "admin1");
        });
        log.info("======selectList======");
        log.info("users:{}", users);
        Page page = new Page(1, 10);
        page.setOrder(new Order("id", Direction.DESC));
        PageResult<SysUser> pageResult = userDao.selectPage(page, (qb) -> {
            qb.like("username", "%admin%");
        });
        log.info("======selectPage======");
        log.info("pageResult:{}", JSONUtil.toJsonStr(pageResult));
    }
}
