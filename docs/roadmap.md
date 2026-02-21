# 开发路线图 / Development Roadmap

## V1.0 — MVP（当前版本）

- [x] Milestone A：项目骨架（账户/钱包/来源管理）
- [ ] Milestone B：手动记账（金额键盘、分类选择、转账）
- [ ] Milestone C：首页 Dashboard（月度概览、余额卡片、饼图）
- [ ] Milestone D：账单列表与搜索
- [ ] Milestone E：CSV 导入（Revolut / Wise / Poste Italiane）
- [ ] Milestone F：通知监听（微信/支付宝）
- [ ] Milestone F2：极速记账（桌面小组件、App Shortcuts）
- [ ] Milestone G：打包交付

## V2 — 银行自动同步

- 接入 GoCardless Bank Account Data（免费层）
- 搭建轻量后端（Supabase 或 Cloudflare Workers）
- 支持 Revolut、Poste Italiane、Wise 自动同步交易
- 上架 Google Play 后配置 GoCardless Production 密钥

## V3 — 报表与智能化

- 月度/分类饼图、趋势折线图
- ECB 实时汇率自动同步
- 预算设置与超支提醒
- 规则引擎升级（正则、商户字典、优先级）
- 云端同步 + 多设备支持（需要用户账户体系）

## V4 — 扩展

- OCR 拍小票识别
- AI 自动分类（基于历史记录）
- 多用户协作账本（例如家庭共享）
