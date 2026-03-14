package com.cky.proxy.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cky.proxy.server.domain.dto.TrafficRankingDTO;
import com.cky.proxy.server.domain.dto.TrafficTrendDTO;
import com.cky.proxy.server.domain.entity.TsHourReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface TsHourReportMapper extends BaseMapper<TsHourReport> {

    @Select("SELECT proxy_client_rule_id as proxyClientRuleId, COALESCE(SUM(upward_traffic_bytes) + SUM(downward_traffic_bytes), 0) AS totalTraffic " +
            "FROM ts_hour_report " +
            "WHERE date >= #{startDate} AND date <= #{endDate} " +
            "GROUP BY proxy_client_rule_id " +
            "ORDER BY totalTraffic DESC " +
            "LIMIT #{limit}")
    List<TrafficRankingDTO> selectTrafficRanking(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("limit") int limit);

    @Select("SELECT date, COALESCE(SUM(upward_traffic_bytes),0) AS uploadBytes, COALESCE(SUM(downward_traffic_bytes),0) AS downloadBytes " +
            "FROM ts_hour_report " +
            "WHERE date >= #{startDate} AND date <= #{endDate} " +
            "GROUP BY date " +
            "ORDER BY date ASC")
    List<TrafficTrendDTO> selectTrafficTrend(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
