package com.homefellas.rm;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.joda.time.DateTime;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.homefellas.batch.NotificationTypeEnum;
import com.homefellas.batch.PushTypeEnum;
import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.note.Attachment;
import com.homefellas.rm.note.AttachmentMetaData;
import com.homefellas.rm.notification.Device;
import com.homefellas.rm.reminder.Alarm;
import com.homefellas.rm.reminder.RepeatOccurance;
import com.homefellas.rm.repeatsetup.RepeatSetup;
import com.homefellas.rm.repeatsetup.TaskTemplate;
import com.homefellas.rm.share.Invite;
import com.homefellas.rm.share.Share;
import com.homefellas.rm.task.AppleIOSCalEvent;
import com.homefellas.rm.task.Task;
import com.homefellas.rm.user.Contact;
import com.homefellas.rm.user.GroupContact;
import com.homefellas.rm.user.PersonalPointScore;
import com.homefellas.user.Member;
import com.homefellas.user.Profile;
import com.homefellas.user.UserTestModelBuilder;

public class RMTestModelBuilder extends UserTestModelBuilder {


	public static void main(Object[] args)
	{
		
	}
	
	public static AppleIOSCalEvent appleIOSCalEvent(Member member)
	{
		AppleIOSCalEvent appleIOSCalEvent = new AppleIOSCalEvent();
		appleIOSCalEvent.generateGUIDKey();
		appleIOSCalEvent.setMember(member);
		appleIOSCalEvent.setDevice(factory.getStrategy().getStringValue());
		
		return appleIOSCalEvent;
	}
	
	public static PersonalPointScore pps(Member member, DateTime createDate)
	{
		PersonalPointScore personalPointScore = new PersonalPointScore();
		personalPointScore.generateGUIDKey();
		personalPointScore.setMember(member);
		personalPointScore.setCreateDate(new java.sql.Date(createDate.getMillis()));
		
		return personalPointScore;
	}
	
	public static GroupContact groupContact(Member member, Set<Contact> contacts)
	{
		GroupContact groupContact = new GroupContact();
		groupContact.setGroupName(factory.getStrategy().getStringValue());
		groupContact.setMember(member);
		groupContact.setContacts(contacts);
		groupContact.generateGUIDKey();
		return groupContact;
	}
	public static Contact contact(Profile contactSharedWith, Member loggedInProfile)
	{
		Contact contact = new Contact();
		contact.generateGUIDKey();
		contact.setContact(contactSharedWith);
		contact.setContactCounter(1);
		contact.setContactOwner(loggedInProfile);
		return contact;
	}
	
	public static Device device(Profile profile)
	{
		Device device = new Device();
		device.generateGUIDKey();
		device.setProfile(profile);
//		device.setDeviceId(device.generateUnquieId());
		device.setPushTypeOrdinal(PushTypeEnum.APPLE.ordinal());
		
		return device;
		
	}
	
	public static RepeatSetup repeatSetup(String taskId, Member member)
	{
		RepeatSetup repeatSetup = new RepeatSetup();
		repeatSetup.generateGUIDKey();
		repeatSetup.setClonedTask(new TaskTemplate(taskId));
		repeatSetup.setMember(member);
		repeatSetup.setRepeatOccurance(RepeatOccurance.day.toString());
		return repeatSetup;
	}
	public static Alarm alarm(Task task, Member member, DateTime alarmTime)
	{
		Alarm alarm = new Alarm();
		alarm.setAlarmTime(alarmTime);
		alarm.setMessage(factory.getStrategy().getStringValue());
		alarm.generateGUIDKey();
		alarm.setNotificationType(NotificationTypeEnum.EMAIL.ordinal());
		alarm.setReminderIntervale(5);
		alarm.setReminderIntervaleType(RepeatOccurance.hour.ordinal());
		alarm.setSnoozeTimeInMin(5);
		alarm.setTask(task);
		alarm.setMember(member);
		return alarm;
	}
	
	public static Alarm alarm(Task task, DateTime alarmTime)
	{
		return alarm(task, task.getTaskCreator().getMember(), alarmTime);
	}
	
	public static Share buildShare(boolean newShare, Profile profile)
	{
		return buildShare(newShare, profile.getMember());
	}
	public static Share buildShare(boolean newShare, Member user)
	{
		Share share = new Share();
//		share.setInvite(invite);
//		share.setTask(task);
		share.setUser(user);
		
		
		newObjectCheck(share, newShare);
		return share;
	}
	
	public static Invite invite(Task task, Profile profile)
	{
		Map<String, String>emails = new HashMap<String, String>();
		emails.put(profile.getEmail(),profile.getName());
		
		return invite(task, emails);
	}
	
	public static Invite invite(Task task, Map<String, String>emails)
	{
		Invite invite = new Invite();
		invite.setMessage(factory.getStrategy().getStringValue());
		invite.setSubject(factory.getStrategy().getStringValue());
		invite.setShareId(task.getId());
		invite.setShareClassName(Task.class.getName());
		invite.setInviter(task.getTaskCreator().getMember());
		
		invite.setDirectLink("http://reminded.me");
		invite.setNotificationType(NotificationTypeEnum.EMAIL.ordinal());
		
		invite.setEmailAddresses(emails);
		
		
		return invite;
	}
	
	public static Invite invite(Calendar calendar, Profile profile)
	{
		Map<String, String>emails = new HashMap<String, String>();
		emails.put(profile.getEmail(),profile.getName());
		
		return invite(calendar, emails);
	}
	
	public static Invite invite(Calendar calendar, Map<String, String>emails)
	{
		Invite invite = new Invite();
		invite.setMessage(factory.getStrategy().getStringValue());
		invite.setSubject(factory.getStrategy().getStringValue());
		invite.setShareId(calendar.getId());
		invite.setShareClassName(Calendar.class.getName());
		invite.setInviter(calendar.getMember().getMember());
		invite.setDirectLink("http://reminded.me");
		invite.setNotificationType(NotificationTypeEnum.EMAIL.ordinal());
		
		invite.setEmailAddresses(emails);
		
		
		return invite;
	}

	
	
	public static Invite buildInvite(boolean newInvite, String shareId, String className)
	{
		Invite invite = new Invite();
		invite.setMessage(factory.getStrategy().getStringValue());
		invite.setSubject(factory.getStrategy().getStringValue());
		invite.setShareId(shareId);
		invite.setShareClassName(className);
//		invite.setShares(shares);

		invite.setDirectLink("http://reminded.me");
		invite.setNotificationType(NotificationTypeEnum.EMAIL.ordinal());
		
		int emailAddresses = new Random().nextInt(10)+1;
		Map<String, String>emails = new HashMap<String, String>();
		for (int i=0;i<emailAddresses;i++)
		{
			if (i%2==0)
				emails.put(factory.getStrategy().getStringValue(), factory.getStrategy().getStringValue());
			else
				emails.put(factory.getStrategy().getStringValue(), null);
		}
		invite.setEmailAddresses(emails);
		
		newObjectCheck(invite, newInvite);
		
		return invite;
	}
	

	public static AttachmentMetaData buildAttachmentMetaData(boolean newA, Attachment attachment)
	{
		AttachmentMetaData attachmentMetaData = new AttachmentMetaData();
		
		attachmentMetaData.setName(randomString());
		attachmentMetaData.setValue(randomString());
		attachmentMetaData.setAttachment(attachment);
		
		newObjectCheck(attachmentMetaData, newA);
		return attachmentMetaData;
	}
	
	
	public static Calendar calendar(Member member)
	{
		Profile profile = new Profile();
		if (member != null)
			profile.setId(member.getId());
		profile.setMember(member);
		Calendar calendar = new Calendar();
		calendar.setMember(profile);
		calendar.setCalendarName(factory.getStrategy().getStringValue());
		return calendar;
	}
	
	public static Calendar buildCalendar(boolean newCalendar, String calendarName, Member member)
	{
		Profile profile = new Profile();
		if (member != null)
			profile.setId(member.getId());
	
		profile.setMember(member);
		Calendar calendar = new Calendar();
		calendar.setMember(profile);
		calendar.setCalendarName(calendarName);
		
		newObjectCheck(calendar, newCalendar);
		return calendar;
	}

	
	public static Task task(Profile profile)
	{
		Task task = new Task();
		task.setTaskCreator(profile);
		task.generateGUIDKey();
		task.setTitle(factory.getStrategy().getStringOfLength(50));
		return task;
	}
	

	
	public static class DateTimeTypeConverter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {

		@Override
		  public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
		    return new JsonPrimitive(src.toString());
		  }

		  @Override
		  public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
		      throws JsonParseException {
		    try {
		      return new DateTime(json.getAsString());
		    } catch (IllegalArgumentException e) {
		      // May be it came in formatted as a java.util.Date, so try that
		      Date date = context.deserialize(json, Date.class);
		      return new DateTime(date);
		    }
		  }
  
	}
}
