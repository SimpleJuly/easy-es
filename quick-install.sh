#!/bin/bash

# Easy-ES 快速本地安装脚本（跳过测试）

echo "🚀 开始快速安装 Easy-ES 到本地仓库（跳过测试）..."
echo ""

mvn clean install -DskipTests -T 1C

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 安装成功！版本: 3.1.0"
    echo ""
else
    echo ""
    echo "❌ 安装失败，请查看错误信息"
    exit 1
fi
