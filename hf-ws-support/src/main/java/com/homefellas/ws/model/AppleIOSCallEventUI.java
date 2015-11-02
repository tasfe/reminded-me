package com.homefellas.ws.model;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Index;

import com.homefellas.rm.notification.Device;
import com.homefellas.rm.task.AppleIOSCalEvent;
import com.homefellas.user.Member;
import com.homefellas.user.Profile;

public class AppleIOSCallEventUI extends AbstractUI {

private String lastModifiedDeviceId;
	
	private Member member;
	private String device;
	
	AppleIOSCallEventUI() {}
	
	public AppleIOSCallEventUI(AppleIOSCalEvent appleIOSCalEvent)
	{
		super(appleIOSCalEvent.getId(), appleIOSCalEvent.getLastModifiedDeviceId(), appleIOSCalEvent.getCreatedDate(), appleIOSCalEvent.getModifiedDate(), appleIOSCalEvent.getCreatedDateZone(), appleIOSCalEvent.getModifiedDateZone(), appleIOSCalEvent.getClientUpdateTimeStamp());
		
		this.member = new Member(appleIOSCalEvent.getMember().getId());
		this.device = appleIOSCalEvent.getDevice();
	}

	public String getLastModifiedDeviceId() {
		return lastModifiedDeviceId;
	}

	public void setLastModifiedDeviceId(String lastModifiedDeviceId) {
		this.lastModifiedDeviceId = lastModifiedDeviceId;
	}

	public Member getMember() {
		return member;
	}

	public void setMember(Member member) {
		this.member = member;
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	
	
	

}
