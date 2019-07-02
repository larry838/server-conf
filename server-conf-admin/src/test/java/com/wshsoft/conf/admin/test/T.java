package com.wshsoft.conf.admin.test;

import org.apache.commons.lang3.StringUtils;

public class T {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
       String str="default#test,podsite#test";
       String[] proList =StringUtils.split(str, ",");
      // if (proList==null || proList.length==0){
    	   
     //  }
       String[] b = new String[proList.length];
       for (int i = 0; i < b.length; i++) 
   	{
    	  
   		b[i] = proList[i].substring(0, proList[i].indexOf("#"));
   	}
       
       System.out.println(b);
	}

}
