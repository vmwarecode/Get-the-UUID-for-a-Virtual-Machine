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

import java.util.List;

import com.vmware.vim25.InvalidCollectorVersionFaultMsg;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectUpdate;
import com.vmware.vim25.ObjectUpdateKind;
import com.vmware.vim25.PropertyChange;
import com.vmware.vim25.PropertyChangeOp;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertyFilterUpdate;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.UpdateSet;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.WaitOptions;

/**
 * This class contain some utility routines to work with tasks. Mostly these routines involve
 * waiting for the task to complete, and returning the result.
 */

public class TaskUtils {

    /**
     * Waits for a specific task until it finishes
     */
    public static Object[] wait(VimPortType vimPort, ManagedObjectReference objmor,
            String[] filterProps, String[] endWaitProps, Object[][] expectedVals)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {
        ManagedObjectReference serviceInstance = new ManagedObjectReference();
        serviceInstance.setType("ServiceInstance");
        serviceInstance.setValue("ServiceInstance");
        ServiceContent serviceContent = vimPort.retrieveServiceContent(serviceInstance);

        // version string is initially null
        String version = "";
        Object[] endVals = new Object[endWaitProps.length];
        Object[] filterVals = new Object[filterProps.length];

        PropertyFilterSpec spec = com.vmware.utils.ObjectUtils.createPropertyFilterSpec(
                objmor, filterProps);

        ManagedObjectReference filterSpecRef = vimPort.createFilter(
                serviceContent.getPropertyCollector(), spec, true);

        boolean reached = false;

        UpdateSet updateset = null;
        List<PropertyFilterUpdate> filtupary = null;
        List<ObjectUpdate> objupary = null;
        List<PropertyChange> propchgary = null;
        while (!reached) {
            updateset = vimPort.waitForUpdatesEx(serviceContent.getPropertyCollector(), version,
                    new WaitOptions());
            if (updateset == null || updateset.getFilterSet() == null) {
                continue;
            }
            version = updateset.getVersion();

            // Make this code more general purpose when PropCol changes later.
            filtupary = updateset.getFilterSet();

            for (PropertyFilterUpdate filtup : filtupary) {
                objupary = filtup.getObjectSet();
                for (ObjectUpdate objup : objupary) {
                    // TODO: Handle all kinds of updates.
                    if (objup.getKind() == ObjectUpdateKind.MODIFY
                            || objup.getKind() == ObjectUpdateKind.ENTER
                            || objup.getKind() == ObjectUpdateKind.LEAVE) {
                        propchgary = objup.getChangeSet();
                        for (PropertyChange propchg : propchgary) {
                            updateValues(endWaitProps, endVals, propchg);
                            updateValues(filterProps, filterVals, propchg);
                        }
                    }
                }
            }

            Object expctdval = null;
            // Check if the expected values have been reached and exit the loop
            // if done.
            // Also exit the WaitForUpdates loop if this is the case.
            for (int chgi = 0; chgi < endVals.length && !reached; chgi++) {
                for (int vali = 0; vali < expectedVals[chgi].length && !reached; vali++) {
                    expctdval = expectedVals[chgi][vali];

                    reached = expctdval.equals(endVals[chgi]) || reached;
                }
            }
        }

        // Destroy the filter when we are done.
        vimPort.destroyPropertyFilter(filterSpecRef);
        return filterVals;
    }

    private static void updateValues(String[] props, Object[] vals, PropertyChange propchg) {
        for (int findi = 0; findi < props.length; findi++) {
            if (propchg.getName().lastIndexOf(props[findi]) >= 0) {
                if (propchg.getOp() == PropertyChangeOp.REMOVE) {
                    vals[findi] = "";
                } else {
                    vals[findi] = propchg.getVal();
                }
            }
        }
    }

    /**
     * Returns a boolean value specifying whether the Task is succeeded or failed.
     *
     * @param task
     *            the ManagedObjectReference representing the Task.
     * @return the value representing the Task result.
     *
     * @throws InvalidCollectorVersionFaultMsg
     * @throws RuntimeFaultFaultMsg
     * @throws InvalidPropertyFaultMsg
     */
    public static boolean getTaskResultAfterDone(VimPortType vimPort, ManagedObjectReference task)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, InvalidCollectorVersionFaultMsg {

        boolean retVal = false;

        // info has a property - state for state of the task
        Object[] result = wait(vimPort, task, new String[] { "info.state", "info.error" },
                new String[] { "state" }, new Object[][] { new Object[] { TaskInfoState.SUCCESS,
                        TaskInfoState.ERROR } });

        if (result[0].equals(TaskInfoState.SUCCESS)) {
            retVal = true;
        }
        if (result[1] instanceof LocalizedMethodFault) {
            throw new RuntimeException(((LocalizedMethodFault) result[1]).getLocalizedMessage());
        }
        return retVal;
    }
}