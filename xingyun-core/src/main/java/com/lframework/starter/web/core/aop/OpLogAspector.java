package com.lframework.starter.web.core.aop;

import com.lframework.starter.common.utils.ArrayUtil;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.components.oplog.OpLogType;
import com.lframework.starter.web.core.components.security.AbstractUserDetails;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.OpLogUtil;
import com.lframework.starter.web.core.utils.SpelUtil;
import com.lframework.starter.web.inner.vo.oplogs.CreateOpLogsVo;
import com.lframework.xingyun.core.utils.StructuredOpLogUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;

@Aspect
public class OpLogAspector {

  private static final Logger log = LoggerFactory.getLogger(OpLogAspector.class);

  @Pointcut("@annotation(com.lframework.starter.web.core.annotations.oplog.OpLog)")
  public void opLogCutPoint() {
  }

  @Around("opLogCutPoint()")
  public Object opLog(ProceedingJoinPoint joinPoint) throws Throwable {

    String logId = IdUtil.getUUID();
    OpLogUtil.init(logId);
    AbstractUserDetails currentUser = SecurityUtil.getCurrentUser();
    Object value = null;

    try {
      value = joinPoint.proceed();

      if (currentUser == null) {
        currentUser = SecurityUtil.getCurrentUser();
      }

      if (currentUser == null) {
        return value;
      }

      MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
      List<String> paramNameList = Arrays.asList(methodSignature.getParameterNames());
      List<Object> paramList = Arrays.asList(joinPoint.getArgs());
      EvaluationContext ctx = SpelUtil.buildContext();

      for (int i = 0; i < paramNameList.size(); i++) {
        ctx.setVariable(paramNameList.get(i), paramList.get(i));
      }
      ctx.setVariable("_result", value);

      Map<String, Object> variables = OpLogUtil.getVariables();
      if (!CollectionUtil.isEmpty(variables)) {
        variables.forEach(ctx::setVariable);
      }

      Method method = methodSignature.getMethod();
      OpLog opLog = method.getAnnotation(OpLog.class);

      Object[] params;
      if (!ArrayUtil.isEmpty(opLog.params())) {
        params = new Object[opLog.params().length];
        for (int i = 0; i < opLog.params().length; i++) {
          params[i] = SpelUtil.parse(opLog.params()[i], ctx);
        }
      } else {
        params = new String[0];
      }

      List<String[]> paramsList = buildParamsList(opLog, params);
      List<CreateOpLogsVo> createOpLogsVoList = new ArrayList<>();

      for (String[] strArr : paramsList) {
        String extra = OpLogUtil.getExtra();
        if (extra == null && opLog.autoSaveParams()) {
          autoSaveExtra(paramNameList, ctx);
          extra = OpLogUtil.getExtra();
        }

        String finalExtra = StructuredOpLogUtil.buildStructuredExtra(
            StructuredOpLogUtil.resolveNameKey(opLog), opLog.name(), strArr, extra);

        CreateOpLogsVo vo = new CreateOpLogsVo();
        vo.setName(StringUtil.format(opLog.name(), strArr));

        OpLogType opLogTypeBean = (OpLogType) ApplicationUtil.getBean(opLog.type());
        vo.setLogType(opLogTypeBean.getCode());
        vo.setExtra(finalExtra);
        vo.setIp(currentUser.getIp());
        vo.setNameKey(StructuredOpLogUtil.resolveNameKey(opLog));
        vo.setNameTemplate(opLog.name());
        vo.setNameParams(com.lframework.starter.web.core.utils.JsonUtil.toJsonString(strArr));
        createOpLogsVoList.add(vo);
      }

      if (CollectionUtil.isNotEmpty(createOpLogsVoList)) {
        OpLogUtil.addLogs(createOpLogsVoList);
      }
      OpLogUtil.submitLog(currentUser);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      OpLogUtil.clear();
    }

    return value;
  }

  private List<String[]> buildParamsList(OpLog opLog, Object[] params) {

    List<String[]> paramsList = new ArrayList<>();
    if (opLog.loopFormat() && Arrays.stream(params).anyMatch(t -> t instanceof Collection)) {
      String[] strParams = new String[params.length];
      List<Integer> collectionIndexes = new ArrayList<>();
      for (int i = 0; i < params.length; i++) {
        if (params[i] instanceof Collection) {
          collectionIndexes.add(i);
        } else {
          strParams[i] = params[i] == null ? null : params[i].toString();
        }
      }
      paramsList.add(strParams);

      if (!CollectionUtil.isEmpty(collectionIndexes)) {
        for (Integer collectionIndex : collectionIndexes) {
          List<String[]> tmpParamsList = new ArrayList<>();
          for (String[] paramsArr : paramsList) {
            Collection<?> collection = (Collection<?>) params[collectionIndex];
            for (Object o : collection) {
              String[] tmp = new String[paramsArr.length];
              for (int j = 0; j < paramsArr.length; j++) {
                if (j == collectionIndex.intValue()) {
                  tmp[j] = o == null ? null : o.toString();
                } else {
                  tmp[j] = paramsArr[j];
                }
              }
              tmpParamsList.add(tmp);
            }
          }
          paramsList.clear();
          paramsList.addAll(tmpParamsList);
        }
      }
    } else {
      String[] strParams = new String[params.length];
      for (int i = 0; i < params.length; i++) {
        strParams[i] = params[i] == null ? null : params[i].toString();
      }
      paramsList.add(strParams);
    }
    return paramsList;
  }

  private void autoSaveExtra(List<String> paramNameList, EvaluationContext ctx) {

    if (!CollectionUtil.isNotEmpty(paramNameList)) {
      return;
    }

    if (paramNameList.size() == 1) {
      OpLogUtil.setExtra(ctx.lookupVariable(paramNameList.get(0)));
      return;
    }

    Map<String, Object> paramMap = new LinkedHashMap<>(paramNameList.size(), 1F);
    for (String paramName : paramNameList) {
      paramMap.put(paramName, ctx.lookupVariable(paramName));
    }
    OpLogUtil.setExtra(paramMap);
  }
}
