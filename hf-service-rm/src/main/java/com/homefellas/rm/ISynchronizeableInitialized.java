package com.homefellas.rm;

import com.homefellas.rm.calendar.Calendar;
import com.homefellas.rm.notification.Device;
import com.homefellas.rm.task.TimelessTaskStat;
import com.homefellas.rm.user.GroupContact;
import com.homefellas.rm.user.PersonalPointScore;

/**
 * This interface defines the models that do not have any bearing on time.  They are synchronized once during initialize and not 
 * again.
 * @author prc9041
 *
 */
public interface ISynchronizeableInitialized extends ISynchronizeable
{

	public enum SynchronizeableInitializedObject 
	{ 
		Calendar(Calendar.class),
		Device(Device.class),
		groupcontact(GroupContact.class, false),
		pps(PersonalPointScore.class, false),
		tts(TimelessTaskStat.class);
//		Category(Category.class); 
		
		private Class<? extends ISynchronizeableInitialized> clazz;
		
		//if you set this value to false, the sync download will not retrieve this object...and it will need to be manually retrieved
		private boolean retreiveSynchronized=true;
		
		private SynchronizeableInitializedObject(Class<? extends ISynchronizeableInitialized> clazz)
		{
			this.clazz = clazz;
		}
		
		private SynchronizeableInitializedObject(Class<? extends ISynchronizeableInitialized> clazz, boolean isSync)
		{
			this.clazz = clazz;
			this.retreiveSynchronized = isSync;
		}
		
		public String getClassName()
		{
			return clazz.getName();
		}
		
		public String getClassSimpleName()
		{
			return clazz.getSimpleName();
		}
		
		public ISynchronizeableInitialized getClassInstance()
		{
			try
			{
				return (ISynchronizeableInitialized)Class.forName(getClassName()).newInstance();
			}
			catch (ClassNotFoundException classNotFoundException)
			{
				classNotFoundException.printStackTrace();
				return null;
			}
			catch (InstantiationException e)
			{
				e.printStackTrace();
				return null;
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
				return null;
			}
		
		}

		public boolean isRetreiveSynchronized()
		{
			return retreiveSynchronized;
		}
		
		
	}
}
