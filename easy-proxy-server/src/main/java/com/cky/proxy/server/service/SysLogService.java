package com.cky.proxy.server.service;

import java.util.Date;
import java.util.List;

import com.cky.proxy.server.dao.SysLogDao;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.util.BeanContext;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;

/**
 * 系统日志服务
 */
public class SysLogService {
    private final SysLogDao sysLogDao;

    public SysLogService() {
        this.sysLogDao = BeanContext.getSysLogDao();
    }

    private void validateForCreate(SysLog sysLog) {
        if (sysLog.getLogType() == null || sysLog.getLogType().isEmpty()) {
            throw new RuntimeException("日志类型不能为空");
        }
        if (sysLog.getLogContent() == null || sysLog.getLogContent().isEmpty()) {
            throw new RuntimeException("日志内容不能为空");
        }
    }

    /**
     * 分页查询系统日志
     */
    public PageResult<SysLog> getSysLogsPageable(Page page, String logType, String keyword) {
        // 默认按创建时间倒序
        if (page.getOrders() == null || page.getOrders().length == 0) {
            page.setOrder(new Order("create_time", Direction.DESC));
        }
        return sysLogDao.selectPage(page, wrapper -> {
            if (logType != null && !logType.isEmpty()) {
                wrapper.eq("log_type", logType);
            }
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.like("log_content", keyword);
            }
        });
    }

    /**
     * 根据ID查询日志详情
     */
    public SysLog getSysLogById(Integer id) {
        return sysLogDao.selectById(id);
    }

    /**
     * 添加系统日志
     */
    public SysLog addSysLog(SysLog sysLog) {
        validateForCreate(sysLog);
        sysLog.setCreateTime(new Date());
        sysLogDao.insert(sysLog);
        return sysLog;
    }

    /**
     * 删除系统日志
     */
    public boolean deleteSysLog(Integer id) {
        SysLog existing = sysLogDao.selectById(id);
        if (existing == null) {
            return false;
        }
        sysLogDao.deleteById(id);
        return true;
    }
}
