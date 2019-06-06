package com.wshsoft.conf.admin.dao;

import com.wshsoft.conf.admin.core.model.ServerConfUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Carry_xie 2018-03-01
 */
@Mapper
public interface ServerConfUserDao {

    public List<ServerConfUser> pageList(@Param("offset") int offset,
                                      @Param("pagesize") int pagesize,
                                      @Param("username") String username,
                                      @Param("permission") int permission);
    public int pageListCount(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("username") String username,
                             @Param("permission") int permission);

    public int add(ServerConfUser ServerConfUser);

    public int update(ServerConfUser ServerConfUser);

    public int delete(@Param("username") String username);

    public ServerConfUser load(@Param("username") String username);

}
