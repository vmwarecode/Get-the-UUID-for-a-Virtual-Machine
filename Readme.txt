This sample code shows how to get the UUID of a VirtualMachine, and the UUID of the vCenter that it is part of.

How To Run

In order to run this sample code you must provide four arguments:
[1] The server name or IP address
[2] The user name to log in as
[3] The password to use
[4] The name of the VM

You will need to get the vim25.jar library from the VMware vSphere JDK.  It is in the
VMware-vSphere-SDK-5.5.0\vsphere-ws\java\JAXWS\lib directory.

You can run this sample code by downloading the zip file below, unzipping it and running a command similar to the following:
java -cp vim25.jar com.vmware.sample.Uuids <ip_or_name> <user> <password>
for example:
java -cp vim25.jar com.vmware.sample.Uuids 10.67.119.68 SimpleUser SimplePassword vm1

Output

You will see the output similar to the following when you run the sample:
Name: vm1a
VM MoRef: VirtualMachine-vm-12
VM InstanceUUID: 52f7b088-357e-bb81-59ec-9d9389c7d89e
vCenter InstanceUUID: A7B9E382-060E-4A02-8970-148D6822A1DA
