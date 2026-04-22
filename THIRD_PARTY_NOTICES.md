# Third-Party Notices Draft

本文件是仓库当前直接依赖的初版第三方依赖说明草稿，用于商业交付前的许可证盘点。

说明：

- 这是草稿，不是完整的法务结论。
- 当前只覆盖仓库中最核心、最直接的依赖与风险点。
- 传递依赖、前端构建链与运行时镜像中的完整清单，仍建议在发版前再导出一次正式 SBOM 或许可证清单。

## 已确认可开源商用的核心直接依赖

| 模块 | 依赖 | 当前项目版本 | 许可证 | 官方来源 |
| --- | --- | --- | --- | --- |
| backend-api | Spring Boot | 4.0.3 | Apache-2.0 | https://github.com/spring-projects/spring-boot |
| backend-api | Aliyun OSS SDK for Java | 3.17.4 | Apache-2.0 | https://github.com/aliyun/aliyun-oss-java-sdk |
| backend-api | WxJava / weixin-java-miniapp | 4.7.0 | Apache-2.0 | https://github.com/binarywang/WxJava |
| backend-api | EasyExcel | 3.3.2 | Apache-2.0 | https://github.com/alibaba/easyexcel |
| admin-web | Vue | ^3.5.25 | MIT | https://github.com/vuejs/core |
| admin-web | Element Plus | ^2.13.2 | MIT | https://github.com/element-plus/element-plus |
| admin-web | Vite | ^7.3.1 | MIT | https://github.com/vitejs/vite |
| miniprogram | Vue | ^3.4.21 | MIT | https://github.com/vuejs/core |
| miniprogram | Vite | 5.2.8 | MIT | https://github.com/vitejs/vite |

## 需要额外法务/合规复核的直接依赖

| 模块 | 依赖 | 当前项目版本 | 风险点 | 官方来源 |
| --- | --- | --- | --- | --- |
| backend-api | MySQL Connector/J | Spring Boot 管理版本 | GPLv2 + Universal FOSS Exception。若闭源对外交付，需要重点确认分发边界。 | https://github.com/mysql/mysql-connector-j/blob/release/9.x/LICENSE |
| miniprogram | uni-app / `@dcloudio/*` | 3.0.0-4080720251210001 | 并非单纯 MIT/Apache-2.0，需遵守 DCloud 官方许可协议，特别是 App/Runtime 相关场景。 | https://dcloud.io/license/uni-app.html |

## 仓库层面的高优先级合规风险

1. 仓库包含外部站点抓取逻辑，涉及懂车帝与工信部站点的数据抓取、图片下载、HTML 存档与验证码自动化，商业使用前应确认站点条款、数据授权与展示范围。
2. 仓库曾出现生产数据库密码、OSS AccessKey、微信小程序密钥等敏感信息，需按“已泄露”处理并完成轮换。
3. 仓库当前没有项目级 `LICENSE` / `NOTICE` 正式文件，也没有完整的第三方归因清单；商业交付前应补齐。

## 待补充清单

- backend-api 其余直接依赖：Jackson、JJWT、Lombok、H2
- admin-web 其余直接依赖：Pinia、Vue Router
- miniprogram 其余直接依赖：Vue I18n、其余 `@dcloudio/*`
- 构建链依赖：TypeScript、`@vitejs/plugin-vue`、`vue-tsc` 等
