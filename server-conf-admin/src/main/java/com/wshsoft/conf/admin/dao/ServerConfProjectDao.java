package com.wshsoft.conf.admin.dao;

import com.wshsoft.conf.admin.core.model.ServerConfProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by Carry_xie on 16/10/8.
 */
@Mapper
public interface ServerConfProjectDao {

    public List<ServerConfProject> findAll();

    public int save(ServerConfProject ServerConfProject);

    public int update(ServerConfProject ServerConfProject);

    public int delete(@Param("appname") String appname);

    public ServerConfProject load(@Param("appname") String appname);

}