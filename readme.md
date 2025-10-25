# 项目简介

从什么值得买的好价排行榜中收集优惠信息,并推送至邮箱或微信   
推送内容包含商品图片、标题、价格、点值的数量、评论数量等信息, 点击商品标题的超链接即可跳转至什么值得买查看正文   
项目运行过程中会在根目录下自动创建`database.db`数据库文件,数据库中会记录已推送的优惠信息,避免重复推送   

---
技术框架: Java11 + Maven + SqlLite + Hibernate   
支持GitHub Actions定时运行,无需搭建服务器   
有改进的建议可以提issue给我. 麻烦给个star支持一下:heart:

# GitHub Actions运行

*  fork本仓库
*  新增Actions secrets. 选择邮箱推送需要填写`EMAILACCOUNT`和`EMAILPASSWORD`.选择微信推送需要填写`SPT`

| secret        |   | 说明                                                                                                                                   |
|---------------|---|--------------------------------------------------------------------------------------------------------------------------------------|
| EMAILACCOUNT  | 选填 | 接收优惠信息的邮箱                                                                                                                            |
| EMAILPASSWORD | 选填 | 邮箱的授权码,[参考qq邮箱的这篇文档](https://service.mail.qq.com/cgi-bin/help?subtype=1&&id=28&&no=1001256)                                          |
| SPT           | 选填 | WxPusher极简推送使用的身份ID,[参考WxPusher文档](https://wxpusher.zjiecode.com/docs/#/?id=spt)                                                     |
| GIT_TOKEN     | 必填 | [参考这篇文章的1-6步骤](https://zhuanlan.zhihu.com/p/501872439),只勾选repo的权限,Expiration设置为No Expiration                                         |
| COOKIE        | 选填 | 请求什么值得买服务器时请求头携带的cookie参数. 不填的话会用selenium模拟浏览器行为自动获取cookie(推荐), 自动的代码失效时再考虑填写固定的cookie值(请使用F12查看cookie值, 并确保不要将cookie明文泄漏出去) |

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdmActions.png" width = "700" height = "350" alt="图片名称" align=center />


* 打开fork项目的workFlow开关

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/enableWorkFlow.png" width = "700" height = "350" alt="图片名称" align=center />

* 修改`./.github/workflows/zdm_crawler.yml`文件中红框所示的内容

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdm%E4%BF%AE%E6%94%B9actions%E9%85%8D%E7%BD%AE.png" alt="图片名称" align=center />


* 手动触发一次,测试下能不能跑通

<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/runWorkFlow.png" width = "500" height = "200" alt="图片名称" align=center />



**成功运行的截图:**   
<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdm%E6%88%90%E5%8A%9F%E8%BF%90%E8%A1%8C%E6%88%AA%E5%9B%BE.png" width = "700" height = "400" alt="图片名称" align=center />



<img src="https://raw.githubusercontent.com/lx1169732264/Images/master/zdm%E9%82%AE%E7%AE%B1%E6%88%AA%E5%9B%BE.png" width = "700" height = "400" alt="图片名称" align=center />

* [我自己也fork了一份](https://github.com/PhantomStrikers/zdm),每天都在自动运行的,可以通过这个项目的actions运行记录判断这个项目是否还能work

# 本地运行方式
将GitHub Actions章节中的环境变量配置到本地即可

# 自定义过滤逻辑

### 值/评论的数量过滤
在`./.github/workflows/zdm_crawler.yml`文件中可以配置`minVoted`和`minComments`, 小于该数量的优惠信息将不进行推送

### 标题黑白名单过滤

`white_words.txt`文件中可以自定义白名单关键词;`black_words.txt`文件中可以自定义黑名单关键词;. **多个关键词之间需要换行**   
* 若白名单不为空, 仅会推送优惠信息的标题包含白名单中任意一个关键词的商品信息   
* 若黑名单不为空, 仅会推送优惠信息的标题不包含黑名单中任意一个关键词的商品信息
* 在同时配置了黑白名单两个文件时, 只有白名单会生效. 在两个文件内容都为空时, 代表关闭过滤规则


# CHANGELOG

| 日期         | 说明                                                                                                            |
|------------|---------------------------------------------------------------------------------------------------------------|
| 2025/10/25 | 1.添加参数隐藏Selenium自动化特征 2.重试接口会大幅增加间隔时间  3.调低了环境变量`maxPageSize`最大翻页数量  4.删除环境变量`cookie`,最近检测变严格了,固定的cookie是走不通的 |
| 2025/08/28 | 处理超时异常                                                                                                        |
| 2025/08/19 | 1.增加cookie自动刷新机制; 2.调用接口支持重试                                                                                  |
| 2025/08/08 | 1.用selenium模拟浏览器行为自动获取cookie; 2.调用什么值得买的分页接口增加随机延时                                                            |
| 2025/08/06 | 请求头增加自定义的cookie参数                                                                                             |
| 2025/05/08 | 将接口调用的工具类从cn.hutool.http.HttpUtil切换到java.net.http.HttpRequest                                                 |
| 2025/01/02 | 用SqlLite改写已推送优惠信息的记录方式,升级JDK版本到11                                                                             |
| 2024/11/22 | 1.新增WxPusher推送方式; 2.为了适应移动端小屏幕展示效果,将跳转至什么值得买的超链接从详情列改为商品标题列,并移除了详情列,让表格的其余列有更充足的宽度                            |
| 2024/10/23 | 支持白名单匹配模式以允许用户设置感兴趣的关键词进行推送                                                                                   |
| 2024/10/22 | 邮箱登陆切换到 stmp ssl 465 端口模式以解决 QQ 邮箱不再支持 stmp 明文模式问题                                                            |
| 2023/5/4   | 已推送优惠信息按日期在logs文件夹下归类记录.避免单个文件记录数据量过大的问题                                                                      |
| 2023/1/31  | 实现定时按什么值得买的好价排行榜,收集优惠信息并推送至邮箱的功能                                                                              |


