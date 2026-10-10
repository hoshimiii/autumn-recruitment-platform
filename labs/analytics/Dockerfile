FROM maven:3.9.9-eclipse-temurin-17@sha256:f58d59b6273e785ac0a4477f6e9b5ba1d7731c75b906c0f7b34076f1851318cc AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q test package

FROM eclipse-temurin:17-jre-jammy@sha256:8993f1aed8b25fcea7a7047a7949c1866fa558fc6830d938c22c4f13b26be9d7
ARG HADOOP_VERSION=3.4.2
ARG HADOOP_SHA512=53582b3dae515316933c8486cdbd688a93530deb4bcc842e6a599f33dfeb51b17d79f61dc15b6febe95715325c2d2f4fc503bfae6ec3226851644ba7ebae8930
ARG HBASE_VERSION=2.6.6
ARG HBASE_SHA512=665f944952d3afe9efc79dcce4e366b017f9c18eab1942ca34e679edd10abff70374695c5a7b7a506492002f345047e30bb4c586066a4769160764ab6c16f9c7
ENV JAVA_HOME=/opt/java/openjdk HADOOP_HOME=/opt/hadoop HBASE_HOME=/opt/hbase HBASE_CONF_DIR=/opt/hbase/conf HADOOP_HEAPSIZE=256 HBASE_HEAPSIZE=384 HBASE_MASTER_OPTS=-Xmx384m HBASE_REGIONSERVER_OPTS=-Xmx512m HBASE_LOG_DIR=/lab/state/logs
ENV PATH="${HADOOP_HOME}/bin:${HBASE_HOME}/bin:${PATH}"
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl netcat-openbsd \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL "https://downloads.apache.org/hadoop/common/hadoop-${HADOOP_VERSION}/hadoop-${HADOOP_VERSION}-lean.tar.gz" -o /tmp/hadoop.tgz \
    && echo "${HADOOP_SHA512}  /tmp/hadoop.tgz" | sha512sum -c - \
    && mkdir -p /opt/hadoop \
    && tar -xzf /tmp/hadoop.tgz --strip-components=1 -C /opt/hadoop \
    && curl -fsSL "https://archive.apache.org/dist/hbase/${HBASE_VERSION}/hbase-${HBASE_VERSION}-hadoop3-bin.tar.gz" -o /tmp/hbase.tgz \
    && echo "${HBASE_SHA512}  /tmp/hbase.tgz" | sha512sum -c - \
    && mkdir -p /opt/hbase \
    && tar -xzf /tmp/hbase.tgz --strip-components=1 -C /opt/hbase \
    && rm -f /tmp/hadoop.tgz /tmp/hbase.tgz
COPY hbase-site.xml /opt/hbase/conf/hbase-site.xml
COPY --from=build /src/target/analytics-lab.jar /lab/analytics-lab.jar
COPY data /lab/data
COPY entrypoint.sh /lab/entrypoint.sh
RUN chmod 755 /lab/entrypoint.sh
WORKDIR /lab
ENTRYPOINT ["/lab/entrypoint.sh"]
