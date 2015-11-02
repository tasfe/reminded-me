package com.homefellas.rm.user;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.homefellas.rm.AbstractRMTestDao;
import com.homefellas.rm.RMTestModelBuilder;
import com.homefellas.user.Profile;

public class PersonalPointScoreServiceTest extends AbstractRMTestDao
{

	@Autowired
	protected PersonalPointScoreService pointScoreService;
	
	@Test
	public void testGetLatestPersonalPointScoresTX()
	{
		Profile profile = createProfile();
		PersonalPointScore personalPointScore1 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().minusDays(2));
		PersonalPointScore personalPointScore2 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().minusDays(1));
		PersonalPointScore personalPointScore3 = RMTestModelBuilder.pps(profile.getMember(), new DateTime());
		PersonalPointScore personalPointScore4 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().plusDays(1));
		PersonalPointScore personalPointScore5 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().plusDays(2));
		
		try
		{
			
			pointScoreService.createPersonalPointScore(personalPointScore1);
			pointScoreService.createPersonalPointScore(personalPointScore2);
			pointScoreService.createPersonalPointScore(personalPointScore3);
			pointScoreService.createPersonalPointScore(personalPointScore4);
			pointScoreService.createPersonalPointScore(personalPointScore5);
			
			List<PersonalPointScore> ppss = pointScoreService.getLatestPersonalPointScoresTX(profile.getEmail());
			Assert.assertTrue(ppss.contains(personalPointScore3));
			
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			Assert.fail();
		}
		
	}

	@Test
	public void testGetPersonalPointScoreByDateTX()
	{
		Profile profile = createProfile();
		PersonalPointScore personalPointScore1 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().minusDays(2));
		PersonalPointScore personalPointScore2 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().minusDays(1));
		PersonalPointScore personalPointScore3 = RMTestModelBuilder.pps(profile.getMember(), new DateTime());
		PersonalPointScore personalPointScore4 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().plusDays(1));
		PersonalPointScore personalPointScore5 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().plusDays(2));
		
		try
		{
			pointScoreService.createPersonalPointScore(personalPointScore1);
			pointScoreService.createPersonalPointScore(personalPointScore2);
			pointScoreService.createPersonalPointScore(personalPointScore3);
			pointScoreService.createPersonalPointScore(personalPointScore4);
			pointScoreService.createPersonalPointScore(personalPointScore5);
			
			DateTime datetime = new DateTime().plusDays(2);
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MMddyyyy");
		
			PersonalPointScore pps = pointScoreService.getPersonalPointScoreByDateTX(profile.getEmail(),fmt.print(datetime.getMillis() ));
			Assert.assertEquals(personalPointScore5, pps);
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testGetPersonalPointScoreByDateRangeTX()
	{
		Profile profile = createProfile();
		PersonalPointScore personalPointScore1 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().minusDays(2));
		PersonalPointScore personalPointScore2 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().minusDays(1));
		PersonalPointScore personalPointScore3 = RMTestModelBuilder.pps(profile.getMember(), new DateTime());
		PersonalPointScore personalPointScore4 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().plusDays(1));
		PersonalPointScore personalPointScore5 = RMTestModelBuilder.pps(profile.getMember(), new DateTime().plusDays(2));
		
		try
		{
			pointScoreService.createPersonalPointScore(personalPointScore1);
			pointScoreService.createPersonalPointScore(personalPointScore2);
			pointScoreService.createPersonalPointScore(personalPointScore3);
			pointScoreService.createPersonalPointScore(personalPointScore4);
			pointScoreService.createPersonalPointScore(personalPointScore5);
			
			DateTime startdt = new DateTime().minusDays(2);
			DateTime enddt = new DateTime().plusDays(1);
			DateTimeFormatter fmt = DateTimeFormat.forPattern("MMddyyyy");
		
			List<PersonalPointScore> ppss= pointScoreService.getPersonalPointScoreByDateRangeTX(profile.getEmail(),fmt.print(startdt.getMillis()), fmt.print(enddt.getMillis()));
			Assert.assertTrue(ppss.contains(personalPointScore2));
			Assert.assertTrue(ppss.contains(personalPointScore3));
			Assert.assertTrue(ppss.contains(personalPointScore1));
			Assert.assertTrue(ppss.contains(personalPointScore4));
			Assert.assertFalse(ppss.contains(personalPointScore5));
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			Assert.fail();
		}
	}

}
