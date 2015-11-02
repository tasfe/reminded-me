package com.homefellas.ws.model;

import com.homefellas.rm.calendar.CalendarStoreUserDefinedCategory;
import com.homefellas.user.Member;

public class CalendarStoreUserDefinedCategoryUI extends AbstractUI
{

	private String categoryName; 
	
	private CalendarStoreSubCategoryUI calendarStoreSubCategory;
	private Member member;
	
	CalendarStoreUserDefinedCategoryUI() {
		
	}
	
	public CalendarStoreUserDefinedCategoryUI(CalendarStoreUserDefinedCategory calendarStoreUserDefinedCategory)
	{
		super(calendarStoreUserDefinedCategory.getId(), null, calendarStoreUserDefinedCategory.getCreatedDate(), calendarStoreUserDefinedCategory.getModifiedDate(), calendarStoreUserDefinedCategory.getCreatedDateZone(), calendarStoreUserDefinedCategory.getModifiedDateZone(), calendarStoreUserDefinedCategory.getClientUpdateTimeStamp());
		
		this.categoryName = calendarStoreUserDefinedCategory.getCategoryName();
		
		this.member = new Member(calendarStoreUserDefinedCategory.getMember().getId());
		this.calendarStoreSubCategory = new CalendarStoreSubCategoryUI(calendarStoreUserDefinedCategory.getCalendarStoreSubCategory());
	}

	public String getCategoryName()
	{
		return categoryName;
	}

	public CalendarStoreSubCategoryUI getCalendarStoreSubCategory()
	{
		return calendarStoreSubCategory;
	}

	public Member getMember()
	{
		return member;
	}

	void setCategoryName(String categoryName)
	{
		this.categoryName = categoryName;
	}

	void setCalendarStoreSubCategory(
			CalendarStoreSubCategoryUI calendarStoreSubCategory)
	{
		this.calendarStoreSubCategory = calendarStoreSubCategory;
	}

	void setMember(Member member)
	{
		this.member = member;
	}
	
	
}
