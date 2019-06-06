package com.wshsoft.conf.admin.dao;

import com.wshsoft.conf.admin.core.model.ServerConfNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by Carry_xie on 16/10/8.
 */
@Mapper
public interface ServerConfNodeDao {

	public List<ServerConfNode> pageList(@Param("offset") int offset,
									  @Param("pagesize") int pagesize,
									  @Param("env") String env,
									  @Param("appname") String appname,
									  @Param("key") String key);
	public int pageListCount(@Param("offset") int offset,
							 @Param("pagesize") int pagesize,
							 @Param("env") String env,
							 @Param("appname") String appname,
							 @Param("key") String key);

	public int delete(@Param("env") String env, @Param("key") String key);

	public void insert(ServerConfNode ServerConfNode);

	public ServerConfNode load(@Param("env") String env, @Param("key") String key);

	public int update(ServerConfNode ServerConfNode);
	
}
