definition(
    name: "Beacon Trap",
    namespace: "dcm.beacontrap",
    author: "Dominick Meglio",
    description: "Integrates with the Beacon Trap app to monitor location with iBeacons and Eddystones",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "prefBeacons")
	page(name: "prefPeople")
	page(name: "prefUrlInfo")
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
	for (def i = 0; i < beaconCount; i++) {
		def beaconName = this.getProperty("beacon${i+1}")
		for (def j = 0; j < peopleCount; j++) {
			def personName = this.getProperty("person${j+1}")
			createOrUpdateChildDevice(beaconName, personName)
		}
	}
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def createOrUpdateChildDevice(beacon,person) {
	def childDevice = getChildDevice("beacon:" + beacon + ":"+ person)
    if (!childDevice) {
		addChildDevice("hubitat", "Virtual Presence", "beacon:" + beacon + ":"+ person, 1234, [name: "${person} - ${beacon}", isComponent: false])
    }
}

def prefBeacons() {
	if (state.accessToken == null)
		createAccessToken()
    return dynamicPage(name: "prefBeacons", title: "Beacons", nextPage: "prefPeople", uninstall:false, install: false) {
		section("Beacons") {
			input("beaconCount", "number", title: "How many beacons do you want to define?", required: true, defaultValue: 0, submitOnChange: true)
			if (beaconCount != null) {
				for (def i = 0; i < beaconCount; i++)
					input("beacon${i+1}", "string", title: "Name of Beacon ${i+1}", required: true)
			}
		}
	}
}

def prefPeople() {
	return dynamicPage(name: "prefPeople", title: "People", nextPage: "prefUrlInfo", uninstall:false, install: false) {
		section("People") {
			input("peopleCount", "number", title: "How many people do you want to define?", required: true, defaultValue: 0, submitOnChange: true)
			if (peopleCount != null) {
				for (def i = 0; i < peopleCount; i++)
					input("person${i+1}", "string", title: "Name of Person ${i+1}", required: true)
			}
		}
	}
}

def prefUrlInfo() {
	return dynamicPage(name: "prefUrlInfo", title: "URLs", install: true, uninstall: true) {
		section("URLs") {
			for (def i = 0; i < beaconCount; i++) {
				def beaconName = java.net.URLEncoder.encode(this.getProperty("beacon${i+1}"), "UTF-8")
				for (def j = 0; j < peopleCount; j++) {
					def personName = java.net.URLEncoder.encode(this.getProperty("person${j+1}"), "UTF-8")
					def enterUrl = "${getFullApiServerUrl()}/enter/${beaconName}/${personName}?access_token=${state.accessToken}"
					def exitUrl = "${getFullApiServerUrl()}/exit/${beaconName}/${personName}?access_token=${state.accessToken}"
					paragraph "URL for ${beaconName} ${personName} enter ${enterUrl}"
					paragraph "URL for ${beaconName} ${personName} exit ${exitUrl}"
				}
			}
		}
	}
}

mappings {
    path("/enter/:beacon/:person") {
        action: [
            POST: "postEnter"
        ]
    }
    path("/exit/:beacon/:person") {
        action: [
            POST: "postExit"
        ]
    }
}

def postEnter() {
	handleEvent("arrived", java.net.URLDecoder.decode(params.beacon), java.net.URLDecoder.decode(params.person))
}

def postExit() {
	handleEvent("departed", java.net.URLDecoder.decode(params.beacon), java.net.URLDecoder.decode(params.person))
}

def handleEvent(event, beacon, person) {
log.debug beacon
log.debug person
	def device = getChildDevice("beacon:${beacon}:${person}")
	if (event == "arrived")
		device.arrived()
	else if (event == "departed")
		device.departed()
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}