---
name: WSL2 安装配置
category: 系统运维
order: 15
---

## 介绍

WSL2 (Windows Subsystem for Linux 2) 是 Windows 10/11 的原生 Linux 子系统，基于真正的 Linux 内核（而非模拟），提供接近原生的性能和完整的 Linux 环境支持。相比 WSL1，WSL2 在文件系统、系统调用、网络等方面都有显著提升。

核心特点：
- **真实 Linux 内核**：完整的 Linux 5.10+ 内核，原生的系统调用性能
- **完整 Linux 文件系统**：支持所有 Linux 文件权限和符号链接
- **更好的 Windows 文件系统访问**：mnt/ 路径直接挂载 Windows 盘符
- **完全网络隔离**：独立网卡，支持端口转发和防火墙规则
- **便携容器化**：可直接导入导出虚拟硬盘，环境迁移简便

## 语法

### 安装 WSL2

```bash
wsl --install -d Ubuntu-22.04
```
- 描述: 一键安装 WSL2 和指定版本的 Ubuntu 发行版，默认会在后台下载并完成全部配置

### 检查 WSL 状态

```bash
wsl --list --verbose
```
- 描述: 列出已安装的 WSL 发行版，显示状态（是否正在运行）、版本和默认发行版

### 设置默认发行版

```bash
wsl --setdefault Ubuntu-22.04
```
- 描述: 设置指定的发行版为默认启动的 Linux 环境

### 进入发行版

```bash
wsl -d Ubuntu-22.04
```
- 描述: 手动进入指定的 WSL 发行版（不使用默认）

### 更新 WSL2 内核

```bash
wsl --update
```
- 描述: 更新 WSL2 内核到最新版本，支持新特性和性能优化

### 导出发行版

```bash
wsl --export Ubuntu-22.04 Ubuntu-22.04.tar
```
- 描述: 将整个发行版导出为 tar 文件，用于备份或迁移到其他 Windows 机器

### 导入发行版

```bash
wsl --import Ubuntu-22.04 Ubuntu-22.04.tar Ubuntu-22.04.tar
```
- 描述: 从 tar 文件导入发行版，实现环境的完整迁移和复制

### 卸载发行版

```bash
wsl --unregister Ubuntu-22.04
```
- 描述: 完全移除指定的 WSL 发行版及其所有数据

### 停止 WSL2 服务

```bash
wsl --shutdown
```
- 描述: 关闭所有正在运行的 WSL 发行版，释放系统资源

### 访问 Windows 文件

```bash
cd /mnt/c/Users/用户名/Documents
ls -la
```
- 描述: 直接访问 Windows 系统的文件，C 盘挂载在 /mnt/c 下

### 设置 WSL2 内存限制

```bash
wsl --set-default-memory 8GB
```
- 描述: 设置 WSL2 虚拟机内存限制，防止占用过多 Windows 内存

### 查看系统资源使用

```bash
htop
ps aux
df -h
```
- 描述: 在 WSL2 内部查看系统进程、内存和磁盘使用情况

## 示例

### 完整的 WSL2 开发环境搭建

- 描述: 从零开始配置完整的 WSL2 Ubuntu 开发环境

```bash
# 1. 安装 WSL2 和 Ubuntu
wsl --install -d Ubuntu-22.04
echo "等待安装完成，完成后在终端输入 Ubuntu-22.04 进入系统"

# 2. 配置基础环境
sudo apt update && sudo apt upgrade -y
sudo apt install -y build-essential curl git vim zsh

# 3. 安装 Docker for WSL2
# 添加 Docker 官方 GPG 密钥
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# 添加 Docker 官方源
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 安装 Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io

# 4. 配置用户免 sudo 使用 Docker
sudo usermod -aG docker $USER
newgrp docker

# 5. 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 6. 安装 Node.js (LTS版本)
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt install -y nodejs

# 7. 安装 Oh My Zsh
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

# 8. 设置开机自动进入 WSL2
echo 'export WSL2_DEFAULT_USER=$(whoami)' >> ~/.bashrc
echo 'cd /mnt/c/Users/用户名/Documents' >> ~/.zshrc

# 9. 退出并重新进入
exit
echo "现在可以通过 'wsl -d Ubuntu-22.04' 进入开发环境"
```

### WSL2 与 Windows 文件共享配置

- 描述: 优化 WSL2 的文件系统性能，解决 Windows/文件系统慢的问题

```bash
# 1. 检查当前 WSL 版本
wsl --list --verbose

# 2. 如果是 WSL1，转换为 WSL2
wsl --set-version Ubuntu-22.04 2

# 3. 优化 Windows 文件系统挂载
# 在 ~/.bashrc 或 ~/.zshrc 中添加：
echo '# 优化 Windows 文件系统挂载' >> ~/.bashrc
echo 'export WSLENV=WT_SESSION:' >> ~/.bashrc
echo 'export HOME=/home/'$(whoami)'' >> ~/.bashrc

# 4. 创建符号链接到 Windows 快捷方式
ln -s /mnt/c/Users/用户名/Desktop ~/Desktop
ln -s /mnt/c/Users/用户名/Documents ~/Documents

# 5. 配置 SSH 服务（远程访问）
sudo apt install -y openssh-server
sudo systemctl start ssh
sudo systemctl enable ssh

# 6. 获取 WSL2 IP 地址并配置端口转发
# 在 Windows PowerShell 中运行：
# netsh interface portproxy add v4tov4 listenport=2222 listenaddress=0.0.0.0 connectport=22 connectaddress=$(wsl hostname -I | cut -d' ' -f1)
```

### WSL2 容器化项目部署

- 描述: 使用 Docker 在 WSL2 中部署完整的 Web 应用

```bash
# 1. 创建项目目录
mkdir ~/projects && cd ~/projects

# 2. 创建 Docker Compose 配置文件
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  web:
    build: .
    ports:
      - "3000:3000"
    volumes:
      - .:/app
      - /app/node_modules
    environment:
      - NODE_ENV=development
    depends_on:
      - db
  db:
    image: postgres:14
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
EOF

# 3. 创建 Dockerfile
cat > Dockerfile << 'EOF'
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
EOF

# 4. 初始化 Node.js 项目
npm init -y
npm install express mongoose cors

# 5. 启动容器
docker-compose up -d

# 6. 查看容器状态
docker-compose ps
docker logs -f web
```

### WSL2 性能优化脚本

- 描述: 优化 WSL2 系统性能和资源使用

```bash
#!/bin/bash
# WSL2 优化脚本

# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 清理包缓存
sudo apt autoremove -y
sudo apt autoclean

# 3. 安装性能监控工具
sudo apt install -y htop iotop iftop

# 4. 配置 ZSH 插件
# 如果使用 Oh My Zsh，安装常用插件
git clone https://github.com/zsh-users/zsh-syntax-highlighting.git ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting
git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions

# 更新 .zshrc 启用插件
sed -i 's/plugins=(git)/plugins=(git zsh-syntax-highlighting zsh-autosuggestions)/' ~/.zshrc

# 5. 配置 Git
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
git config --global core.editor "vim"

# 6. 创建开发快捷脚本
mkdir -p ~/bin

cat > ~/bin/update-dev.sh << 'EOF'
#!/bin/bash
echo "更新开发环境..."
sudo apt update && sudo apt upgrade -y
echo "更新 Node.js 包..."
npm update -g
echo "清理 Docker..."
docker system prune -f
echo "优化磁盘..."
sudo apt autoremove -y && sudo apt autoclean
EOF

chmod +x ~/bin/update-dev.sh

echo "WSL2 优化完成！"
echo "使用方法："
echo "  1. Ubuntu-22.04 进入 WSL2"
echo "  2. update-dev.sh 更新开发环境"
echo "  3. htop 监控系统资源"
```

### WSL2 跨平台开发工作流

- 描述: 配置统一的开发环境，支持 Windows、WSL2、Docker 三端协同

```bash
# 1. 创建项目同步配置
# 在 Windows 创建项目目录，WSL2 同步链接
mkdir -p ~/Projects/Sync
cd ~/Projects/Sync

# 2. 创建跨平台项目管理脚本
cat > manage-project.sh << 'EOF'
#!/bin/bash
# WSL2 项目管理脚本

PROJECT_NAME=${1:-}
ACTION=${2:-}

if [[ -z "$PROJECT_NAME" ]]; then
    echo "用法: $0 <项目名> [start|stop|status]"
    exit 1
fi

PROJECT_DIR="/mnt/c/Users/用户名/Projects/$PROJECT_NAME"
WSL_DIR="/home/$(whoami)/Projects/$PROJECT_NAME"

case $ACTION in
    "start")
        if [[ ! -d "$WSL_DIR" ]]; then
            ln -s "$PROJECT_DIR" "$WSL_DIR"
            echo "已链接项目: $PROJECT_NAME"
        fi
        cd "$WSL_DIR"
        echo "进入项目目录: $(pwd)"
        ;;
    "stop")
        cd ~
        echo "退出项目目录"
        ;;
    "status")
        if [[ -L "$WSL_DIR" ]]; then
            echo "项目 $PROJECT_NAME 已链接: $WSL_DIR -> $(readlink -f $WSL_DIR)"
        else
            echo "项目 $PROJECT_NAME 未链接"
        fi
        ;;
    *)
        echo "未知操作: $ACTION"
        exit 1
        ;;
esac
EOF

chmod +x manage-project.sh

# 3. 创建环境变量配置
cat > ~/.env << 'EOF'
# WSL2 开发环境变量
export PROJECT_HOME="/home/$(whoami)/Projects"
export CODE_DIR="/mnt/c/Users/用户名/Projects"
export DOCKER_HOST=unix:///var/run/docker.sock
export PATH="$HOME/bin:$PATH"

# 开发工具别名
alias ll='ls -la'
alias gs='git status'
alias gp='git pull'
alias gc='git commit -m'
alias dc='docker-compose'
alias dps='docker ps'
alias dpsa='docker ps -a'

# 快速进入项目
alias work='manage-project.sh start'
alias leave='manage-project.sh stop'
EOF

# 4. 初始化常用项目
cd ~/Projects
mkdir -p Web Development DevOps System
echo "项目目录结构已创建："
echo "  Web/      - Web开发项目"
echo "  Development/ - 学习项目"
echo "  DevOps/   - DevOps工具项目"
echo "  System/   - 系统工具项目"

# 5. 创建快速启动脚本
cat > start-dev.sh << 'EOF'
#!/bin/bash
echo "启动 WSL2 开发环境..."

# 检查 Docker 服务
if ! sudo systemctl is-active --quiet docker; then
    echo "启动 Docker 服务..."
    sudo systemctl start docker
fi

# 启动开发服务
docker-compose up -d

# 打开浏览器
if command -v firefox >/dev/null 2>&1; then
    firefox http://localhost:3000 &
elif command -v google-chrome >/dev/null 2>&1; then
    google-chrome http://localhost:3000 &
fi

echo "开发环境已启动！"
echo "访问地址: http://localhost:3000"
echo "按 Ctrl+C 停止"
EOF

chmod +x start-dev.sh

echo "跨平台开发工作流配置完成！"
echo "使用方法："
echo "  1. 在 Windows 中创建项目到 /mnt/c/Users/用户名/Projects/"
echo "  2. WSL2 中运行 'work 项目名' 链接项目"
echo "  3. start-dev.sh 启动开发环境"
echo "  4. leave 退出项目"
```

### WSL2 问题排查和故障恢复

- 描述: WSL2 常见问题的排查和解决方案

```bash
#!/bin/bash
# WSL2 问题排查脚本

echo "=== WSL2 系统状态检查 ==="

# 1. 检查 WSL 版本
echo "1. WSL 发行版列表："
wsl --list --verbose

# 2. 检查磁盘空间
echo "2. 磁盘使用情况："
df -h

# 3. 检查内存使用
echo "3. 内存使用情况："
free -h

# 4. 检查网络连接
echo "4. 网络连接状态："
ip addr show

# 5. 检查 Docker 服务
echo "5. Docker 服务状态："
sudo systemctl status docker --no-pager

# 6. 检查 WSL2 虚拟机大小
echo "6. WSL2 虚拟机文件："
ls -la ~/.local/share/WSL/

# 7. 常见问题修复
echo "=== 常见问题修复 ==="

# 问题1: 文件系统性能差
fix_fs_performance() {
    echo "修复文件系统性能问题..."
    # 检查当前是否已优化
    if ! grep -q "WSL2" /etc/fstab; then
        echo "添加文件系统优化配置..."
        echo "# WSL2 文件系统优化" | sudo tee -a /etc/fstab
        echo "none /mnt/c drvfs metadata=8075,fuid=2,gid=2 0 0" | sudo tee -a /etc/fstab
        sudo mount -a
    fi
}

# 问题2: Docker 启动失败
fix_docker() {
    echo "修复 Docker 启动问题..."
    sudo systemctl daemon-reload
    sudo systemctl restart docker
    sudo usermod -aG docker $USER
    newgrp docker
}

# 问题3: 内存不足
fix_memory() {
    echo "检查内存限制..."
    if [[ $(ulimit -v) -gt 8589934592 ]]; then
        echo "设置内存限制为 8GB"
        echo "* soft memlock 8589934592" | sudo tee -a /etc/security/limits.conf
        echo "* hard memlock 8589934592" | sudo tee -a /etc/security/limits.conf
    fi
}

# 执行修复选项
read -p "选择修复选项 (fs/docker/memory/all): " choice
case $choice in
    "fs") fix_fs_performance ;;
    "docker") fix_docker ;;
    "memory") fix_memory ;;
    "all")
        fix_fs_performance
        fix_docker
        fix_memory
        ;;
    *)
        echo "未选择修复选项"
        ;;
esac

echo "修复完成！"
```

### WSL2 自动化备份脚本

- 描述: 自动备份 WSL2 发行版和重要数据

```bash
#!/bin/bash
# WSL2 自动备份脚本

BACKUP_DIR="/mnt/c/Users/用户名/WSL2-Backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_NAME="Ubuntu-22.04_${TIMESTAMP}"

echo "开始 WSL2 备份: $BACKUP_NAME"

# 1. 创建备份目录
mkdir -p "$BACKUP_DIR"

# 2. 导出 WSL 发行版
echo "导出 WSL 发行版..."
wsl --export Ubuntu-22.04 "$BACKUP_DIR/${BACKUP_NAME}.tar"

# 3. 备份重要配置文件
echo "备份配置文件..."
tar -czf "$BACKUP_DIR/${BACKUP_NAME}_config.tar.gz" \
    ~/.bashrc ~/.zshrc \
    ~/.ssh \
    ~/.gitconfig \
    ~/.docker \
    ~/.local/bin

# 4. 备份项目数据
echo "备份项目数据..."
tar -czf "$BACKUP_DIR/${BACKUP_NAME}_projects.tar.gz" \
    ~/Projects

# 5. 创建备份清单
echo "创建备份清单..."
cat > "$BACKUP_DIR/${BACKUP_NAME}_manifest.txt" << EOF
WSL2 发行版备份清单
备份时间: $(date)
备份版本: Ubuntu-22.04
备份内容:
  - ${BACKUP_NAME}.tar - 完整的 WSL2 发行版
  - ${BACKUP_NAME}_config.tar.gz - 用户配置文件
  - ${BACKUP_NAME}_projects.tar.gz - 项目数据
备份大小:
  $(du -h "$BACKUP_DIR/${BACKUP_NAME}.tar")
  $(du -h "$BACKUP_DIR/${BACKUP_NAME}_config.tar.gz")
  $(du -h "$BACKUP_DIR/${BACKUP_NAME}_projects.tar.gz")
EOF

# 6. 清理旧备份（保留最近7天）
echo "清理旧备份..."
find "$BACKUP_DIR" -name "Ubuntu-22.04_*.tar" -mtime +7 -delete
find "$BACKUP_DIR" -name "Ubuntu-22.04_*_config.tar.gz" -mtime +7 -delete
find "$BACKUP_DIR" -name "Ubuntu-22.04_*_projects.tar.gz" -mtime +7 -delete
find "$BACKUP_DIR" -name "Ubuntu-22.04_*_manifest.txt" -mtime +7 -delete

# 7. 生成恢复脚本
cat > "$BACKUP_DIR/restore_${BACKUP_NAME}.sh" << EOF
#!/bin/bash
# WSL2 恢复脚本
# 恢复备份: $BACKUP_NAME

echo "开始恢复 WSL2 备份..."

# 1. 导入 WSL 发行版
echo "导入 WSL 发行版..."
wsl --import Ubuntu-22.04 Ubuntu-22.04.tar "$BACKUP_DIR/${BACKUP_NAME}.tar"

# 2. 恢复配置文件
echo "恢复配置文件..."
tar -xzf "$BACKUP_DIR/${BACKUP_NAME}_config.tar.gz" -C ~/

# 3. 恢复项目数据
echo "恢复项目数据..."
mkdir -p ~/Projects
tar -xzf "$BACKUP_DIR/${BACKUP_NAME}_projects.tar.gz" -C ~/

# 4. 设置权限
chmod 600 ~/.ssh/*
chmod 755 ~/.local/bin/*

echo "恢复完成！"
echo "使用方法：wsl -d Ubuntu-22.04 进入恢复的环境"
EOF

chmod +x "$BACKUP_DIR/restore_${BACKUP_NAME}.sh"

echo "备份完成！"
echo "备份位置: $BACKUP_DIR"
echo "恢复脚本: $BACKUP_DIR/restore_${BACKUP_NAME}.sh"

# 8. 创建定时任务示例
cat > "$BACKUP_DIR/backup_crontab_example.txt" << EOF
# WSL2 自动备份定时任务（每小时执行一次）
0 * * * * $HOME/backup_wsl2.sh >> $BACKUP_DIR/backup.log 2>&1

# 或者每天凌晨2点执行
0 2 * * * $HOME/backup_wsl2.sh >> $BACKUP_DIR/backup.log 2>&1

# 或者每周日凌晨执行
0 2 * * 0 $HOME/backup_wsl2.sh >> $BACKUP_DIR/backup.log 2>&1
EOF

echo "定时任务示例已创建: $BACKUP_DIR/backup_crontab_example.txt"
```