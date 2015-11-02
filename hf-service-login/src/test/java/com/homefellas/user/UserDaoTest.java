package com.homefellas.user;

import static org.junit.Assert.assertEquals;
import static com.homefellas.user.UserTestModelBuilder.*;

import javax.annotation.PostConstruct;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;



public class UserDaoTest extends AbstractUserTestDao
{

	
	@Autowired
	private IUserDao userDao;

	protected  RegisterTicket casRegisterTO;
	
	@Before
	public void buildTestData()
	{
//		super.recreateSchema();
		
		casRegisterTO = buildCasRegisterTO();
		userDao.createRegisterTicket(casRegisterTO);
	}

	@Autowired
	private UserService userService;
	
	@PostConstruct
	protected void disableTGT()
	{
		userService.setGenerateTGT(false);
	}
	
	@Test
	@Transactional
	public void testGetUserDetailsById() {
		Profile profile1 = createProfile();
		Profile profile3 = createGuest();
		
		UserDetails classUnderTest1 = userDao.getUserDetailsById(profile1.getMember().getUsername());
		assertEquals(profile1.getMember(), classUnderTest1);
		
//		UserDetails classUnderTest2 = loginDao.getUserDetailsById(partnerUser.getUsername());
//		assertEquals(partnerUser, classUnderTest2);
		
		UserDetails classUnderTest3 = userDao.getUserDetailsById(profile3.getMember().getUsername());
		assertEquals(profile3.getMember(), classUnderTest3);
	}
	
	@Test
	@Transactional
	public void getGetCasRegisterTO()
	{
		RegisterTicket classUnderTest = userDao.getCasRegisterTO(casRegisterTO.getTicket(), casRegisterTO.getEmail());
		assertEquals(casRegisterTO, classUnderTest);
	}
	
	@Test
	@Transactional
	public void testGetRole()
	{
		assertEquals(roleUser, userDao.getRole(RoleEnum.HF_USER_ROLE));
		
		assertEquals(roleAdmin, userDao.getRole(RoleEnum.HF_ADMIN_ROLE));
	}
}
