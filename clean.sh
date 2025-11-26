#!/bin/bash

# Easy-ES 清理脚本
# 清理 Maven 编译缓存和目标文件

echo "🧹 开始清理 Easy-ES 项目..."
echo ""

# 清理所有模块的 target 目录
echo "📦 清理所有 target 目录..."
mvn clean

if [ $? -eq 0 ]; then
    echo "✅ target 目录清理完成"
else
    echo "❌ 清理失败"
    exit 1
fi

echo ""
echo "🗑️  是否清理本地 Maven 仓库中的 easy-es 缓存?"
read -p "这将删除 ~/.m2/repository/org/dromara/easy-es/ (y/N): " clean_local_repo
clean_local_repo=${clean_local_repo:-N}

if [[ $clean_local_repo =~ ^[Yy]$ ]]; then
    echo "正在清理本地仓库..."
    rm -rf ~/.m2/repository/org/dromara/easy-es/
    if [ $? -eq 0 ]; then
        echo "✅ 本地仓库缓存清理完成"
    else
        echo "⚠️  本地仓库缓存清理失败（可能不存在）"
    fi
else
    echo "⏭️  跳过本地仓库清理"
fi

echo ""
echo "✅ 清理完成！"
echo ""
echo "💡 提示: 现在可以运行 ./quick-install.sh 重新安装"
