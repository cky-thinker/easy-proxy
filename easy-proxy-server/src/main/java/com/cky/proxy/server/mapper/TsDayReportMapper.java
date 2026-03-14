package com.cky.proxy.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cky.proxy.server.domain.dto.ClientTrafficDayReport;
import com.cky.proxy.server.domain.dto.TrafficRankingDTO;
import com.cky.proxy.server.domain.dto.TrafficTrendDTO;
import com.cky.proxy.server.domain.entity.TsDayReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Date;
import java.util.List;

public interface TsDayReportMapper extends BaseMapper<TsDayReport> {

    @Select("<script>" +
            "SELECT proxy_client_id, date, SUM(upward_traffic_bytes) as uploadBytes, SUM(downward_traffic_bytes) as downloadBytes " +
            "FROM ts_day_report " +
            "WHERE 1=1 " +
            "<if test='proxyClientId != null'> AND proxy_client_id = #{proxyClientId} </if>" +
            "<if test='startDate != null'> AND date &gt;= #{startDate} </if>" +
            "<if test='endDate != null'> AND date &lt;= #{endDate} </if>" +
            "GROUP BY proxy_client_id, date " +
            "ORDER BY date DESC" +
            "</script>")
    IPage<ClientTrafficDayReport> selectClientTrafficDayReports(IPage<ClientTrafficDayReport> page, @Param("proxyClientId") Integer proxyClientId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Select("SELECT proxy_client_rule_id as proxyClientRuleId, COALESCE(SUM(upward_traffic_bytes) + SUM(downward_traffic_bytes), 0) AS totalTraffic " +
            "FROM ts_day_report " +
            "WHERE date >= #{startDate} AND date <= #{endDate} " +
            "GROUP BY proxy_client_rule_id " +
            "ORDER BY totalTraffic DESC " +
            "LIMIT #{limit}")
    List<TrafficRankingDTO> selectTrafficRanking(@Param("startDate") Date startDate, @Param("endDate") Date endDate, @Param("limit") int limit);

    @Select("SELECT date, COALESCE(SUM(upward_traffic_bytes),0) AS uploadBytes, COALESCE(SUM(downward_traffic_bytes),0) AS downloadBytes " +
            "FROM ts_day_report " +
            "WHERE date >= #{startDate} AND date <= #{endDate} " +
            "GROUP BY date " +
            "ORDER BY date ASC")
    List<TrafficTrendDTO> selectTrafficTrend(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
