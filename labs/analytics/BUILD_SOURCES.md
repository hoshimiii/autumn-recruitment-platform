# Pinned build sources

The Dockerfile and Compose file pin the three Docker Official Image multi-arch
indexes by digest. Tags are included for readability; the digest controls the
actual image selection.

| Purpose | Pinned source |
| --- | --- |
| Maven + JDK 17 build stage | `maven:3.9.9-eclipse-temurin-17@sha256:f58d59b6273e785ac0a4477f6e9b5ba1d7731c75b906c0f7b34076f1851318cc` |
| Eclipse Temurin JDK 17 runtime | `eclipse-temurin:17-jre-jammy@sha256:8993f1aed8b25fcea7a7047a7949c1866fa558fc6830d938c22c4f13b26be9d7` |
| ZooKeeper service | `zookeeper:3.9.6-jre-17@sha256:d0ae1665a1e85022261dc05a10e030c83895060401cd5c1bf990ed6b4d38c31b` |

The image build downloads only these Apache binary artifacts and checks each
against its published SHA-512 value before extraction:

| Artifact | Official Apache asset | SHA-512 |
| --- | --- | --- |
| Hadoop 3.4.2 lean binary | [downloads.apache.org](https://downloads.apache.org/hadoop/common/hadoop-3.4.2/hadoop-3.4.2-lean.tar.gz) | `53582b3dae515316933c8486cdbd688a93530deb4bcc842e6a599f33dfeb51b17d79f61dc15b6febe95715325c2d2f4fc503bfae6ec3226851644ba7ebae8930` |
| HBase 2.6.6 Hadoop 3 binary | [archive.apache.org](https://archive.apache.org/dist/hbase/2.6.6/hbase-2.6.6-hadoop3-bin.tar.gz) | `665f944952d3afe9efc79dcce4e366b017f9c18eab1942ca34e679edd10abff70374695c5a7b7a506492002f345047e30bb4c586066a4769160764ab6c16f9c7` |

The versions follow the HBase compatibility guide: HBase 2.6.2+ supports
Hadoop 3.4.0+; HBase 2.5.x has preliminary JDK 17 support. HBase 2.6 is the
selected line and Java 17 is used for both build and runtime. The small Maven
project pins Hadoop and HBase APIs to these same release lines. These versions
are lab pins, not claims that this stack is production hardened.
