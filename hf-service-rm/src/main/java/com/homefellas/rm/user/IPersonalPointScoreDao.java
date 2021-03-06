package com.homefellas.rm.user;

import java.sql.Date;
import java.util.List;

import org.hibernate.Query;

public interface IPersonalPointScoreDao
{

	public PersonalPointScore getPersonalPointScoreByDate(String email, Date date);
	public List<PersonalPointScore> getPersonalPointScoreByDateRange(String email, Date start, Date end);
	
	public PersonalPointScore createPersonalPointScore(PersonalPointScore personalPointScore);
	public PersonalPointScore updatePersonalPointScore(PersonalPointScore personalPointScore);
	public PersonalPointScore getPersonalPointScoreById(String id);
}
