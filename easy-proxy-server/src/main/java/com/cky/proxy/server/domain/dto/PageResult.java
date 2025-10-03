package com.cky.proxy.server.domain.dto;

import java.util.List;

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

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalPage() { return totalPage; }
    public void setTotalPage(int totalPage) { this.totalPage = totalPage; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
