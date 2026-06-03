package com.lframework.xingyun.basedata.service.storecenter;

import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.vo.storecenter.CreateStoreCenterVo;
import com.lframework.xingyun.basedata.vo.storecenter.QueryStoreCenterSelectorVo;
import com.lframework.xingyun.basedata.vo.storecenter.QueryStoreCenterVo;
import com.lframework.xingyun.basedata.vo.storecenter.UpdateStoreCenterVo;

public interface StoreCenterService extends BaseMpService<StoreCenter> {

    /**
     * 查询列表
     *
     * @return
     */
    PageResult<StoreCenter> query(Integer pageIndex, Integer pageSize, QueryStoreCenterVo vo);

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    StoreCenter findById(String id);

    /**
     * 根据ID删除(逻辑删除)
     *
     * @param id
     */
    void deleteById(String id);

    /**
     * 创建
     *
     * @param vo
     * @return
     */
    String create(CreateStoreCenterVo vo);

    /**
     * 修改
     *
     * @param vo
     */
    void update(UpdateStoreCenterVo vo);

    /**
     * 选择器
     *
     * @return
     */
    PageResult<StoreCenter> selector(Integer pageIndex, Integer pageSize, QueryStoreCenterSelectorVo vo);
}
