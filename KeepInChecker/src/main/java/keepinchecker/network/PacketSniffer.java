/** 
 * Copyright 2017 Brandon Ragsdale 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *  
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */


package keepinchecker.network;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapIpV4Address;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;

import keepinchecker.constants.Constants;
import keepinchecker.database.entity.KeepInCheckerPacket;
import keepinchecker.database.manager.KeepInCheckerPacketManager;

public class PacketSniffer {
	
	private static final KeepInCheckerPacketManager packetManager = new KeepInCheckerPacketManager();
	
	private Map<Timestamp, Packet> packetMap = new HashMap<>();
	
	public void sniffPackets() throws Exception {
		PcapNetworkInterface networkInterface = getNetworkInterface();
		if (networkInterface != null) {
		    PcapHandle handle = networkInterface.openLive(65536, PromiscuousMode.NONPROMISCUOUS, 5000);
		    handle.loop(2000, new KeepInCheckerPacketListener(handle));
		    
		    sendPacketsToDatabase(packetMap);
		}
	}
	
	protected boolean areGetHostAndRefererValuesEmpty(KeepInCheckerPacket packet) {
		if (packet.getGetValue() == null &&
				packet.getHostValue() == null &&
				packet.getRefererValue() == null) {
			return true;
		}
		
		return false;
	}
	
	private PcapNetworkInterface getNetworkInterface() throws PcapNativeException {
		PcapNetworkInterface networkInterface = null;
		List<PcapNetworkInterface> networkInterfaces = Pcaps.findAllDevs();
		if (networkInterfaces.isEmpty()) {
			return null;
		}
		
		for (PcapNetworkInterface nif : networkInterfaces) {	
			List<PcapAddress> addresses = nif.getAddresses();
			if (addresses.isEmpty()) {
				continue;
			}
			
			for (PcapAddress address : addresses) {
				if (address instanceof PcapIpV4Address &&
						!address.getAddress().isLoopbackAddress()) {	
					networkInterface = nif;
					break;
				}
			}
		}
		
		return networkInterface;
	} 
	
	private void storePacket(Packet packet, PcapHandle handle) {
		packetMap.put(handle.getTimestamp(), packet);
	}
	
	private void sendPacketsToDatabase(Map<Timestamp, Packet> packetMap) throws Exception {
		Set<KeepInCheckerPacket> objectionablePackets = new HashSet<>();
		
		for (Map.Entry<Timestamp, Packet> entry : packetMap.entrySet()) {
			Timestamp packetTime = entry.getKey();
			ZoneId currentTimezone = ZonedDateTime.now().getZone();
			String packetString = PacketParser.convertToHumanReadableFormat(entry.getValue());
			
			for (String objectionableWord : Constants.OBJECTIONABLE_WORDS) {
				if (StringUtils.contains(packetString, objectionableWord)) {
					KeepInCheckerPacket packet = new KeepInCheckerPacket();
					
					packet.setTimestamp(packetTime.getTime());
					packet.setTimezone(currentTimezone.getId());
					
					String parsedGetValue = PacketParser.parse(PacketParser.GET, packetString);
					String parsedHostValue = PacketParser.parse(PacketParser.HOST, packetString);
					String parsedReferValue = PacketParser.parse(PacketParser.REFERER, packetString);
					packet.setGetValue(parsedGetValue.getBytes(StandardCharsets.UTF_8));
					packet.setHostValue(parsedHostValue.getBytes(StandardCharsets.UTF_8));
					packet.setRefererValue(parsedReferValue.getBytes(StandardCharsets.UTF_8));
					
					if (!areGetHostAndRefererValuesEmpty(packet)) {						
						objectionablePackets.add(packet);
					}
					
					break;
				}
			}
		}
		
		if (!objectionablePackets.isEmpty()) {			
			packetManager.savePackets(objectionablePackets);
		}
	}
	
	
	private class KeepInCheckerPacketListener implements PacketListener {
		
		private PcapHandle handle;
		
		public KeepInCheckerPacketListener(PcapHandle handle) {
			this.handle = handle;
		}

		@Override
		public void gotPacket(Packet packet) {
			storePacket(packet, handle);
		}
		
	}

}
