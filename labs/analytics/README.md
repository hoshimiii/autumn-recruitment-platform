# Optional analytics lab

This isolated lab demonstrates a batch pipeline: Hadoop MapReduce aggregates a
fixed synthetic CSV; the job writes the derived counts to HBase and reads them
back through HBase's ZooKeeper registry. Hadoop is the batch processing
platform, HBase is a wide-column store for derived results, and ZooKeeper
coordinates HBase. None replaces the application's SQLite transaction store.

This is a small local learning cluster: MapReduce uses Hadoop's local runner,
HBase runs one master and one region server against its local filesystem, and a
separate ZooKeeper container is used. There is no HDFS, YARN, production data,
Feishu access, credentials, model call, or connection to the application DB.
It demonstrates real Hadoop/HBase/ZooKeeper processes and the client protocol,
not a multi-node or production topology.

## Requirements and commands

Use Docker Desktop or another Docker Engine with Compose, at least 2 GB memory
available to the lab, Java 17 for local tests, and network access for the first
image build and Maven dependency resolution. Build downloads Apache Hadoop
3.4.2-lean and HBase 2.6.6 Hadoop-3 binaries from Apache release assets and
checks their published SHA-512 sums. ZooKeeper uses the Docker Official Image
`zookeeper:3.9.6-jre-17`.

From this directory in PowerShell:

```powershell
.\run.ps1 test       # Java parser/aggregator unit tests; no Docker needed
.\run.ps1 config     # validate Compose configuration; no daemon needed
.\run.ps1 build      # build the custom image
.\run.ps1 start      # start the isolated ZooKeeper service
.\run.ps1 smoke      # fresh ZooKeeper + Hadoop aggregation + HBase put/get assertions
.\run.ps1 stop       # remove only this lab's Compose project and containers
```

`smoke` recreates its ZooKeeper service, runs one lab container, then removes
the Compose project even when the lab command fails. `build` and `config` are
separate so image source checks and daemon-independent configuration checks
can be inspected independently. No ports are published to the host. Runtime
state remains inside disposable containers.

The self-test covers the same strict, unquoted four-column input contract and
aggregation logic as the job. It is not middleware runtime evidence. Container
acceptance requires a successful `build` and `smoke` on a working Docker Engine;
configuration validation alone does not establish that.

The dataset is invented and deliberately tiny. The Java job rejects duplicate
IDs, malformed rows, unsafe key characters, unexpected headers, and invalid
cohorts. Its aggregate key is `cohort|department|job_family`; each row stores
the integer count in HBase column `metrics:count`. Re-running starts from fresh
disposable state, so it cannot leave stale aggregates behind.
