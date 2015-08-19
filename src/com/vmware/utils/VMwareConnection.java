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

package com.vmware.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

/*
 * This class includes several "findObject" methods.  You will find very
 * similar, and in some cases identical methods in the FindObjects class.
 * Why the duplication? Because the two classes have different interface
 * philosophies.  This class has an object oriented design and encapsulates
 * the connection.  The FindObjects class is more like a library: it is a
 * connection of static methods, and they use "raw" (unencapsulated) data
 * from a connection.
 *
 * No attempt has been made to "factor out" repetitive code, especially from
 * the different versions of findObject.  This was done (or not-done) deliberately
 * in order to make it easier to follow the code, for learning purposes.
 */

/**
 * This class keeps track of the connection to vCenter.  It encapsulates the data of that connection
 * (vimPort, vimService, serviceContent, etc. and also provides methods for finding Managed Object
 * References and Object Contents from that connection.
 *
 * This object is not thread safe. (That's an exercise for the reader. :-)
 * <P>
 * Example of use:<br><code>
 *     VMwareConnection conn = new VMwareConnection(serverName, userName, password);<br>
 *     String clusterName = WhichClusterIsMyVMIn.vm2cluster(conn, virtualMachineName);<br>
 *     System.out.printf("VM %s is in cluster %s.%n", virtualMachineName, clusterName);<br>
 *     conn.close();<br>
 * </code>
 */
public class VMwareConnection {
    // These variables represent data that does not change once the connection is
    // established, so it makes sense to store them here, with the connection.
    com.vmware.vim25.VimService vimService;
    com.vmware.vim25.VimPortType vimPort;
    com.vmware.vim25.ServiceContent serviceContent;
    com.vmware.vim25.ManagedObjectReference propertyCollector;
    ManagedObjectReference viewManager;
    boolean connected = false;

    /**
     * Creates a connection to vCenter server.
     *
     * @param serverName
     *            the name or IP address of the vCenter server to connect to
     * @param userName
     *            the user's name to login as
     * @param password
     *            the user's password
     *
     * @throws Exception if an exception occurred
     *
     */
    public VMwareConnection(String serverName, String userName, String password) throws Exception {
        // Set up the URL to connect to the server.
        String url = "https://" + serverName + "/sdk/vimService";

        // Set up the manufactured managed object reference for the ServiceInstance
        ManagedObjectReference serviceInstance = com.vmware.utils.ObjectUtils.createMoRef(
                "ServiceInstance", "ServiceInstance");

        com.vmware.utils.FakeTrustManager.setupTrust();

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
        // Retrieve the ServiceContent object and viewManager object
        serviceContent = vimPort.retrieveServiceContent(serviceInstance);
        propertyCollector = serviceContent.getPropertyCollector();
        viewManager = serviceContent.getViewManager();

        // Do the login
        vimPort.login(serviceContent.getSessionManager(), userName, password, null);
        connected = true;
    }

    /**
     * Closes the connection when he object goes out of scope. Do not call explicitly.
     */
    protected void finalize() throws Exception {
        close();
    }

    /**
     * Shuts down the connection when you are done with it. If called multiple times, will only be
     * executed once.
     */
    public void close() throws Exception {
        // In case the user does an explicit close, we don't want to rerun this when Java
        // does a finalize.
        if (connected) {
            vimPort.logout(serviceContent.getSessionManager());
            connected = false;
        }
    }

    /**
     * Gets the VimService object for this connection.
     */
    public VimService getVimService() {
        return vimService;
    }

    /**
     * Gets the VimPortType (which is usually called the VimPort) object for this connection.
     */
    public VimPortType getVimPort() {
        return vimPort;
    }

    /**
     * Gets the ServiceContent object for this connection.
     */
    public ServiceContent getServiceContent() {
        return serviceContent;
    }

    /**
     * Gets the propertyCollector object for this connection.
     */
    public ManagedObjectReference getPropertyCollector() {
        return propertyCollector;
    }

    /**
     * Gets the viewManager object for this connection.
     */
    public ManagedObjectReference getViewManager() {
        return viewManager;
    }

    /**
     * Returns all ObjectContent objects of the specified type and with each one include the
     * properties specified.
     *
     * @param objectType
     *            the type of the object to retrieve
     * @param properties
     *            zero or more property names
     * @return the list of ObjectContent objects.
     * @throws Exception
     *             if an exception occurred
     */
    public List<com.vmware.vim25.ObjectContent> findAllObjects(String objectType,
            String... properties) throws Exception {

        // Get references to the ViewManager and PropertyCollector
        ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
        ManagedObjectReference propColl = serviceContent.getPropertyCollector();

        // use a container view for virtual machines to define the traversal
        // - invoke the VimPortType method createContainerView (corresponds
        // to the ViewManager method) - pass the ViewManager MOR and
        // the other parameters required for the method invocation
        // (use a List<String> for the type parameter's string[])
        List<String> typeList = new ArrayList<String>();
        typeList.add(objectType);

        ManagedObjectReference cViewRef = vimPort.createContainerView(viewMgrRef,
                serviceContent.getRootFolder(), typeList, true);

        // create an object spec to define the beginning of the traversal;
        // container view is the root object for this traversal
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(cViewRef);
        oSpec.setSkip(true);

        // create a traversal spec to select all objects in the view
        com.vmware.vim25.TraversalSpec tSpec = new TraversalSpec();
        tSpec.setName("traverseEntities");
        tSpec.setPath("view");
        tSpec.setSkip(false);
        tSpec.setType("ContainerView");

        // add the traversal spec to the object spec;
        // the accessor method (getSelectSet) returns a reference
        // to the mapped XML representation of the list; using this
        // reference to add the spec will update the selectSet list
        oSpec.getSelectSet().add(tSpec);

        // specify the properties for retrieval
        // (virtual machine name, network summary accessible, rp runtime props);
        // the accessor method (getPathSet) returns a reference to the mapped
        // XML representation of the list; using this reference to add the
        // property names will update the pathSet list
        com.vmware.vim25.PropertySpec pSpec = new PropertySpec();
        pSpec.setType(objectType);
        if (properties != null) {
            for (String property : properties) {
                pSpec.getPathSet().add(property);
            }
        }

        // create a PropertyFilterSpec and add the object and
        // property specs to it; use the getter methods to reference
        // the mapped XML representation of the lists and add the specs
        // directly to the objectSet and propSet lists
        com.vmware.vim25.PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(pSpec);

        // Create a list for the filters and add the spec to it
        List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
        fSpecList.add(fSpec);

        // get the data from the server
        com.vmware.vim25.RetrieveOptions retrieveOptions = new RetrieveOptions();
        com.vmware.vim25.RetrieveResult props = vimPort.retrievePropertiesEx(propColl, fSpecList,
                retrieveOptions);

        // go through the returned list and print out the data
        if (props != null) {
            return props.getObjects();
        }
        return null;
    }

    /**
     * Returns an ObjectContent which includes a Managed Object Reference (as specificed by the
     * objectType and name parameters) and properties (as specified by the list of properties).
     * Example of use:<br>
     * <code>ObjectContent vm = conn.findObject("VirtualMachine", vmName, "runtime.host");</code><br>
     * <code>ObjectContent hostStorageSystem = conn.findObject("HostStorageSystem", hostStorageSystemMOR.getValue(),"devicePath","model");</code>
     * <br>
     *
     * @param objectType
     *            the type of the object to retrieve
     * @param name
     *            the name of the object to retrieve
     * @param properties
     *            zero or more property names
     *
     * @return ObjectContent containing the entity and all returned properties.
     * @throws Exception
     *             if an exception occurred
     */
    public ObjectContent findObject(String objectType, String name, String... properties)
            throws Exception {

        // Get references to the ViewManager and PropertyCollector
        ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
        ManagedObjectReference propColl = serviceContent.getPropertyCollector();

        // use a container view for virtual machines to define the traversal
        // - invoke the VimPortType method createContainerView (corresponds
        // to the ViewManager method) - pass the ViewManager MOR and
        // the other parameters required for the method invocation
        // (use a List<String> for the type parameter's string[])
        List<String> typeList = new ArrayList<String>();
        typeList.add(objectType);

        ManagedObjectReference cViewRef = vimPort.createContainerView(viewMgrRef,
                serviceContent.getRootFolder(), typeList, true);

        // create an object spec to define the beginning of the traversal;
        // container view is the root object for this traversal
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(cViewRef);
        oSpec.setSkip(true);

        // create a traversal spec to select all objects in the view
        TraversalSpec tSpec = new TraversalSpec();
        tSpec.setName("traverseEntities");
        tSpec.setPath("view");
        tSpec.setSkip(false);
        tSpec.setType("ContainerView");

        // add the traversal spec to the object spec;
        // the accessor method (getSelectSet) returns a reference
        // to the mapped XML representation of the list; using this
        // reference to add the spec will update the selectSet list
        oSpec.getSelectSet().add(tSpec);

        // specify the properties for retrieval
        // (virtual machine name, network summary accessible, rp runtime props);
        // the accessor method (getPathSet) returns a reference to the mapped
        // XML representation of the list; using this reference to add the
        // property names will update the pathSet list
        boolean gotName = false;
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(objectType);
        if (properties != null) {
            for (String property : properties) {
                pSpec.getPathSet().add(property);
                if (property.equalsIgnoreCase("name")) {
                    gotName = true;
                }
            }
        }
        if (!gotName) {
            pSpec.getPathSet().add("name");
        }

        // create a PropertyFilterSpec and add the object and
        // property specs to it; use the getter methods to reference
        // the mapped XML representation of the lists and add the specs
        // directly to the objectSet and propSet lists
        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(pSpec);

        // Create a list for the filters and add the spec to it
        List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
        fSpecList.add(fSpec);

        // get the data from the server
        RetrieveOptions retrieveOptions = new RetrieveOptions();
        RetrieveResult props = null;
        try {
            props = vimPort.retrievePropertiesEx(propColl, fSpecList, retrieveOptions);
        } catch (com.vmware.vim25.InvalidPropertyFaultMsg ipfm) {
            String oneStringOfProperties = "";
            if (properties != null) {
                for (String property : properties) {
                    oneStringOfProperties += property + " ";
                }
            }
            throw new Exception(
                    "One of the properties passed to findObject was not present on the object found.  Here is a list of properties which might be wrong: "
                            + oneStringOfProperties, ipfm);
        }

        // go through the returned list and print out the data
        if (props != null) {
            for (ObjectContent oc : props.getObjects()) {
                String value = null;
                String path = null;
                List<com.vmware.vim25.DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        path = dp.getName();
                        if (path.equals("name")) {
                            value = (String) dp.getVal();
                            if (value.equals(name)) {
                                return oc;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     * Method to retrieve from the server properties of a {@link ManagedObjectReference}.
     *
     * @param mor
     *            {@link ManagedObjectReference} of the entity
     * @param properties
     *            Array of properties to be looked up
     * @return ObjectContent containing the entity and all returned properties.
     * @throws RuntimeException
     *             this method converts all exceptions it gets into a RuntimeException.
     */
    public ObjectContent findObject(ManagedObjectReference mor, String... properties) {
        List<ObjectContent> oCont = null;

        // Create PropertyFilterSpec using the PropertySpec and ObjectPec
        PropertySpec pSpec = new PropertySpec();
        pSpec.setAll(Boolean.FALSE);
        pSpec.setType(mor.getType());
        for (String property : properties) {
            pSpec.getPathSet().add(property);
        }

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(mor);

        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(pSpec);
        try {
            oCont = vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(),
                    Arrays.asList(fSpec), new RetrieveOptions()).getObjects();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return oCont.get(0);
    }

    /**
     * Returns a managed object in relation to a managed object passed in. Example call:
     * <code>ManagedObjectReference folder = conn.getRelatedRef( vmRef, "VirtualMachine", "parent" );</code>
     * which returns a VM's parent folder.
     *
     * @param objectRef
     *            the object that "anchors" the relationship.
     * @param type
     *            the type of the objectRef argument (not the type of the returned object)
     * @param relationship
     *            the string representing a relationship, such as "parent".
     * @return the managed object in relation to a managed object passed in.
     *
     */
    public ManagedObjectReference getRelatedRef(ManagedObjectReference objectRef, String type,
            String relationship) {
        RetrieveResult props = null;

        // Create an Object Spec to define the property collection. Use the setObj method to specify
        // that the vmRef is the root object for this traversal. Set the setSkip method to true to
        // indicate that you don't want to include the virtual machine in the results.
        // don't include the virtual machine in the results
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(objectRef);
        oSpec.setSkip(false);

        // Specify the property for retrieval.
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(type);
        pSpec.getPathSet().add(relationship);

        // Create a PropertyFilterSpec and add the object and property specs to it.
        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(pSpec);

        // Create a list for the filters and add the property filter spec to it.
        List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
        fSpecList.add(fSpec);

        // Get the data from the server.
        RetrieveOptions ro = new RetrieveOptions();
        try {
            props = getVimPort().retrievePropertiesEx(getPropertyCollector(), fSpecList, ro);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Get the parent folder reference.
        ManagedObjectReference foundRef = null;
        if (props != null) {
            for (ObjectContent oc : props.getObjects()) {
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        foundRef = (ManagedObjectReference) dp.getVal();
                    }
                }
            }
        }

        if (foundRef == null) {
            System.out.printf("The %s of the %s %s was not found.%n", relationship,
                    objectRef.getValue(), objectRef.getType());
            throw new RuntimeException("The " + relationship + " of the " + objectRef.getValue()
                    + " " + objectRef.getType() + " was not found.");
        }
        return foundRef;
    }
}
