package com.homefellas.user;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class AbstractUserServiceTest extends AbstractUserTestDao
{

	@Autowired
	private UserService userService;
	
	@Test
	public void testGetRole()
	{
		Role classUnderTest = userService.getRole(RoleEnum.HF_USER_ROLE);
		assertEquals(roleUser, classUnderTest);
		
		classUnderTest = userService.getRole(RoleEnum.HF_ADMIN_ROLE);
		assertEquals(roleAdmin, classUnderTest);
		
		assertInCache(roleCache, roleAdmin);
		assertInCache(roleCache, roleUser);

	}
	
	protected Profile getProfileByEmail(String email)
	{
		return userService.getProfileByEmail(email);
	}
}
