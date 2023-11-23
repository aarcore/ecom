package com.ait.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetaHelper implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {}

    @Override
    public void updateFill(MetaObject metaObject) {}
}
