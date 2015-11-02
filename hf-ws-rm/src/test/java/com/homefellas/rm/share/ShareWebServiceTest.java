package com.homefellas.rm.share;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.homefellas.batch.INotificationServiceTX;
import com.homefellas.batch.Notification;
import com.homefellas.batch.NotificationService;
import com.homefellas.dao.core.IDao;
import com.homefellas.rm.ISynchronizeable;
import com.homefellas.rm.RMTestModelBuilder;
import com.homefellas.rm.RemindedMeService;
import com.homefellas.rm.RemindedMeWebService;
import com.homefellas.rm.ValidationCodeEnum;
import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.calendar.CalendarStoreCategory;
import com.homefellas.rm.calendar.CalendarStoreSubCategory;
import com.homefellas.rm.calendar.CalendarStoreUserDefinedCategory;
import com.homefellas.rm.reminder.Alarm;
import com.homefellas.rm.task.AbstractTask.PriorityEnum;
import com.homefellas.rm.task.AbstractTask.ProgressEnum;
import com.homefellas.rm.task.Task;
import com.homefellas.rm.task.TaskWebService;
import com.homefellas.user.IUserServiceTX;
import com.homefellas.user.Profile;
import com.homefellas.user.UserService;
import com.homefellas.ws.core.AbstractTestRMWebService;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

public class ShareWebServiceTest extends AbstractTestRMWebService
{
	private INotificationServiceTX notificationService;
	private IUserServiceTX userService;
	private IShareServiceTX shareService;
	

	@Before
	public void setupDefaultDatabaseValues()
	{
		super.setupDefaultDatabaseValues();
		
		dao = (IDao)getServer().getSpringBean("dao");
		transactionManager = (PlatformTransactionManager)getServer().getSpringBean("transactionManager");
		notificationService = (NotificationService)getServer().getSpringBean("notificationService");
		userService = (UserService)getServer().getSpringBean("userService");
		shareService = (ShareService)getServer().getSpringBean("shareService");
		RemindedMeService remindedMeService = (RemindedMeService)getServer().getSpringBean("remindedMeService");
		remindedMeService.createDefaultCalendarStoreCategoriesAndSubCategories();

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
		   @Override
		   protected void doInTransactionWithoutResult(TransactionStatus status) {
			   
		   }});
	}
	
	@Test
	public void sharedTaskChangeEndTimeVerifyAlarms()
	{
		Profile owner = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.task(owner);
		
		DateTime orginalDate = new DateTime().plusDays(1);
		task.setEndTime(orginalDate);
		task = createAndRetrieveTask(task);
		
		DateTime alram1OrginalTime = task.getEndTime().minusMinutes(5);
		DateTime alram2OrginalTime = task.getEndTime().minusHours(2);
		Alarm alarm1 = createAndRetrieveAlarm(task, alram1OrginalTime);
		Alarm alarm2 = createAndRetrieveAlarm(task, alram2OrginalTime);
		
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Share share = createAndRetrieveShares(task, sharee);
		
		share = acceptShare(share);
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		List<Object> alarms = syncs.get(Alarm.class.getName());
		alarm1 = getBulkAlarm((String)alarms.get(0), sharee.getEmail());
		alarm2 = getBulkAlarm((String)alarms.get(1), sharee.getEmail());
		Assert.assertEquals(alram1OrginalTime, alarm1.getAlarmTime());
		Assert.assertEquals(alram2OrginalTime, alarm2.getAlarmTime());
		
		syncs = sync(owner.getId(), 0);
		alarms = syncs.get(Alarm.class.getName());
		alarm1 = getBulkAlarm((String)alarms.get(0), owner.getEmail());
		alarm2 = getBulkAlarm((String)alarms.get(1), owner.getEmail());
		Assert.assertEquals(alram1OrginalTime, alarm1.getAlarmTime());
		Assert.assertEquals(alram2OrginalTime, alarm2.getAlarmTime());
		
		List<Object> taskIds = syncs.get(Task.class.getName());
		task = getBulkTasks((String)taskIds.get(0), owner.getEmail());
//		String syncTime = syncs.get(ISynchronizeable.DEVICE_ID).get(0).toString();
		
		DateTime changedDate = new DateTime().plusDays(2);
		task.setEndTime(changedDate);
		DateTime alarm1Changed = task.getEndTime().minusMinutes(5);
		DateTime alarm2Changed = task.getEndTime().minusHours(2);
		alarm1.setAlarmTime(alarm1Changed);
		alarm2.setAlarmTime(alarm2Changed);
		
		
		task = updateTask(task);
		alarm1 = updateAlarm(alarm1);
		alarm2 = updateAlarm(alarm2);
		
		syncs = sync(sharee.getId(), 0);
		alarms = syncs.get(Alarm.class.getName());
		
		alarm1 = getBulkAlarm((String)alarms.get(0), sharee.getEmail());
		alarm2 = getBulkAlarm((String)alarms.get(1), sharee.getEmail());
		Assert.assertTrue(alarm1Changed.getMillis()-alarm1.getAlarmTime().getMillis() < 1000);
		Assert.assertTrue(alarm2Changed.getMillis()-alarm2.getAlarmTime().getMillis() < 1000);
//		Assert.assertEquals(alarm1Changed, alarm1.getAlarmTime());
//		Assert.assertEquals(alarm2Changed, alarm2.getAlarmTime());
		
		syncs = sync(owner.getId(), 0);
		alarms = syncs.get(Alarm.class.getName());
		
		alarm1 = getBulkAlarm((String)alarms.get(0), owner.getEmail());
		alarm2 = getBulkAlarm((String)alarms.get(1), owner.getEmail());
		Assert.assertTrue(alarm1Changed.getMillis()-alarm1.getAlarmTime().getMillis() < 1000);
		Assert.assertTrue(alarm2Changed.getMillis()-alarm2.getAlarmTime().getMillis() < 1000);
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(sharee.getEmail());
		Assert.assertEquals(7, notifications.size());
		Assert.assertTrue(changedDate.getMillis()-notifications.get(4).getToSendTime() < 1000);
//		Assert.assertEquals(changedDate.getMillis(), notifications.get(4).getToSendTime());
		
		notifications = notificationService.getNotificationsToEmailTX(owner.getEmail());
		Assert.assertEquals(5, notifications.size());
		Assert.assertTrue(changedDate.getMillis()-notifications.get(2).getToSendTime() < 1000);
//		Assert.assertEquals(changedDate.getMillis(), notifications.get(3).getToSendTime());
	}
	
	
	@Test
	public void sharedSubTaskGetsAnotherTaskAdded()
	{
		Profile owner = createAndRetrieveProfile();
		Task parent = createAndRetrieveTask(owner);
		Task child1 = createAndRetrieveTask(owner);
		Task child2 = createAndRetrieveTask(owner);
		
		subList(parent, child1);
		subList(parent, child2);
		
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Share share = createAndRetrieveShares(parent, sharee);
		
		share = acceptShare(share);
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		
		List<Object> tasks = syncs.get(Task.class.getName());
		Assert.assertTrue(tasks.size()==3);
		
		Task child3 = createAndRetrieveTask(owner);
		
		long syncTime = (Long)syncs.get(ISynchronizeable.SYSTEM_TIME).get(0);
		parent.setModifiedDate(syncTime);
		subList(parent, child3);
		
		syncs = sync(sharee.getId(), 0);
		tasks = syncs.get(Task.class.getName());
		
		System.out.println(tasks.size());
		Assert.assertTrue(tasks.size()==4);
		
		
		List<Task> tasksModels = getBulkTasks(tasks, sharee.getEmail());
		for (Task task : tasksModels)
		{
			if (task.isaParent())
				Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(),task.getShareStatus());
		}
	}
	
	@Test
	public void alarmsAreSyncedWithSharedTasks()
	{
		Profile owner = createAndRetrieveProfile();
		Task task = createAndRetrieveTask(owner);
		
		Alarm alarm = createAndRetrieveAlarm(task);
		
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Share share = createAndRetrieveShares(task, sharee);
		
		share = acceptShare(share);
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		
		List<Object> tasks = syncs.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task.getId()));
		List<Object> alrams = syncs.get(Alarm.class.getName());
		Assert.assertTrue(alrams.size()>0);
		
	
	}
	
	@Test
	public void publishToAppleStore()
	{
		Profile owner = createAndRetrieveProfile();
		
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember());
		
		//create two tasks that are part of a calnedar
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		List<CalendarStoreCategory> calendarStoreCategories = callWebService(TaskWebService.class, "getCalendarStoreCategories", new GenericType<List<CalendarStoreCategory>>(){}, buildPathParms("", ""));
		
		CalendarStoreUserDefinedCategory calendarStoreUserDefinedCategory = new CalendarStoreUserDefinedCategory();
		CalendarStoreSubCategory calendarStoreSubCategory = calendarStoreCategories.get(0).getCalendarStoreSubCategories().iterator().next();
		calendarStoreUserDefinedCategory.setCalendarStoreSubCategory(calendarStoreSubCategory);
		calendarStoreUserDefinedCategory.setMember(owner.getMember());
		calendarStoreUserDefinedCategory.setCategoryName("new category");
		calendarStoreUserDefinedCategory.generateGUIDKey();
		
		calendarStoreUserDefinedCategory = callWebService(TaskWebService.class, "createCalendarStoreUserDefinedCategory", CalendarStoreUserDefinedCategory.class, calendarStoreUserDefinedCategory);
		
		List<CalendarStoreUserDefinedCategory> calendarStoreUserDefinedCategories = callSecuredWebService(TaskWebService.class, "getCalendarStoreUserDefinedCategories", new GenericType<List<CalendarStoreUserDefinedCategory>>(){}, buildPathParms("{id}", calendarStoreSubCategory.getId()), owner.getEmail());
		Assert.assertTrue(calendarStoreUserDefinedCategories.contains(calendarStoreUserDefinedCategory));
		
		calendar.setCalendarStoreUserDefinedCategory(calendarStoreUserDefinedCategory);
		calendar.setAppleStorePrice(9.99);
		
		calendar = callSecuredWebService(ShareWebService.class, "publishCalendarToAppleStore", Calendar.class, calendar, owner.getEmail());
		
		Assert.assertTrue(calendar.isPublishToAppStore());
	}
	
	@Test
	public void shareSubListAddTask()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2 = createAndRetrieveTask(task2);
		
		Task parentTask = createAndRetrieveTask(owner);
		
		subList(parentTask, task1);
		subList(parentTask, task2);
		
		//share calendar with sharee
		Share share = createAndRetrieveShares(parentTask, sharee);
		
		Assert.assertTrue(notificationService.getNotificationsToEmailTX(sharee.getEmail()).size()==2);
		
		
		acceptShare(task1, sharee.getMember());
		acceptShare(task2, sharee.getMember());
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		List<Object> tasks = syncs.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(parentTask.getId()));
		Assert.assertTrue(tasks.contains(task1.getId()));
		Assert.assertTrue(tasks.contains(task2.getId()));
		
		
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		subList(parentTask, task3);
		
		syncs = sync(sharee.getId(), 0);
		tasks = syncs.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(parentTask.getId()));
		Assert.assertTrue(tasks.contains(task1.getId()));
		Assert.assertTrue(tasks.contains(task2.getId()));
		Assert.assertTrue(tasks.contains(task3.getId()));
	}
	
	@Test
	public void shareCalendarAddTask()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember());
		calendar.setPublicList(true); 
		
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		//share calendar with sharee
		Invite invite = createAndRetrieveCalendarShares(calendar, sharee);
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		List<Object> tasks = syncs.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		Assert.assertTrue(tasks.contains(task2.getId()));
		
		List<Object> calendars = syncs.get((Calendar.class.getName()));
		Assert.assertTrue(calendars.contains(calendar.getId()));
		
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3.addCalendar(calendar);
		task3 = createAndRetrieveTask(task3);
		
		syncs = sync(sharee.getId(), 0);
		tasks = syncs.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		Assert.assertTrue(tasks.contains(task2.getId()));
		Assert.assertTrue(tasks.contains(task3.getId()));
	}
	
	@Test
	public void publicShareDirectLink()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember());
		calendar.setPublicList(true); 
		
		//create two tasks that are part of a calnedar
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		//create a non shared category 
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		//share calendar with sharee
		Invite invite = createAndRetrieveCalendarShares(calendar, sharee);
		Share task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		Share task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		Share task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		
		List<Task> tasks = callSecuredWebService(ShareWebService.class, "getPublicCalendarShareDirectLink", new GenericType<List<Task>>(){}, buildPathParms("{calendarid}", calendar.getId()), owner.getEmail());
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task2));
		
		tasks = callSecuredWebService(ShareWebService.class, "getPublicCalendarShareDirectLink", new GenericType<List<Task>>(){}, buildPathParms("{calendarid}", calendar.getId()),null);
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task2));
		
		tasks = callSecuredWebService(ShareWebService.class, "getPublicCalendarShareDirectLink", new GenericType<List<Task>>(){}, buildPathParms("{calendarid}", calendar.getId()),sharee.getEmail());
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task2));
	}
	
	@Test
	public void privateShareDirectLink()
	{
		Profile owner = createAndRetrieveProfile();
		Profile owner2 = createAndRetrieveProfile("owrn23@homefellas.com");
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember(), false);
		
		//create two tasks that are part of a calnedar
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		//create a non shared category 
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		//share calendar with sharee
		Invite invite = createAndRetrieveCalendarShares(calendar, sharee);
		Share task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		Share task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		Share task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		
		Map<String, String> pathParm = buildPathParms("{calendarid}", calendar.getId());
		pathParm.put("{email}", owner.getEmail());
		List<Task> tasks = callWebService(ShareWebService.class, "getPrivateCalendarShareDirectLink", new GenericType<List<Task>>(){}, pathParm);
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task2));
		
		pathParm = buildPathParms("{calendarid}", calendar.getId());
		pathParm.put("{email}","crap@crap.com");
		ClientResponse response = callWebService(ShareWebService.class, "getPrivateCalendarShareDirectLink",ClientResponse.class,pathParm);
		Assert.assertEquals(400, response.getStatus());
		
		pathParm = buildPathParms("{calendarid}", calendar.getId());
		pathParm.put("{email}", sharee.getEmail());
		tasks = callWebService(ShareWebService.class, "getPrivateCalendarShareDirectLink", new GenericType<List<Task>>(){}, pathParm);
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task2));
		
		pathParm = buildPathParms("{calendarid}", calendar.getId());
		pathParm.put("{email}", owner2.getEmail());
		response = callWebService(ShareWebService.class, "getPrivateCalendarShareDirectLink",ClientResponse.class, pathParm);
		Assert.assertEquals(400, response.getStatus());
	}
	
	
	@Test
	public void shareDeclineAccept()
	{
		//first public then private
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
		
		
		
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task = createAndRetrieveTask(task);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(sharee.getEmail(), sharee.getName());
		String guestEmail = "guest@reminded.me";
		emailAddresses.put(guestEmail, null);
		createShares(task, emailAddresses);
		
		//simulate user clicking on link
		Share shareExistingUser = retrieveShareDirectLink(task, sharee.getEmail());
		Share shareGuestUser = retrieveShareDirectLink(task, guestEmail);
		
		//decline the shares
		shareExistingUser = declineShare(shareExistingUser);
		shareGuestUser = declineShare(shareGuestUser);
		
		//verify the share is declined
		shareExistingUser = getBulkShare(shareExistingUser.getId(), sharee.getEmail());
		shareGuestUser = getBulkShare(shareExistingUser.getId(), guestEmail);
		Assert.assertEquals(shareExistingUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.DECLINED.ordinal());
		Assert.assertEquals(shareGuestUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.DECLINED.ordinal());
		
		//simulate user clicking on link again
		shareExistingUser = retrieveShareDirectLink(task, sharee.getEmail());
		shareGuestUser = retrieveShareDirectLink(task, guestEmail);
		
		//now accept the link
		shareExistingUser = acceptShare(shareExistingUser);
		shareGuestUser = acceptShare(shareGuestUser);
		
		//verify the share is declined
		shareExistingUser = getBulkShare(shareExistingUser.getId(), sharee.getEmail());
		shareGuestUser = getBulkShare(shareExistingUser.getId(), guestEmail);
		Assert.assertEquals(shareExistingUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.APPROVED.ordinal());
		Assert.assertEquals(shareGuestUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.APPROVED.ordinal());
		

		//now private
		task = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, owner, null, PriorityEnum.HIGH, null,null);
		task = createAndRetrieveTask(task);
		
		emailAddresses = new HashMap<String, String>();
		emailAddresses.put(sharee.getEmail(), sharee.getName());
		guestEmail = "guest@reminded.me";
		emailAddresses.put(guestEmail, null);
		createShares(task, emailAddresses);
		
		//simulate user clicking on link
		shareExistingUser = retrieveShareDirectLink(task, sharee.getEmail());
		shareGuestUser = retrieveShareDirectLink(task, guestEmail);
		
		//decline the shares
		shareExistingUser = declineShare(shareExistingUser);
		shareGuestUser = declineShare(shareGuestUser);
		
		//verify the share is declined
		shareExistingUser = getBulkShare(shareExistingUser.getId(), sharee.getEmail());
		shareGuestUser = getBulkShare(shareExistingUser.getId(), guestEmail);
		Assert.assertEquals(shareExistingUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.DECLINED.ordinal());
		Assert.assertEquals(shareGuestUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.DECLINED.ordinal());
		
		//simulate user clicking on link again
		shareExistingUser = retrieveShareDirectLink(task, sharee.getEmail());
		shareGuestUser = retrieveShareDirectLink(task, guestEmail);
		
		//now accept the link
		shareExistingUser = acceptShare(shareExistingUser);
		shareGuestUser = acceptShare(shareGuestUser);
		
		//verify the share is declined
		shareExistingUser = getBulkShare(shareExistingUser.getId(), sharee.getEmail());
		shareGuestUser = getBulkShare(shareExistingUser.getId(), guestEmail);
		Assert.assertEquals(shareExistingUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.APPROVED.ordinal());
		Assert.assertEquals(shareGuestUser.getShareApprovedStatusOrdinal(), ShareApprovedStatus.APPROVED.ordinal());
		
	}
	
	@Test
	public void registerSharedAccount()
	{
		Profile owner = createAndRetrieveProfile();
		
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1 = createAndRetrieveTask(task1);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		String email = "newemail@reminded.me";
		emailAddresses.put(email, "new user");
		createShares(task1, emailAddresses);
		
		Profile newUser = createAndRetrieveProfile(email);
		Assert.assertTrue(newUser.isPrimaryKeySet());
		
		Assert.assertNotNull(retrieveShare(task1, newUser));
	}
	
	@Test
	public void declineListShare()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember());
		
		//create two tasks that are part of a calnedar
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		//create a non shared category 
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		//Profile sharee3 = createAndRetrieveProfile("sharee3@homefellas.com");
		Invite invite = createAndRetrieveCalendarShares(calendar, sharee);
		
		ShareCalendar shareCalendar = shareService.getShareCalendarByCalendarAndMemberTX(calendar, sharee.getMember());
		
		shareCalendar = callSecuredWebService(ShareWebService.class, "declineListShare", ShareCalendar.class, buildPathParms("{id}", shareCalendar.getId()), sharee.getEmail());
		
		Share share1 = retrieveShare(task1, sharee);
		Assert.assertNotNull(share1);
		Assert.assertEquals(ShareApprovedStatus.DECLINED.ordinal(), share1.getShareApprovedStatusOrdinal());
		Share share2 = retrieveShare(task2, sharee);
		Assert.assertNotNull(share2);
		Assert.assertEquals(ShareApprovedStatus.DECLINED.ordinal(), share2.getShareApprovedStatusOrdinal());
		Share share3 = retrieveShare(task3, sharee);
		Assert.assertNull(share3);
		
		
	}
	
	@Test
	public void acceptListShare()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember());
		
		//create two tasks that are part of a calnedar
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		//create a non shared category 
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		//Profile sharee3 = createAndRetrieveProfile("sharee3@homefellas.com");
		Invite invite = createAndRetrieveCalendarShares(calendar, sharee);
		
		ShareCalendar shareCalendar = shareService.getShareCalendarByCalendarAndMemberTX(calendar, sharee.getMember());
		
		shareCalendar = callSecuredWebService(ShareWebService.class, "acceptListShare", ShareCalendar.class, buildPathParms("{id}", shareCalendar.getId()), sharee.getEmail());
		
		Share share1 = retrieveShare(task1, sharee);
		Assert.assertNotNull(share1);
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share1.getShareApprovedStatusOrdinal());
		Share share2 = retrieveShare(task2, sharee);
		Assert.assertNotNull(share2);
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share2.getShareApprovedStatusOrdinal());
		Share share3 = retrieveShare(task3, sharee);
		Assert.assertNull(share3);
		
		
	}
	
	
	@Test
	public void registerGuestOnShareCreation()
	{
		Profile owner = createAndRetrieveProfile();
		Task existingTask = createAndRetrieveTask(owner);
		
		//existing member with shares.
		Profile existingMemberWithShares = createAndRetrieveProfile("existingmembershares@reminded.me");
		Share existingShare = createAndRetrieveShares(existingTask, existingMemberWithShares);
		
		//check to make sure task is marked as shared
		Task taskUnderTest = getBulkTasks(existingTask.getId(), owner.getMember().getEmail());
		Assert.assertTrue(taskUnderTest.isHasBeenShared());
		
		//existing member with no shares
		Profile existingMemberWithoutShares = createAndRetrieveProfile("existingmembernonshares@reminded.me");
		
		//new member
		String shareeEmail = "sharee@reminded.me";
		
		Task newTaskToShare = createAndRetrieveTask(owner);
		
		Map<String,String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(shareeEmail, null);
		emailAddresses.put(existingMemberWithoutShares.getMember().getEmail(), existingMemberWithoutShares.getName());
		emailAddresses.put(existingMemberWithShares.getMember().getEmail(), existingMemberWithShares.getName());
		Invite invite = createShares(newTaskToShare, emailAddresses);
		
		Profile newMember = userService.getProfileByEmailTX(shareeEmail);
		Assert.assertNotNull(newMember);
		
		Share share1 = retrieveShare(newTaskToShare, newMember);
		Assert.assertNotNull(share1);
		Share share2 = retrieveShare(newTaskToShare, existingMemberWithoutShares);
		Assert.assertNotNull(share2);
		Share share3 = retrieveShare(newTaskToShare, existingMemberWithShares);
		Assert.assertNotNull(share3);
		
		Assert.assertTrue(notificationService.getNotificationsForINotificationTX(share1).get(0).isPrimaryKeySet());
		Assert.assertTrue(notificationService.getNotificationsForINotificationTX(share2).get(0).isPrimaryKeySet());
		Assert.assertTrue(notificationService.getNotificationsForINotificationTX(share3).get(0).isPrimaryKeySet());
	}
	
	@Test
	public void taskStatusIsDefaultedWhenShareIsCreated()
	{
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
		Profile profile = createAndRetrieveProfile();
		Task task = createAndRetrieveTask(profile);
		
		Assert.assertEquals(ShareApprovedStatus.OWNER_TASK.ordinal(), task.getShareStatus());
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		long syncTime = (Long)syncs.get(ISynchronizeable.SYSTEM_TIME).get(0);
		Share share = createAndRetrieveShares(task, sharee);
		
		syncs = sync(sharee.getId(), syncTime);
		String taskId = (String)syncs.get(Task.class.getName()).get(0);
		
		Task taskUnderClass = getBulkTasks(taskId, sharee.getMember().getEmail());
		Assert.assertEquals(task.getId(), taskUnderClass.getId());
		Assert.assertEquals(ShareApprovedStatus.NO_ACTION.ordinal(), taskUnderClass.getShareStatus());
	}
	
	@Test
	public void sendNotificationsToShares()
	{
		Profile profile = RMTestModelBuilder.buildBasicMember(true, passwordEncoder);	
		profile.setName("name");
		profile = createAndRetrieveProfile(profile);
		Task task = createAndRetrieveTask(profile);
		
		
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Invite invite2 = createShares(task, sharee);
		
		Share task1Share = retrieveShare(task, sharee);
		
		Invite invite = new Invite();
		invite.setShareId(task.getId());
		String message = "Here is my message";
		String subject = "subject";
		invite.setMessage(message);
		invite.setSubject(subject);
		
		ClientResponse response = callWebService(ShareWebService.class, "sendNotificationsToShares", ClientResponse.class, invite);
		Assert.assertEquals(200, response.getStatus());
		
		List<Notification> notifications = notificationService.getNotificationsForINotificationTX(task1Share);
		Notification notification = notifications.get(1);
		
		Assert.assertEquals(message, notification.getBody());
		Assert.assertEquals(subject, notification.getSubject());
	}
	
	@Test
	public void updateTaskAndCheckNonNotification()
	{
		Task task = createAndRetrieveTask();
		task.setProgress(ProgressEnum.ALMOST_THERE.ordinal());
		
		updateTask(task);
		
		List<Notification> notifications = notificationService.getNotificationsForINotificationTX(task);
		Assert.assertTrue(notifications.isEmpty());
	}
	@Test
	public void completeTaskAndCheckNotification()
	{
		Profile profile = RMTestModelBuilder.buildBasicMember(true, passwordEncoder);	
		profile.setName("name");
		profile = createAndRetrieveProfile(profile);
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task.setTitle("title");
		task = createAndRetrieveTask(task);
		
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
		Share share = createAndRetrieveShares(task, sharee);
		
		acceptShare(share);
		
		task.setProgress(ProgressEnum.DONE.ordinal());
		updateTask(task);
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(sharee.getEmail());
		Notification notification = notifications.get(0);
		Assert.assertEquals(4, notifications.size());
//		Assert.assertTrue(notification.getBody().contains(task.getTaskCreator().getName()+" has Completed "));
	}
	
	@Test
	public void cancelTaskAndCheckNotification()
	{
		Profile profile = RMTestModelBuilder.buildBasicMember(true, passwordEncoder);	
		profile.setName("name");
		profile = createAndRetrieveProfile(profile);
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task.setTitle("title");
		task = createAndRetrieveTask(task);
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
		Share share = createAndRetrieveShares(task, sharee);
		
		acceptShare(share);
		
		task.setProgress(ProgressEnum.DELETE.ordinal());
		updateTask(task);
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(sharee.getEmail());
		Assert.assertEquals(4, notifications.size());
//		Assert.assertEquals("Task "+task.getTitle()+" Canceled", notification.getSubject());
//		Assert.assertTrue(notification.getBody().contains(task.getTaskCreator().getName()+" has Canceled "));
		
	}
	
	protected Task createAndRetrievePrivateTask(Profile profile)
	{
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, profile, null, PriorityEnum.HIGH, null, null);
		task1 = createAndRetrieveTask(task1);
		
		return task1;
	}
	
	
	@Test
	public void shareCalendar()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Calendar calendar = createAndRetrieveCalendar("calendar", owner.getMember());
		
		//create two tasks that are part of a calnedar
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task2.addCalendar(calendar);
		task2 = createAndRetrieveTask(task2);
		
		//create a non shared category 
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		//share calendar with sharee
		Invite invite = createAndRetrieveCalendarShares(calendar, sharee);
		Share task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		Share task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		Share task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		
		//lets update task3 and make sure nothing has changed
		task3 = getBulkTasks(task3.getId(), owner.getMember().getEmail());
		task3.setProgress(ProgressEnum.ALMOST_THERE.ordinal());
		task3 = updateTask(task3);
		
		task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		
		//lets share task3 with another user now.
		Profile sharee2 = createAndRetrieveProfile("sharee2@homefellas.com");
		Invite invite2 = createShares(task3, sharee2);
		
		task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		Share task3Share2 = retrieveShare(task3, sharee2);
		Assert.assertNotNull(task3Share2);
		
		
		//lets create a new task and add it to the new shared category
		Task task4 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task4.addCalendar(calendar);
		task4 = createAndRetrieveTask(task4);
		
		task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		Share task4Share = retrieveShare(task4, sharee);
		Assert.assertNotNull(task4Share);
		task3Share2 = retrieveShare(task3, sharee2);
		Assert.assertNotNull(task3Share2);
		
		//update task3 and mark it part of the share club
		task3 = getBulkTasks(task3.getId(), owner.getMember().getEmail());
		task3.addCalendar(calendar);
		task3 = updateTask(task3);
		
		task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		task3Share = retrieveShare(task3, sharee);
		Assert.assertNotNull(task3Share);
		task4Share = retrieveShare(task4, sharee);
		Assert.assertNotNull(task4Share);
		task3Share2 = retrieveShare(task3, sharee2);
		Assert.assertNotNull(task3Share2);
		
		//now lets remove task3 and task4 from the shared calendar and make sure the shares are removed.
		//let create another non-shared calendar and set it to task3
		Calendar calendarNonShared = createAndRetrieveCalendar("non-shared calendar", owner.getMember());
		task3 = getBulkTasks(task3.getId(), owner.getMember().getEmail());
		task3.getCalendars().clear();
		task3.addCalendar(calendarNonShared);
		task3 = updateTask(task3);
		
		task4 = getBulkTasks(task4.getId(), owner.getMember().getEmail());
		task4.getCalendars().clear();
		task4 = updateTask(task4);
		
		task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		task4Share = retrieveShare(task4, sharee);
		Assert.assertNull(task4Share);
		task3Share2 = retrieveShare(task3, sharee2);
		Assert.assertNotNull(task3Share2);
		
		//add another user to the calendar shjare
		Profile sharee3 = createAndRetrieveProfile("sharee3@homefellas.com");
		invite = createAndRetrieveCalendarShares(calendar, sharee3);
		
		task1Share = retrieveShare(task1, sharee);
		Assert.assertNotNull(task1Share);
		task2Share = retrieveShare(task2, sharee);
		Assert.assertNotNull(task2Share);
		task3Share = retrieveShare(task3, sharee);
		Assert.assertNull(task3Share);
		task4Share = retrieveShare(task4, sharee);
		Assert.assertNull(task4Share);
		task3Share2 = retrieveShare(task3, sharee2);
		Assert.assertNotNull(task3Share2);
		
		Share task1bShare = retrieveShare(task1, sharee3);
		Assert.assertNotNull(task1bShare);
		Share task2bShare = retrieveShare(task2, sharee3);
		Assert.assertNotNull(task2bShare);
		Share task3bShare = retrieveShare(task3, sharee3);
		Assert.assertNull(task3bShare);
		Share task4bShare = retrieveShare(task4, sharee3);
		Assert.assertNull(task4bShare);
		
	}
	
	@Test
	public void taskUpdateMultipleShareNotifications()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Task task = createAndRetrieveTask(owner);
		
		Invite invite = createShares(task, sharee);
		
		Share share = retrieveShare(task, sharee);
		
		task.setProgress(ProgressEnum.STARTED.ordinal());
		task = callWebService(TaskWebService.class, "updateTask", Task.class, task);
		
		//update task, so a notification should go out to sharee
		List<Notification> notifications = notificationService.getNotificationsForINotificationTX(share);
		Assert.assertTrue(notifications.size()==1);
		
		Map<String, String> pathParms = buildPathParms("{taskid}", String.valueOf(task.getId()));
		pathParms.put("{memberid}", sharee.getId());
		share = callWebService(ShareWebService.class, "acceptShare", Share.class, pathParms);
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
		
		//sharee approved the share, so the owner should be notified.
		notifications = notificationService.getNotificationsForINotificationTX(share);
		Assert.assertEquals(2, notifications.size());
		
		task.setProgress(ProgressEnum.ALMOST_THERE.ordinal());
		task = callWebService(TaskWebService.class, "updateTask", Task.class, task);
		
		notifications = notificationService.getNotificationsForINotificationTX(share);
		Assert.assertTrue(notifications.size()==2);
	}
	
	@Test
	public void updateShare()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		Task task = createAndRetrieveTask(owner);
		
		Invite invite = createShares(task, sharee);
		
		Share share = retrieveShare(task, sharee);
		
		share.setViewed(true);
		
		Share share2 = callWebService(ShareWebService.class, "updateShare", Share.class, share);
		Assert.assertTrue(share2.isViewed());
		
	}
	
	@Test
	public void syncAcceptedShare()
	{
		String device1 = "device1";
		String device2 = "device2";
		
		String email3 = "shared3@gmail.com";
		Profile profile3 = createAndRetrieveProfile(email3);
		
		Profile owner = createAndRetrieveProfile();
		
		//create two tasks
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, owner, null, PriorityEnum.HIGH, null,null);
		Task task1 = createAndRetrieveTask(task);
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile3.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(0));
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		List<Object> tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.isEmpty());
		
		long syncTime = (Long)syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0);
		
		//create a share
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email3, "");
		Invite invite = createShares(task1, emailAddresses);
		
		//verify that the shares are correct
		List<Share> shares = callWebService(ShareWebService.class, "getSharesForTasks", new GenericType<List<Share>>(){}, buildPathParms("{taskid}", task1.getId()));		
		Assert.assertTrue(shares.size()==1);
		
		Share share = shares.get(0);
		
		//accept the share
		pathParms = buildPathParms("{taskid}", String.valueOf(task1.getId()));
		pathParms.put("{memberid}", profile3.getId());
		share = callWebService(ShareWebService.class, "acceptShare", Share.class, pathParms);
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
		
		//sync and make sure it is still there
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile3.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(syncTime));
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
	}
	
	@Test
	public void wrongEmailShareError()
	{		
		String email1 = "shared1@gmail.com";
		Profile profile1 = createAndRetrieveProfile(email1);
		
		String email2 = "shared2@gmail.com";
		Profile profile2 = createAndRetrieveProfile(email2);
		
		String email3 = "shared3@gmail.com";
		Profile profile3 = createAndRetrieveProfile(email3);
		
		Profile owner = createAndRetrieveProfile();
		
		//create two tasks
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, owner, null, PriorityEnum.HIGH, null,null);
		Task task1 = createAndRetrieveTask(task);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email1, "");
		emailAddresses.put(email2, "");
		emailAddresses.put(email3, "");
		Invite invite = createShares(task1, emailAddresses);
		
		Map<String, String> pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", profile1.getId());
		Share share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		
		Notification notification = notificationService.getNotificationsForINotificationTX(share).get(0);
		
		Assert.assertTrue(notification.getBody().contains(email1));
		Assert.assertFalse(notification.getBody().contains(email2));
		Assert.assertFalse(notification.getBody().contains(email3));
		
		pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", profile2.getId());
		share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		
		notification = notificationService.getNotificationsForINotificationTX(share).get(0);
		
		Assert.assertFalse(notification.getBody().contains(email1));
		Assert.assertTrue(notification.getBody().contains(email2));
		Assert.assertFalse(notification.getBody().contains(email3));
		
		pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", profile3.getId());
		share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		
		notification = notificationService.getNotificationsForINotificationTX(share).get(0);
		
		Assert.assertFalse(notification.getBody().contains(email1));
		Assert.assertFalse(notification.getBody().contains(email2));
		Assert.assertTrue(notification.getBody().contains(email3));
		
	}
	
	@Test
	public void singleShareBulk()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		//create two tasks
		Task task1 = createAndRetrieveTask(owner);
		
		//create shares
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email, "Tim Delesio");
		Invite invite1 = createShares(task1, emailAddresses);
		
		Map<String, String> pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", profile.getId());
		Share share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		
		pathParms = buildPathParms("{ids}", share.getId());
		List<Share> shares = callWebService(ShareWebService.class, "getBulkShares", new GenericType<List<Share>>(){}, pathParms);
		
		Assert.assertTrue(shares.contains(share));
	}
	
	@Test
	public void doubleShares()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		//create two tasks
		Task task1 = createAndRetrieveTask(owner);
		
		//create shares
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email, "Tim Delesio");
		Invite invite1 = createShares(task1, emailAddresses);
		Invite invite2 = createShares(task1, emailAddresses);
		
		//get invites
		List<Invite> invites = callWebService(ShareWebService.class, "getRecievedInvitesForMember", new GenericType<List<Invite>>(){}, buildPathParms("{memberid}", profile.getId()));		
		Assert.assertTrue(invites.contains(invite1));
		Assert.assertFalse(invites.contains(invite2));
	}
	
	@Test
	public void shareTaskUninvite()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		//create two tasks
		Task task1 = createAndRetrieveTask(owner);
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, owner, null, PriorityEnum.HIGH, null, null);
		task2 = createAndRetrieveTask(task2);
		
		//create shares
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email, "Tim Delesio");
		Invite invite1 = createShares(task1, emailAddresses);
		
		Invite invite2 = createShares(task2, emailAddresses);
		
		//get invites
		List<Invite> invites = callWebService(ShareWebService.class, "getRecievedInvitesForMember", new GenericType<List<Invite>>(){}, buildPathParms("{memberid}", profile.getId()));
		
		Assert.assertTrue(invites.contains(invite1));
		Assert.assertTrue(invites.contains(invite2));
		
		//get share
		Map<String, String> pathParms = buildPathParms("{taskid}", task2.getId());
		pathParms.put("{memberid}", profile.getId());
		Share share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		Assert.assertEquals(share.getTask().getId(), task2.getId());
		Assert.assertEquals(share.getUser().getId(), profile.getId());
		
		//uninvite from task2
		callWebService(ShareWebService.class, "deleteShare", Boolean.class, buildPathParms("{shareid}", share.getId()));
		
		//verify uninvite
//		pathParms = buildPathParms("{taskid}", task2.getId());
//		pathParms.put("{memberid}", profile.getId());
//		ClientResponse response = callWebService(ShareWebService.class, "getShareForUserAndTask", ClientResponse.class, pathParms);
//		Assert.assertEquals(400, response.getStatus());
		Map<String, List<Object>> syncs = sync(profile.getId(), 0);
		Assert.assertFalse(syncs.get(Task.class.getName()).contains(task2.getId()));
		
		
		invites = callWebService(ShareWebService.class, "getRecievedInvitesForMember", new GenericType<List<Invite>>(){}, buildPathParms("{memberid}", profile.getId()));
		
		Assert.assertTrue(invites.contains(invite1));
		Assert.assertFalse(invites.contains(invite2));
		
		//try and access the private task now via direct link
		pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task2.getId());
		pathParms.put("{email}", email);
		ClientResponse response = callWebService(ShareWebService.class, "getPrivateShareDirectLink", ClientResponse.class, pathParms);
		assertValidationError(response, ValidationCodeEnum.MEMBER_DOES_NOT_HAVE_AUTHORIZATION);
		
		
	}
	
	@Test
	public void acceptPublicShare()
	{
		Profile owner = createAndRetrieveProfile();
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Task task1 = createAndRetrieveTask(owner);
		
		Map<String, String> pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", profile.getId());
		Share response = callWebService(ShareWebService.class, "acceptShare", Share.class, pathParms);
		
		Assert.assertTrue(response.getId()!=null);
	}
	
	@Test
	public void getShareForUserAndTask()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(owner);
		Task task2 = createAndRetrieveTask(owner);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(profile.getMember().getEmail(), profile.getName());
		
		Invite invite1 = createShares(task1, emailAddresses);		
		Invite invite2 = createShares(task2, emailAddresses);
		
		Map<String, String> pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", profile.getId());
		Share share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		
		Assert.assertEquals(task1, share.getTask());
		Assert.assertEquals(profile.getMember(), share.getUser());
		
		pathParms = buildPathParms("{taskid}", task2.getId());
		pathParms.put("{memberid}", profile.getId());
		share = callWebService(ShareWebService.class, "getShareForUserAndTask", Share.class, pathParms);
		
		Assert.assertEquals(task2, share.getTask());
		Assert.assertEquals(profile.getMember(), share.getUser());
	}
	
	@Test
	public void getRecievedInvitesForMember()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(owner);
		Task task2 = createAndRetrieveTask(owner);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email, "Tim Delesio");
		Invite invite1 = createShares(task1, emailAddresses);
		
		Invite invite2 = createShares(task2, emailAddresses);
		
		List<Invite> invites = callWebService(ShareWebService.class, "getRecievedInvitesForMember", new GenericType<List<Invite>>(){}, buildPathParms("{memberid}", profile.getId()));
		
		Assert.assertTrue(invites.contains(invite1));
		Assert.assertTrue(invites.contains(invite2));
	}
	
	@Test
	public void getSentInvitesForMember()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(owner);
		Task task2 = createAndRetrieveTask(owner);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email, "Tim Delesio");
		Invite invite1 = createShares(task1, emailAddresses);
		
		Invite invite2 = createShares(task2, emailAddresses);
		
		List<Invite> invites = callWebService(ShareWebService.class, "getSentInvitesForMember", new GenericType<List<Invite>>(){}, buildPathParms("{memberid}", owner.getId()));
		
		Assert.assertTrue(invites.contains(invite1));
		Assert.assertTrue(invites.contains(invite2));
	}
	
	@Test
	public void deleteShare()
	{
		Profile profile = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@homefellas.com");
		
		Task task1 = createAndRetrieveTask(profile);
		
		Invite invite = createShares(task1, sharee);
		
		List<Share> shares = callWebService(ShareWebService.class, "getSharesForTasks", new GenericType<List<Share>>(){}, buildPathParms("{taskid}", task1.getId()));
		
		Assert.assertTrue(shares.size()==1);
		
		Share share = shares.get(0);
		
		callWebService(ShareWebService.class, "deleteShare", Boolean.class, buildPathParms("{shareid}", share.getId()));
		
		shares = callWebService(ShareWebService.class, "getSharesForTasks", new GenericType<List<Share>>(){}, buildPathParms("{taskid}", task1.getId()));
		
		Assert.assertTrue(shares.isEmpty());
		
		Map<String, List<Object>> syncs = sync(sharee.getId(), 0);
		Assert.assertTrue(syncs.get(Share.class.getName()).isEmpty());
		Assert.assertTrue(syncs.get(Task.class.getName()).isEmpty());
		
	}
	
	
	@Test
	public void getSharesForUser()
	{
		String email = "tdelesio@gmail.com";
		Profile profile = createAndRetrieveProfile(email);
		Profile owner = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(owner);
		Task task2 = createAndRetrieveTask(owner);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(email, "Tim Delesio");
		Invite invite1 = createShares(task1, emailAddresses);
		
		Invite invite2 = createShares(task2, emailAddresses);
		
		List<Share> shares = callWebService(ShareWebService.class, "getSharesForUser", new GenericType<List<Share>>(){}, buildPathParms("{userid}", profile.getId()));
		Assert.assertEquals(2, shares.size());
	}
	
	@Test
	public void getShareDirectLink()
	{
		//public task
		Profile profile = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(profile);
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task1.getId());
		Share share = callWebService(ShareWebService.class, "getPublicShareDirectLink", Share.class, pathParms);
		
		Assert.assertEquals(task1, share.getTask());
		
		//private task along with a share
		task1 = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, profile, null, PriorityEnum.HIGH, null, null);
		task1 = createAndRetrieveTask(task1);
		
		//share the task with a user
		Invite invite = new Invite();
		invite.setDirectLink("http://localhost");
		invite.setMessage("join my task");
		invite.setSubject("subject");
		Map<String, String> emailMap = new HashMap<String, String>();
		emailMap.put("share1@homefellas.com", "share1");
		invite.setEmailAddresses(emailMap);
		invite.setShareId(task1.getId());
		
		invite = callWebService(ShareWebService.class, "shareTask", Invite.class, invite);
		
		//try and access the private task now via direct link
		pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task1.getId());
		pathParms.put("{email}", "share1@homefellas.com");
		share = callWebService(ShareWebService.class, "getPrivateShareDirectLink", Share.class, pathParms);
		
		Assert.assertEquals(task1, share.getTask());
		Assert.assertFalse(share.isViewed());
		Assert.assertFalse(share.isBlurred());
		
		//mark the share as viewed
		pathParms = new HashMap<String, String>();
		pathParms.put("{shareid}", String.valueOf(share.getId()));
		share = callWebService(ShareWebService.class, "shareViewed", Share.class, pathParms);
		
		Assert.assertTrue(share.isViewed());
		Assert.assertFalse(share.isBlurred());
		
		//try and access the task again to see if it is blurred
		pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task1.getId());
		pathParms.put("{email}", "share1@homefellas.com");
		share = callWebService(ShareWebService.class, "getPrivateShareDirectLink", Share.class, pathParms);
		
		Assert.assertEquals(task1, share.getTask());
		Assert.assertTrue(share.isViewed());
		Assert.assertTrue(share.isBlurred());
		
		//create another profile
		Profile profile2 = createAndRetrieveProfile("profile2@homefellas.com");
		
		//share the task with a member now
		invite = new Invite();
		invite.setDirectLink("http://localhost");
		invite.setMessage("join my task");
		invite.setSubject("subject");
		emailMap = new HashMap<String, String>();
		emailMap.put(profile2.getMember().getEmail(), "share2");
		invite.setEmailAddresses(emailMap);
		invite.setShareId(task1.getId());
		
		invite = callWebService(ShareWebService.class, "shareTask", Invite.class, invite);
		
		//try and access the private task now via direct link
		pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task1.getId());
		String email = profile2.getMember().getEmail();
		pathParms.put("{email}", email);
		share = callWebService(ShareWebService.class, "getPrivateShareDirectLink", Share.class, pathParms);
		
		Assert.assertEquals(task1, share.getTask());
		Assert.assertFalse(share.isViewed());
		Assert.assertFalse(share.isBlurred());
		
		//mark the share as viewed
		pathParms = new HashMap<String, String>();
		pathParms.put("{shareid}", String.valueOf(share.getId()));
		share = callWebService(ShareWebService.class, "shareViewed", Share.class, pathParms);
		
		Assert.assertTrue(share.isViewed());
		Assert.assertFalse(share.isBlurred());
	}
	

	@Test
	public void acceptShare()
	{
		Profile profile = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(profile);
		
		Invite invite = createAndRetrieveShares(task1);
		
		Assert.assertTrue(invite.isPrimaryKeySet());
		
		List<Share> shares = callWebService(ShareWebService.class, "getSharesForTasks", new GenericType<List<Share>>(){}, buildPathParms("{taskid}", task1.getId()));
		
		Assert.assertTrue(shares.size()==1);
		
		Share share = shares.get(0);
		
		share = callWebService(ShareWebService.class, "acceptShareOld", Share.class, buildPathParms("{shareid}", String.valueOf(share.getId())));
		Assert.assertEquals(ShareApprovedStatus.APPROVED.ordinal(), share.getShareApprovedStatusOrdinal());
	}
	
	@Test
	public void declineShare()
	{
		Profile profile = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(profile);
		
		Invite invite = createAndRetrieveShares(task1);
		
		Assert.assertTrue(invite.isPrimaryKeySet());
		
		List<Share> shares = callWebService(ShareWebService.class, "getSharesForTasks", new GenericType<List<Share>>(){}, buildPathParms("{taskid}", task1.getId()));
		
		Assert.assertTrue(shares.size()==1);
		
		Share share = shares.get(0);
		
		share = callWebService(ShareWebService.class, "declineShare", Share.class, buildPathParms("{shareid}", String.valueOf(share.getId())));
		Assert.assertEquals(ShareApprovedStatus.DECLINED.ordinal(), share.getShareApprovedStatusOrdinal());
	}
	
	@Test
	public void shareTask()
	{
		Profile profile = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(profile);
		
		Invite invite = createAndRetrieveShares(task1);
		
		Assert.assertTrue(invite.isPrimaryKeySet());
	}

}
