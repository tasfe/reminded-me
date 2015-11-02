package com.homefellas.rm.repeatsetup;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Proxy;
import org.joda.time.DateTime;

import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.task.AbstractTask;
import com.homefellas.rm.task.Task;

//@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@Entity
@Table(name="t_tasktemplates")
@Proxy(lazy=false)
@XmlRootElement
public class TaskTemplate extends AbstractTask {

	@Column(nullable=false)
	private String orginalTaskId;
	
	/**
	 * This is the set of calendars.  
	 * @see Calendar
	 */
	@ManyToMany(fetch=FetchType.EAGER,targetEntity=Calendar.class)
	@JoinTable(name="t_taskTemplateCalendars",joinColumns=@JoinColumn(name="taskId"),inverseJoinColumns=@JoinColumn(name="calendarId"))
	protected Set<Calendar> calendars;
	
	
	
	public TaskTemplate()
	{
		
	}
	
	public TaskTemplate(String id)
	{
		super(id);
	}
	
	public TaskTemplate(Task task)
	{
		this.id = generateUnquieId();
		this.orginalTaskId = task.getId();
		this.shareStatus = task.getShareStatus();
		this.hasBeenShared = task.isHasBeenShared();
		this.subTasksCount = task.getSubTasksCount();
		this.timeLessTask = task.isTimeLessTask();
		this.title = task.getTitle();
		this.aParent = task.isaParent();
		this.sortTimeStamp = new DateTime(task.getSortTimeStamp());
		this.sortTimeStampMilli = task.getSortTimeStampMilli();
		this.startTime = new DateTime(task.getStartTime());
		this.sortOrder = task.getSortOrder();
		this.startTimeMilli = task.getStartTimeMilli();
		this.startTimeZone = task.getStartTimeZone();
		this.endTimeZone = task.getEndTimeZone();
		this.endTime = task.getEndTime();
		this.endTimeMilli = task.getEndTimeMilli();
		this.show = task.isShow();
		this.publicTask = task.isPublicTask();
		this.taskLocation = task.getTaskLocation();
		this.priority = task.getPriority();
		this.progress = task.getProgress();
		this.modelName = task.getModelName();
		this.taskCreator = task.getTaskCreator();
		this.notes = task.getNotes();
		
		
		this.calendars = new HashSet<Calendar>();
		if (task.getCalendars()!=null)
			for (Calendar calendar : task.getCalendars())
				this.calendars.add(calendar);
		
		this.parentId = task.getParentId();
//		this.theme = task.getTheme();
		this.lastModifiedDeviceId = task.getLastModifiedDeviceId();
	}

	/**
	 * This is the set of calendars.  
	 * @see Calendar
	 * @return Set<Calednar>
	 */
	public Set<Calendar> getCalendars()
	{
		return calendars;
	}

	public void setCalendars(Set<Calendar> calendars)
	{
		this.calendars = calendars;
	}
	
	
	public void addCalendar(Calendar calendar)
	{
		if (calendars == null)
			calendars = new HashSet<Calendar>();
		
		calendars.add(calendar);
	}

	public String getOrginalTaskId()
	{
		return orginalTaskId;
	}

	public void setOrginalTaskId(String orginalTaskId)
	{
		this.orginalTaskId = orginalTaskId;
	}
	
	
}
