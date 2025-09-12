package com.cky.proxy.server.domain.dto;

import java.util.List;

import lombok.Data;

@Data
public class PageResult<T> {
	private int page;
	/**
	 * 每页结果数
	 */
	private int pageSize;
	/**
	 * 总页数
	 */
	private int totalPage;
	/**
	 * 总数
	 */
	private int total;

    private List<T> list;

    public PageResult(int page, int pageSize, int totalPage, int total, List<T> data) {
        this.page = page;
        this.pageSize = pageSize;
        this.totalPage = totalPage;
        this.total = total;
        this.list = data;
    }
}
