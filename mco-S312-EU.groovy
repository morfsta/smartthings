/**
 *  MCO 2 Channel Switch
 *
 *  Copyright Morfsta 2016 (from original code developed by Anders Mellbratt)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 
 *  Code from https://github.com/joeltamkin/smartthings-EnerwaveRSM2/blob/master/Enerwave%20ZWN-RSM2%20Device, also under Apache License
 *
 *	Also using code from https://github.com/jialong/smartthings/blob/master/device-types/aeon_smartstrip under MIT License, see below
 *
 *
 *  The MIT License (MIT)
 *  
 *  Copyright (c) 2014 Jialong Wu
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *
 */
metadata {
	definition (name: "MCO 2 Channel Switch S312-EU", namespace: "morfsta", author: "morfsta") {
    capability "Switch"
    capability "Polling"
    capability "Configuration"
    capability "Refresh"
    capability "Zw Multichannel"
        
        attribute "switch1", "string"
		attribute "switch2", "string"
        
        command "on1"
		command "off1"
		command "on2"
		command "off2"

    fingerprint deviceId: "0x1001", inClusters:"0x25 0x27 0x85 0x60 0x8E 0x72 0x86 0xEF 0x20 0x60"
}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("switch1", "device.switch1", canChangeIcon: true) {
                        state "on", label: "switch1", action: "off1", icon: "st.switches.switch.on", backgroundColor: "#79b821"
                        state "off", label: "switch1", action: "on1", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
                }
                
        standardTile("switch2", "device.switch2", canChangeIcon: true) {
                        state "on", label: "switch2", action: "off2", icon: "st.switches.switch.on", backgroundColor: "#79b821"
                        state "off", label: "switch2", action: "on2", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
                }
                
        standardTile("refresh", "device.switch1", inactiveLabel: false, decoration: "flat") {
                        state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
                }

        standardTile("configure", "device.switch2", inactiveLabel: false, decoration: "flat") {
        				state "default", label:"", action:"configure", icon:"st.secondary.configure"
                }


        main(["switch1", "switch2"])
        details(["switch1","switch2","refresh","configure"])
        
    }
}

// 0x25 - Switch binary
// 0x27 - Switch all
// 0x70 - Configuration
// 0x72
// 0x85 - Association
// 0x86 - Version
// 0x8E
// 0x7A
// 0x73
// 0xEF - Mark
// 0x60 - Multi channel


def parse(String description) {
    def result = null
    def cmd = zwave.parse(description, [0x20:1, 0x60:3, 0x25:1, 0x70:1, 0x32:1, 0x72:1])
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "Parsed ${description} to ${cmd} to ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

//Reports

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd) 
{
    log.debug "multichannelv3.MultiChannelCapabilityReport $cmd"
    if (cmd.endPoint == 2 ) {
        def currstate = device.currentState("switch2").getValue()
        if (currstate == "on")
        	sendEvent(name: "switch2", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
        	sendEvent(name: "switch2", value: "on", isStateChange: true, display: false)
    }
    else if (cmd.endPoint == 1 ) {
        def currstate = device.currentState("switch1").getValue()
        if (currstate == "on")
        sendEvent(name: "switch1", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
        sendEvent(name: "switch1", value: "on", isStateChange: true, display: false)
    }
}


def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
        log.debug "BasicReport $cmd.value"
        def map = [name: "switch1", type: "physical"]
        if (cmd.value == 0) {
        	map.value = "off"
        }
        else if (cmd.value == 255) {
        	map.value = "on"
        }
        //refresh()
        return map
        
}


def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
        [name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
   def map = [ name: "switch$cmd.sourceEndPoint" ]
   
   def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x25: 1, 0x20: 1])
   if (encapsulatedCommand && cmd.commandClass == 50) {
      zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint)
   } else {
   switch(cmd.commandClass) {
      case 32:
         if (cmd.parameter == [0]) {
            map.value = "off"
         }
         if (cmd.parameter == [255]) {
            map.value = "on"
         }
         createEvent(map)
         break
      case 37:
         if (cmd.parameter == [0]) {
            map.value = "off"
         }
         if (cmd.parameter == [255]) {
            map.value = "on"
         }
         createEvent(map)
         break
    }
    }
}

// handle commands

def refresh() {
	delayBetween([
    	log.debug("refreshing s1"),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
    	log.debug("refreshing s2"),
        zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
		//zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:2, commandClass:37, command:2).format()
  	])  
}

def poll() {
	log.debug "Executing 'poll'"
	delayBetween([
		//zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}

def configure() {
    log.debug "MCO2-configure"
    
    // currently hard-coded to two button switch
    enableEpEvents("1,2")

    // not sure the association is needed
    // documentation says last group is automatically associated
    // with controller node id by default, will test
    //commands([
    //    //zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier:3),
    //    zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId)
    //], 800)
}

def enableEpEvents(enabledEndpoints) {
    log.debug "MCO2-enabledEndpoints"
    state.enabledEndpoints = enabledEndpoints.split(",").findAll()*.toInteger()
    null
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
    log.debug("ManufacturerSpecificReport ${cmd.inspect()}")
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}


def on1() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[255]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
	])
}

def off1() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),

    ])
}

def on2() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[255]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
    ])
}

def off2() {
	delayBetween([
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:1, parameter:[0]).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:2, destinationEndPoint:2, commandClass:37, command:2).format(),
		zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:1, commandClass:37, command:2).format()
	])
}
