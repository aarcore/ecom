package com.ait.mall.core.mapper;

import com.ait.common.mapper.CoreMapper;
import com.ait.mall.core.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends CoreMapper<AuditLog> {
}
