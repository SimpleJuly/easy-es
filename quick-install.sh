#!/bin/bash

# Easy-ES 快速本地安装脚本（跳过测试和文档生成）

echo "🚀 开始快速安装 Easy-ES 到本地仓库（跳过测试和文档生成）..."
echo ""

# -DskipTests: 跳过测试
# -Dmaven.javadoc.skip=true: 跳过 Javadoc 生成
# -T 1C: 使用多线程构建（每个 CPU 核心一个线程）
mvn clean install -DskipTests -Dmaven.javadoc.skip=true -T 1C

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 安装成功！版本: 3.1.0"
    echo ""
else
    echo ""
    echo "❌ 安装失败，请查看错误信息"
    exit 1
fi
