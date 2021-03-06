package com.homefellas.rm.user;

import java.util.List;

import org.hibernate.Query;

import com.homefellas.dao.hibernate.core.HibernateCRUDDao;
import com.homefellas.user.Member;
import com.homefellas.user.Profile;

public class ContactDao extends HibernateCRUDDao implements IContactDao
{
	
	public Contact getContactByContactEmailAndOwnerEmail(String ownerOwnEmail, String contactEmail)
	{ 
		Query query = getQuery("from Contact c where c.contactOwner.email = ? and c.contact.member.email = ?");
		
		query.setString(0, ownerOwnEmail);
		query.setString(1, contactEmail);
		
		return (Contact)query.uniqueResult();
		
	}

	public List<Contact> getContactsForUser(String email, int maxResults)
	{
		Query query = getQuery("from Contact c where c.contactOwner.email = ? order by c.contactCounter DESC, c.modifiedDate DESC");
		
		query.setString(0, email);
		
		if (maxResults!=0)
			query.setMaxResults(maxResults);
		return query.list();
	}
	
	public List<GroupContact> getGroupContactsForUser(String email, int maxResults)
	{
		Query query = getQuery("from GroupContact gc where gc.member.email = ?");
		
		query.setString(0, email);
		
		if (maxResults!=0)
			query.setMaxResults(maxResults);
		
		return query.list();
	}	
	
	public Contact getContactByContactId(String id)
	{
		Query query = getQuery("from Contact c where c.contact.id = ?");
		query.setString(0, id);
				
		return (Contact)query.uniqueResult();
	}
	

	@Override
	public List<Contact> getContactsByIds(List<String> ids)
	{
		Query query = getQuery("from Contact c where c.contact.id in (:ids)");
		query.setParameterList("ids", ids);
				
		return query.list();
	}
	
	@Override
	public Contact createContact(Contact contact)
	{
		save(contact);
		return contact;
	}

	@Override
	public Contact updateContact(Contact contact)
	{
		updateObject(contact);
		return contact;
	}
	
}
