# 高校教材订购与库存全流程管理系统

在线演示（支持三角色免密体验）：[https://textbook.layyy.site/](https://textbook.layyy.site/)

基于 Spring Boot 3 与 Vue 3 构建的前后端分离教务教材业务管理系统，覆盖教材目录维护、师生教材征订、缺书自动登记挂起、采购审批补货及出入库流水核销的完整业务闭环。

## 技术栈

- **后端**：Java 17、Spring Boot 3、Spring Security + JWT 认证、MyBatis-Plus、H2（默认免配置演示模式）/ MySQL 8.0（生产模式）
- **前端**：Vue 3、Vite、Pinia、Vue Router、Element Plus（纯白极简定制主题）

## 业务流程闭环与角色权限

1. **多角色权限隔离**：内置教务管理员（`ADMIN`）、任课教师（`TEACHER`）、学生代表（`STUDENT`）、书库管理员（`STAFF`）与采购专员（`PURCHASER`）五类角色。登录页提供访客一键快捷登录按钮，便于直接体验不同角色菜单与审批流。
2. **订单与库存联动**：提交征订订单并审核时自动核验中心书库实时库存；若库存不足则自动生成缺书登记单并挂起订单，待采购专员完成补货入库后即可继续发放签收。

## 快速构建与启动

### 1. 构建前端并合入后端单包托管（可选）

```bash
cd frontend
npm install
npm run build
```

### 2. 启动后端服务（默认 `demo` Profile 自动初始化 H2 内存库与 12 套专业教材演示数据）

```bash
cd backend
./mvnw spring-boot:run
```

默认监听端口 `8080`；如需连接外部 MySQL 8.0，设置环境变量 `SPRING_PROFILES_ACTIVE=mysql` 并执行 `backend/sql/mysql-init.sql` 即可。
