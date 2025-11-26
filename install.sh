#!/bin/bash

# Easy-ES 本地安装脚本
# 用于将项目安装到本地 Maven 仓库

echo "=========================================="
echo "  Easy-ES 本地安装脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Maven 是否已安装
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}错误: 未找到 Maven 命令，请先安装 Maven${NC}"
    exit 1
fi

# 显示 Maven 版本
echo -e "${YELLOW}Maven 版本:${NC}"
mvn -version
echo ""

# 询问是否跳过测试
read -p "是否跳过测试? (Y/n): " skip_tests
skip_tests=${skip_tests:-Y}

if [[ $skip_tests =~ ^[Yy]$ ]]; then
    SKIP_TESTS="-DskipTests"
    echo -e "${YELLOW}将跳过测试${NC}"
else
    SKIP_TESTS=""
    echo -e "${YELLOW}将执行测试${NC}"
fi

echo ""
echo -e "${YELLOW}开始执行 Maven 安装...${NC}"
echo ""

# 执行 Maven 安装
mvn clean install $SKIP_TESTS

# 检查执行结果
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=========================================="
    echo -e "  安装成功！"
    echo -e "==========================================${NC}"
    echo ""
    echo "现在可以在其他项目中使用以下依赖："
    echo ""
    echo "<dependency>"
    echo "    <groupId>org.dromara.easy-es</groupId>"
    echo "    <artifactId>easy-es-boot-starter</artifactId>"
    echo "    <version>3.1.0</version>"
    echo "</dependency>"
    echo ""
else
    echo ""
    echo -e "${RED}=========================================="
    echo -e "  安装失败！"
    echo -e "==========================================${NC}"
    echo ""
    echo "请检查错误信息并修复问题后重试"
    exit 1
fi
