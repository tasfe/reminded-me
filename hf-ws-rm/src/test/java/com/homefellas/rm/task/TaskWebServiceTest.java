package com.homefellas.rm.task;

import static com.homefellas.rm.RMTestModelBuilder.task;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.interactions.touch.UpAction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.homefellas.batch.INotificationServiceTX;
import com.homefellas.batch.Notification;
import com.homefellas.batch.NotificationService;
import com.homefellas.dao.core.IDao;
import com.homefellas.exception.IValidationCode;
import com.homefellas.rm.ISynchronizeable;
import com.homefellas.rm.RMTestModelBuilder;
import com.homefellas.rm.RemindedMeWebService;
import com.homefellas.rm.ValidationCodeEnum;
import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.calendar.GenericCalendarEnum;
import com.homefellas.rm.notification.ClientNotification;
import com.homefellas.rm.notification.ClientNotification.ClientNotificationStatusEnum;
import com.homefellas.rm.notification.ClientNotificationWebService;
import com.homefellas.rm.reminder.Alarm;
import com.homefellas.rm.reminder.RepeatOccurance;
import com.homefellas.rm.repeatsetup.RepeatSetup;
import com.homefellas.rm.repeatsetup.TaskTemplate;
import com.homefellas.rm.share.Share;
import com.homefellas.rm.share.ShareApprovedStatus;
import com.homefellas.rm.task.AbstractTask.PriorityEnum;
import com.homefellas.rm.task.AbstractTask.ProgressEnum;
import com.homefellas.user.Profile;
import com.homefellas.ws.core.AbstractTestRMWebService;
import com.homefellas.ws.core.JodaDateTimeJsonDeSerializer;
import com.homefellas.ws.model.TaskUI;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

public class TaskWebServiceTest extends AbstractTestRMWebService
{
	
	protected Category categoryDefaultWork;
	protected Category categoryDefaultPersonal;
	protected Calendar calendarDefaultWork;
	private INotificationServiceTX notificationService;
	
	private TaskWebService taskWebService;
	
	@Before
	public void setupDefaultDatabaseValues()
	{
		super.setupDefaultDatabaseValues();
		
		dao = (IDao)getServer().getSpringBean("dao");
		transactionManager = (PlatformTransactionManager)getServer().getSpringBean("transactionManager");
		notificationService = (NotificationService)getServer().getSpringBean("notificationService");
		
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
		   @Override
		   protected void doInTransactionWithoutResult(TransactionStatus status) {
				
				categoryDefaultWork = GenericCategoryEnum.Work.getCategory();
				dao.save(categoryDefaultWork);
				
				categoryDefaultPersonal = GenericCategoryEnum.Personal.getCategory();
				dao.save(categoryDefaultPersonal);
				
				calendarDefaultWork = GenericCalendarEnum.Work.getCalendar();
				dao.save(calendarDefaultWork);
		   }});
		
	}
	
	/***********************************************************************************************
	 * 
	 * 								START TESTS
	 * 
	 ***********************************************************************************************/
	@Test
	public void multipleAlarms()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.task(profile);
		task.setEndTime(new DateTime().plusDays(3));
		task = createAndRetrieveTask(task);
		
		Alarm alarm = createAndRetrieveAlarm(task);
		
		Map<String, List<Object>> syncs = sync(profile.getId(), 0);
		long syncTime = ((Long)syncs.get(ISynchronizeable.SYSTEM_TIME).get(0)).longValue();
		
		task = getBulkTasks(syncs.get(Task.class.getName()), profile.getEmail()).get(0);
		alarm = getBulkAlarms(syncs.get(Alarm.class.getName()), profile.getEmail()).get(0);
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		task.setEndTime(new DateTime().plusDays(4));
		task.setModifiedDate(System.currentTimeMillis());
		task = updateTask(task);
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		alarm.setAlarmTime(new DateTime().plusDays(4).plusHours(1));
		alarm.setModifiedDate(System.currentTimeMillis());
		alarm = updateAlarm(alarm);
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		syncs = sync(profile.getId(), syncTime);
		syncTime = ((Long)syncs.get(ISynchronizeable.SYSTEM_TIME).get(0)).longValue();
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		task = getBulkTasks(syncs.get(Task.class.getName()), profile.getEmail()).get(0);
		alarm = getBulkAlarms(syncs.get(Alarm.class.getName()), profile.getEmail()).get(0);
		
		task.setEndTime(new DateTime().plusDays(5));
		task.setModifiedDate(System.currentTimeMillis());
		task = updateTask(task);
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		alarm.setAlarmTime(new DateTime().plusDays(5).plusHours(1));
		alarm.setModifiedDate(System.currentTimeMillis());
		alarm = updateAlarm(alarm);
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(profile.getEmail());
		Assert.assertEquals(3, notifications.size());
		
		try{Thread.sleep(100);}catch(Exception e){}
		
		syncs = sync(profile.getId(), 0);
		List<Object> ids = syncs.get(Alarm.class.getName());
		
		List<Alarm> alarms = getBulkAlarms(ids, profile.getEmail());
		Assert.assertEquals(1, alarms.size());
	}
	
	@Test
	public void taskEndDateChanged()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.task(profile);
		task.setEndTime(new DateTime().plusDays(3));
		task = createAndRetrieveTask(task);
		
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
		
		Share share = createAndRetrieveShares(task, sharee);
		
		acceptShare(share);
		
		task.setEndTime(new DateTime().plusDays(2));
		task = updateTask(task);
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(sharee.getEmail());
		
		Assert.assertEquals(5, notifications.size());
		Assert.assertEquals(task.getEndTime().getMillis(), notifications.get(4).getToSendTime());
		
		notifications = notificationService.getNotificationsToEmailTX(profile.getEmail());
		Assert.assertEquals(3, notifications.size());
		
		Assert.assertEquals(task.getEndTime().getMillis(), notifications.get(2).getToSendTime());
	}
	
	@Test
	public void taskUpdateBeforeRepeatSetupCreate()
	{
		Profile profile1 = createAndRetrieveProfile();
		Task task1 = createAndRetrieveTask(profile1);
		
		RepeatSetup repeatSetup = RMTestModelBuilder.repeatSetup(task1.getId(), profile1.getMember());
		task1.setRepeatSetup(repeatSetup);
		
		updateTask(task1);
		
		repeatSetup = callWebService(TaskWebService.class, "createRepeatSetup", RepeatSetup.class, repeatSetup);
		
		task1 = getBulkTasks(task1.getId(), profile1.getEmail());
		
		Assert.assertEquals(repeatSetup, task1.getRepeatSetup());
	}
	
	@Test
	public void appleIOSEvent()
	{
		Profile owner = createAndRetrieveProfile();
		
		Task task = createAndRetrieveTask(owner);
		
		AppleIOSCalEvent appleIOSCalEvent = RMTestModelBuilder.appleIOSCalEvent(owner.getMember());
		task.setAppleIOSCalEvent(appleIOSCalEvent);
		
		updateTask(task);
	}
	
	@Test
	public void repeatSetupDeleteReAdd()
	{
		Profile owner = createAndRetrieveProfile();
		
		Task task = createAndRetrieveTask(owner);
		
	
		RepeatSetup repeatSetup = RMTestModelBuilder.repeatSetup(task.getId(), owner.getMember());
		repeatSetup = callWebService(TaskWebService.class, "createRepeatSetup", RepeatSetup.class, repeatSetup);
		
		repeatSetup = callSecuredWebService(TaskWebService.class, "deleteRepeatSetup", RepeatSetup.class, buildPathParms("{id}", repeatSetup.getId()), owner.getEmail());
		
//		ClientResponse response = callSecuredWebService(TaskWebService.class, "cancelTask", ClientResponse.class, buildPathParms("{taskid}", task.getId()), owner.getId());
		Map<String, String> pathParms = buildPathParms("{id}", task.getId());
		pathParms.put("{status}", "5");
		task = callSecuredWebService(TaskWebService.class, "updateTaskStatus", Task.class, pathParms, owner.getEmail());
//		Assert.assertEquals(200, response.getStatus());
		
	}
	
	@Test
	public void notifyShareeWhenStatusChanges()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee1 = createAndRetrieveProfile("sharee1@reminded.me");
		
		Task task = createAndRetrieveTask(owner);
		
		Share share = createAndRetrieveShares(task, sharee1);
		
		acceptShare(share);
		
		task = share.getTask();
		task.setProgress(ProgressEnum.ON_HOLD.ordinal());
		task = updateTask(task);
		
		Assert.assertEquals(ProgressEnum.ON_HOLD.ordinal(), task.getProgress());
		Assert.assertTrue(task.isHasBeenShared());
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(sharee1.getEmail());
		Assert.assertEquals(4, notifications.size());
	}
	
	@Test
	public void notifyShareeWhenStatusChangesNoAccept()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee1 = createAndRetrieveProfile("sharee1@reminded.me");
		
		Task task = createAndRetrieveTask(owner);
		
		Share share = createAndRetrieveShares(task, sharee1);
		
		
		task = share.getTask();
		task.setProgress(ProgressEnum.ON_HOLD.ordinal());
		task = updateTask(task);
		
		Assert.assertEquals(ProgressEnum.ON_HOLD.ordinal(), task.getProgress());
		Assert.assertTrue(task.isHasBeenShared());
		
		List<Notification> notifications = notificationService.getNotificationsToEmailTX(sharee1.getEmail());
		Assert.assertEquals(2, notifications.size());
	}
	
	
	@Test
	public void getAllDependantTaskObjects()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee1 = createAndRetrieveProfile("sharee1@reminded.me");
		Profile sharee2 = createAndRetrieveProfile("sharee2@reminded.me");


		Calendar calendar = createAndRetrieveCalendar("new calendar", owner.getMember());
		Task task1 = task(owner);
		task1.setEndTime(new DateTime().plusDays(1));
		task1.addCalendar(calendar);
		task1 = createAndRetrieveTask(task1);
	
		
		
		Share share1 = createAndRetrieveShares(task1, sharee1);
		Share share2 = createAndRetrieveShares(task1, sharee2);
		
//		List<Share> shares = shareService.getSharesForTaskTX(task1);
//		Assert.assertTrue(shares.contains(share1));
//		Assert.assertTrue(shares.contains(share2));
//		
		Alarm alarm1 = createAndRetrieveAlarm(task1, task1.getEndTime().minusMinutes(5));
		Alarm alarm2 = createAndRetrieveAlarm(task1, task1.getEndTime().minusHours(2));
		
//		dao.refresh(task1);

		
//		Task task = dao.loadByPrimaryKey(Task.class, task1.getId());
//		Assert.assertTrue(task.getShares().contains(share1));
//		Assert.assertTrue(task.getShares().contains(share2));
//		
//		Assert.assertTrue(task.getAlarms().contains(alarm1));
//		Assert.assertTrue(task.getAlarms().contains(alarm2));
//		try
//		{
//			List<Task> tasks = taskService.getUpcomingTasksTX(owner.getEmail(), 10, 0);
			List<TaskUI> tasks = callSecuredWebService(TaskWebService.class, "getUpcomingTasks", new GenericType<List<TaskUI>>(){}, buildPathParms("", ""), owner.getEmail());
			
//			Assert.assertTrue(tasks.contains(task1));
			
			TaskUI taskUnderTest = tasks.get(0);
			Assert.assertEquals(2, taskUnderTest.getShares().size());
			
			Assert.assertEquals(2, taskUnderTest.getAlarms().size());
//			}
//			catch (ValidationException e)
//			{
//				Assert.fail(e.getMessage());
//			}
	}
	
	@Test
	public void taskShareStatusSetToShared()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
		Task task = createAndRetrieveTask(owner);
		
		Share share = createAndRetrieveShares(task, sharee);
		
		Map<String, List<Object>> syncs = sync(owner.getId(), 0);
		List<Object> ids = syncs.get(Task.class.getName());
		
		List<Task> tasks = getBulkTasks(ids, owner.getEmail());
		Assert.assertEquals(ShareApprovedStatus.SHARED_TASK.ordinal(), tasks.get(0).getShareStatus());
		
		syncs = sync(sharee.getId(), 0);
		ids = syncs.get(Task.class.getName());
		
		tasks = getBulkTasks(ids, sharee.getEmail());
		Assert.assertEquals(ShareApprovedStatus.NO_ACTION.ordinal(), tasks.get(0).getShareStatus());
		
	}
	
	@Test
	public void getTasksByStatusAndMemberId()
	{
		Profile profile = createAndRetrieveProfile();
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		task1.setProgress(ProgressEnum.RUNNING_LATE.ordinal());
		
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		task2.setProgress(ProgressEnum.ON_HOLD.ordinal());
		
		task2 = createAndRetrieveTask(task2);
		
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		task3.setProgress(ProgressEnum.RUNNING_LATE.ordinal());
		
		task3 = createAndRetrieveTask(task3);
		
		Map<String, String> pathparm = buildPathParms("{id}", profile.getId());
		pathparm.put("{status}", String.valueOf(ProgressEnum.RUNNING_LATE.ordinal()));
		List<Task> tasks = callWebService(TaskWebService.class, "getTasksByStatusAndMemberId", new GenericType<List<Task>>(){}, pathparm);
		
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task3));
		Assert.assertFalse(tasks.contains(task2));
	}
	
	@Test
	public void repeatTasks()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		task = createAndRetrieveTask(task);
		
		RepeatSetup repeatSetup = new RepeatSetup();
		repeatSetup.generateGUIDKey();
		repeatSetup.setMember(profile.getMember());
		repeatSetup.setClonedTask(new TaskTemplate(task.getId()));
		repeatSetup.setRepeatOccurance(RepeatOccurance.hour.toString());
		
		repeatSetup = callWebService(TaskWebService.class, "createRepeatSetup", RepeatSetup.class, repeatSetup);
		
		task.setRepeatSetup(repeatSetup);
		updateTask(task);
		
		List<RepeatSetup> repeatSetups = callSecuredWebService(TaskWebService.class, "getBulkRepeatSetup", new GenericType<List<RepeatSetup>>(){}, buildPathParms("{ids}", repeatSetup.getId()), "invalid@reminded.me");
		Assert.assertTrue(repeatSetups.isEmpty());
		
		repeatSetups = callSecuredWebService(TaskWebService.class, "getBulkRepeatSetup", new GenericType<List<RepeatSetup>>(){}, buildPathParms("{ids}", repeatSetup.getId()), profile.getEmail());
		Assert.assertEquals(repeatSetup.getId(), repeatSetups.get(0).getId());
		
		repeatSetup.setRepeatOccurance(RepeatOccurance.day.toString());
		callSecuredWebService(TaskWebService.class, "updateRepeatSetup", RepeatSetup.class, repeatSetup, "invalid@reminded.me");
		//check failure
		
		repeatSetups = callSecuredWebService(TaskWebService.class, "getBulkRepeatSetup", new GenericType<List<RepeatSetup>>(){}, buildPathParms("{ids}", repeatSetup.getId()), profile.getEmail());
		Assert.assertEquals(RepeatOccurance.hour.toString(), repeatSetups.get(0).getRepeatOccurance());
		
		//now pass
		callSecuredWebService(TaskWebService.class, "updateRepeatSetup", RepeatSetup.class, repeatSetup, profile.getEmail());
		repeatSetups = callSecuredWebService(TaskWebService.class, "getBulkRepeatSetup", new GenericType<List<RepeatSetup>>(){}, buildPathParms("{ids}", repeatSetup.getId()), profile.getEmail());
		Assert.assertEquals(RepeatOccurance.day.toString(), repeatSetups.get(0).getRepeatOccurance());
		
		//finally delete
		callSecuredWebService(TaskWebService.class, "deleteRepeatSetup", RepeatSetup.class, buildPathParms("{id}", repeatSetup.getId()), profile.getEmail());
		Assert.assertFalse(sync(profile.getId(), 0).get(RepeatSetup.class.getName()).contains(repeatSetup.getId()));
	}
	
	@Test
	public void notifyTaskCreatorAndShareeOnTaskEndDate()
	{
		//create task, share it and make sure the end notifications are created.
		Profile profile = createAndRetrieveProfile();
		Profile sharee = createAndRetrieveProfile("sharee@reminded.me");
	
		//create a task with an end date
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null, new DateTime().plusDays(1));
		task1 = createAndRetrieveTask(task1);
		
		Share share = createAndRetrieveShares(task1, sharee);
		
		acceptShare(share);
		
		List<Notification> notifications = notificationService.getNotificationsForINotificationTX(task1);
		Assert.assertEquals(profile.getEmail(), notifications.get(0).getSendTo());
		Assert.assertEquals(sharee.getEmail(), notifications.get(1).getSendTo());
		
		
		//create task without end date, share it, then update task and add end date and make sure it is sent out
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null, null);
		task2 = createAndRetrieveTask(task2);
		
		share = createAndRetrieveShares(task2, sharee);
		
		notifications = notificationService.getNotificationsForINotificationTX(task2);
		Assert.assertTrue(notifications.isEmpty());
		
		task2.setEndTime(new DateTime().plusDays(1));
		updateTask(task2);

//		notifications = notificationService.getNotificationsForINotification(task2);
//		Assert.assertEquals(profile.getEmail(), notifications.get(0).getSendTo());
//		Assert.assertEquals(sharee.getEmail(), notifications.get(1).getSendTo());
		
		//create a 2 tasks as part of a calendar, share the calendar and verify the notifications are passed along
	}
	
	@Test
	public void clientNotification()
	{
		Profile profile = createAndRetrieveProfile();
		
		ClientNotification clientNotification = new ClientNotification();
		clientNotification.setMember(profile.getMember());
		clientNotification.setReferenceClassName(Task.class.getName());
		clientNotification.setReferenceId(profile.getId());
		clientNotification.generateGUIDKey();
		
		clientNotification = callWebService(ClientNotificationWebService.class, "createClientNotifications", ClientNotification.class, clientNotification);
		
		clientNotification.setNotificationType(ClientNotificationStatusEnum.read.toString());
		clientNotification = callSecuredWebService(ClientNotificationWebService.class, "updateClientNotifications", ClientNotification.class, clientNotification, profile.getEmail());
		
		List<ClientNotification> notifications = callSecuredWebService(ClientNotificationWebService.class, "getBulkClientNotifications", new GenericType<List<ClientNotification>>(){}, buildPathParms("{ids}", clientNotification.getId()), profile.getEmail());
		Assert.assertEquals(ClientNotificationStatusEnum.read.toString(), notifications.get(0).getNotificationType());
	}
	
	@Test
	public void test401Error()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task = createAndRetrieveTask(task);
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", task.getId());
		ClientResponse response = callWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms, null);
		
		Assert.assertEquals(401, response.getStatus());
	}
	
	
	@Test
	public void syncUpdateSyncWithDeleteStatus()
	{
		String deviceid = "1";
		Profile profile = createAndRetrieveProfile();
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		task1 = createAndRetrieveTask(task1);
		
		Map<String, List<Object>> syncs = sync(profile.getId(), 0);
		long syncTime = (Long)syncs.get(ISynchronizeable.SYSTEM_TIME).get(0);
		
		Assert.assertTrue(syncs.get(Task.class.getName()).contains(task1.getId()));
		
		task1.setProgress(ProgressEnum.DELETE.ordinal());
		task1.setLastModifiedDeviceId(deviceid);
		task1.setModifiedDateZone(new DateTime(syncTime));
		task1 = updateTask(task1);
//		task1 = callWebService(TaskWebService.class, "updateTask", Task.class, task1);
		
		Task canceledTask = getBulkTasks(task1.getId(), profile.getMember().getEmail());
		Assert.assertEquals(ProgressEnum.DELETE.ordinal(), canceledTask.getProgress());
		
		syncs = sync(profile.getId(), syncTime, deviceid);
		
		//this was changed from false to true but i am not sure what it should be
		Assert.assertTrue(syncs.get(Task.class.getName()).contains(task1.getId()));
//		Assert.assertFalse(syncs.get(Task.class.getName()).contains(task1.getId()));
		
	}
	
	@Test
	public void syncDeleteSync()
	{
		Profile profile = createAndRetrieveProfile();
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		task1 = createAndRetrieveTask(task1);
		
		Map<String, List<Object>> syncs = sync(profile.getId(), 0);
		long syncTime = (Long)syncs.get(ISynchronizeable.SYSTEM_TIME).get(0);
		
		Assert.assertTrue(syncs.get(Task.class.getName()).contains(task1.getId()));
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", task1.getId());
		ClientResponse response = callSecuredWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms, profile.getMember().getEmail());
		assertOKStatus(response);
		
		Task canceledTask = getBulkTasks(task1.getId(), profile.getMember().getEmail());
		Assert.assertEquals(ProgressEnum.DELETE.ordinal(), canceledTask.getProgress());
		
		Assert.assertEquals(200, response.getStatus());
		
		syncs = sync(profile.getId(), 0);
		
		Assert.assertFalse(syncs.get(Task.class.getName()).contains(task1.getId()));
		
	}
	
	@Test
	public void makeSureThatSharesGetModifiedTimeOnTaskUpdate()
	{
		
		Profile sharer = createAndRetrieveProfile("sharer@homefellas.com");
		
		Profile profile = createAndRetrieveProfile();
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		task1 = createAndRetrieveTask(task1);
		
		//owner sycns and task should be there
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		List<Object> tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		
		
		//sharee syncs, no task yet
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", sharer.getId());
		pathParms.put("{lastModifiedTime}", "0");
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		tasks = syncedTasks.get(Task.class.getName());
		Assert.assertFalse(tasks.contains(task1.getId()));
		long syncTime = (Long)syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0);
		
		//owner shares with sharer
		Map<String, String> emailAddresses =  new HashMap<String, String>();
		emailAddresses.put(sharer.getMember().getEmail(), sharer.getName());
		createShares(task1, emailAddresses);
		
		//sharee syncs, should now get the task
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", sharer.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(syncTime));
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		syncTime = (Long)syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0);
		
		//owner now performs an update
		task1.setProgress(ProgressEnum.STARTED.ordinal());
		task1 = updateTask(task1);
//		task1 = callWebService(TaskWebService.class, "updateTask", Task.class, task1);
		
		//sharee syncs again and now should get the updated task
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", sharer.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(syncTime));
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		syncTime = (Long)syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0);
	}
	
	@Test
	public void createDeleteSyncTask()
	{
		Profile profile = createAndRetrieveProfile();
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		task2 = createAndRetrieveTask(task2);
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", task1.getId());
		ClientResponse response = callSecuredWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms, profile.getMember().getEmail());
		assertOKStatus(response);
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		List<Object> tasks = syncedTasks.get(Task.class.getName());
		
		Assert.assertTrue(tasks.contains(task2.getId()));
		Assert.assertFalse(tasks.contains(task1.getId()));
		
		
	}
	
	
	
	@Test
	public void testLocalizedTimeDate()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,new DateTime(DateTimeZone.forOffsetHours(2)));
		
		DateTime actualEndTime = task.getEndTime(); 
		
		
		task = createAndRetrieveTask(task);
		
		String id = task.getId();
		List<Task> tasks  = callSecuredWebService(TaskWebService.class, "getBulkTasks", new GenericType<List<Task>>(){}, buildPathParms("{ids}", id), profile.getMember().getEmail());
		
		Assert.assertTrue(tasks.contains(task));
		
		Task taskUnderTest = tasks.get(0);
		DateTime endTime = taskUnderTest.getEndTime();
		
		String expectedString = DateTimeFormat.forPattern(JodaDateTimeJsonDeSerializer.dateFormat).print(actualEndTime);
		String actualString = DateTimeFormat.forPattern(JodaDateTimeJsonDeSerializer.dateFormat).print(endTime);
		Assert.assertEquals(expectedString, actualString);
	}
	
	
	@Test
	public void bulkSpeedTest()
	{
		Profile profile = createAndRetrieveProfile();
		Calendar calendar1 = createAndRetrieveCalendar("calendar1", profile.getMember());
		StringBuffer ids = new StringBuffer();
		
		int size=10;
		for (int i=0;i<size-1;i++)
		{
			Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
			task.addCalendar(calendar1);
			task = createAndRetrieveTask(task);
//			Task task1 = createAndRetrieveTask(profile);
			ids.append(task.getId());
			ids.append(",");
		}
		Task task1 = createAndRetrieveTask(profile);
		ids.append(task1.getId());
		long start = System.currentTimeMillis();
		List<Task> tasks  = callSecuredWebService(TaskWebService.class, "getBulkTasks", new GenericType<List<Task>>(){}, buildPathParms("{ids}", ids.toString()),profile.getMember().getEmail());
		
		
		long end = System.currentTimeMillis();
		
		Assert.assertTrue(tasks.size()==size);
	}
	
	
	@Test
	public void getBulkTask()
	{
		Profile profile = createAndRetrieveProfile();
		
		Task task1 = createAndRetrieveTask(profile);
		Task task2 = createAndRetrieveTask(profile);
		Task task3 = createAndRetrieveTask(profile);
		
		String id = task1.getId()+","+task2.getId()+","+task3.getId();
		List<Task> tasks  = callSecuredWebService(TaskWebService.class, "getBulkTasks", new GenericType<List<Task>>(){}, buildPathParms("{ids}", id), profile.getMember().getEmail());
		Assert.assertTrue(tasks.contains(task1));
		Assert.assertTrue(tasks.contains(task2));
		Assert.assertTrue(tasks.contains(task3));
		
	}
	
	@Test
	public void getBulkCategory()
	{
		Profile profile = createAndRetrieveProfile();
		
		Category category1 = createAndRetrieveCategory("category1", profile.getMember());
		Category category2 = createAndRetrieveCategory("category2", profile.getMember());
		Category category3 = createAndRetrieveCategory("category3", profile.getMember());
		
		String id = category1.getId()+","+category2.getId()+","+category3.getId();
		List<Category> categories  = callWebService(TaskWebService.class, "getBulkCategories", new GenericType<List<Category>>(){}, buildPathParms("{ids}", id));
		Assert.assertTrue(categories.contains(category1));
		Assert.assertTrue(categories.contains(category2));
		Assert.assertTrue(categories.contains(category3));
		
	}
	
	@Test
	public void getBulkCalendar()
	{
		Profile profile = createAndRetrieveProfile();
		
		Calendar calendar1 = createAndRetrieveCalendar("calendar1", profile.getMember());
		Calendar calendar2 = createAndRetrieveCalendar("calendar2", profile.getMember());
		Calendar calendar3 = createAndRetrieveCalendar("calendar3", profile.getMember());
		
		String id = calendar1.getId()+","+calendar2.getId()+","+calendar3.getId();
		List<Calendar> calendars = callWebService(TaskWebService.class, "getBulkCalendars", new GenericType<List<Calendar>>(){}, buildPathParms("{ids}", id));
		Assert.assertTrue(calendars.contains(calendar1));
		Assert.assertTrue(calendars.contains(calendar2));
		Assert.assertTrue(calendars.contains(calendar3));
		
	}

	
	@Test
	public void createTaskWithGenericCategory()
	{
		//create default profile
		Profile profile = createAndRetrieveProfile();
		
		//test1, add default category
		Set<Category> categories = new HashSet<Category>();
		categories.add(categoryDefaultPersonal);
		
		//build task
		Task task = RMTestModelBuilder.buildSampleTask(true, categories, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		
		//create task
		Task classUnderTest = createAndRetrieveTask(task);
		
		//asserts
		Assert.assertTrue(classUnderTest.getCategories().contains(categoryDefaultPersonal));

		//test 2, user created plus default
		//build category
		Category category = createAndRetrieveCategory("User Created Category", profile.getMember());

		//create list of categories
		categories = new HashSet<Category>();
		categories.add(category);
		categories.add(categoryDefaultWork);
		
		//build task
		task = RMTestModelBuilder.buildSampleTask(true, categories, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		
		//create task
		classUnderTest = createAndRetrieveTask(task);
		
		//sasert
		Assert.assertTrue(classUnderTest.getCategories().contains(category));
		Assert.assertTrue(classUnderTest.getCategories().contains(categoryDefaultWork));
		
		
	}
	

	
	@Test
	public void getCalendarsForMember()
	{	
		//create a profile
		Profile profile = createAndRetrieveProfile();
		
		//create a calendar
		Calendar calendar = createAndRetrieveCalendar("sample", profile.getMember()); 

		//retrieve calendars
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile.getId());
		List<Calendar> underTest = callWebService(TaskWebService.class, "getCalendarsForMember", new GenericType<List<Calendar>>() {}, pathParms, null);
		
		//assert
		assertTrue(underTest.contains(calendar));
		assertTrue(underTest.contains(calendarDefaultWork));
		
		//test 2
		//create another profile
		Profile profile2 = createAndRetrieveProfile("profile2@homefellas.com");
		
		//create another calendar
		Calendar calendar2 = createAndRetrieveCalendar("another sample", profile2.getMember());
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile2.getId());
		underTest = callWebService(TaskWebService.class, "getCalendarsForMember", new GenericType<List<Calendar>>() {}, pathParms, null);
		
		//assert
		assertTrue(underTest.contains(calendar2));
		assertTrue(underTest.contains(calendarDefaultWork));
		Assert.assertFalse(underTest.contains(calendar));
	}
	
	@Test
	public void getCategoriesForMember()
	{	
		//create profile
		Profile profile = createAndRetrieveProfile();
		
		//create category
		Category category = createAndRetrieveCategory("new category", profile.getMember());
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile.getId());
		List<Category> underTest = callWebService(TaskWebService.class, "getCategoriesForMember", new GenericType<List<Category>>() {}, pathParms, null);
		
		assertTrue(underTest.contains(category));
		assertTrue(underTest.contains(categoryDefaultPersonal));
		assertTrue(underTest.contains(categoryDefaultWork));
		
		Profile profile2 = createAndRetrieveProfile("profile2@homefellas.com");
		
		//create another calendar
		Category category2 = createAndRetrieveCategory("another sample", profile2.getMember());
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile2.getId());
		underTest = callWebService(TaskWebService.class, "getCategoriesForMember", new GenericType<List<Category>>() {}, pathParms, null);
		
		assertTrue(underTest.contains(category2));
		assertTrue(underTest.contains(categoryDefaultPersonal));
		assertTrue(underTest.contains(categoryDefaultWork));
		Assert.assertFalse(underTest.contains(category));
	}
	
	@Test
	public void createCalendar()
	{
		Profile profile = createAndRetrieveProfile();
		
		Calendar createdCalendar = createAndRetrieveCalendar("Some calendar", profile.getMember());
		
		
		Calendar calendar = new Calendar();
		ClientResponse clientResponse = callWebService(TaskWebService.class, "createCalendar", ClientResponse.class, calendar);
		
		assertValidationErrors(clientResponse, new ArrayList<IValidationCode>(Arrays.asList(ValidationCodeEnum.MEMBER_ID_IS_NULL, ValidationCodeEnum.CALENDAR_TITLE_IS_NULL)));
		
		Assert.assertTrue(createdCalendar.isPublicList());
		
		createdCalendar.setPublicList(false);
		
		createdCalendar = callSecuredWebService(TaskWebService.class, "updateCalendar", Calendar.class, createdCalendar, profile.getEmail());
		
		Assert.assertFalse(createdCalendar.isPublicList());
		
	}
	
	
	
	
	@Test
	public void getTasksForMember()
	{
		Profile profile = createAndRetrieveProfile();
		
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task = createAndRetrieveTask(task);
		
		assertTrue(task.isPrimaryKeySet());
		
		List<Task> tasks = retrieveTaskes(profile);
		Assert.assertEquals(task, tasks.get(0));
		
//		Task[] tasks = retrieveTaskes(profile.getMember());		
//		Assert.assertEquals(task, tasks[0]);
	}
	
	@Test
	public void clearCompletedForMember()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task.setProgress(ProgressEnum.DONE.ordinal());
		task = createAndRetrieveTask(task);
		
		
//		Response response = putToWebservice("/clearCompleted", Response.class, profile.getMember());
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile.getId());
		ClientResponse response = callWebService(TaskWebService.class, "clearCompletedForMember", ClientResponse.class, pathParms, null);
		assertOKStatus(response);
		
		List<Task> tasks = retrieveTaskes(profile);
		Assert.assertTrue(!tasks.get(0).isShow());
	}
	
	@Test
	public void showCompletedForMember()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task.setProgress(ProgressEnum.DONE.ordinal());
		task = createAndRetrieveTask(task);
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile.getId());
		ClientResponse response = callWebService(TaskWebService.class, "clearCompletedForMember", ClientResponse.class, pathParms, null);
		assertOKStatus(response);
		
		List<Task> tasks = retrieveTaskes(profile);
		Assert.assertTrue(!tasks.get(0).isShow());
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberid}", profile.getId());
		response = callWebService(TaskWebService.class, "showCompletedForMember", ClientResponse.class, pathParms, null);
		assertOKStatus(response);
		
		tasks = retrieveTaskes(profile);
		Assert.assertTrue(tasks.get(0).isShow());
	}
	
	@Test
	public void updateTask()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task = createAndRetrieveTask(task);
		
		task.setPriority(PriorityEnum.LOW.ordinal());
		task.setProgress(ProgressEnum.ALMOST_THERE.ordinal());
		
		task.setModifiedDateZone(new DateTime());
//		task = callWebService(TaskWebService.class, "updateTask", Task.class, task);
		task = updateTask(task);
		Assert.assertEquals(PriorityEnum.LOW.ordinal(), task.getPriority());
		Assert.assertEquals(ProgressEnum.ALMOST_THERE.ordinal(), task.getProgress());
	}
	
//	@Test
//	public void completeTask()
//	{
//		Profile profile = createAndRetrieveProfile();
//		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
//		task = createAndRetrieveTask(task);
//		
//		ClientResponse response = completeTask(task);
//		assertOKStatus(response);
//		
//		List<Task> tasks = retrieveTaskes(profile);
//		Assert.assertEquals(tasks.get(0).getProgress(), ProgressEnum.DONE.ordinal());
//	}
	
	@Test
	public void getTaskById()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task = createAndRetrieveTask(task);
		
//		ClientResponse classUnderTest = getToWebService("/get/id/"+task.getId(), ClientResponse.class);
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task.getId());
//		ClientResponse classUnderTest = getToWebService(TaskWebService.class, "getTaskById", pathParms, ClientResponse.class);
		ClientResponse classUnderTest = callSecuredWebService(TaskWebService.class, "getTaskById", ClientResponse.class, pathParms, profile.getMember().getEmail());
		Assert.assertEquals(task, classUnderTest.getEntity(Task.class));
	}
	
	
	
	@Test
	public void cancelTask()
	{
		Profile profile = createAndRetrieveProfile();
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime(),null);
		task = createAndRetrieveTask(task);
		
		TaskWebService.setMemberEmail(profile.getMember().getEmail());
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", task.getId());
		ClientResponse response = callWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms, null);
		assertOKStatus(response);
		TaskWebService.setMemberEmail(null);
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{taskId}", task.getId());
		task = callSecuredWebService(TaskWebService.class, "getTaskById", Task.class, pathParms, task.getTaskCreator().getMember().getEmail());
//		task = getToWebService(TaskWebService.class, "getTaskById", pathParms, Task.class);
//		task = getToWebService("/get/id/"+task.getId(), Task.class);
		Assert.assertEquals(ProgressEnum.DELETE.ordinal(), task.getProgress());
		Assert.assertEquals(false, task.isShow());
	}

	
}
