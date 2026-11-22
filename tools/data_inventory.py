# -*- coding: utf-8 -*-
"""盘点各业务表是否有可供手动测试的实例数据。"""
import subprocess

M = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

CHECKS = [
    ("zk_user.user", "学员/员工账号"),
    ("zk_user.teacher", "讲师"),
    ("zk_course.course", "课程"),
    ("zk_course.course_catalogue", "课程目录(章/节)"),
    ("zk_course.category", "课程分类"),
    ("zk_course.course_teacher", "课程-讲师"),
    ("zk_exam.question", "题目"),
    ("zk_learning.lesson", "我的课表"),
    ("zk_learning.learning_record", "学习记录"),
    ("zk_learning.note", "笔记"),
    ("zk_learning.interaction_question", "提问"),
    ("zk_learning.interaction_reply", "回答"),
    ("zk_learning.sign_record", "签到"),
    ("zk_learning.learning_plan", "学习计划"),
    ("zk_trade.order", "订单"),
    ("zk_trade.order_detail", "订单明细"),
    ("zk_trade.cart", "购物车"),
    ("zk_trade.refund_apply", "退款申请"),
    ("zk_pay.pay_order", "支付单"),
    ("zk_pay.pay_channel", "支付渠道"),
    ("zk_promotion.coupon", "优惠券"),
    ("zk_promotion.user_coupon", "用户券"),
    ("zk_promotion.exchange_code", "兑换码"),
    ("zk_cert.certificate", "证书"),
    ("zk_exam.wrong_question", "错题"),
    ("zk_media.media", "视频"),
    ("zk_media.file", "文件"),
    ("zk_message.user_inbox", "站内信"),
    ("zk_message.notice_task", "通知任务"),
    ("zk_message.message_template", "消息模板"),
    ("zk_remark.liked_record", "点赞"),
]


def q(sql):
    r = subprocess.run([M, "-h", "192.168.150.101", "-uroot", "-p123", "-N", "-B",
                        "--default-character-set=utf8mb4", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="ignore")
    return (r.stdout or r.stderr).strip()


print("%-38s %-22s %s" % ("表", "含义", "行数"))
missing = []
for table, label in CHECKS:
    out = q("select count(*) from %s" % table)
    if out.isdigit():
        n = int(out)
        print("%-38s %-22s %d" % (table, label, n))
        if n == 0:
            missing.append((table, label))
    else:
        print("%-38s %-22s (表不存在或查询失败)" % (table, label))
print("\n空表（需要补种子数据）:", [t for t, _ in missing] or "无")
