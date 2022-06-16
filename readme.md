# 项目简介

定时按什么值得买的好价排行榜,收集优惠信息并推送至邮箱,基于java实现,欢迎各位值友来捉虫 
* 邮件的正文内容是html,优惠信息将以表格的形式显示在正文中
* 已推送的优惠信息会被记录在项目根目录的`pushed.txt`文件中,每次运行项目都会自动提交代码,更新这个文件
* 有其他改进的建议可以提issue给我
* 麻烦给个star支持一下:heart:

# 运行

*  fork本仓库
*  新增Actions secrets

| secret        | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| EMAILACCOUNT  | 接收优惠信息的邮箱                                           |
| EMAILPASSWORD | 邮箱的授权码,[参考qq邮箱的这篇文档](https://service.mail.qq.com/cgi-bin/help?subtype=1&&id=28&&no=1001256) |
| GIT_TOKEN     | [参考这篇文章](http://t.zoukankan.com/joe235-p-15152380.html),只勾选repo的权限,Expiration设置为No Expiration |

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/Actions%20secrets.png" width = "700" height = "350" alt="图片名称" align=center />



* 打开fork项目的workFlow开关

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/enableWorkFlow.png" width = "700" height = "350" alt="图片名称" align=center />

* 点击pica_crawler_actions.yml,编辑git actions. 修改红框所示的内容

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/gitActions.png" width = "700" height = "350" alt="图片名称" align=center />



* 手动触发一次,测试下能不能跑通

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/runWorkFlow.png" width = "500" height = "200" alt="图片名称" align=center />



**成功运行的截图:**   
<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/%E6%88%90%E5%8A%9F%E8%BF%90%E8%A1%8C%E6%88%AA%E5%9B%BE.png" width = "700" height = "350" alt="图片名称" align=center />





# 部分优惠信息不会被推送的原因



### 标题关键词过滤

`black_words.txt`文件中可以自定义关键词,多个关键词换行

在优惠信息的标题中包含任意一个关键词时,就不会被推送   





# CHANGELOG




v1.0   2023/1/31
---------------
* 实现定时按什么值得买的好价排行榜,收集优惠信息并推送至邮箱的功能



