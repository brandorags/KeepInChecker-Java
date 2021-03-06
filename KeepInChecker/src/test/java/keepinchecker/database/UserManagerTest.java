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


package keepinchecker.database;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import keepinchecker.database.entity.User;
import keepinchecker.database.manager.UserManager;
import keepinchecker.setup.KeepInCheckerTestCase;

public class UserManagerTest extends KeepInCheckerTestCase {
	
	private UserManager userManager;
	
	@Test
	public void testSaveGetUser() throws Exception {
		userManager = new UserManager();
		
		User user = new User();
		ArrayList<byte[]> partnerEmails = new ArrayList<>(Arrays.asList("test1@example.com".getBytes(StandardCharsets.UTF_8),
				"test2@example.com".getBytes(StandardCharsets.UTF_8)));
		user.setUserId(1);
		user.setUserName("TestUser".getBytes(StandardCharsets.UTF_8));
		user.setUserEmail("test@example.com".getBytes(StandardCharsets.UTF_8));
		user.setUserEmailPassword("password".getBytes(StandardCharsets.UTF_8));
		user.setPartnerEmails(partnerEmails);
		user.setEmailFrequency(User.EMAIL_FREQUENCY_DAILY);
		user.setEmailLastSentDate(123456789);
		
		userManager.saveUser(user);
		User userFromDb = userManager.getUser();
		
		assertEquals("User ID should have been autogenerated and saved to the database", 1, userFromDb.getUserId());
		assertEquals("User name should have been saved to the database", "TestUser", 
				new String(userFromDb.getUserName(), StandardCharsets.UTF_8));
		assertEquals("User email should have been saved to the database", "test@example.com",
				new String(userFromDb.getUserEmail(), StandardCharsets.UTF_8));
		assertEquals("User email password should have been saved to the database", "password", 
				new String(userFromDb.getUserEmailPassword(), StandardCharsets.UTF_8));
		assertEquals("Partner emails should have been saved to the database", 2, userFromDb.getPartnerEmails().size());	
		for (byte[] partnerEmail : userFromDb.getPartnerEmails()) {
			List<String> partnerEmailsAsStrings = new ArrayList<>(Arrays.asList("test1@example.com",
					"test2@example.com"));
			assertTrue("Partner email should have been saved to the database", 
					partnerEmailsAsStrings.contains(new String(partnerEmail, StandardCharsets.UTF_8)));
		}
		assertEquals("Email frequency should have been saved to the database", User.EMAIL_FREQUENCY_DAILY, userFromDb.getEmailFrequency());
		assertEquals("Email last sent date should have been saved to the database", 123456789, userFromDb.getEmailLastSentDate());
	}

}
