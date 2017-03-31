package com.baytouch.helpdesk.ejb;

import java.util.Date;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;

import com.baytouch.helpdesk.dao.SearchDao;

@Stateless
public class BuildIndex {
	
	// @Inject	TestClass testClass;

	@Inject
	SearchDao sdao;
		
//	@Schedule(minute = "*/0", hour = "*/12", persistent = false)
//	@Schedule(minute = "*/720", hour = "*", persistent = false)
	@Schedule(hour = "*/6", persistent = false)
    public void scheduledIndexBuild() {
    	System.out.println("*** Building Index at: " +  new Date().toString() + " ***");
    	sdao.loadIndex();
    	// String tMsgs = testClass.checkSupportCalls();
    	System.out.println("*** Finished building Index ***" ) ; 
    }  
}
