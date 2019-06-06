package com.wshsoft.conf.admin.dao;

import com.wshsoft.conf.admin.core.model.ServerConfNodeMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by Carry_xie on 16/10/8.
 */
@Mapper
public interface ServerConfNodeMsgDao {

	public void add(ServerConfNodeMsg ServerConfNode);

	public List<ServerConfNodeMsg> findMsg(@Param("readedMsgIds") List<Integer> readedMsgIds);

	public int cleanMessage(@Param("messageTimeout") int messageTimeout);

}
