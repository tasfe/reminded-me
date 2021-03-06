package com.homefellas.rm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Test;

import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.share.Invite;
import com.homefellas.rm.share.SentShare;
import com.homefellas.rm.share.Share;
import com.homefellas.rm.share.ShareWebService;
import com.homefellas.rm.task.AbstractTask.PriorityEnum;
import com.homefellas.rm.task.AbstractTask.ProgressEnum;
import com.homefellas.rm.task.Category;
import com.homefellas.rm.task.Task;
import com.homefellas.rm.task.TaskWebService;
import com.homefellas.rm.task.TimelessTaskStat;
import com.homefellas.rm.user.Contact;
import com.homefellas.rm.user.GroupContact;
import com.homefellas.rm.user.PersonalPointScore;
import com.homefellas.user.Profile;
import com.homefellas.user.UserValidationCodeEnum;
import com.homefellas.ws.core.AbstractTestRMWebService;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;


public class RemindedMeWebServiceTest extends AbstractTestRMWebService
{
	@Test
	public void tts()
	{
		Profile owner = createAndRetrieveProfile();
		TimelessTaskStat timelessTaskStat = new TimelessTaskStat();
		timelessTaskStat.setMember(owner.getMember());
		timelessTaskStat.generateGUIDKey();
		
		String path = "tts";
		
		timelessTaskStat = createGeneric(TimelessTaskStat.class, timelessTaskStat, path);
		
		Map<String, String> pathParms = buildPathParms("{model}", path);
		pathParms.put("{ids}", timelessTaskStat.getId());	
		List<TimelessTaskStat> list = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<TimelessTaskStat>>(){}, pathParms, owner.getEmail());
		timelessTaskStat = list.get(0);
		
		timelessTaskStat.setTasksCount(100);
		timelessTaskStat = updateGeneric(TimelessTaskStat.class, timelessTaskStat, path, owner.getEmail());
		
		buildPathParms("{model}", path);
		pathParms.put("{ids}", timelessTaskStat.getId());	
		list = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<TimelessTaskStat>>(){}, pathParms, owner.getEmail());
		timelessTaskStat = list.get(0);
		Assert.assertEquals(100, timelessTaskStat.getTasksCount());
	}
	
	
	@Test
	public void pps()
	{
		Profile owner = createAndRetrieveProfile();
		
		PersonalPointScore personalPointScore1 = new PersonalPointScore();
		personalPointScore1.setMember(owner.getMember());
		personalPointScore1.generateGUIDKey();
		
		PersonalPointScore personalPointScore2 = new PersonalPointScore();
		personalPointScore2.setMember(owner.getMember());
		personalPointScore2.generateGUIDKey();
		personalPointScore2.setCreateDate(new Date(new DateTime().plusDays(1).getMillis()));
		
		String path = "pps";
		personalPointScore1 = createGeneric(PersonalPointScore.class, personalPointScore1, path);
		personalPointScore2 = createGeneric(PersonalPointScore.class, personalPointScore2, path);
		
//		List<PersonalPointScore> list = getGenericBulk(PersonalPointScore.class, personalPointScore1.getId()+","+personalPointScore2.getId(), path, owner.getEmail());
		Map<String, String> pathParms = buildPathParms("{model}", path);
		pathParms.put("{ids}", personalPointScore1.getId());	
		List<PersonalPointScore> list = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<PersonalPointScore>>(){}, pathParms, owner.getEmail());
		personalPointScore1 = list.get(0);
		
		personalPointScore1.setTodayEarnedPts(100);
		personalPointScore1 = updateGeneric(PersonalPointScore.class, personalPointScore1, path, owner.getEmail());
		
		buildPathParms("{model}", path);
		pathParms.put("{ids}", personalPointScore1.getId());	
		list = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<PersonalPointScore>>(){}, pathParms, owner.getEmail());
		personalPointScore1 = list.get(0);
		Assert.assertEquals(100, personalPointScore1.getTodayEarnedPts());
	}
	
	
	@Test
	public void genericSyncInitial()
	{
		Profile owner = createAndRetrieveProfile();
		Profile profile = createAndRetrieveProfile("contact@reminded.me");
		
		Contact contact = RMTestModelBuilder.contact(profile, owner.getMember());
		Set<Contact> contacts = new HashSet<Contact>();
		contacts.add(contact);
		GroupContact groupContact = RMTestModelBuilder.groupContact(owner.getMember(), null);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String json = "";
		try
		{
			json = objectMapper.writeValueAsString(groupContact);
		}
		catch (Exception exception)
		{
			Assert.fail(exception.getMessage());
		}
		groupContact = callWebService(RemindedMeWebService.class, "create", GroupContact.class, buildPathParms("{model}", "groupcontact"), json);
		
		try
		{
			json = objectMapper.writeValueAsString(contact);
		}
		catch (Exception exception)
		{
			Assert.fail(exception.getMessage());
		}
		contact = callWebService(RemindedMeWebService.class, "create", Contact.class, buildPathParms("{model}", "contact"), json);
		
		Map<String, String> pathParms = buildPathParms("{model}", "groupcontact");
		pathParms.put("{ids}", groupContact.getId());
		
		List<GroupContact> groupContacts = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<GroupContact>>(){}, pathParms, owner.getEmail());
		Assert.assertTrue(groupContacts.contains(groupContact));
		
		groupContact.setContacts(contacts);
		try
		{
			json = objectMapper.writeValueAsString(groupContact);
		}
		catch (Exception exception)
		{
			Assert.fail(exception.getMessage());
		}
		groupContact = callSecuredWebService(RemindedMeWebService.class, "update", GroupContact.class, buildPathParms("{model}", "groupcontact"), json, owner.getEmail());
		
		groupContacts = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<GroupContact>>(){}, pathParms, owner.getEmail());
		Assert.assertTrue(groupContacts.get(0).getContacts().contains(contact));
		
		pathParms = buildPathParms("{model}", "groupcontact");
		pathParms.put("{id}", groupContact.getId());
		callSecuredWebService(RemindedMeWebService.class, "delete", Boolean.class, pathParms, owner.getEmail());
		
		
		
	
		
	}
	
	@Test
	public void genericSync()
	{
		Profile owner = createAndRetrieveProfile();
		Profile profile = createAndRetrieveProfile("contact@reminded.me");
		
		Contact contact = RMTestModelBuilder.contact(profile, owner.getMember());
		contact.setContactCounter(1);
		ObjectMapper objectMapper = new ObjectMapper();
		String json = "";
		try
		{
			json = objectMapper.writeValueAsString(contact);
		}
		catch (Exception exception)
		{
			Assert.fail(exception.getMessage());
		}
		contact = callWebService(RemindedMeWebService.class, "create", Contact.class, buildPathParms("{model}", "contact"), json);
		
		Map<String, String> pathParms = buildPathParms("{model}", "contact");
		pathParms.put("{ids}", contact.getId());
		
		List<Contact> contacts = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<Contact>>(){}, pathParms, owner.getEmail());
		Assert.assertTrue(contacts.contains(contact));
		
		contact.incrementCounter();
		try
		{
			json = objectMapper.writeValueAsString(contact);
		}
		catch (Exception exception)
		{
			Assert.fail(exception.getMessage());
		}
		contact = callSecuredWebService(RemindedMeWebService.class, "update", Contact.class, buildPathParms("{model}", "contact"), json, owner.getEmail());
		
		contacts = callSecuredWebService(RemindedMeWebService.class, "getBulk", new GenericType<List<Contact>>(){}, pathParms, owner.getEmail());
		Assert.assertEquals(2, contacts.get(0).getContactCounter());
		
		pathParms = buildPathParms("{model}", "contact");
		pathParms.put("{id}", contact.getId());
		callSecuredWebService(RemindedMeWebService.class, "delete", Boolean.class, pathParms, owner.getEmail());
		
		Assert.assertTrue(sync(owner.getId(), 0).get(Contact.class.getName()).isEmpty());
		
	
		
	}
	
	@Test
	public void datelessTasks()
	{
		Profile owner = createAndRetrieveProfile();
		Task task1 = RMTestModelBuilder.task(owner);
		task1.setTimeLessTask(true);
		task1 =createAndRetrieveTask(task1);  
		
		List tasks = syncRnage(owner.getId(), 0, 0, 0, "deviceid").get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		
		Task task2 = createAndRetrieveTask(owner);
		
		tasks = syncRnage(owner.getId(), 0, 0, 0, "deviceid").get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		Assert.assertTrue(tasks.contains(task2.getId()));
	}
	
	@Test
	public void sentSharesSync()
	{
		Profile owner = createAndRetrieveProfile();
		Profile sharee1 = createAndRetrieveProfile("sharee@reminded.me");
		Profile sharee2 = createAndRetrieveProfile("sharee2@reminded.me");
		
		Task task1 = createAndRetrieveTask(owner);
		Task taskSharedWithOwner = createAndRetrieveTask(sharee1);
		
		//share task with sharee1 and 2
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(sharee1.getEmail(), sharee1.getName());
		emailAddresses.put(sharee2.getEmail(), sharee2.getName());
		createShares(task1, emailAddresses);
		
		//share task with owner
		createShares(taskSharedWithOwner, owner);
		
		Share share1 = retrieveShare(task1, sharee1);
		Share share2 = retrieveShare(task1, sharee2);
		Share share3 = retrieveShare(taskSharedWithOwner, owner);
		
		//map should contain both tasks, and both shares
		Map<String, List<Object>> syncedTasks = sync(owner.getId(), 0);
		
		List tasks =syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task1.getId()));
		Assert.assertTrue(tasks.contains(taskSharedWithOwner.getId()));		
		
		List shares = syncedTasks.get(Share.class.getName());
		Assert.assertTrue(shares.contains(share3.getId()));
		
		List sentShares = syncedTasks.get(SentShare.class.getName());
		Assert.assertTrue(sentShares.contains(share1.getId()));
		Assert.assertTrue(sentShares.contains(share2.getId()));
		
		
	}
	
	@Test
	public void syncInit()
	{
		Profile profile = createAndRetrieveProfile();
		
		Calendar calendar = createAndRetrieveCalendar("user defined calendar", profile.getMember());
		
		Task task = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null, null);
		task.addCalendar(calendar);
		
		task = createAndRetrieveTask(task);
		Calendar createdCalendar = task.getCalendars().iterator().next();
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "initialSynchronize",  new GenericType<Map<String, List<Object>>>() {}, buildPathParms("{memberId}", profile.getId()));
		List syncs = syncedTasks.get(Calendar.class.getName());
		String classUnderTestId = (String)syncs.get(0);
		Assert.assertEquals(createdCalendar.getId(), classUnderTestId);
	}
	@Test
	public void syncFilteredTimeStartEnd()
	{
		//create a sample profile
		Profile profile = createAndRetrieveProfile();
		
		//create some tasks that will be used for all tests...these should never be returned
		Task taskNeverReturnAlwaysBefore = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().minusYears(1),new DateTime().minusMonths(11));
		taskNeverReturnAlwaysBefore = createAndRetrieveTask(taskNeverReturnAlwaysBefore);
		Task taskNeverReturnAlwaysAfter = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().plusYears(1),new DateTime().plusMonths(13));
		taskNeverReturnAlwaysAfter = createAndRetrieveTask(taskNeverReturnAlwaysAfter);
		
		//set the bounds of the start and end
		DateTime start = new DateTime().minusMonths(1);
		DateTime end = new DateTime().plusMonths(1);
		
		//return tasks with start date in between time range, null end date
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().plusDays(1),null);
		task1 = createAndRetrieveTask(task1);
		
		//return tasks with end date in between time range, null start date
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null, new DateTime().plusDays(1));
		task2 = createAndRetrieveTask(task2);
		
		//return tasks with end date in between time range, start date way before time range
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().minusMonths(2), new DateTime().plusDays(1));
		task3 = createAndRetrieveTask(task3);
		
		//return tasks with start date in between time range, end date way after time range
		Task task4 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().plusDays(1),new DateTime().plusMonths(2));
		task4 = createAndRetrieveTask(task4);
		
		//return tasks with both start and end date in between time range
		Task task5 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().plusDays(1),new DateTime().plusDays(2));
		task5 = createAndRetrieveTask(task5);
		
		//task start and end time are outside of time range
		Task task6 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().minusMonths(2),new DateTime().plusMonths(2));
		task6 = createAndRetrieveTask(task6);
		
		//task cancled but in between range
		Task taskCanceled = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, new DateTime().plusDays(1),new DateTime().plusDays(2));
		taskCanceled = createAndRetrieveTask(taskCanceled);
		
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", taskCanceled.getId());
		
		//call ws to cancel
		ClientResponse response = callSecuredWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms, profile.getEmail());
		Assert.assertEquals(200, response.getStatus());
		
//		Task taskUnderTest = callWebService(TaskWebService.class, "getTaskById", Task.class, buildPathParms("{taskId}", task2.getId()));
		Task taskUnderTest = getBulkTasks(taskCanceled.getId(), profile.getMember().getEmail());
		Assert.assertEquals(ProgressEnum.DELETE.ordinal(), taskUnderTest.getProgress());
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//call sync
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{modifiedTime}", "0");
		pathParms.put("{firstModifiedTime}", String.valueOf(start.getMillis()));
		pathParms.put("{lastModifiedTime}", String.valueOf(end.getMillis()));
		pathParms.put("{deviceid}", "12345");
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjectsInBetween", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		//make sure that inital sync gets all the correct tasks
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task3.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task4.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task5.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task6.getId()));
		
		assertFalse(syncedTasks.get(Task.class.getName()).contains(taskCanceled.getId()));
		assertFalse(syncedTasks.get(Task.class.getName()).contains(taskNeverReturnAlwaysAfter.getId()));
		assertFalse(syncedTasks.get(Task.class.getName()).contains(taskNeverReturnAlwaysBefore.getId()));
		
		
		
	}
	
	
	@Test
	public void syncShareError()
	{
		//a bug was reported where a sync was done, and the shares were not being marked as being synced.
		//create a sample profile
		Profile owner = createAndRetrieveProfile();
		Profile sharer = createAndRetrieveProfile("sharer@homefellas.com");
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, owner, null, PriorityEnum.HIGH, null,null);
		task1 = createAndRetrieveTask(task1);
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, owner, null, PriorityEnum.HIGH, null,null);
		task2 = createAndRetrieveTask(task2);
		
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, false, null, sharer, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		Map<String, String> emailAddresses = new HashMap<String, String>();
		emailAddresses.put(sharer.getMember().getEmail(), sharer.getName());
		
		Invite invite1 = createShares(task1, emailAddresses);		
		Invite invite2 = createShares(task2, emailAddresses);
		
		//accept share1
		Map<String, String> pathParms = buildPathParms("{taskid}", task1.getId());
		pathParms.put("{memberid}", sharer.getId());
		Share share1Task1 = callWebService(ShareWebService.class, "acceptShare", Share.class, pathParms);
		
		//accept share2
		pathParms = buildPathParms("{taskid}", task2.getId());
		pathParms.put("{memberid}", sharer.getId());
		Share share2Task2 = callWebService(ShareWebService.class, "acceptShare", Share.class, pathParms);
		
		//initial sync
		long synctime = 0;
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", sharer.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(synctime));
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		//set the last sync time
		synctime = System.currentTimeMillis();
		
		String deviceID = (String)syncedTasks.get(ISynchronizeable.DEVICE_ID).get(0);
		Assert.assertNotNull(deviceID);
		
		//check to make sure all tasks are there
		List<Object> tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.contains(task3.getId()));

//		List<Object> shares = syncedTasks.get(Share.class.getName());
		Assert.assertTrue(tasks.contains(share1Task1.getTask().getId()));
		Assert.assertTrue(tasks.contains(share2Task2.getTask().getId()));
		
		//sleep
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", sharer.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(synctime));
		pathParms.put("{deviceid}", deviceID);
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		tasks = syncedTasks.get(Task.class.getName());
		Assert.assertTrue(tasks.isEmpty());
		
//		shares = syncedTasks.get(Share.class.getName());
//		Assert.assertTrue(shares.isEmpty());
	}
	
	@Test
	public void updateWithdeviceId()
	{
		Profile profile = createAndRetrieveProfile();
		Task task1 = createAndRetrieveTask(profile);
		Task task2 = createAndRetrieveTask(profile);
		Task task3 = createAndRetrieveTask(profile);
		
		//inital sync device 1
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task3.getId()));
			
		//store device1 id
		String device1 = (String)syncedTasks.get(ISynchronizeable.DEVICE_ID).get(0);
		String lastModified1 = String.valueOf(syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0));
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//sync on device 2
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task3.getId()));
				
		//store device 2
		String device2 = (String)syncedTasks.get(ISynchronizeable.DEVICE_ID).get(0);
		String lastModified2 = String.valueOf(syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0));
		
		//make sure devices dont match
		Assert.assertNotSame(device1, device2);
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//device 1 now updates task1
		task1.setProgress(ProgressEnum.ALMOST_THERE.ordinal());
		task1.setLastModifiedDeviceId(device1);
		
		task1 = callWebService(TaskWebService.class, "updateTask", Task.class, task1);
		
		//sync device 1 and make sure update isn't returned
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", lastModified1);
		pathParms.put("{deviceid}", device1);
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		Assert.assertTrue(syncedTasks.get(Task.class.getName()).size()==0);
		
		//sync device 2 and make sure the update is returned
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", lastModified2);
		pathParms.put("{deviceid}", device2);
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
	
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
	}
	
	@Test
	public void getModel()
	{
		Profile profile = createAndRetrieveProfile();
		
		assertTrue(profile.isPrimaryKeySet());
		
		Calendar calendar = RMTestModelBuilder.buildCalendar(true, "sample", profile.getMember());
		calendar = callWebService(TaskWebService.class, "createCalendar", Calendar.class, calendar);
		
		assertTrue(calendar.isPrimaryKeySet());
		
		Map<String, String> pathParms = new HashMap<String, String>(2);
		pathParms.put("{fqcn}", Calendar.class.getName());
		pathParms.put("{id}", calendar.getId());
		Calendar classUnderTest = callWebService(RemindedMeWebService.class, "getModel", Calendar.class, pathParms);
		Assert.assertEquals(calendar, classUnderTest);
		
		pathParms = new HashMap<String, String>(2);
		pathParms.put("{fqcn}", "com.homefellas.test.FakeClass");
		pathParms.put("{id}", calendar.getId());
		ClientResponse clientResponse = callWebService(RemindedMeWebService.class, "getModel", ClientResponse.class, pathParms);
		assertTrue(clientResponse.getStatus()==500);
		
//		System.out.println(clientResponse.getEntity(List.class));
//		List<IValidationCode> codes = clientResponse.getEntity(List.class);
//		assertTrue(codes.contains(ValidationCodeEnum.MODEL_DOES_NOT_EXIST.toString()));
		
//		Assert.assertEquals(ValidationCodeEnum.MODEL_DOES_NOT_EXIST.toString(), clientResponse.getEntity(IValidationCode.class).toString());
		
		pathParms = new HashMap<String, String>(2);
		pathParms.put("{fqcn}", Calendar.class.getName());
		pathParms.put("{id}", "12345");
		clientResponse = callWebService(RemindedMeWebService.class, "getModel", ClientResponse.class, pathParms);
		assertTrue(clientResponse.getStatus()==500);
//		Assert.assertEquals(ValidationCodeEnum.MODEL_DOES_NOT_EXIST.toString(), clientResponse.getEntity(IValidationCode.class));
	}

	
	
	@Test
	public void testFilteredSync()
	{
		long synctime = System.currentTimeMillis();
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//create a sample profile
		Profile profile = createAndRetrieveProfile();
		
		//create 4 sample tasks and make sure they are all synced
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task1 = createAndRetrieveTask(task1);
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task2 = createAndRetrieveTask(task2);
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
//		setLoggedInUserEmail(profile.getMember().getEmail());	
//		setEmail();
		
		//cancel task 2
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", task2.getId());
		
		TaskWebService.setMemberEmail(profile.getMember().getEmail());
//		ClientResponse response = setEmailAndCallWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms, profile.getMember().getEmail());
		ClientResponse response = callWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms);
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getEntity(Boolean.class));
		TaskWebService.setMemberEmail(null);
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//sync on 0 and make sure task 2 isn't returned
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//make sure that inital sync gets only task 1
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertFalse(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
		
		//create another task
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task3 = createAndRetrieveTask(task3);
		
		//sync with sync time
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(synctime));
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task3.getId()));
		assertFalse(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
		
		synctime = System.currentTimeMillis();
		
		try{Thread.sleep(10);}catch (InterruptedException e) {}
		
		//cancel task3
		TaskWebService.setMemberEmail(profile.getMember().getEmail());
		pathParms = new HashMap<String, String>();
		pathParms.put("{taskid}", task3.getId());
		response = callWebService(TaskWebService.class, "cancelTask", ClientResponse.class, pathParms);
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getEntity(Boolean.class));
		TaskWebService.setMemberEmail(null);
		
		//sync again but we should get the deleted task this time
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(synctime));
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		assertFalse(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task3.getId()));
		assertFalse(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
	}
	
	@Test
	public void testSync()
	{
		//test sync on a not found member id
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", "9871");
		pathParms.put("{lastModifiedTime}", "0");
		ClientResponse response = callWebService(RemindedMeWebService.class, "synchronizeObjects", ClientResponse.class, pathParms);
		
		assertValidationError(response, UserValidationCodeEnum.MEMBER_NOT_FOUND);
		
		//create a sample profile
		Profile profile = createAndRetrieveProfile();
		
		//create 4 sample tasks and make sure they are all synced
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task1 = createAndRetrieveTask(task1);
		assertTrue(task1.isPrimaryKeySet());
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.MEDIUM, null,null);
		task2 = createAndRetrieveTask(task2);
		assertTrue(task2.isPrimaryKeySet());
		
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.MEDIUM, null,null);
		task3 = createAndRetrieveTask(task3);
		assertTrue(task3.isPrimaryKeySet());
		
		Task task4 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.LOW, null,null);
		task4 = createAndRetrieveTask(task4);
		assertTrue(task4.isPrimaryKeySet());
		
		//simulate a new client and request all tasks.  No time passed
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		Map<String, List<Object>> syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
		
		
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task2.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task3.getId()));
		assertTrue(syncedTasks.get(Task.class.getName()).contains(task4.getId()));
		assertTrue(syncedTasks.get(ISynchronizeable.SYSTEM_TIME).get(0)!=null);
		
		//test 2 now lets create some categories
		Category category1 = RMTestModelBuilder.buildSampleCategory(true, true, profile.getMember());
//		category1 = postToWebservice(buildURI(getTaskServiceURI(), "/createCategory"), Category.class, category1);
		category1 = callWebService(TaskWebService.class, "createCategory", Category.class, category1);
		Assert.assertTrue(category1.isPrimaryKeySet());
		
		Category category2 = RMTestModelBuilder.buildSampleCategory(true, true, profile.getMember());
//		category2 = postToWebservice(buildURI(getTaskServiceURI(), "/createCategory"), Category.class, category2);
		category2 = callWebService(TaskWebService.class, "createCategory", Category.class, category2);
		Assert.assertTrue(category2.isPrimaryKeySet());
		
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		
		
		
		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);

		//test 3 make changes to some tasks and resync.  Let's make sure that our update fails
		//set the modied date in the past so that the server has a fresher copy
		task1.setModifiedDateZone(new DateTime().minusMillis(100000000));
		
//		ClientResponse clientResponse = postToWebservice(buildURI(getTaskServiceURI(), "/updateTask"), ClientResponse.class, task1);
		ClientResponse clientResponse = callWebService(TaskWebService.class, "updateTask", ClientResponse.class, task1);
		Assert.assertEquals(400, clientResponse.getStatus());
		
		assertValidationError(clientResponse, ValidationCodeEnum.SYNCHRONIZATION_REQUIRED);
//		Assert.assertEquals(ValidationCodeEnum.SYNCHRONIZATION_REQUIRED.toString(), clientResponse.getEntity(List.class).get(0));
		
		//test 5 now lets sync set the task modified date to far in the future
		long futureTimeNoUTC = System.currentTimeMillis()+5000000;
		task1.setModifiedDateZone(new DateTime(futureTimeNoUTC));
		
//		clientResponse = postToWebservice(buildURI(getTaskServiceURI(), "/updateTask"), ClientResponse.class, task1);
		clientResponse = callWebService(TaskWebService.class, "updateTask", ClientResponse.class, task1);
		Assert.assertEquals(200, clientResponse.getStatus());
	}
	
	@Test
	public void testTimeSync()
	{
		Profile profile = createAndRetrieveProfile();
		
		//create a task
		Task task1 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.HIGH, null,null);
		task1 = createAndRetrieveTask(task1);
		assertTrue(task1.isPrimaryKeySet());
		
		Task task2 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.MEDIUM, null,null);
		task2 = createAndRetrieveTask(task2);
		assertTrue(task2.isPrimaryKeySet());
		
		Task task3 = RMTestModelBuilder.buildSampleTask(true, null, null, true, null, profile, null, PriorityEnum.MEDIUM, null,null);
		task3 = createAndRetrieveTask(task3);
		assertTrue(task3.isPrimaryKeySet());
		
//		//3 tasks have been created.  now lets create some themes
//		Theme theme1 = RMTestModelBuilder.buildTheme(true, profile.getMember());
//		theme1 = executeWebServiceCall(TaskWebService.class, "createTheme", theme1).getEntity(Theme.class);
//		
//		Theme theme2 = RMTestModelBuilder.buildTheme(true, profile.getMember());
//		theme2 = executeWebServiceCall(TaskWebService.class, "createTheme", theme2).getEntity(Theme.class);
//		
		//simular internet connection lose
		long ts = System.currentTimeMillis();
		
		//update task1 via another connection.  First need to retrieve all the new stuff
		Map<String, String> pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", "0");
		Map<String, List<Object>> syncedTasks = executeWebServiceCall(RemindedMeWebService.class, "synchronizeObjects", pathParms).getEntity(new GenericType<Map<String, List<Object>>>() {});
		List<Object> tasks = syncedTasks.get(Task.class.getName());
		assertTrue(tasks.contains(task1.getId()));
		assertTrue(tasks.contains(task2.getId()));
		assertTrue(tasks.contains(task3.getId()));
//		List<Object> themes = syncedTasks.get(Theme.class.getName());
//		
//		assertTrue(themes.contains(theme1.getId()));
//		assertTrue(themes.contains(theme2.getId()));
//		
		//ok now lets update task1 and theme 1 over this new connection
//		task1.setTheme(theme1);
		task1 = executeWebServiceCall(TaskWebService.class, "updateTask", task1).getEntity(Task.class);
//		Assert.assertEquals(theme1.getId(), task1.getTheme().getId());
		
//		theme1.setBackgroundColor("#ffffff");
//		theme1.setForegroundColor("#000000");
//		theme1 = executeWebServiceCall(TaskWebService.class, "updateTheme", theme1).getEntity(Theme.class);
//		Assert.assertEquals("#ffffff", theme1.getBackgroundColor());
		
		//ok all updates have been processed.  Now internet has been restored so we need to resysnc based on the time that was lost
		pathParms = new HashMap<String, String>();
		pathParms.put("{memberId}", profile.getId());
		pathParms.put("{lastModifiedTime}", String.valueOf(ts));
		syncedTasks = executeWebServiceCall(RemindedMeWebService.class, "synchronizeObjects", pathParms).getEntity(new GenericType<Map<String, List<Object>>>() {});
		tasks = syncedTasks.get(Task.class.getName());
		assertTrue(tasks.contains(task1.getId()));
		assertFalse(tasks.contains(task2.getId()));
		assertFalse(tasks.contains(task3.getId()));
//		themes = syncedTasks.get(Theme.class.getName());
//		assertTrue(themes.contains(theme1.getId()));
//		assertFalse(themes.contains(theme2.getId()));
		
		
		//test 6 now lets re-sync based on now time
//		syncedTasks = getToWebservice("/sync/"+profile.getId()+"/"+System.currentTimeMillis(), new GenericType<Map<String, List<Object>>>() {});
//		pathParms = new HashMap<String, String>();
//		pathParms.put("{memberId}", profile.getId());
//		pathParms.put("{lastModifiedTime}", new DateTime(System.currentTimeMillis()).toString());		
//		syncedTasks = callWebService(RemindedMeWebService.class, "synchronizeObjects", new GenericType<Map<String, List<Object>>>() {}, pathParms);
//		
//		Assert.assertEquals(SynchronizeableObject.values().length, syncedTasks.size());
//		assertTrue(syncedTasks.get(Task.class.getName()).contains(task1.getId()));
	}

}
