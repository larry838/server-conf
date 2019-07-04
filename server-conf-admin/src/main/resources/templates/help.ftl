<!DOCTYPE html>
<html>
<head>
    <title>配置管理中心</title>
    <#import "./common/common.macro.ftl" as netCommon>
    <@netCommon.commonStyle />
</head>
<body class="hold-transition skin-blue sidebar-mini <#if cookieMap?exists && cookieMap["adminlte_settings"]?exists && "off" == cookieMap["adminlte_settings"].value >sidebar-collapse</#if> ">
<div class="wrapper">
    <!-- header -->
    <@netCommon.commonHeader />
    <!-- left -->
    <@netCommon.commonLeft "help" />
    
    <!-- Content Wrapper. Contains page content -->
    <div class="content-wrapper">
        <!-- Content Header (Page header) -->
        <section class="content-header">
            <h1>使用教程<small></small></h1>

        </section>

        <!-- Main content -->
        <section class="content">
        <p>
        
                       Server-CONF 是一个轻量级分布式配置管理平台，拥有"轻量级、秒级动态推送、多环境、跨语言、跨机房、配置监听、权限控制、版本回滚"等特性，开箱即用。</p>
        <h3><li> 
               <a href="${request.contextPath}/static/doc/Sever-conf.pdf" download="Sever-conf使用手册.pdf" target="_blank">Sever-conf 使用手册</a></li>
	      <br/> <li> 
               <a href="${request.contextPath}/static/doc/Server-CONF.pptx" download="Server-CONF架构图.pptx" target="_blank">Server-CONF架构图</a></li>
	      <br/> <li> 
               <a href="${request.contextPath}/static/doc/server-conf-demo.rar" download="server-conf-demo.rar" target="_blank">示例demo代码</a></li></h3>
	       
        <!-- /.content -->
    </div>
    <!-- /.content-wrapper -->
    
    <!-- footer -->
    <@netCommon.commonFooter />
</div>
<@netCommon.commonScript />
</body>
</html>