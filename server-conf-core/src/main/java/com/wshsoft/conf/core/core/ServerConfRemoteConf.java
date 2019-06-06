package com.wshsoft.conf.core.core;

import com.wshsoft.conf.core.exception.ServerConfException;
import com.wshsoft.conf.core.model.ServerConfParamVO;
import com.wshsoft.conf.core.util.BasicHttpUtil;
import com.wshsoft.conf.core.util.json.BasicJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Carry_xie 2018-11-28
 */
public class ServerConfRemoteConf {
    private static Logger logger = LoggerFactory.getLogger(ServerConfRemoteConf.class);


    private static String adminAddress;
    private static String env;
    private static String accessToken;

    private static List<String> adminAddressArr = null;

    public static void init(String adminAddress, String env, String accessToken) {

        // valid
        if (adminAddress==null || adminAddress.trim().length()==0) {
            throw new ServerConfException("server-conf adminAddress can not be empty");
        }
        if (env==null || env.trim().length()==0) {
            throw new ServerConfException("server-conf env can not be empty");
        }


        ServerConfRemoteConf.adminAddress = adminAddress;
        ServerConfRemoteConf.env = env;
        ServerConfRemoteConf.accessToken = accessToken;


        // parse
        ServerConfRemoteConf.adminAddressArr = new ArrayList<>();
        if (adminAddress.contains(",")) {
            ServerConfRemoteConf.adminAddressArr.add(adminAddress);
        } else {
            ServerConfRemoteConf.adminAddressArr.addAll(Arrays.asList(adminAddress.split(",")));
        }

    }


    // ---------------------- rest api ----------------------

    /**
     * get and valid
     *
     * @param url
     * @param requestBody
     * @param timeout
     * @return
     */
    private static Map<String, Object> getAndValid(String url, String requestBody, int timeout){

        // resp json
        String respJson = BasicHttpUtil.postBody(url, requestBody, timeout);
        if (respJson == null) {
            return null;
        }

        // parse obj
        Map<String, Object> respObj = BasicJson.parseMap(respJson);
        int code = Integer.valueOf(String.valueOf(respObj.get("code")));
        if (code != 200) {
            logger.info("request fail, msg={}", (respObj.containsKey("msg")?respObj.get("msg"):respJson) );
            return null;
        }
        return respObj;
    }


    /**
     * find
     *
     * @param keys
     * @return
     */
    public static Map<String, String> find(Set<String> keys) {
        for (String adminAddressUrl: ServerConfRemoteConf.adminAddressArr) {

            // url + param
            String url = adminAddressUrl + "/conf/find";

            ServerConfParamVO paramVO = new ServerConfParamVO();
            paramVO.setAccessToken(accessToken);
            paramVO.setEnv(env);
            paramVO.setKeys(new ArrayList<String>(keys));

            String paramsJson = BasicJson.toJson(paramVO);

            // get and valid
            Map<String, Object> respObj = getAndValid(url, paramsJson, 5);

            // parse
            if (respObj!=null && respObj.containsKey("data")) {
                Map<String, String> data = (Map<String, String>) respObj.get("data");
                return data;
            }
        }

        return null;
    }

    public static String find(String key) {
        Map<String, String> result = find(new HashSet<String>(Arrays.asList(key)));
        if (result!=null) {
            return result.get(key);
        }
        return null;
    }


    /**
     * monitor
     *
     * @param keys
     * @return
     */
    public static boolean monitor(Set<String> keys) {

        for (String adminAddressUrl: ServerConfRemoteConf.adminAddressArr) {

            // url + param
            String url = adminAddressUrl + "/conf/monitor";

            ServerConfParamVO paramVO = new ServerConfParamVO();
            paramVO.setAccessToken(accessToken);
            paramVO.setEnv(env);
            paramVO.setKeys(new ArrayList<String>(keys));

            String paramsJson = BasicJson.toJson(paramVO);

            // get and valid
            Map<String, Object> respObj = getAndValid(url, paramsJson, 60);

            return respObj!=null?true:false;
        }
        return false;
    }

}
