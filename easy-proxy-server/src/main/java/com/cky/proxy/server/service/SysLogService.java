package com.cky.proxy.server.service;

import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cky.proxy.server.domain.dto.PageResult;
import com.cky.proxy.server.domain.entity.SysLog;
import com.cky.proxy.server.mapper.SysLogMapper;
import com.cky.proxy.server.util.BeanContext;
import com.cky.proxy.server.util.PageUtil;

import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;

/**
 * 系统日志服务
 */
public class SysLogService {
    private final SysLogMapper sysLogMapper;

    public SysLogService() {
        this.sysLogMapper = BeanContext.getSysLogMapper();
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
        
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        if (logType != null && !logType.isEmpty()) {
            wrapper.eq(SysLog::getLogType, logType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysLog::getLogContent, keyword);
        }
        
        IPage<SysLog> mybatisPage = PageUtil.toMybatisPage(page);
        IPage<SysLog> result = sysLogMapper.selectPage(mybatisPage, wrapper);
        return PageUtil.toPageResult(page, result);
    }

    /**
     * 根据ID查询日志详情
     */
    public SysLog getSysLogById(Integer id) {
        return sysLogMapper.selectById(id);
    }

    /**
     * 添加系统日志
     */
    public SysLog addSysLog(SysLog sysLog) {
        validateForCreate(sysLog);
        sysLog.setCreateTime(new Date());
        sysLogMapper.insert(sysLog);
        return sysLog;
    }

    /**
     * 删除系统日志
     */
    public boolean deleteSysLog(Integer id) {
        SysLog existing = sysLogMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        sysLogMapper.deleteById(id);
        return true;
    }
}
