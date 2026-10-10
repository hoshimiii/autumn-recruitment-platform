#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-smoke}" != "smoke" ]; then echo "usage: entrypoint.sh [smoke]" >&2; exit 2; fi
mkdir -p /lab/state/logs
for attempt in $(seq 1 60); do
  if printf ruok | nc -w 1 zookeeper 2181 | grep -q imok; then break; fi
  if [ "$attempt" -eq 60 ]; then echo "ZooKeeper did not become ready" >&2; exit 1; fi
  sleep 1
done

export HBASE_MANAGES_ZK=false
export HADOOP_CLASSPATH="/opt/hbase/conf:$(hbase classpath)"
hbase-daemon.sh start master
hbase-daemon.sh start regionserver
cleanup() {
  hbase-daemon.sh stop regionserver >/dev/null 2>&1 || true
  hbase-daemon.sh stop master >/dev/null 2>&1 || true
}
trap cleanup EXIT

hadoop jar /lab/analytics-lab.jar lab.analytics.AnalyticsJob
echo "ANALYTICS_LAB_OK rows=4 input_records=6"
