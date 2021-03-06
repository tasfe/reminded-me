package com.homefellas.ws.model;

import com.homefellas.rm.calendar.Calendar;

public class CalendarUI extends AbstractUI
{
	
	private String calendarName;
	private String title;
	private String description;
	
	private boolean generic;
	private boolean publicList;		
	private boolean publishToAppStore;
	
	private double appleStorePrice;
	
	private int approvalStatusOrdinal;
	private int pushTypeOrdinal;

	private CalendarStoreUserDefinedCategoryUI calendarStoreUserDefinedCategory;
	
	CalendarUI()
	{
	}
	
	public CalendarUI(Calendar calendar)
	{
		super(calendar.getId(), calendar.getLastModifiedDeviceId(), calendar.getCreatedDate(), calendar.getModifiedDate(), calendar.getCreatedDateZone(), calendar.getModifiedDateZone(), calendar.getClientUpdateTimeStamp());
		
		this.title=calendar.getTitle();
		this.description=calendar.getDescription();
		this.calendarName=calendar.getCalendarName();
		
	
		this.generic=calendar.isGeneric();
		this.publicList=calendar.isPublicList();
		this.publishToAppStore=calendar.isPublishToAppStore();
		
		this.appleStorePrice=calendar.getAppleStorePrice();
	
		this.approvalStatusOrdinal=calendar.getApprovalStatusOrdinal();
		this.pushTypeOrdinal=calendar.getPushTypeOrdinal();
		
		if (calendar.getCalendarStoreUserDefinedCategory() !=null)
			this.calendarStoreUserDefinedCategory = new CalendarStoreUserDefinedCategoryUI(calendar.getCalendarStoreUserDefinedCategory());
	}

	public String getCalendarName()
	{
		return calendarName;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public boolean isGeneric()
	{
		return generic;
	}

	public boolean isPublicList()
	{
		return publicList;
	}

	public boolean isPublishToAppStore()
	{
		return publishToAppStore;
	}

	public double getAppleStorePrice()
	{
		return appleStorePrice;
	}

	public int getApprovalStatusOrdinal()
	{
		return approvalStatusOrdinal;
	}

	public int getPushTypeOrdinal()
	{
		return pushTypeOrdinal;
	}

	public CalendarStoreUserDefinedCategoryUI getCalendarStoreUserDefinedCategory()
	{
		return calendarStoreUserDefinedCategory;
	}

	void setCalendarName(String calendarName)
	{
		this.calendarName = calendarName;
	}

	void setTitle(String title)
	{
		this.title = title;
	}

	void setDescription(String description)
	{
		this.description = description;
	}

	void setGeneric(boolean generic)
	{
		this.generic = generic;
	}

	void setPublicList(boolean publicList)
	{
		this.publicList = publicList;
	}

	void setPublishToAppStore(boolean publishToAppStore)
	{
		this.publishToAppStore = publishToAppStore;
	}

	void setAppleStorePrice(double appleStorePrice)
	{
		this.appleStorePrice = appleStorePrice;
	}

	void setApprovalStatusOrdinal(int approvalStatusOrdinal)
	{
		this.approvalStatusOrdinal = approvalStatusOrdinal;
	}

	void setPushTypeOrdinal(int pushTypeOrdinal)
	{
		this.pushTypeOrdinal = pushTypeOrdinal;
	}

	void setCalendarStoreUserDefinedCategory(
			CalendarStoreUserDefinedCategoryUI calendarStoreUserDefinedCategory)
	{
		this.calendarStoreUserDefinedCategory = calendarStoreUserDefinedCategory;
	}
	
	
}
