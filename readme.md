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

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdmActions.png" width = "700" height = "350" alt="图片名称" align=center />



* 打开fork项目的workFlow开关

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/enableWorkFlow.png" width = "700" height = "350" alt="图片名称" align=center />

* 点击zdm_crawler.yml,编辑git actions. 修改红框所示的内容

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdm%E4%BF%AE%E6%94%B9actions%E9%85%8D%E7%BD%AE.png" width = "700" height = "500" alt="图片名称" align=center />



* 手动触发一次,测试下能不能跑通

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/runWorkFlow.png" width = "500" height = "200" alt="图片名称" align=center />



**成功运行的截图:**   
<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdm%E6%88%90%E5%8A%9F%E8%BF%90%E8%A1%8C%E6%88%AA%E5%9B%BE.png" width = "700" height = "400" alt="图片名称" align=center />



<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdm%E9%82%AE%E7%AE%B1%E6%88%AA%E5%9B%BE.png" width = "700" height = "400" alt="图片名称" align=center />

* [我自己也fork了一份](https://github.com/PhantomStrikers/zdm),每天都在自动运行的,可以通过这个项目的actions运行记录判断这个项目是否还能work



# 自定义过滤逻辑

### 标题关键词过滤

`black_words.txt`文件中可以自定义关键词,**多个关键词换行**

在优惠信息的标题中包含任意一个关键词时,就不会被推送   

### 标题白名单匹配

`white_words.txt`文件中可以自定义白名单,**多个白名单换行**

设置之后，会有两个效果：

- `black_words.txt` 中的关键词过滤功能将失效
- 仅会推送优惠信息的标题包含 `white_words.txt` 中任意一个白名单的商品信息


# CHANGELOG

| 日期        | 说明                                                         |
| ------------- | ------------------------------------------------------------ |
| 2023/5/4  | 已推送优惠信息按日期在logs文件夹下归类记录.避免单个文件记录数据量过大的问题 |
| 2023/1/31  | 实现定时按什么值得买的好价排行榜,收集优惠信息并推送至邮箱的功能  |
| 2024/10/22  | 邮箱登陆切换到 stmp ssl 465 端口模式以解决 QQ 邮箱不再支持 stmp 明文模式问题  |
| 2024/10/23  | 支持白名单匹配模式以允许用户设置感兴趣的关键词进行推送 |



