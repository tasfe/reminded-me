package com.homefellas.rm.share;

import static com.homefellas.rm.RMTestModelBuilder.buildCalendar;
import static com.homefellas.rm.RMTestModelBuilder.buildInvite;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import com.homefellas.batch.Notification;
import com.homefellas.batch.NotificationService;
import com.homefellas.exception.ValidationException;
import com.homefellas.rm.AbstractRMTestDao;
import com.homefellas.rm.ISynchronizeable.SynchronizeableObject;
import com.homefellas.rm.RMTestModelBuilder;
import com.homefellas.rm.ValidationCodeEnum;
import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.notification.ClientNotification;
import com.homefellas.rm.reminder.Alarm;
import com.homefellas.rm.share.Share.ShareStatus;
import com.homefellas.rm.task.Task;
import com.homefellas.rm.task.TaskUpdateOperation;
import com.homefellas.rm.user.Contact;
import com.homefellas.user.Profile;
import com.homefellas.user.UserService;

public class ShareServiceTest extends AbstractRMTestDao
{

	@Resource(name="userService")
	private UserService userService;
	
	@Resource(name="shareService")
	private ShareService shareService;
	
	@Resource(name="notificationService")
	private NotificationService notificationService;
	
	

	@Resource(name="shareDao")
	private ShareDao shareDao;
	
	@Test
	public void getTasksByCalendarDirectLinkTX()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Profile profile3Guest1 = createGuest();
		Profile profile4Guest2 = createGuest();
		
		Task task2 = createTask(profile1);
		
		
		Calendar calendar = buildCalendar(true, "test calendar", profile1.getMember());
		calendar.setPublicList(false);
		try
		{
		calendarService.createCalendar(calendar);
		
		task2.addCalendar(calendar);
		taskService.updateTaskWithCalendar(task2, profile1.getEmail());
		 
		Invite invite1 = buildInvite(true, calendar.getId(), Calendar.class.getName());
		Map<String, String>emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile3Guest1.getMember().getEmail(), null);
		emailAddresses.put(profile4Guest2.getMember().getEmail(), null);
		invite1.setEmailAddresses(emailAddresses);
		
			invite1 = shareService.shareCalender(invite1);
			
			Assert.assertTrue(invite1.isPrimaryKeySet());
			Assert.assertNotNull(shareService.getCalenderShares(calendar));
			Assert.assertNotNull(shareService.getShareForUserAndTask(task2, profile3Guest1.getMember()));
			Assert.assertNotNull(shareService.getShareForUserAndTask(task2, profile4Guest2.getMember()));
			
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		//test no access
		try
		{
			shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile2.getEmail());
			
			Assert.fail();
		}
		catch (ValidationException e)
		{
			assertValidation(e, ValidationCodeEnum.MEMBER_DOES_NOT_HAVE_AUTHORIZATION);
		}
		
		//test bad email
		try
		{
			shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), "12345");
			
			Assert.fail();
		}
		catch (ValidationException e)
		{
			assertValidation(e, ValidationCodeEnum.LOGGED_IN_MEMBER_DOES_NOT_EXIST);
		}
		
		List<Task> tasks;
		
		try
		{
			//test task owner
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile1.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
			//test sharee
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile3Guest1.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile4Guest2.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
			calendar.setPublicList(true);
			calendarService.updateCalendar(calendar, profile1.getEmail());
			
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile1.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile2.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile3Guest1.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
			tasks = shareService.getTasksByCalendarDirectLinkTX(calendar.getId(), profile4Guest2.getEmail());
			Assert.assertTrue(tasks.contains(task2));
			
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		
		
		//test public

	}
	
	@Test
	public void shareAndCreateContact()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Profile sharee1 = createProfile();
		Profile sharee2 = createProfile();
		
		Task task1 = createTask(profile1);
		Task task2 = createTask(profile1);
		Task task3 = createTask(profile2);
		
		Share share1 = createShare(task1, sharee1);
		Share share2 = createShare(task1, sharee2);
		Share share3 = createShare(task2, sharee1);
		Share share4 = createShare(task3, sharee1);
		
		Contact contact = shareDao.getContactByProfileId(sharee1.getId(), profile1.getId());
		Assert.assertEquals(2, contact.getContactCounter());
		Assert.assertEquals(1, shareDao.getContactByProfileId(sharee2.getId(), profile1.getId()).getContactCounter());
		Assert.assertEquals(1, shareDao.getContactByProfileId(sharee1.getId(), profile2.getId()).getContactCounter());
		
		
	
	}
	
	@Test
	public void shareTaskVerifyAlamrs()
	{
		Profile profile = createProfile();
		DateTime endTime = new DateTime().plusDays(1);
		Task task = RMTestModelBuilder.task(profile);
		task.setEndTime(endTime);
		task = createTask(task);
		
		Alarm alarm1 = createAlarm(task, endTime.minusHours(1));
		Alarm alarm2 = createAlarm(task, endTime.minusMinutes(5));
		
		Profile sharee = createGuest();
		Share share = createShare(task, sharee);
		
		try
		{
			shareService.acceptShareTX(share.getId());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());	
		}
		
		List<Alarm> alarms = reminderService.getAlarmByTaskAndMember(task, sharee.getMember());
		Assert.assertEquals(2, alarms.size());
		
	}
	
	
	@Test
	public void shareSubList()
	{
		Profile profile = createProfile(); 
		Task parentTask = RMTestModelBuilder.task(profile);
		parentTask.setSubTasksCount(3);
		parentTask = createTask(parentTask);
		
		Task child1 = RMTestModelBuilder.task(profile);
		child1.setParentId(parentTask.getId());
		child1 = createTask(child1);
		
		Task child2 = RMTestModelBuilder.task(profile);
		child2.setParentId(parentTask.getId());
		child2 = createTask(child2);
		
		Task child3 = RMTestModelBuilder.task(profile);
		child3.setParentId(parentTask.getId());
		child3.setSubTasksCount(2);
		child3 = createTask(child3);
		
		Task grandChild1  = RMTestModelBuilder.task(profile);
		grandChild1.setParentId(child3.getId());
		grandChild1 = createTask(grandChild1);
		
		Task grandChild2  = RMTestModelBuilder.task(profile);
		grandChild2.setParentId(child3.getId());
		grandChild2 = createTask(grandChild2);
		
		Profile sharee = createProfile();
		createShare(parentTask, sharee);
		
		Share share1 = shareService.getShareForUserAndTask(parentTask, sharee.getMember());
		Assert.assertNotNull(share1);
		
		Share share2 = shareService.getShareForUserAndTask(child1, sharee.getMember());
		Assert.assertNotNull(share2);
		
		Share share3 = shareService.getShareForUserAndTask(child2, sharee.getMember());
		Assert.assertNotNull(share3);
		
		Share share4 = shareService.getShareForUserAndTask(child3, sharee.getMember());
		Assert.assertNotNull(share4);
		
		Share share5 = shareService.getShareForUserAndTask(grandChild1, sharee.getMember());
		Assert.assertNotNull(share5);
		
		Share share6 = shareService.getShareForUserAndTask(grandChild2, sharee.getMember());
		Assert.assertNotNull(share6);
		
	}
	@Test
	public void notificationSentToShareeOnTaskEndReached()
	{
		Profile profile = createProfile(); 
		Task task = RMTestModelBuilder.task(profile);
		task.setEndTime(new DateTime().plusDays(2));
		task = createTask(task);
		Profile sharee = createProfile();
		
		Share share = createShare(task, sharee);
		try
		{
			shareService.acceptShareTX(share.getId());
			
			List<Notification> notifications = notificationService.getNotificationsForINotificationAndToEmailTX(task, sharee.getEmail());
			Assert.assertEquals(1, notifications.size());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void acceptDeclineShareCaledarTX()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Profile profile4Guest2 = createGuest();
		
		Task task2 = createTask(profile1);
		Task task3 = createTask(profile1);
		
		
		Calendar calendar = new Calendar();
		try
		{
		calendar.setCalendarName("Stuff to do");
		calendar.setMember(profile2);
		calendarService.createCalendar(calendar);
		
		task2.addCalendar(calendar);
		taskService.updateTaskWithCalendar(task2, profile1.getEmail());
		
		task3.addCalendar(calendar);
		task2.addCalendar(calendar);
		taskService.updateTaskWithCalendar(task3, profile1.getEmail());
		
		Invite invite = new Invite();
		Map<String, String>emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile4Guest2.getEmail(), null);
		invite.setEmailAddresses(emailAddresses);
		invite.setInviter(profile2.getMember());
		invite.setMessage("some message");
		invite.setSubject("some subject");
		invite.setDirectLink("http://reminded.me");
		invite.setShareId(calendar.getId());
	
			shareService.shareCalender(invite);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		//test to make sure authorization works
		try
		{
			ShareCalendar shareCalendar = shareService.getCalenderShares(calendar).get(0);
			shareService.acceptDeclineShareCaledarTX(shareCalendar.getId(), ShareApprovedStatus.APPROVED, profile2.getEmail());
			Assert.fail();
		}
		catch (ValidationException exception)
		{
			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.MEMBER_DOES_NOT_HAVE_AUTHORIZATION));
		}
		
		//test for invalid calendar share
		try
		{
			shareService.acceptDeclineShareCaledarTX(calendar.getId(), ShareApprovedStatus.APPROVED, profile2.getEmail());
			Assert.fail();
		}
		catch (ValidationException exception)
		{
			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.SHARE_CALENDAR_NOT_FOUND));
		}
		
		//test to make sure that both tasks are set to approved
		try
		{
			ShareCalendar shareCalendar = shareService.getCalenderShares(calendar).get(0);
			Assert.assertNotNull(shareCalendar);
			
			shareService.acceptDeclineShareCaledarTX(shareCalendar.getId(), ShareApprovedStatus.APPROVED, profile4Guest2.getEmail());
			
			Share share = shareService.getShareForUserAndTask(task2, profile4Guest2.getMember());
			Assert.assertNotNull(share);
			Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
			
			share = shareService.getShareForUserAndTask(task3, profile4Guest2.getMember());
			Assert.assertNotNull(share);
			Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		
	}
	
	@Test
	public void sendNotificationsToShares() throws ValidationException
	{
		Profile profile1 = createProfile();
		Task task6 = createTask(profile1);
		
		Invite invite = new Invite();
		invite.setMessage("send notification to shares");
		invite.setSubject("subject");
		invite.setShareId(task6.getId());
		shareService.messageShares(invite);
		
	}
	
	@Test
	public void shareCalender()
	{
		Profile profile1 = createProfile();
		Profile profile3Guest1 = createGuest();
		Profile profile4Guest2 = createGuest();
		
		Task task2 = createTask(profile1);
		
		
		Calendar calendar = buildCalendar(true, "test calendar", profile1.getMember());
		try {
			calendarService.createCalendar(calendar);
		

		
		task2.addCalendar(calendar);
		taskService.updateTaskWithCalendar(task2, profile1.getEmail());
		 
		Invite invite1 = buildInvite(true, calendar.getId(), Calendar.class.getName());
		Map<String, String>emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile3Guest1.getMember().getEmail(), null);
		emailAddresses.put(profile4Guest2.getMember().getEmail(), null);
		invite1.setEmailAddresses(emailAddresses);
		
			invite1 = shareService.shareCalender(invite1);
			
			Assert.assertTrue(invite1.isPrimaryKeySet());
			Assert.assertNotNull(shareService.getCalenderShares(calendar));
			Assert.assertNotNull(shareService.getShareForUserAndTask(task2, profile3Guest1.getMember()));
			Assert.assertNotNull(shareService.getShareForUserAndTask(task2, profile4Guest2.getMember()));
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
	
		
		
		
	}
	@Test
	public void processSharesForSharedCalendars()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
			
		Calendar userCreatedCalendar = createCalendar(profile1);
		Calendar calendarUserCreated = createCalendar(profile1);
		
		Task task1 = RMTestModelBuilder.task(profile1);
		task1.addCalendar(userCreatedCalendar);
		task1.addCalendar(calendarUserCreated);
			
		task1 = createTask(task1);
		
		Task task2 = createTask(profile1);
		Task task5 = createTask(profile1);
		
		createCalendarShare(userCreatedCalendar, profile2);
		
		
		//test 1 task without calendar
		try
		{
			shareService.processSharesForSharedCalendars(task5, false, null);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		Assert.assertTrue(shareService.getSharesForTask(task5).isEmpty());
		
		//test 2 task with calendars, but none of them shared 
		try
		{
			Calendar nonSharedCalendar = buildCalendar(true, "nonshare calendar", profile1.getMember());
			calendarService.createCalendar(nonSharedCalendar);
			
			task2.addCalendar(nonSharedCalendar);
			taskService.updateTaskWithCalendar(task2, task2.getTaskCreator().getEmail());
			shareService.processSharesForSharedCalendars(task2, false, null);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		Assert.assertTrue(shareService.getSharesForTask(task2).isEmpty());
		
		//test 3 task with calendars with one shared and one not shared
		try
		{
			shareService.processSharesForSharedCalendars(task1, false, null);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		//check to see if one share is created
		Assert.assertEquals(task1,shareService.getSharesForTask(task1).get(0).getTask());
		Assert.assertEquals(profile2.getMember(),shareService.getSharesForTask(task1).get(0).getUser());
		
		//test 5 task with 1 calendar, then share calendar, then add new task to that calendar 
//		Task newTask = buildSampleTask(true, null, null, false, null, profile1, null, PriorityEnum.LOW, null, null);
		Calendar sharedCalendar = createCalendar(profile1);
		Task newTask = RMTestModelBuilder.task(profile1);
		newTask.setTitle(" new Task 11");
		newTask.addCalendar(sharedCalendar);
		newTask = createTask(newTask);
		
//		createCalendarShare(sharedCalendar, profile2);
//		Calendar sharedCalendar = buildCalendar(true, "nonshare calendar", profile1.getMember());
		
		
//		dao.save(sharedCalendar);
//		newTask.addCalendar(sharedCalendar);
//		dao.save(newTask);
		
//		createCalendarShare(sharedCalendar, profile2);
//		Invite invite3Task1 = RMTestModelBuilder.invite(sharedCalendar, profile2);
//		ShareCalendar shareCalendar1Profile1Calendar1 = new ShareCalendar();
//		shareCalendar1Profile1Calendar1.generateUnquieId();
//		shareCalendar1Profile1Calendar1.setCalendar(sharedCalendar);
//		shareCalendar1Profile1Calendar1.setUser(profile2.getMember());
//		shareCalendar1Profile1Calendar1.setInvite(invite3Task1);
//		dao.save(shareCalendar1Profile1Calendar1);
		
		
		
		Invite invite3Task1 = buildInvite(true, calendarUserCreated.getId(), Calendar.class.getName());
		invite3Task1.setInviter(calendarUserCreated.getMember().getMember());
		
		shareDao.createInvite(invite3Task1);
		
		ShareCalendar shareCalendar1Profile1Calendar1 = new ShareCalendar();
		shareCalendar1Profile1Calendar1.generateUnquieId();
		shareCalendar1Profile1Calendar1.setCalendar(sharedCalendar);
		shareCalendar1Profile1Calendar1.setUser(profile2.getMember());
		shareCalendar1Profile1Calendar1.setInvite(invite3Task1);
		shareDao.createShareCalendar(shareCalendar1Profile1Calendar1);
		
		try
		{
			shareService.processSharesForSharedCalendars(newTask, false, null);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		Assert.assertEquals(newTask,shareService.getSharesForTask(newTask).get(0).getTask());
		
		//test 7 update task but dont touch the calendars
		newTask.setTitle("new title");
		try
		{
			shareService.processSharesForSharedCalendars(newTask, true, null);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		Assert.assertEquals(newTask,shareService.getSharesForTask(newTask).get(0).getTask());
		
		//test 6 update same task and remove calendar and make sure share is removed
		try
		{
			List<String> oldCalendarKeys = new ArrayList<String>(newTask.getCalendars().size());
			Set<Calendar> oldListOfCalendars = newTask.getCalendars();
			if (oldListOfCalendars!=null&&!oldListOfCalendars.isEmpty())
			{
				for (Calendar calendar:oldListOfCalendars)
					oldCalendarKeys.add(calendar.getId());
			}
			newTask.getCalendars().clear();
			shareService.processSharesForSharedCalendars(newTask, true, oldCalendarKeys);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		Assert.assertTrue(shareService.getSharesForTask(newTask).isEmpty());
		
		
		
		
	}
	
	@Test
	public void getCalenderShares()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Calendar calendarUserCreated = createCalendar(profile1);
		
		ShareCalendar shareCalendar1Profile1Calendar1 = createCalendarShare(calendarUserCreated, profile2);
		
		List<ShareCalendar> calendarShares = shareService.getCalenderShares(calendarUserCreated);
		Assert.assertTrue(calendarShares.contains(shareCalendar1Profile1Calendar1));
	}
	
	@Test
	public void updateModifiedTimeOfShares()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Profile profile3Guest1 = createGuest();
		Profile profile4Guest2 = createGuest();
		
		Task task6 = createTask(profile1);
		
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		Share share3Profile4Task6 = createShare(task6, profile4Guest2);
		Share share4Profile2Task6 = createShare(task6, profile2);
		
		long now = System.currentTimeMillis();
		
		try {Thread.sleep(1000);}catch (InterruptedException e){e.printStackTrace();}
		
		try
		{
			shareService.updateSharesWithModifiedTimeAndNotifyUser(task6, profile1.getEmail(), TaskUpdateOperation.TITLE);
		
		
			Share share = shareService.getShareById(share1Profile3Task6.getId());
			Assert.assertTrue(share.getModifiedDateZone().getMillis()>now);
			
			share = shareService.getShareById(share3Profile4Task6.getId());
			Assert.assertTrue(share.getModifiedDateZone().getMillis()>now);
			
			share = shareService.getShareById(share4Profile2Task6.getId());
			Assert.assertTrue(share.getModifiedDateZone().getMillis()>now);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
	}
	

	
	
	
	@Test
	public void getShareForUserAndIShare()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Profile profile3Guest1 = createGuest();
		Profile profile4Guest2 = createGuest();
		
		Task task2 = createTask(profile1);
		Task task3 = createTask(profile1);
		Task task6 = createTask(profile1);
		Task task9 = createTask(profile2);
		
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		
		Calendar calendarUserCreated = createCalendar(profile1);
		
		ShareCalendar shareCalendar1Profile1Calendar1 = createCalendarShare(calendarUserCreated, profile2);
		try
		{
			Share shareUnderTest = (Share)shareService.getShareForUserAndIShare(task6, profile3Guest1.getMember());
		
			Assert.assertEquals(share1Profile3Task6, shareUnderTest);
			
			ShareCalendar calendarUnderTest = (ShareCalendar)shareService.getShareForUserAndIShare(calendarUserCreated, profile2.getMember());
			
			Assert.assertEquals(shareCalendar1Profile1Calendar1, calendarUnderTest);
		}
		catch (ValidationException validationException)
		{
			Assert.fail(validationException.getMessage());
		}
	}

	
	
	
	
//	@Test
//	public void buildAndMergeIShareable()
//	{
//		Invite invite1 = buildInvite(true, task1.getId(), Task.class.getName());
//		invite1.setInviter(task1.getTaskCreator().getMember());
//		
//		dao.save(invite1);
//		
//		Invite invite2 = buildInvite(true, calendarUserCreated.getId(), Calendar.class.getName());
//		invite2.setInviter(calendarUserCreated.getMember());
//		
//		dao.save(invite2);
//		
//		try
//		{
//			Share share = (Share)shareService.buildAndMergeIShareable(profile4Guest2.getMember(), task1, invite1);
//			
//			Assert.assertTrue(share.isPrimaryKeySet());
//			
//			
//			ShareCalendar shareCalendar = (ShareCalendar)shareService.buildAndMergeIShareable(profile4Guest2.getMember(), calendarUserCreated, invite2);
//			
//			Assert.assertTrue(shareCalendar.isPrimaryKeySet());
//		}
//		catch (ValidationException e)
//		{
//			Assert.fail(e.getMessage());
//		}
//	}
	
	@Test
	public void updateShareNullChecks()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		
		Task task6 = createTask(profile1);
		
		Share share4Profile2Task6 = createShare(task6, profile2);
		
		try
		{
			share4Profile2Task6.setUser(null);
			share4Profile2Task6.setTask(new Task());
			Share share = shareService.updateShare(share4Profile2Task6);
			Assert.fail();
			
		}
		catch (ValidationException e)
		{
			assertValidation(e, ValidationCodeEnum.TASK_ID_IS_NULL);
			assertValidation(e, ValidationCodeEnum.USER_ID_IS_NULL);
		}
		
	}
	@Test 
	public void testUpdateShare()
	{
		Profile profile1 = createProfile();
		Profile profile3Guest1 = createGuest();
		Task task6 = createTask(profile1);
		
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		
		share1Profile3Task6.setStatus(ShareStatus.DELETED.ordinal());
		try
		{
			Share share = shareService.updateShare(share1Profile3Task6);
			Assert.assertEquals(ShareStatus.DELETED.ordinal(), shareService.getShareById(share1Profile3Task6.getId()).getStatus());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
	}
	
//	@Test 
//	public void testUpdateInvite()
//	{
//		invite1Task6.setMessage("abcd");
//		try
//		{
//			Invite invite = shareService.updateInviteTX(invite1Task6);
//			Assert.assertEquals("abcd", dao.loadByPrimaryKey(Invite.class, invite1Task6.getId()).getMessage());
//		}
//		catch (ValidationException e)
//		{
//			Assert.fail(e.getMessage());
//		}
//	}
	
//	@Test
//	public void testBulkShare()
//	{
//		Profile profile1 = createProfile();
//		Profile profile2 = createProfile();
//		Profile profile3Guest1 = createGuest();
//	
//		Task task6 = createTask(profile1);
//		Task task9 = createTask(profile2);
//		
//		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
//		Share share2Profile3Task9 = createShare(task9, profile3Guest1);
//		
//		String ids = share1Profile3Task6.getId()+","+share2Profile3Task9.getId();
//		List<Share> shares = shareService.getBulkShares(ids);
//		Assert.assertTrue(shares.contains(share1Profile3Task6));
//		Assert.assertTrue(shares.contains(share2Profile3Task9));
//	}
//	
	
//	@Test 
//	public void testBulkInvite()
//	{
//		Profile profile1 = createProfile();
//		Profile profile3Guest1 = createGuest();
//		
//		Task task6 = createTask(profile1);
//		Task task9 = createTask(profile1);
//		
//		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
//		Share share2Profile3Task9 = createShare(task9, profile3Guest1);
//		
//		List<Invite> invites = shareService.getInvitesSharedWithEmailTX(profile3Guest1.getId());
//		Invite id1 = invites.get(0);
//		Invite id2 = invites.get(1);
//		String ids = id1.getId()+","+id2.getId();
//		invites = shareService.getBulkInvites(ids);
//		Assert.assertTrue(invites.contains(id1));
//		Assert.assertTrue(invites.contains(id2));
//	}
	@Test
	public void testLinkReplace()
	{
		Profile profile1 = createProfile();
		Profile profile4Guest2 = createGuest();
		
		Task task1 = createTask(profile1);
		
		Invite invite = RMTestModelBuilder.buildInvite(true, task1.getId(), Task.class.getName());
		invite.setMessage("${link}");
		
		
		Map<String, String>emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile4Guest2.getMember().getEmail(), null);
		invite.setEmailAddresses(emailAddresses);
		try
		{			
			shareService.shareTask(invite);						
		}
		catch (ValidationException exception)
		{
			Assert.fail(exception.getMessage());
		}
				
		Share share = shareService.getShareForUserAndTask(task1, profile4Guest2.getMember());
		Notification notification = notificationService.getNotificationsForINotificationTX(share).get(0);
//		List<Invite> invites = shareService.getInvitesSharedWithEmail(profile.getId());
//		invite = invites.get(0);
		Assert.assertFalse(notification.getBody().contains("${link}"));
		
	}
	
	@Test
	public void acceptPublicShare()
	{
		Profile profile1 = createProfile();
		Profile profile3Guest1 = createGuest();
		Profile profile4Guest2 = createGuest();
		
		Task task1 = createTask(profile1);
		Task task6 = createTask(profile1);
		Task task9 = RMTestModelBuilder.task(profile1);
		task9.setPublicTask(false);
		task9 = createTask(task9);
		
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		
		//test to make sure an existing share is marked as public
		try
		{
			shareService.acceptPublicShareTX(task6.getId(), profile3Guest1.getId());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		Share share = shareService.getShareById(share1Profile3Task6.getId());
		assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
		
		
		//test to make sure private task is rejected
		try
		{
			shareService.acceptPublicShareTX(task9.getId(), profile1.getId());
			Assert.fail();
		}
		catch (ValidationException e)
		{
			assertValidation(e, ValidationCodeEnum.CANNOT_ACCEPT_PRIVATE_TASK);
		}
		
		
		//test to make sure public task is created
		try
		{
			Share shareundertest = shareService.acceptPublicShareTX(task1.getId(), profile4Guest2.getId());
			Assert.assertEquals(shareService.getShareForUserAndTask(task1, profile4Guest2.getMember()), shareundertest);
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		
	}
	
	@Test
	public void getInvitesSharedWithEmail()
	{
		Profile profile1 = createProfile();	
		Profile profile2 = createProfile();
		
		Task task1 = createTask(profile1);
		Task task2 = createTask(profile1);
		
		Invite invite1 = RMTestModelBuilder.buildInvite(true, task1.getId(), Task.class.getName());
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile2.getMember().getEmail(), profile2.getName());
		invite1.setEmailAddresses(emailAddresses);
		try
		{
			shareService.shareTask(invite1);
		
		
			Invite invite2 = RMTestModelBuilder.buildInvite(true, task2.getId(), Task.class.getName());
			emailAddresses = new HashMap<String, String>();
			emailAddresses.put(profile2.getMember().getEmail(), profile2.getName());
			invite2.setEmailAddresses(emailAddresses);
			shareService.shareTask(invite2);
			
			List<Invite> invites = shareService.getInvitesSharedWithEmailTX(profile2.getMember().getId());
			Assert.assertTrue(invites.contains(invite1));
			Assert.assertTrue(invites.contains(invite2));
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
	}
		
	
//	@Test
//	public void getInvitedContactsByMemberId()
//	{
//		//get profile 1 contacts.  This should consist of everyone we have shared with.  profile should have contacts with
////		List<Profile> searchResults = shareService.getInvitedContactsByMemberId(profile1.getId(), profile3Guest1.getMember().getEmail());
//		profile3Guest1.setName("test");
//		dao.updateObject(profile3Guest1);
//		
//		List<Profile> searchResults = shareService.getInvitedContactsByMemberId(profile1.getId(), profile3Guest1.getName().substring(0,3));
////		Assert.assertTrue(searchResults.contains(profile3Guest1));
//	}
	
	@Test
	public void deleteShare()
	{
		Profile profile2 = createProfile();
		Profile profile3Guest1 = createGuest();
		
		Task task9 = createTask(profile2);
		Share share2Profile3Task9 = createShare(task9, profile3Guest1);
		
		shareService.deleteShare(share2Profile3Task9);
		
		Share shareUnderTest = shareDao.getShareById(share2Profile3Task9.getId());
		Assert.assertEquals(ShareStatus.DELETED.ordinal(), shareUnderTest.getStatus());
	}
	
	@Test
	public void getShareForTaskAndEmail()
	{
		Profile profile1 = createProfile();
		Profile profile3Guest1 = createGuest();
		Task task6 = createTask(profile1);
		
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		
		Assert.assertEquals(share1Profile3Task6, shareService.getShareForTaskAndEmail(task6, profile3Guest1.getEmail()));
	}
	
	@Test
	public void createShareOnRegister()
	{
		Profile profile1 = createProfile();	
		Profile profile4Guest2 = createGuest();
		
		Task task9 = createTask(profile1);
		
		profile4Guest2.getMember().setSharedTaskId(task9.getId());
		try
		{
			shareService.createShareOnRegister(profile4Guest2);
			
//			shareCounter++;
//			assertTableRowChecks();
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		profile4Guest2.getMember().setSharedTaskId("213123123");
		try
		{
			shareService.createShareOnRegister(profile4Guest2);
			
			Assert.fail();
			
		}
		catch (ValidationException e)
		{
			assertValidation(e, ValidationCodeEnum.TASK_NOT_FOUND);
		}
	}
	
	@Test
	public void getSyncShareIdsForMember()
	{		
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Task task6 = createTask(profile1);
		
		Share share4Profile2Task6 = createShare(task6, profile2);
		
		List returnLst = shareService.getSyncShareIdsForMember(SynchronizeableObject.Calendar, profile2.getId());
		Assert.assertTrue(returnLst.isEmpty());
		
		try
		{
			shareService.acceptShareTX(share4Profile2Task6.getId());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		Share share = shareDao.getShareById(share4Profile2Task6.getId());
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
		
		returnLst = shareService.getSyncShareIdsForMember(SynchronizeableObject.Task, profile2.getId());
		Assert.assertTrue(returnLst.contains(task6.getId()));
	}
	
	@Test
	public void getShareDirectLink()
	{
		Task taskUnderTest;
		Profile profile1 = createProfile();	
		Profile profile2 = createProfile();
		Profile profile4Guest2 = createGuest();
		
		Task task1 = createTask(profile1);
		Task task6 = createTask(profile1);
		Task task9 = RMTestModelBuilder.task(profile2);
		task9.setPublicTask(false);
		task9 = createTask(task9);

		//test4 test to make sure a public task is just returned
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task1.getId(), task1.getTaskCreator().getMember().getEmail()).getTask();
			Assert.assertEquals(task1, taskUnderTest);
		}
		catch (ValidationException exception)
		{
//			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.TASK_IS_PRIVATE));
			Assert.fail(exception.getMessage());
		}
		
		//4a  check to see if null email is passed, it still returns a valid task
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task1.getId(), null).getTask();
			Assert.assertEquals(task1, taskUnderTest);
		}
		catch (ValidationException exception)
		{
//			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.TASK_IS_PRIVATE));
			Assert.fail(exception.getMessage());
		}
		
		//test5 bad taskid is passed
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task6.getTaskCreator().getId(), task6.getTaskCreator().getMember().getEmail()).getTask();
			Assert.fail();
		}
		catch (ValidationException exception)
		{
			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.TASK_NOT_FOUND));
		}
				
		//test6 task own is the requester
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task6.getId(), task6.getTaskCreator().getMember().getEmail()).getTask();
			Assert.assertEquals(task6, taskUnderTest);
			
		}
		catch (ValidationException exception)
		{
			Assert.fail(exception.getMessage());
		}
		
		//test7 null profile id passed, but valid email
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task6.getId(), profile4Guest2.getMember().getEmail()).getTask();
			Assert.assertEquals(task6, taskUnderTest);
		}
		catch (ValidationException exception)
		{
			Assert.fail(exception.getMessage());
		}
		
		//test8 task is not owned or authorized by id
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task9.getId(), profile1.getEmail()).getTask();
			Assert.fail();
		}
		catch (ValidationException exception)
		{
			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.MEMBER_DOES_NOT_HAVE_AUTHORIZATION));
		}
		
		
		//test9 task is not owned or authorized by email
		try
		{
			taskUnderTest = shareService.getShareDirectLink(task9.getId(), profile4Guest2.getMember().getEmail()).getTask();
			Assert.fail();
		}
		catch (ValidationException exception)
		{
			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.MEMBER_DOES_NOT_HAVE_AUTHORIZATION));
		}
		
		//test10 task is public and a share exists
		profile2.getMember().setSharedTaskId(task1.getId());
		try
		{
			shareService.createShareOnRegister(profile2);
			Share shareUnderTest = shareService.getShareDirectLink(task1.getId(), profile2.getMember().getEmail());
			Assert.assertEquals(task1, shareUnderTest.getTask());
			Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), shareUnderTest.getShareApprovedStatusOrdinal());
			Assert.assertEquals(profile2.getMember(), shareUnderTest.getUser());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}

	}
	
	@Test
	public void acceptShare()
	{ 
		Profile profile1 = createProfile();
		Profile profile3Guest1 = createGuest();
		
		Task task6 = createTask(profile1);
		
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		
		try
		{
			shareService.acceptShareTX(share1Profile3Task6.getId());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		Share share = shareDao.getShareById(share1Profile3Task6.getId());
		assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
	}
	
	@Test
	public void declineShare()
	{
		Profile profile1 = createProfile();
		Profile profile3Guest1 = createGuest();
		Task task6 = createTask(profile1);
			
		Share share1Profile3Task6 = createShare(task6, profile3Guest1);
		
		shareService.declineShare(share1Profile3Task6.getId(), false);
		
		Share share = shareDao.getShareById(share1Profile3Task6.getId());
		assertEquals(ShareApprovedStatus.DECLINED.ordinal(), share.getShareApprovedStatusOrdinal());
	}
	
	@Test
	public void shareTask()
	{
		Profile profile1 = createProfile();	
		Task task1 = createTask(profile1);
			

		

		Invite invite = RMTestModelBuilder.buildInvite(true, "bad_id", Task.class.getName());
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put("share1@reminded.me", "Jimmy");
		invite.setEmailAddresses(emailAddresses);
		//test when bad task id is passed
		try
		{
			shareService.shareTask(invite);
			Assert.fail();
		}
		catch (ValidationException exception)
		{
			Assert.assertTrue(exception.getValidationErrors().contains(ValidationCodeEnum.TASK_NOT_FOUND));
		}
		
//		assertTableRowChecks();
				
		//test to see if it works with an existing member...share(1), invite(1), notification(1)
//		shareCounter++;
//		inviteCounter++;
//		notificationCounter+=2;
		
		invite = RMTestModelBuilder.buildInvite(true, task1.getId(), Task.class.getName());
		emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile1.getMember().getEmail(), "profile");
		invite.setEmailAddresses(emailAddresses);
		try
		{			
			shareService.shareTask(invite);
			
			Map<String, List> syncs = remindedMeService.synchronizeObjects(profile1.getId(), "12345", null);
			List clientNotificationIds = syncs.get(ClientNotification.class.getName());
			
//			for (Object id : clientNotificationIds)
//			{
//				ClientNotification clientNotification = dao.loadByPrimaryKey(ClientNotification.class, (String)id);
//				System.out.println(clientNotification.getNotification().getBody());
//			} 
//			List<ClientNotification> clientNotifications = clientNotificationService.getBulkClientNotificationsTX(clientNotificationIds.toString(), profile1.getEmail());
//			Assert.assertEquals(clientNotificationIds.size(), clientNotifications.size());
		}
		catch (ValidationException exception)
		{
			Assert.fail(exception.getMessage());
		}
		
		
//		assertTableRowChecks();
		
		
	}

	
}
