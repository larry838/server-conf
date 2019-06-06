package com.wshsoft.conf.admin.dao;

import com.wshsoft.conf.admin.core.model.ServerConfNodeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by Carry_xie on 16/10/8.
 */
@Mapper
public interface ServerConfNodeLogDao {

	public List<ServerConfNodeLog> findByKey(@Param("env") String env, @Param("key") String key);

	public void add(ServerConfNodeLog ServerConfNode);

	public int deleteTimeout(@Param("env") String env,
							 @Param("key") String key,
							 @Param("length") int length);

}
