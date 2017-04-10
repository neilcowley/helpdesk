package com.baytouch.helpdesk.ejb;

import java.util.Date;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import com.baytouch.helpdesk.dao.SupportCallDao;

@Stateless
public class ReadMail  {

	@Inject
	SupportCallDao scdao;
	
	/** Scheduled to run every 10 minutes within the hour
	 * https://developer.jboss.org/thread/254669 - Known problems with Wildfly 
	 * By default @Scheudule timers are persistent and they store information about which class and method to invoke
	 */
    @Schedule(minute = "*/10", hour = "*", persistent = false) 
    public void scheduledCheck() {
    	System.out.println("Checking for new Support requests at: " +  new Date().toString());
    	String tMsgs = scdao.readMail();
    	// String tMsgs = testClass.checkSupportCalls();
    	System.out.println("Finished checking messages. Process: " + tMsgs); 
    }
}
