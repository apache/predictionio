---
title: Deploying with AWS CloudFormation
---

##Overview

You can scale PredictionIO on AWS with [CloudFormation](http://aws.amazon.com/cloudformation/). Here we have defined a PredictionIO CloudFormation stack that you can deploy a functional, fully distributed PredictionIO cluster in minutes.

###  Instances

The PredictionIO CloudFormation stack creates two types of instance: **compute
and storage**. By default, the stack will launch **1 compute Instance and 3 Storage instances**. 

The compute instance acts as Spark master. You can launch extra compute instances by updating the stack. They will join the master and become slave compute instances as Spark workers. The extra compute instances are fully elastic and can be increased or decreased at anytime.

The storage instances form the core of the HDFS, ZooKeeper quorum, and
HBase storage. Their roles are described below.

* Storage instance 1 (master) will become the Hadoop NameNode, a Hadoop
  DataNode, and the HBase Master.
* Storage instance 2 will become a Hadoop DataNode, the HBase Backup Master,
  and an HBase RegionServer.
* Storage instance 3 will become a Hadoop DataNode, and an HBase RegionServer.

In addition, PredictionIO Event Server will be launched on all storage instances.

Extra storage instances can be added to the cluster by updating the stack. They cannot be removed once they
are spinned up.


### Networking

The stack will automatically create a VPC and a subnet with an Internet gateway.
All cluster instances will be launched inside this subnet using a single security
group that enables all TCP communications among all instances within the same group.
All compute instances (including those that are launched after stack creation) will
receive public IPs. All core storage instances will receive public Elastic IPs.

## Step-by-Step

First, you need to have an active Amazon Web Services account with permissions to use the following
services:

* Auto Scaling
* CloudFormation
* EC2
* VPC

From your main AWS console, locate CloudFormation and click on it.

![CloudFormation on AWS Console](/images/cloudformation/cf-01.png)

This will bring you to the CloudFormation console below.

![CloudFormation Console](/images/cloudformation/cf-02.png)

### Select the PredictionIO CloudFormation Stack Template

From the CloudFormation console, click on the **Create New Stack** blue button
as shown above. This will bring up the **Select Template** screen. Name your
stack as you like. Within the *Template* section, choose **Specify an Amazon S3
template URL**, and put
https://s3.amazonaws.com/cloudformation.prediction.io/pio.json as the value.

![CloudFormation Stack Template Selection](/images/cloudformation/cf-03.png)

Click **Next** when you are done.

### Specify Stack Parameters

The next screen shows the stack parameters. You must enter your AWS SSH key pair. For the other parameters, you can change them to meet your needs or simply use the default values. 


![Stack Parameters](/images/cloudformation/cf-04.png)

| Parameter | Description |
|-----------|-------------|
| AWS-KeyPair | The AWS SSH key pair name that can be used to access all instances in the cluster. |
| AvailabilityZone | Specify the availability zone that the PredictionIO cluster will be launched in. All instances of the cluster will be launched into the same zone for optimal network performance between one another.
| ComputeInstanceType | The EC2 instance type of all compute instances. Memory-optimized EC2 instances are recommended. |
| ComputeInstanceExtra | Number of extra compute instances besides the core compute instance. This can be increased and decreased. |
| StorageInstanceExtra | Number of extra storage instances besides core storage instances. **Never decrease this value or you will risk data corruption.** |
| StorageInstanceExtraSize | Size in GB of each extra storage instance. This can be changed when you add an extra storage instance. |
| StorageInstanceExtraVolumeType | The EBS volume type of each extra storage instance. Valid values are *standard* and *gp2*. This can be changed when you add an extra storage instance. |
 StorageInstanceType | The EC2 instance type of all storage instances. General purpose EC2 instances are recommended. |
| StorageInstanceSize | Size in GB of each core storage instance. This cannot be changed once the cluster is launched. |
| StorageInstanceVolumeType | The EBS volume type of each core storage instance. Valid values are *standard* and *gp2*. This cannot be changed once the cluster is launched. |


Click **Next** when you are done. You will arrive at the **Options** screen. You can skip this step if you do not have other options to specify.

At the **Review** screen, click **Create** to finish.

## Using the Cluster

You should see the following when the cluster is being created after the
previous step.

![Stack Creation](/images/cloudformation/cf-05.png)

Once the stack creation has finished, you can click on **Events** and select
**Outputs** to arrive to the following screen.

![Completed Stack](/images/cloudformation/cf-06.png)

Take note of **PIOComputeMasterPublicIp** and **PIOStorageMasterPublicIp**. We
will now access the cluster and make sure everything is in place.

WARNING: Sometimes the stack is created successfully but not all
cluster services would launch due to potential network
glitches or system issues within a cluster instance. In this case, simply
delete and create the stack again.

### Verify Compute Instances

SSH to the master compute instance using the **PIOComputeMasterPublicIp**. In this
example, let us assume the IP address be 54.175.145.84, and your private key
file be **yourkey.pem**.

```bash
$ ssh -i yourkey.pem -A -L 8080:localhost:8080 ubuntu@54.175.145.84
```

Once you are in, point your web browser to http://localhost:8080. You should see
something similar to the following.

![Example Spark UI](/images/cloudformation/spark.png)

NOTE:In the example above **NumberOfComputeWorkers** is **2**. This is because the example has 1 compute instance and 1 extra compute instance. If you do not have any extra compute instances, you will see only 1 worker on the above page.

### Verify Storage Instances

SSH to the storage instance using the **PIOStorageMasterPublicIp**. In
this example, let us assume the IP address be 54.175.1.36, and your private key
file be **yourkey.pem**.

```bash
$ ssh -i yourkey.pem -A -L 50070:localhost:50070 -L 60010:localhost:60010 ubuntu@54.175.1.36
```

Once you are in, point your web browser to http://localhost:50070 and click on
**Datainstances** at the top menu. You should see the following page.

![Example HDFS UI](/images/cloudformation/hdfs.png)

WARNING:All **3 storage instances** must be up for proper operation.

If all **3 storage instances** are working properly, you can then verify HBase by pointing
your web browser to http://localhost:60010. You should see something similar to
the following.

![Example HBase UI](/images/cloudformation/hbase.png)

If you do not specify any extra storage instances, you should see 2 region servers.
There should also be 1 backup master.

### Running Quick Start

You can now start with the fully-distributed PredictionIO
cluster. Let's start with the [recommendation quick
start](/templates/recommendation/quickstart/) with a few twists.

1. Skip the installation steps and run `pio status`. You should see
   everything functional.

2. Run through the section **Create a Sample App** as described. The
   installation directory of PredictionIO is `/opt/PredictionIO`.

3. Run through the section **Collecting Data** as described, except that you
   will be connecting to the Event Server at the master core storage instance.
   Assuming the private IP of the master core storage instance is `10.0.0.123`, add
   `--url http://10.0.0.123:7070` to the `import_eventserver.py` command.

4. Copy HBase configuration to the engine template directory. The full path of
   the configuration file is `/opt/hbase-0.98.9-hadoop2/conf/hbase-site.xml`.
   (This step will not be required in future releases.)

5. Run through the section **Deploy the Engine as a Service** up to the
   subsection **Training**. Assuming the private DNS name of the master compute
   instance is `ip-10-0-0-234.ec2.internal`, add
   `-- --master spark://ip-10-0-0-234.ec2.internal:7077` after the `pio train`
   command. This will send the training to the compute cluster instead of the
   local machine. The Spark master URL must match exactly the one shown on its
   web UI. Repeat the same steps for subsection **Deploying**, which will
   create an Engine Server backed by the compute cluster.
