#!/usr/bin/env bash
# 包装脚本：约定把下载解压后的 Nacos 放在本目录的 nacos/ 下
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NACOS_HOME="$SCRIPT_DIR/nacos"
if [ ! -f "$NACOS_HOME/bin/startup.sh" ]; then
  echo "[error] cannot find nacos binary."
  echo "download nacos-server-2.2.3.tar.gz and extract into:"
  echo "  $NACOS_HOME"
  echo "url: https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.tar.gz"
  exit 1
fi
echo "[start] starting nacos in standalone mode..."
bash "$NACOS_HOME/bin/startup.sh" -m standalone
echo "[done] console: http://REDACTED:8848/nacos"
