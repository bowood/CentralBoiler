preferences {
	input("deviceIP", "string", title:"IP Address", description: "IP Address", required: true, displayDuringSetup: true)
}

metadata {
	// Automatically generated. Make future change here.
    // Original ideas from "swap-file" found in smartthings forum
	definition (name: "CentralBoiler", author: "bowood") {
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Temperature Measurement"
	}

	// UI tile definitions
	tiles (scale: 2){
		
		valueTile("water", "device.water", inactiveLabel: false, width: 2,  height: 2 ) {
			state "temperature", label:'${currentValue}°',
   					backgroundColors: [
							[value: 173, color: "#153591"],
							[value: 176, color: "#1e9cbb"],
							[value: 179, color: "#90d2a7"],
							[value: 181, color: "#44b621"],
							[value: 184, color: "#f1d801"],
							[value: 187, color: "#d04e00"],
							[value: 190, color: "#bc2323"]
					]
		}
            
		valueTile("fire", "device.fire", inactiveLabel: false, width: 2,  height: 2 ) {
			state "temperature", label:'${currentValue}°',
   					backgroundColors: [
							[value: 200, color: "#153591"],
							[value: 300, color: "#1e9cbb"],
							[value: 400, color: "#90d2a7"],
							[value: 500, color: "#44b621"],
							[value: 600, color: "#f1d801"],
							[value: 700, color: "#d04e00"],
							[value: 800, color: "#bc2323"]
					]
		}
		valueTile("refresh", "command.refresh",width: 2,  height: 2) {
			state "default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon"
		}
		
		main(["water"])
        
		details(["water","fire","refresh"])
        
	}
}

def poll() {
	log.trace 'Poll Called'
	runCmd()
}

def refresh() {
	log.trace 'Refresh Called'
	runCmd()
}

def runCmd() {
	def host = deviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def LocalDevicePort = "80"
	def porthex = convertPortToHex(LocalDevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"

	//log.debug "The device id configured is: $device.deviceNetworkId"

	def headers = [:] 
	headers.put("HOST", "$host:$LocalDevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	
	//log.debug "The Header is $headers"
	
	//def path = '/api/LiveData.xml'
    def path = '/'
	def body = ''
	//log.debug "Uses which method: $DevicePostGet"
	def method = "GET"

	try {
		log.debug "Making TED5000 request to $device.deviceNetworkId"
		def hubAction = new physicalgraph.device.HubAction(
method: method,
path: path,
body: body,
headers: headers
		)
		hubAction.options = [outputMsgToS3:false]
		//log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}

def parse(String description) {
	//this is automatically called when the hub action returns
	def msg = parseLanMessage(description).body
	//log.debug "Got Reply: $msg"
    
    def water_search = (msg =~ /Water Temp:<\/span> <span class='ContentText'>(.*)<\/span>/)[0][1]
    def fire_search = (msg =~ /Fire Temp:<\/span><span class='ContentText'>(.*)<\/span>/)[0][1]
    
	log.debug "Got Reply: $water_search, $fire_search"
    
	def evt1 = createEvent (name: "water", value: Math.round(water_search.toFloat()).toInteger(), unit:"F")
    def evt2 = createEvent (name: "fire", value: fire_search.toInteger(), unit:"F")
	return [evt1,evt2]
}