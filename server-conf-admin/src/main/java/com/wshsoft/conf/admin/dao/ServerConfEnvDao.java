package com.wshsoft.conf.admin.dao;

import com.wshsoft.conf.admin.core.model.ServerConfEnv;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by Carry_xie on 2018-05-30
 */
@Mapper
public interface ServerConfEnvDao {

    public List<ServerConfEnv> findAll();

    public int save(ServerConfEnv ServerConfEnv);

    public int update(ServerConfEnv ServerConfEnv);

    public int delete(@Param("env") String env);

    public ServerConfEnv load(@Param("env") String env);

}