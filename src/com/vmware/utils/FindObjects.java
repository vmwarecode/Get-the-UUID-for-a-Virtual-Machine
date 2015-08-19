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
import java.util.List;

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

/*
 * This class includes several "findObject" methods.  You will find very
 * similar, and in some cases identical methods in the VMwareConnection class.
 * Why the duplication? Because the two classes have different interface
 * philosophies.  This class has a library style interface: it is a
 * connection of static methods, and they use "raw" (unencapsulated) data
 * from a connection. VMwareConnection has an object oriented design and
 * encapsulates the connection.
 *
 * No attempt has been made to "factor out" repetitive code, especially from
 * the different versions of findObject.  This was done (or not-done) deliberately
 * in order to make it easier to follow the code, for learning purposes.
 */

/**
 * Utility methods for working with Java Objects in VMware's vCenter Java libraries. These methods
 * work mostly with Managed Object Reference objects and Object Content objects.
 */

public class FindObjects {

    /**
     * Returns all ObjectContent objects of the specified type and with each one include the
     * properties specified.
     *
     * @param objectType
     *            the type of the managed object
     * @param properties
     *            zero or more property names
     * @return the managed objects specified or null if there are none.
     * @throws Exception
     *             if an exception occurred
     */
    public static List<com.vmware.vim25.ObjectContent> findAllObjects(
            com.vmware.vim25.VimPortType vimPort, com.vmware.vim25.ServiceContent serviceContent,
            String objectType, String... properties) throws Exception {

        // Get references to the ViewManager and PropertyCollector
        com.vmware.vim25.ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
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
        com.vmware.vim25.ObjectSpec oSpec = new ObjectSpec();
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
     * Returns a Managed Object Reference for the object matching the type and name passed in; no
     * properties are returned. The vimPort must already be connected, and the service content must
     * be from the vimPort.
     *
     * @param objectType
     *            the type of the managed object to return
     * @param name
     *            the name of the managed object to return
     * @return the managed object specified or null if it is not found.
     * @throws Exception
     *             if an exception occurred
     */
    public static ManagedObjectReference findObject(VimPortType vimPort,
            ServiceContent serviceContent, String objectType, String name) throws Exception {

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
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(objectType);
        pSpec.getPathSet().add("name");

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
        RetrieveResult props = vimPort.retrievePropertiesEx(propColl, fSpecList, retrieveOptions);

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
                            // Uncomment the next line if needed to help with troubleshooting
                            // System.out.printf("found = [%s], looking for [%s]%n", value, name);
                            if (value.equals(name)) {
                                // Uncomment the next line if needed to help with troubleshooting
                                // System.out.printf("found = %s", value);
                                return oc.getObj();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns an ObjectContent object for the object with type and name passed in and including the
     * properties specified in the propertySpec.
     *
     * @param propertySpec
     *            the property spec which describes the properties to return
     * @param name
     *            the name of the managed object to return.
     * @return the managed objects specified or null if there are none.
     * @throws Exception
     *             if an exception occurred
     */
    public static ObjectContent findObject(VimPortType vimPort, ServiceContent serviceContent,
            PropertySpec propertySpec, String name) throws Exception {

        // Get references to the ViewManager and PropertyCollector
        ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
        ManagedObjectReference propColl = serviceContent.getPropertyCollector();

        // use a container view for virtual machines to define the traversal
        // - invoke the VimPortType method createContainerView (corresponds
        // to the ViewManager method) - pass the ViewManager MOR and
        // the other parameters required for the method invocation
        // (use a List<String> for the type parameter's string[])
        List<String> typeList = new ArrayList<String>();
        typeList.add(propertySpec.getType());

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

        // create a PropertyFilterSpec and add the object and
        // property specs to it; use the getter methods to reference
        // the mapped XML representation of the lists and add the specs
        // directly to the objectSet and propSet lists
        PropertyFilterSpec fSpec = new PropertyFilterSpec();
        fSpec.getObjectSet().add(oSpec);
        fSpec.getPropSet().add(propertySpec);

        // Create a list for the filters and add the spec to it
        List<PropertyFilterSpec> fSpecList = new ArrayList<PropertyFilterSpec>();
        fSpecList.add(fSpec);

        // get the data from the server
        RetrieveOptions retrieveOptions = new RetrieveOptions();
        RetrieveResult props = vimPort.retrievePropertiesEx(propColl, fSpecList, retrieveOptions);

        // go through the returned list and print out the data
        if (props != null) {
            for (ObjectContent oc : props.getObjects()) {
                String value = null;
                String path = null;
                List<DynamicProperty> dps = oc.getPropSet();
                if (dps != null) {
                    for (DynamicProperty dp : dps) {
                        path = dp.getName();
                        if (path.equals("name")) {
                            value = (String) dp.getVal();
                            // Uncomment the next line if needed to help with troubleshooting
                            // System.out.printf("found = [%s], looking for [%s]%n", value, name);
                            if (value.equals(name)) {
                                System.out.printf("found = %s", value);
                                return oc;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}