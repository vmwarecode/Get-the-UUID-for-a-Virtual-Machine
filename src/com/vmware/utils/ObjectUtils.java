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

import java.util.Arrays;
import java.util.List;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;

/**
 * Utility methods for working with Java Objects in VMware's vCenter Java libraries. These methods
 * work mostly with Managed Object Reference objects and Object Content objects.  This class is
 * entirely static methods, so you do not need to create any objects of this class.
 */

public class ObjectUtils {

    /**
     * Creates a PropertySpec out of a type and as many properties as you pass in.
     *
     * @param objectType
     *            the type to add to the PropertySpec
     * @param properties
     *            one or more strings which will become properties in the PropertySpec
     * @return The newly created PropertySpec.
     */
    public static com.vmware.vim25.PropertySpec createPropertySpec(String objectType,
            String... properties) {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(objectType);
        for (String property : properties) {
            pSpec.getPathSet().add(property);
        }
        return pSpec;
    }

    /**
     * Creates a PropertyFilterSpec out of a ManagedObjectReference and a list of properties.
     *
     * @param objmor
     *            the type to add ot the PropertySpec
     * @param filterProps
     *            one or more strings which will become properties in the PropertySpec.
     * @return The newly created PropertySpec.
     */
    public static com.vmware.vim25.PropertyFilterSpec createPropertyFilterSpec(
            com.vmware.vim25.ManagedObjectReference objmor, String[] filterProps) {
        PropertyFilterSpec spec = new PropertyFilterSpec();
        com.vmware.vim25.ObjectSpec oSpec = new com.vmware.vim25.ObjectSpec();
        oSpec.setObj(objmor);
        oSpec.setSkip(Boolean.FALSE);
        spec.getObjectSet().add(oSpec);

        PropertySpec pSpec = new PropertySpec();
        pSpec.getPathSet().addAll(Arrays.asList(filterProps));
        pSpec.setType(objmor.getType());
        spec.getPropSet().add(pSpec);
        return spec;
    }

    /**
     * Prints out a Managed Object Reference. Prints out the MOR in this format: text: [type:value]
     * where text is the first argument and type and name come from the MOR. This function is
     * designed to be used for troubleshooting and debugging.
     *
     * @param text
     *            the text which will be used to label the MOR when it is printed.
     * @param MOR
     *            the Managed Object Reference which will be printed.
     */
    public static void printMOR(String text, ManagedObjectReference MOR) {
        System.out.printf("%s: [%s:%s]%n", text, MOR.getType(), MOR.getValue());
    }

    /**
     * Prints out a Object Content. Prints out the OC in this format: text: [type:value] where text
     * is the first argument and type and name come from the OC. This function is designed to be
     * used for troubleshooting and debugging, and is the same as
     * <code>printMOR(oc.getObject());</code><br>
     * it does not print out any properties.
     *
     * @param text
     *            the text which will be used to label the MOR when it is printed.
     * @param OC
     *            the Object Content which will be printed
     */
    public static void printOC(String text, com.vmware.vim25.ObjectContent OC) {
        System.out.printf("%s: [%s:%s]%n", text, OC.getObj().getType(), OC.getObj().getValue());
    }

    /**
     * Creates a ManagedObjectReference, given a type and a value to put in it. This method does no
     * checking, so you can easily put in an unrecognized type. These types are case-sensitive, so
     * "virtualmachine" will NOT work if you mean "VirtualMachine".  Example of use:<p><code>
     * ManagedObjectReference serviceInstance = ObjectUtils.createMoRef("ServiceInstance", "ServiceInstance");
     * </code>
     * @param type
     *            the type to put in the newly created MOR
     * @param value
     *            the value (often a name) to put in the newly created MOR
     */
    public static ManagedObjectReference createMoRef(String type, String value) {
        ManagedObjectReference moref = new ManagedObjectReference();
        moref.setType(type);
        moref.setValue(value);
        return moref;
    }

    /**
     * Returns a specific property from an ObjectContent object, if that property is a
     * String. This method can only return properties that were fetched from the server when the OC
     * was created. It does not automatically fetch missing properties. A casting exception will be
     * thrown if the property is not a String. <i>Note: if you are writing a lot of Java code to
     * work with ObjectContents, you may want to create a child class called "MyObjectContent" (or
     * similar) and have this method be a class method on your new class. That's a more object
     * oriented design than used here.</i>
     *
     * @param objectContent
     *            the object to get the property from
     * @param propertyName
     *            the name of the property to return
     * @return the object named in the parameter, if the object has it, or null if not.
     */
    public static String getPropertyValue(ObjectContent objectContent, String propertyName) {
        List<com.vmware.vim25.DynamicProperty> dps = objectContent.getPropSet();
        if (dps != null) {
            for (DynamicProperty dp : dps) {
                String path = dp.getName();
                if (path.equals(propertyName)) {
                    return (String) dp.getVal();
                }
            }
        }
        return null;
    }

    /**
     * Returns a specific property from an ObjectContent object. This method can only
     * return properties that were fetched from the server when the OC was created. It does not
     * automatically fetch missing properties. <i>Note: if you are writing a lot of Java code to
     * work with ObjectContents, you may want to create a child class called "MyObjectContent" (or
     * similar) and have this method be a class method on your new class. That's a more object
     * oriented design than used here.</i><p><code>
     * ObjectContent cluster;<br>
     * ...<br>
     * String clusterName = (String) ObjectUtils.getPropertyObject(cluster, "name");
     * </code>
     *
     * @param objectContent
     *            the object to get the property from
     * @param propertyName
     *            the name of the property to return
     * @return the object named in the parameter, if the object has it, or null if not.
     */
    public static Object getPropertyObject(ObjectContent objectContent, String propertyName) {
        List<DynamicProperty> dps = objectContent.getPropSet();
        if (dps != null) {
            for (DynamicProperty dp : dps) {
                String path = dp.getName();
                if (path.equals(propertyName)) {
                    return dp.getVal();
                }
            }
        }
        return null;
    }

}