package com.homefellas.rm;

import static com.homefellas.rm.RMTestModelBuilder.buildInvite;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.Test;

import com.homefellas.batch.Notification;
import com.homefellas.batch.NotificationService;
import com.homefellas.batch.NotificationTypeEnum;
import com.homefellas.batch.PushTypeEnum;
import com.homefellas.exception.ValidationException;
import com.homefellas.model.core.AbstractModel;
import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.calendar.GenericCalendarEnum;
import com.homefellas.rm.notification.Device;
import com.homefellas.rm.reminder.Alarm;
import com.homefellas.rm.share.IShareDao;
import com.homefellas.rm.share.Invite;
import com.homefellas.rm.share.SentShare;
import com.homefellas.rm.share.Share;
import com.homefellas.rm.share.ShareApprovedStatus;
import com.homefellas.rm.share.ShareService;
import com.homefellas.rm.task.AbstractTask.PriorityEnum;
import com.homefellas.rm.task.AbstractTask.ProgressEnum;
import com.homefellas.rm.task.Task;
import com.homefellas.rm.user.Contact;
import com.homefellas.user.Profile;
import com.homefellas.user.Role;
import com.homefellas.user.RoleEnum;
import com.homefellas.user.UserService;

public class RemindedMeServiceTest extends AbstractRMTestDao
{
	
	@Resource(name="remindedMeDao")
	private IRemindedMeDao remindedMeDao;
	
	@Resource(name="shareService")
	private ShareService shareService;
	
	@Resource(name="shareDao")
	private IShareDao shareDao;
	
	@Resource(name="userService")
	private UserService userService;
	
	@Resource(name="notificationService")
	private NotificationService notificationService;
	
	@Test
	public void getDeviceIdsForDailyDYKNotification()
	{
		Profile profileRecieveNotification = createProfile();
		Profile profileRecieveNotificationNoRegisteredDevice = createProfile();
		Profile profileNotRecieveNotification = createProfile();
		
		Device device = createDevice(profileRecieveNotification, PushTypeEnum.APPLE); 
		profileNotRecieveNotification.setDailyDYKNotification(false);
		boolean foundprofileRecieveNotification=false;
		try
		{
			userService.updateProfileTX(profileNotRecieveNotification);
			
			notificationService.sendDailyAppleDYKNotificaiton("some message", Device.class.getName());
			
			List<Notification> notifications = clientNotificationService.getNotificationQueue();
			for (Notification notification : notifications)
			{
				if (notification.getNotificationTypeOrdinal() != NotificationTypeEnum.PUSH.ordinal())
					continue;
				
				if (notification.getiNotificationId().equals(device.getId()))
				{
					foundprofileRecieveNotification=true;
				}
				else
					Assert.fail("found a push that should be be sent");
			}
			
			if (!foundprofileRecieveNotification)
				Assert.fail("push not found");
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
	}
	
	
	@Test 
	public void getBulk()
	{
		Profile profile1 = createProfile();
		Profile sharee1 = createProfile();
		Profile sharee2 = createProfile();
		
		Task task1 = createTask(profile1);
		Task task2 = createTask(profile1);
		
		Share share1 = createShare(task1, sharee1);
		Share share2 = createShare(task1, sharee2);
		Share share3 = createShare(task2, sharee1);
		
		try
		{
			Map<String, List> syns = remindedMeService.synchronizeObjects(profile1.getId(), "12345", null);
			List contacts = syns.get(Contact.class.getName());
			String ids="";
			for (int i=0;i<contacts.size();i++)
			{
				String contactId = (String)contacts.get(i);
				ids += contactId;
				if (i != contacts.size()-1)
					ids += ",";
			}
			List<? extends AbstractModel> contactsUnderTest = remindedMeService.getBulkTX(ids, profile1.getId(), "contact");
			Assert.assertEquals(contacts.size(), contactsUnderTest.size());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		try
		{
			List<? extends AbstractModel> contactsUnderTest = remindedMeService.getBulkTX("", profile1.getId(), "invalid");
			Assert.fail();
		}
		catch (ValidationException e)
		{
			assertValidation(e, ValidationCodeEnum.INVALID_BULK_PATH);
		}
	}
	
	@Test
	public void timeRangeSyncWithTaskDependancies()
	{
		Profile profile = createProfile();
		Task task1 = RMTestModelBuilder.task(profile);
		task1.setStartTime(new DateTime().minusDays(6));
		task1 = createTask(task1);
		
		Task task2 = RMTestModelBuilder.task(profile);
		task2.setEndTime(new DateTime().plusDays(6));
		task2 = createTask(task2);
		
		Task task3 = RMTestModelBuilder.task(profile);
		task3.setTimeLessTask(true);
		task3 = createTask(task3);
		
		Profile sharee = createGuest();
		
		Share share = createShare(task1, sharee);
		Alarm alarm = createAlarm(task2, new DateTime().plusHours(5));
		
		Map<String, List> syncs;
		try
		{
			syncs = remindedMeService.synchronizeFilteredDateRangeTX(profile.getId(), "1234567890", null, new DateTime().minusDays(7), new DateTime().plusDays(7));
			
			List tasks = syncs.get(Task.class.getName());
			Assert.assertTrue(tasks.contains(task1.getId()));
			Assert.assertTrue(tasks.contains(task2.getId()));
			Assert.assertFalse(tasks.contains(task3.getId()));
			
			List sentShares = syncs.get(SentShare.class.getName());
			Assert.assertTrue(sentShares.contains(share.getId()));
			
			List alarms = syncs.get(Alarm.class.getName());
			Assert.assertTrue(alarms.contains(alarm.getId()));
			
			syncs = remindedMeService.synchronizeFilteredDateRangeTX(profile.getId(), "1234567890", null, null, null);
			
			tasks = syncs.get(Task.class.getName());
			Assert.assertFalse(tasks.contains(task1.getId()));
			Assert.assertFalse(tasks.contains(task2.getId()));
			Assert.assertTrue(tasks.contains(task3.getId()));
			
			syncs = remindedMeService.synchronizeFilteredDateRangeTX(sharee.getId(), "1234567890", null, new DateTime().minusDays(7), new DateTime().plusDays(7));
			tasks = syncs.get(Task.class.getName());
			Assert.assertTrue(tasks.contains(task1.getId()));
			Assert.assertFalse(tasks.contains(task2.getId()));
			
			sentShares = syncs.get(SentShare.class.getName());
			Assert.assertTrue(sentShares.contains(share.getId()));
			
			alarms = syncs.get(Alarm.class.getName());
			Assert.assertTrue(alarms.isEmpty());
			
			List shares = syncs.get(Share.class.getName());
			Assert.assertTrue(shares.contains(share.getId()));
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		

	}
	
	
	@Test
	public void synchronizeObjectsWithShare()
	{
		Profile profile1 = createProfile();
		Profile profile2 = createProfile();
		Profile profile3Guest1 = createGuest();
		Profile profile4Guest2 = createGuest();
		
		Task task6 = createTask(profile1);
		Task task8 = createTask(profile2);
		Task task9 = createTask(profile2);
		
		Share share4Profile2Task6 = createShare(task6, profile2);
		
		Invite invite2Task9 = buildInvite(true, task9.getId(), Task.class.getName());
		invite2Task9.setInviter(task9.getTaskCreator().getMember());
		
		shareDao.createInvite(invite2Task9);
		
		//test getting all the objects
		Map<String, List> synchronizeables=null;

		try
		{
			shareService.acceptShareTX(share4Profile2Task6.getId());
		}
		catch (ValidationException e)
		{
			Assert.fail(e.getMessage());
		}
		
		Share share = shareService.getShareById(share4Profile2Task6.getId());
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
		
		try
		{
			synchronizeables = remindedMeService.synchronizeObjects(profile2.getId(), null, null);
		}
		catch (ValidationException e)
		{
			e.printStackTrace();
			Assert.fail();
		}
		
		
		List<Object> list = synchronizeables.get(Task.class.getName());
		Assert.assertTrue(list.contains(share.getTask().getId()));
		Assert.assertTrue(list.contains(task9.getId()));
		Assert.assertTrue(list.contains(task8.getId()));
		
		list = synchronizeables.get(Share.class.getName());
		Assert.assertTrue(list.contains(share4Profile2Task6.getId()));
		
//		list = synchronizeables.get(Invite.class.getName());
//		Assert.assertTrue(list.contains(invite2Task9.getId()));
	}
	
	
	
	@Test
	public void synchronizeObjects()
	{
		Profile profile1 = createProfile();
		Task task1 = RMTestModelBuilder.task(profile1);
		task1.setStartTime(new DateTime());
		task1.setEndTime((new DateTime()).plusMonths(1));
		task1.setPriority(PriorityEnum.MEDIUM.ordinal());
		task1 = createTask(task1);
		
		Task task2 = RMTestModelBuilder.task(profile1);
		task2.setProgress(ProgressEnum.RUNNING_LATE.ordinal());
		task2.setStartTime((new DateTime()).plusDays(1));
		task2.setEndTime( (new DateTime()).plusDays(2));
		task2.setPriority(PriorityEnum.HIGH.ordinal());
		task2 = createTask(task2);
		
		Task task3 = RMTestModelBuilder.task(profile1);
		task3.setProgress(ProgressEnum.DELETE.ordinal());
		task3.setStartTime((new DateTime()).plusDays(2));
		task3.setEndTime((new DateTime()).plusDays(3));
		task3.setPriority(PriorityEnum.HIGH.ordinal());
		task3 = createTask(task3);
		
		Task task4 = RMTestModelBuilder.task(profile1);
		task4.setProgress(ProgressEnum.ALMOST_THERE.ordinal());
		task4.setStartTime((new DateTime()).plusDays(3));
		task4.setEndTime((new DateTime()).plusDays(5));
		task4.setPriority(PriorityEnum.LOW.ordinal());
		task4 = createTask(task4);
		
		Task task5 = RMTestModelBuilder.task(profile1);
		task5.setStartTime((new DateTime()).plusDays(4));
		task5.setPriority(PriorityEnum.HIGH.ordinal());
		task5 = createTask(task5);
		
		Task task6 = RMTestModelBuilder.task(profile1);
		task6.setProgress(ProgressEnum.RUNNING_LATE.ordinal());
		task6.setPriority(PriorityEnum.HIGH.ordinal());
		task6 = createTask(task6);
		
		Task task7 = RMTestModelBuilder.task(profile1);
		task7.setProgress(ProgressEnum.DONE.ordinal());
		task7.setStartTime((new DateTime()).plusMinutes(5));
		task7.setEndTime((new DateTime()).plusHours(1));
		task7.setPriority(PriorityEnum.LOW.ordinal());
		task7 = createTask(task7);
		
		Calendar calendarUserCreated = createCalendar(profile1);
//		Category categoryUserCreatedPublic = createCategory(profile1);
		
		//test getting all the objects
		Map<String, List> synchronizeables=null;
		
		try
		{
			synchronizeables = remindedMeService.synchronizeObjects(profile1.getId(), null, null);
		}
		catch (ValidationException e)
		{
			e.printStackTrace();
			Assert.fail();
		}
		List<Object> list = synchronizeables.get(Task.class.getName());
		Assert.assertTrue(list.contains(task1.getId()));
		Assert.assertTrue(list.contains(task2.getId()));
//		Assert.assertTrue(list.contains(task3.getId()));
		Assert.assertTrue(list.contains(task4.getId()));
		Assert.assertTrue(list.contains(task5.getId()));
		Assert.assertTrue(list.contains(task6.getId()));
		Assert.assertTrue(list.contains(task7.getId()));
		
//		list = synchronizeables.get(Category.class.getName());
//		Assert.assertTrue(list.contains(categoryUserCreatedPrivate.getId()));
//		Assert.assertTrue(list.contains(categoryUserCreatedPublic.getId()));
		
		list = synchronizeables.get(Calendar.class.getName());
		Assert.assertTrue(list.contains(calendarUserCreated.getId()));
		
		list = synchronizeables.get(ISynchronizeable.SYSTEM_TIME);
		long modifiedTime = (Long)list.get(0);
		Assert.assertTrue(list.size()>0);
		
		list = synchronizeables.get(ISynchronizeable.DEVICE_ID);
		Assert.assertTrue(list.size()>0);
		
		try
		{
			
		try{Thread.sleep(50);}catch (Exception e){}
		//set a modified time
		task1.setModifiedDateZone(new DateTime());
		taskService.updateTaskEndTime(task1, profile1.getEmail());
//		dao.updateObject(task1);
		
		try{Thread.sleep(50);}catch (Exception e){}
		task2.setModifiedDateZone(new DateTime());
		taskService.updateTaskEndTime(task2, profile1.getEmail());
//		dao.updateObject(task2);
		
//		try{Thread.sleep(50);}catch (Exception e){}
//		categoryUserCreatedPublic.setModifiedDateZone(new DateTime());
//		dao.updateObject(categoryUserCreatedPublic);
		
		
			synchronizeables = remindedMeService.synchronizeObjects(profile1.getId(), "1", new DateTime(modifiedTime));
		}
		catch (ValidationException e)
		{
			e.printStackTrace();
			Assert.fail();
		}
		list = synchronizeables.get(Task.class.getName());
		Assert.assertTrue(list.contains(task1.getId()));
		Assert.assertTrue(list.contains(task2.getId()));
		
//		list = synchronizeables.get(Category.class.getName());
//		Assert.assertTrue(list.contains(categoryUserCreatedPublic.getId()));
		
		list = synchronizeables.get(ISynchronizeable.SYSTEM_TIME);
		Assert.assertTrue(list.size()>0);
		
		list = synchronizeables.get(ISynchronizeable.DEVICE_ID);
		Assert.assertTrue(list.get(0).equals("1"));
		
	}
	
	@Test
	public void testCreateDefaultRoles()
	{
//		dao.flush();
		((RemindedMeService)remindedMeService).createDefaultRoles();
		
		for (RoleEnum authorizationEnum :RoleEnum.values())
		{
			Role role = remindedMeDao.getRole(authorizationEnum.getId());
			Assert.assertEquals(authorizationEnum.getRole(), role.getRoleName());
			
		}

	}

	
	@Test
	public void testCreateDefaultCalendars()
	{
//		dao.flush();
		((RemindedMeService)remindedMeService).createDefaultCalendars();
		
		for (GenericCalendarEnum genericCalendarEnum:GenericCalendarEnum.values())
		{
			Assert.assertEquals(genericCalendarEnum.getCalendar(), remindedMeDao.getCalendar(genericCalendarEnum.getCalendar().getId()));
		}
	}

	@Test
	public void testCreateDefaultDatabaseEntries()
	{
//		remindedMeService.createDefaultDatabaseEntries();
		
//		for (GenericCategoryEnum genericCategoryEnum:GenericCategoryEnum.values())
//		{
//			System.out.println(genericCategoryEnum.getCategory().getCategoryName()+" - "+remindedMeDao.getCategory(genericCategoryEnum.getCategory().getId()).getCategoryName());
//			Assert.assertEquals(genericCategoryEnum.getCategory(), remindedMeDao.getCategory(genericCategoryEnum.getCategory().getId()));
//		}
//		
//		for (GenericCategoryEnum genericCategoryEnum:GenericCategoryEnum.values())
//		{
//			System.out.println(genericCategoryEnum.getCategory().getCategoryName()+" - "+remindedMeDao.getCategory(genericCategoryEnum.getCategory().getId()).getCategoryName());
//			Assert.assertEquals(genericCategoryEnum.getCategory(), remindedMeDao.getCategory(genericCategoryEnum.getCategory().getId()));
//		}
		
		for (RoleEnum authorizationEnum :RoleEnum.values())
		{
			Role role = remindedMeDao.getRole(authorizationEnum.getId());
			Assert.assertEquals(authorizationEnum.getRole(), role.getRoleName());
			
		}
	}

}
