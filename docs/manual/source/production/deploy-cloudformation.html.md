---
title: Deploying with AWS CloudFormation
---

PredictionIO can be deployed on your Amazon Web Services account by the [AWS
CloudFormation](http://aws.amazon.com/cloudformation/) tool. Using AWS
CloudFormation, you can deploy a functional, fully distributed PredictionIO
cluster with compute and storage nodes that are ready to use and scale in
minutes.

## Prerequisites

An active Amazon Web Services account with permissions to use the following
services:

* Auto Scaling
* CloudFormation
* EC2
* VPC

## Architecture

### Nodes

The PredictionIO CloudFormation stack creates two main types of nodes: compute
and storage nodes.

The stack will launch at least 1 compute node that acts as the master compute
node. This node will become the Spark master. Additional compute nodes can be
launched by updating the stack, which will join the master and become slave
compute nodes as Spark workers.

For the case of storage nodes, the stack will launch at exactly **3** core
storage nodes. These nodes form the core of the HDFS, ZooKeeper quorum, and
HBase storage. Their roles are described below.

* Core storage node 1 (master) will become the Hadoop NameNode, a Hadoop
  DataNode, and the HBase Master.
* Core storage node 2 will become a Hadoop DataNode, the HBase Backup Master,
  and an HBase RegionServer.
* Core storage node 3 will become a Hadoop DataNode, and an HBase RegionServer.

In addition, PredictionIO Event Server will be launched on all storage nodes.

Extra storage nodes can be added to the cluster by updating the stack, which
will join the storage cluster automatically. They cannot be removed once they
are spinned up.

### Networking

The stack will automatically create a VPC and a subnet with an Internet gateway.
All cluster nodes will be launched inside this subnet using a single security
group that enables all TCP communications among all nodes within the same group.
All compute nodes (including those that are launched after stack creation) will
receive public IPs. All core storage nodes will receive public Elastic IPs.

## Bring Up AWS CloudFormation Control Panel

From your main AWS console, locate CloudFormation and click on it.

![CloudFormation on AWS Console](/images/cloudformation/cf-01.png)

This will bring you to the CloudFormation console below.

![CloudFormation Console](/images/cloudformation/cf-02.png)

## Select the PredictionIO CloudFormation Stack Template

From the CloudFormation console, click on the **Create New Stack** blue button
as shown above. This will bring up the **Select Template** screen. Name your
stack as you like. Within the *Template* section, choose **Specify an Amazon S3
template URL**, and put
https://s3.amazonaws.com/cloudformation.prediction.io/TBD.json as the value.

![CloudFormation Stack Template Selection](/images/cloudformation/cf-03.png)

Click **Next** when you are done.

## Specify Stack Parameters

The next screen will show you parameters that you can change for your stack. The
following table describes these parameters in details.

![Stack Parameters](/images/cloudformation/cf-04.png)

| Parameter | Description |
|-----------|-------------|
| ClusterAZ | Specify the availability zone that the PredictionIO cluster will be launched in. All instances of the cluster will be launched into the same zone for optimal network performance between one another.
| ComputeInstanceType | The EC2 instance type of all compute nodes. Memory-optimized EC2 instances are recommended. |
| ExtraNodeStorageSize | Size in GB of each extra storage node. This can be changed when you add an extra storage node. |
| ExtraNodeStorageVolumeType | The EBS volume type of each extra storage node. Valid values are *standard* and *gp2*. This can be changed when you add an extra storage node. |
| KeyName | The AWS SSH key pair name that can be used to access all nodes in the cluster. |
| NumberOfComputeWorkers | Number of extra compute nodes besides the master compute node. This can be increased and decreased. |
| NumberOfExtraStorageNodes | Number of extra storage nodes besides core storage nodes. **Never decrease this value or you will risk data corruption.** |
| StorageInstanceType | The EC2 instance type of all storage nodes. General purpose EC2 instances are recommended. |
| StorageSize | Size in GB of each core storage node. This cannot be changed once the cluster is launched. |
| StorageVolumeType | The EBS volume type of each core storage node. Valid values are *standard* and *gp2*. This cannot be changed once the cluster is launched. |

Click **Next** when you are done. You will arrive at the **Options** screen. You
may click **Next** again if you do not have other options to specify.

At the **Review** screen, click **Create** to finish.

## Using the Cluster

You should see the following when the cluster is being created after the
previous step.

![Stack Creation](/images/cloudformation/cf-05.png)

Once the stack creation has finished, you can click on **Events** and select
**Outputs** to arrive to the following screen.

NOTE: If your browser window is wide enough, you should see the **Outputs** tab
without clicking on **Events**.

![Completed Stack](/images/cloudformation/cf-06.png)

Take note of **PIOComputeMasterPublicIp** and **PIOStorageMasterPublicIp**. We
will now access the cluster and make sure everything is in place.

WARNING: Sometimes even though the stack is created successfully, not all
cluster services would be launched successfully due to potential network
glitches or system issues within a cluster instance. In this case, simply
delete and create the stack again.

### Verify Compute Nodes

SSH to the master compute node using the **PIOComputeMasterPublicIp**. In this
example, let us assume the IP address be 54.175.145.84, and your private key
file be **yourkey.pem**.

```bash
$ ssh -i yourkey.pem -A -L 8080:localhost:8080 ubuntu@54.175.145.84
```

Once you are in, point your web browser to http://localhost:8080. You should see
something similar to the following.

![Example Spark UI](/images/cloudformation/spark.png)

Notice that the case above was with **NumberOfComputeWorkers** set to **2**. If
you do not have extra compute worker nodes, you will see only one worker.

### Verify Storage Nodes

SSH to the master core storage node using the **PIOStorageMasterPublicIp**. In
this example, let us assume the IP address be 54.175.1.36, and your private key
file be **yourkey.pem**.

```bash
$ ssh -i yourkey.pem -A -L 50070:localhost:50070 -L 60010:localhost:60010 ubuntu@54.175.1.36
```

Once you are in, point your web browser to http://localhost:50070 and click on
**Datanodes** at the top menu. You should see something similar to the
following.

![Example HDFS UI](/images/cloudformation/hdfs.png)

All **3 core storage nodes** must be up for proper operation.

If all **3 core storage nodes** are working properly, verify HBase by pointing
your web browser to http://localhost:60010. You should see something similar to
the following.

![Example HBase UI](/images/cloudformation/hbase.png)

If you do not specify any extra storage nodes, you should see 2 region servers.
There should also be 1 backup master.

### Running Quick Start

You can now get your hands dirty with your fully-distributed PredictionIO
cluster. Let's start with the [recommendation quick
start](/templates/recommendation/quickstart/) with a few twists.

1. Skip all the way down to `pio status`. Run the command and you should see
   everything functional.

2. Run through the section **Create a Sample App** as described. The
   installation directory of PredictionIO is `/opt/PredictionIO`.

3. Run through the section **Collecting Data** as described, except that you
   will be connecting to the Event Server at the master core storage node.
   Assuming the private IP of the master core storage node is `10.0.0.123`, add
   `--url http://10.0.0.123:7070` to the `import_eventserver.py` command.

4. Copy HBase configuration to the engine template directory. The full path of
   the configuration file is `/opt/hbase-0.98.9-hadoop2/conf/hbase-site.xml`.
   (This step will not be required in future releases.)

5. Run through the section **Deploy the Engine as a Service** up to the
   subsection **Training**. Assuming the private DNS name of the master compute
   node is `ip-10-0-0-234.ec2.internal`, add
   `-- --master spark://ip-10-0-0-234.ec2.internal:7077` after the `pio train`
   command. This will send the training to the compute cluster instead of the
   local machine. The Spark master URL must match exactly the one shown on its
   web UI. Repeat the same steps for subsection **Deploying**, which will
   create an Engine Server backed by the compute cluster.
