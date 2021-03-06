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


package keepinchecker.main;

import java.nio.file.Paths;
import java.sql.SQLException;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.sun.javafx.PlatformUtil;

import keepinchecker.constants.Constants;
import keepinchecker.database.entity.KeepInCheckerPacket;
import keepinchecker.database.entity.User;
import keepinchecker.database.manager.UserManager;
import keepinchecker.gui.KeepInCheckerSystemTray;

public class KeepInChecker {
	
	public static void main(String[] args) throws Exception {
		initializeDatabaseConnection();
		initializeUser();
		setSystemProperties();
		launchBackend();
		launchSystemTray();
	}
	
	private static void initializeDatabaseConnection() throws SQLException {
		ConnectionSource connectionSource = new JdbcConnectionSource(Constants.DATABASE_PATH);
		TableUtils.createTableIfNotExists(connectionSource, User.class);
		TableUtils.createTableIfNotExists(connectionSource, KeepInCheckerPacket.class);
	}
	
	private static void initializeUser() throws Exception {
		UserManager userManager = new UserManager();
		User user = userManager.getUser();
		if (user != null) {
			Constants.USER = user;
		}
	}
	
	private static void setSystemProperties() {
		String pathToResources = Paths.get("../lib").toAbsolutePath().normalize().toString();
		String pcapLibPackage = "org.pcap4j.core.pcapLibName";
		String packetLibPackage = "org.pcap4j.core.packetLibName";
		
		if (PlatformUtil.isMac()) {
			System.setProperty(pcapLibPackage, pathToResources + "/libpcap.dylib");
		} else if (PlatformUtil.isLinux()) {
		    	System.setProperty(pcapLibPackage, pathToResources + "/libpcap.so");
		} else if (PlatformUtil.isWindows()) {
			System.setProperty(pcapLibPackage, pathToResources + "\\wpcap");
			System.setProperty(packetLibPackage, pathToResources + "\\Packet");
		}
	}
	
	private static void launchBackend() {
		Thread backendThread = new Thread(new KeepInCheckerBackend());
		backendThread.start();
	}
	
	private static void launchSystemTray() {
		KeepInCheckerSystemTray systemTray = new KeepInCheckerSystemTray();
		systemTray.run();
	}

}
