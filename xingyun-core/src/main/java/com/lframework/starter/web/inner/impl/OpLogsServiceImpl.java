package com.lframework.starter.web.inner.impl;

import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.inner.entity.OpLogs;
import com.lframework.starter.web.inner.mappers.OpLogsMapper;
import com.lframework.starter.web.inner.service.OpLogsService;
import com.lframework.starter.web.inner.vo.oplogs.CreateOpLogsVo;
import com.lframework.starter.web.inner.vo.oplogs.QueryOpLogsVo;
import com.lframework.xingyun.core.utils.StructuredOpLogUtil;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class OpLogsServiceImpl extends BaseMpServiceImpl<OpLogsMapper, OpLogs> implements OpLogsService {

  private static final Logger log = LoggerFactory.getLogger(OpLogsServiceImpl.class);

  @Override
  public String create(CreateOpLogsVo vo) {

    OpLogs data = doCreate(vo);
    save(data);
    return data.getId();
  }

  @Override
  public void create(Collection<CreateOpLogsVo> vos) {

    if (CollectionUtil.isEmpty(vos)) {
      return;
    }

    List<OpLogs> datas = vos.stream().map(this::doCreate).collect(Collectors.toList());
    saveBatch(datas);
  }

  @Override
  public PageResult<OpLogs> query(Integer pageIndex, Integer pageSize, QueryOpLogsVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<OpLogs> datas = doQuery(vo);
    datas.forEach(this::fillStructuredFieldsIfMissing);
    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public OpLogs findById(String id) {

    OpLogs data = doGetById(id);
    if (data != null) {
      fillStructuredFieldsIfMissing(data);
    }
    return data;
  }

  @Override
  public void clearLogs(LocalDateTime endTime) {

    log.info("开始清除创建时间早于{}的操作日志", endTime);
    doClearLogs(endTime);
  }

  protected OpLogs doCreate(CreateOpLogsVo vo) {

    OpLogs data = new OpLogs();
    data.setId(IdUtil.getId());
    data.setName(vo.getName());
    data.setLogType(vo.getLogType());
    data.setNameKey(vo.getNameKey());
    data.setNameTemplate(vo.getNameTemplate());
    data.setNameParams(vo.getNameParams());

    if (StringUtil.isBlank(data.getNameKey())) {
      data.setNameKey(StructuredOpLogUtil.parseNameKey(vo.getExtra()));
    }
    if (StringUtil.isBlank(data.getNameTemplate())) {
      data.setNameTemplate(StructuredOpLogUtil.parseNameTemplate(vo.getExtra()));
    }
    if (StringUtil.isBlank(data.getNameParams())) {
      data.setNameParams(StructuredOpLogUtil.parseNameParams(vo.getExtra()));
    }

    if (StringUtil.isNotBlank(vo.getCreateBy())) {
      data.setCreateBy(vo.getCreateBy());
    }
    if (StringUtil.isNotBlank(vo.getCreateById())) {
      data.setCreateById(vo.getCreateById());
    }
    if (StringUtil.isNotBlank(vo.getExtra())) {
      data.setExtra(StructuredOpLogUtil.parseOriginalExtra(vo.getExtra()));
    }
    data.setIp(vo.getIp());
    return data;
  }

  protected List<OpLogs> doQuery(QueryOpLogsVo vo) {

    return getBaseMapper().query(vo);
  }

  protected OpLogs doGetById(String id) {

    return getBaseMapper().findById(id);
  }

  protected void doClearLogs(LocalDateTime endTime) {

    getBaseMapper().clearLogs(endTime);
  }

  private void fillStructuredFieldsIfMissing(OpLogs data) {

    if (StringUtil.isBlank(data.getNameKey())) {
      data.setNameKey(StructuredOpLogUtil.parseNameKey(data.getExtra()));
    }
    if (StringUtil.isBlank(data.getNameTemplate())) {
      data.setNameTemplate(StructuredOpLogUtil.parseNameTemplate(data.getExtra()));
    }
    if (StringUtil.isBlank(data.getNameParams())) {
      data.setNameParams(StructuredOpLogUtil.parseNameParams(data.getExtra()));
    }

    data.setExtra(StructuredOpLogUtil.parseOriginalExtra(data.getExtra()));
  }
}
