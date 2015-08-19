/*
 * ******************************************************
 * Copyright VMware, Inc. 2014. All Rights Reserved.
 * ******************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vmware.sample;

import java.util.Map;

import javax.xml.ws.BindingProvider;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.ServiceContent;

/*
 * Coding Conventions Used Here:
 * 1. The connection to vCenter is managed from within the Uuid class. **
 * 2. Many methods are listed as "throws Exception" which means that the exceptions are ignored
 *    and printed out at the call stack.  If used in real development, exceptions should be caught
 *    and recovered from.
 * 3. Managed Object Reference variables are named ending with "MOR".
 *
 * Also: Full path names are used for all java classes when they are first used (for declarations
 * or to call static methods).  This makes it easier to find their source code, so you can understand
 * it.  For example "com.vmware.utils.VMwareConnection conn" rather than "VMwareConnection conn".
 */

/**
 * Prints out the virtual machine's UUID and the vCenter's UUID.
 */
public class Uuids {
    // Variables of the following types for access to the API methods
    // and to the vSphere inventory.
    // -- ManagedObjectReference for the ServiceInstance on the Server
    // -- VimService for access to the vSphere Web service
    // -- VimPortType for access to methods
    // -- ServiceContent for access to managed object services
    private com.vmware.vim25.VimService vimService;
    private com.vmware.vim25.VimPortType vimPort;
    private com.vmware.vim25.ServiceContent serviceContent;
    private com.vmware.vim25.ManagedObjectReference virtualMachine;
    private com.vmware.vim25.ObjectContent objectContents;

    /**
     * Constructs a new <code>Uuids</code> and initializes the connection to vCenter.
     *
     * @param serverName
     *            the name or IP address of the vCenter server to connect to
     * @param userName
     *            the user's name to login as
     * @param password
     *            the user's password
     */
    public Uuids(String serverName, String userName, String password) throws Exception {
        String url = "https://" + serverName + "/sdk/vimService";

        // Set up the manufactured managed object reference for the ServiceInstance
        ManagedObjectReference serviceInstanceMOR = new ManagedObjectReference();
        serviceInstanceMOR.setType("ServiceInstance");
        serviceInstanceMOR.setValue("ServiceInstance");

        // Create a VimService object to obtain a VimPort binding provider.
        // The BindingProvider provides access to the protocol fields
        // in request/response messages. Retrieve the request context
        // which will be used for processing message requests.
        vimService = new com.vmware.vim25.VimService();
        vimPort = vimService.getVimPort();
        Map<String, Object> ctxt = ((BindingProvider) vimPort).getRequestContext();

        // Store the Server URL in the request context and specify true
        // to maintain the connection between the client and server.
        // The client API will include the Server's HTTP cookie in its
        // requests to maintain the session. If you do not set this to true,
        // the Server will start a new session with each request.
        ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        // Retrieve the ServiceContent object and login
        serviceContent = vimPort.retrieveServiceContent(serviceInstanceMOR);
        vimPort.login(serviceContent.getSessionManager(), userName, password, null);
    }

    /**
     * Logs out automatically when Uuids object goes out of scope, and can not be used anymore. This
     * is not recommended for production code, because you can not prodict exactly when the Java
     * runtime will execute this call. But for sample code it does not really matter.
     */
    protected void finalize() throws Throwable {
        vimPort.logout(serviceContent.getSessionManager());
    }

    /**
     * Returns the ServiceContent object for the connected vCenter.
     *
     * @return The ServiceContent object for the connected vCenter.
     */
    public ServiceContent getServiceContent() {
        return serviceContent;
    }

    /**
     * Returns a UUID for a given virutal machine.
     *
     * @param virtualMachineName
     *            the name of the VM
     * @return a string of the form "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" (X is a hexidecimal digit
     *         and letters will be lower case).
     */
    public String getUuid(String virtualMachineName) throws Exception {
        String returnValue = null;
        // If this object doesn't have objectContents set already, then get it set.
        // In the future we will need the "summary.config.instanceUuid" property set, so we specify
        // it here.
        if (objectContents == null) {
            com.vmware.vim25.PropertySpec neededProperties = com.vmware.utils.ObjectUtils
                    .createPropertySpec("VirtualMachine", "name", "summary.config.instanceUuid");

            objectContents = com.vmware.utils.FindObjects.findObject(vimPort, serviceContent,
                    neededProperties, virtualMachineName);
            virtualMachine = objectContents.getObj();
        }
        if (objectContents == null) {
            returnValue = "Did not find a VirtualMachine named " + virtualMachineName
                    + ", so nothing was done.";
        } else {
            //
            returnValue = com.vmware.utils.ObjectUtils.getPropertyValue(objectContents,
                    "summary.config.instanceUuid");
        }
        // System.out.printf("Got: %s%n",returnValue);
        return returnValue;
    }

    /**
     * Returns a string representation of a VM as found in the ManagedObjectReference.
     *
     * @param virtualMachineName
     *            the virtula machine name
     * @return a string representation of a VM
     * @throws Exception
     *             if an exception occurred
     */
    public String getMoRefString(String virtualMachineName) throws Exception {
        String returnValue = null;
        // If this object doesn't have objectContents set already, then get it set.
        // In the future we will need the "summary.config.instanceUuid" property set, so we specify
        // it here.
        if (objectContents == null) {
            PropertySpec neededProperties = com.vmware.utils.ObjectUtils.createPropertySpec(
                    "VirtualMachine", "name", "summary.config.instanceUuid");

            objectContents = com.vmware.utils.FindObjects.findObject(vimPort, serviceContent,
                    neededProperties, virtualMachineName);
            virtualMachine = objectContents.getObj();
        }
        if (objectContents == null) {
            returnValue = "Did not find a VirtualMachine named " + virtualMachineName
                    + ", so nothing was done.";
        } else {
            returnValue = virtualMachine.getType() + "-" + virtualMachine.getValue();
        }

        return returnValue;
    }

    /**
     * Prints UUID information.
     *
     * <p>
     * Run with a command similar to this:<br>
     * <code>java -cp vim25.jar com.vmware.general.Uuids <i>ip_or_name</i> <i>user</i> <i>password</i> <i>vm_name</i></code><br>
     * <code>java -cp vim25.jar com.vmware.general.Uuids 10.20.30.40 JoeUser JoePasswd myvm</code>
     *
     * @param args
     *            the ip_or_name, user, password, and virtuall machine name
     * @throws Exception
     *             if an exception occurred
     *
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 4) {
            System.out.println("Wrong number of arguments, must provide five arguments:");
            System.out.println("[1] The server name or IP address");
            System.out.println("[2] The user name to log in as");
            System.out.println("[3] The password to use");
            System.out.println("[4] The name of the VM");
            System.exit(1);
        }

        // arglist variables
        String serverName = args[0];
        String userName = args[1];
        String password = args[2];
        String virtualMachineName = args[3];

        // This sets up trust management for examples, only. Do not use this code in production.
        com.vmware.utils.FakeTrustManager.setupTrust();

        // Create a new Uuids object, which includes a connection to vCenter.
        // The arguments all have to do with setting up that connection.
        Uuids uuids = new Uuids(serverName, userName, password);

        // Print out UUID data from the object
        System.out.printf("Name: %s%n", virtualMachineName);
        System.out.printf("VM MoRef: %s%n", uuids.getMoRefString(virtualMachineName));
        System.out.printf("VM InstanceUUID: %s%n", uuids.getUuid(virtualMachineName));
        if ("VirtualCenter".equals(uuids.getServiceContent().getAbout().getApiType())) {
            System.out.printf("vCenter InstanceUUID: %s%n", uuids.getServiceContent().getAbout()
                    .getInstanceUuid());
        }
    }
}
