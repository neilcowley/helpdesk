package com.baytouch.helpdesk.dao;

import java.io.Serializable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import org.jsoup.safety.Whitelist;

import com.baytouch.helpdesk.beans.FileControllerBean;
import com.baytouch.helpdesk.entities.Account;
import com.baytouch.helpdesk.entities.AdminUser;
import com.baytouch.helpdesk.entities.AttachInfo;
import com.baytouch.helpdesk.entities.Comments;
import com.baytouch.helpdesk.entities.Company;
import com.baytouch.helpdesk.entities.CompanyUser;
import com.baytouch.helpdesk.entities.DomainSkip;
import com.baytouch.helpdesk.entities.SupportCall;
import com.baytouch.helpdesk.entities.User;

/**
 * DAO - Data Access Object.
 */
@Named 
@RequestScoped
public class SupportCallDao implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String UNASSIGNED = "Unassigned" ; 
	private static final String SYSTEM_SUPPORT="BAYTOUCH-SUPPORT";
	
	private String url; 
	@PersistenceContext(unitName="helpdesk")
	private EntityManager emHelp ;
	@Inject 
	private UserDao udao; 
	@Inject 
	private FileControllerBean fcbean; 
	private static ResourceBundle bundle;
	private ArrayList<String> ignoreDomains ; 
	@Resource(lookup="java:module/ModuleName")
	private String moduleName;
	List<AdminUser> adminUsers = null;
	List<AdminUser> adminNotify = null;
	private String defaultLanguage = "" ; 
	private String language = "" ;
	private String curLanguage = "" ; 
		 	
	/**
	 * Reads the SupportCall database checking for new support call requests. 
	 * ignoreDomains=anvic.co.uk,iamava.co.uk,bounce.unitedinternet.com,1and1.co.uk,1and1.com
	 * @return
	 */
	@Transactional
	public String readMail(){
		
		Properties props = new Properties();
		Integer numMsgs = 0; 
	
	    try {
	    	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    	props.load(cl.getResourceAsStream("config.properties"));
			String host = props.getProperty("mail.smtp.host");
			String provider = props.getProperty("mail.imap.protocol"); 
			String username = props.getProperty("userName");
			String password = props.getProperty("password");
			defaultLanguage = props.getProperty("defaultLanguge");
					
			// Connect to the IMAP server
			Session session = Session.getInstance(props,
				new Authenticator() {
			  		protected PasswordAuthentication  getPasswordAuthentication() {
			  			return new PasswordAuthentication(username, password);
		        }
			});
				
			//System.out.println("1# - Connecting to mail box");
			Store store = session.getStore(provider);
			store.connect(host, null, null);
			
			//System.out.println("2# - Open the Inbox folder") ;
			Folder inbox = store.getFolder("Inbox");
			if (!inbox.exists()) {
			  return ""; 
			}
			inbox.open(Folder.READ_WRITE);
			
			//System.out.println("3# - Open the processed folder"); 
			Folder processed = store.getFolder("processed");
			if(!processed.exists()){
				processed.create(Folder.HOLDS_MESSAGES);   
			}			  
			processed.open(Folder.READ_WRITE);
		//	inbox.open(Folder.READ_ONLY);

			//System.out.println("4# - Find all the unread messages ...");
			FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
			Message messages[] = inbox.search(ft);
		//	Message[] messages = inbox.getMessages(); // TESTING ONLY
			numMsgs = messages.length;
		//	System.out.println("5# - Messages found: " + messages.length);
			
			if(messages.length > 0 ) {
				//System.out.println("*** PROCESSING Support call messages: " + messages.length + " ***");				
				// Get a list of domains to check...
				List<DomainSkip> domainsList = null; 
				TypedQuery<DomainSkip> tq = emHelp.createNamedQuery("findAllDomains",DomainSkip.class);
				domainsList = tq.getResultList() ; 
				
				//System.out.println("Add all ACTIVE domainsList values into a ArrayList so we can search them..."); 
				ignoreDomains = new ArrayList<String>(); 
				for(DomainSkip dom : domainsList){
					if(dom.isActive()){
						ignoreDomains.add(dom.getDomainName());
					}
				}	
				
				//System.out.println("Get a list of all the admin users who require notification of new support calls"); 
				adminNotify = new ArrayList<AdminUser>(); 
				adminNotify = udao.getAllAdminHelpdeskNotifications();
			
				//System.out.println("Stores the details of all calls for the adminGroup"); 
				String adminBody = "";

				// Prepare HashMap to store details of the support calls by assigned, 
				Map<String, List<SupportCall>> scCalls = new HashMap<String, List<SupportCall>>();
				// Get a list of all Admin users for interrogation later
				adminUsers = new ArrayList<AdminUser>();
				// What to do if no admin users are returned?
				adminUsers = udao.getAllAdminUsers();
				// Prepare a slot for unassigned calls
				scCalls.put(UNASSIGNED,  new ArrayList<SupportCall>() );
						
				//System.out.println("7# - iterate around all messages");
				for (int i = 0; i < messages.length; i++) {	
					
					this.url = getURL(props);
					
					//System.out.println("************ Create the support call and check to see if its been assigned *************"); 
					SupportCall sc = createSupportCall(messages[i] , i );
				
					// store all support calls 
					AdminUser userAssign = udao.getAdminUserById(sc.getAssignedToId());
					String emailAssign = UNASSIGNED;
					List<SupportCall> scList = null;
					
					if(userAssign != null){
						emailAssign = userAssign.getEmail();
						if(scCalls.containsKey(emailAssign)){
							scList = scCalls.get(emailAssign);
						}else{
							scList = new ArrayList<SupportCall>();
						}
					}else{
						scList = scCalls.get(emailAssign);
					}
					
					scList.add(sc);
					scCalls.put(emailAssign, scList);
					
					//System.out.println("******** Send out a response to the creator of the support call acknowledging it ***********");
					if(domainAndSubjectCheck(sc)){
						sendResponse(sc); 
					}
					
					// append to the admin body details of the support call...
					adminBody += getCallDetails(sc, true);
				}
				
				//System.out.println("Begin by sending an email of all new support calls to the helpdesk manager group"); 
				setLanguage(null);
				for(AdminUser admin : adminNotify){
					String tBody = "<p>" + getMessage("scd_email_newCallsNotify_body_part1") + ":</p>" + adminBody; 
					sendMail(admin.getEmail(), getMessage("scd_email_newCallsNotify_subject") , tBody);
				}
				
 
				//System.out.println("loop through the batched up assigned support calls emailing the people who have been assigned to each support call."); 
				for (Entry<String, List<SupportCall>> entry : scCalls.entrySet()){
				    String email = entry.getKey();
				    Account admin = udao.getAdminUserByEmail(email);
			    	setLanguage(admin); 
				    List<SupportCall> scList = entry.getValue();
				    String body = "<p>" + getMessage("scd_email_assigned_body_part1") + " :</p>"; 
				    if(email != UNASSIGNED){
				    	for(SupportCall sc : scList){
				    		body += getCallDetails(sc, true);
				    	}
				    	sendMail(email, getMessage("scd_email_assigned_subject") , body);
				    }		    
				}
							
				// Move the processed messages to the processed folder...
				//System.out.println("Moving=" + messages.length + " messages to 'processed' folder");
				inbox.copyMessages(messages, processed);
				Flags deleted = new Flags(Flags.Flag.DELETED);
				inbox.setFlags(messages, deleted, true);
				inbox.expunge(); // or folder.close(true);	

			}
			
			//System.out.println("8# - Close the connection"); 
			inbox.close(false);
			store.close();
	      
	    } catch (IOException | MessagingException ex) {
	    	ex.printStackTrace();
	    }
		return numMsgs.toString();	
	}
	
	/**
	 * Returns a message from the language file relating to the passed in key.
	 * Pass the Account object in to the message and check the object for the language preference  
	 * @param key
	 * @return
	 */
	private String getMessage(String key){
		if(bundle == null || !curLanguage.equals(language)){
			// System.out.println("*** getMessage getting bundle for language: "  + language  + " - key to get=" + key );
			String langVal = !language.equals("") && !language.equals("en") ? "_" + language : "";
			// System.out.println("Properties will be: " + langVal );
			bundle = ResourceBundle.getBundle("com.baytouch.helpdesk.language.select" + langVal);
			curLanguage=language;
		}
		return bundle.getString(key);
	}
		
	private SupportCall createSupportCall(Message msg, int count) throws MessagingException, IOException{
		
		// System.out.println("CREATING SUPPORT CALL FOR: " +  msg.getSubject() );
		SupportCall sc = new SupportCall();
		String from = InternetAddress.toString(msg.getFrom());
		Address[] froms = msg.getFrom();
		String senderEmail = ""; 
		String senderName = ""; 
				
		if(froms!=null){
			senderEmail = ((InternetAddress) froms[0]).getAddress();
			senderName = ((InternetAddress) froms[0]).getPersonal();
		}
		
		if(senderName==null){
			senderName = from; 
		}
		
		String status="new";
		sc.setSenderName(senderName);
		sc.setSenderEmail(senderEmail);
		sc.setDateCreated(msg.getSentDate());
		sc.setSubject( msg.getSubject());
		sc.setCallRefs(sc.getCallRefs(count));
		// System.out.println("Initialise a new set for any attachments"); 
		sc.setAttachments(new LinkedHashSet<AttachInfo>());
		sc.setCallDetails(getTextFromMessage(sc, msg));
		
		// System.out.println("Lookup the name of the person who sent the support call"); 
		Account user = lookupUser(senderEmail); 
		// System.out.println("If the person has an account assign the account to the support call"); 
		if(user != null){
			setCreatedBy(user,sc);
			// System.out.println("Lookup company user"); 
			CompanyUser compUser = lookupCompanyUser(user); 
			if(compUser !=null){
				sc.setCompanyUser(compUser);
				Company company = sc.getCompanyUser().getCompany();
				sc.setCompany(company);
				sc.setCallType(company.getTypeOfSupport());
			}
		}
		// System.out.println("Set the assiged user id if possible");
		sc.setAssignedToId(lookupAdminUser(from));
		if(sc.getAssignedToId()!=null && sc.getCompany() != null ){
			status="in_progress";
		}
		// System.out.println("Set status") ;
		sc.setStatus(status);
		// System.out.println("Create history") ; 
		createHistory(sc);
		// Stackoverflow - http://stackoverflow.com/questions/26463537/get-entity-jpa-id-after-merge
		// The result of the merge operation is not the same as with the persist operation - the entity passed to merge does not become managed. Rather, 
		// a managed copy of the entity is created and returned. This is why the original new entity will not get an id. return the result of merge instead
		// System.out.println("Merge and return");
		return emHelp.merge(sc);
	}
		
	private void createHistory(SupportCall sc){
		// System.out.println("Create a new comment in the history field ..."); 
		DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		// System.out.println("Get a calendar");
		Calendar cal = Calendar.getInstance();
		// System.out.println("Get the details of the message...");
		String detail = getMessage("scd_history_newCallGenerated_part1") + " " + dateFormat.format(cal.getTime()) + 
				getMessage("scd_history_newCallGenerated_part2") + " " + timeFormat.format(cal.getTime()); 	
		// System.out.println("Create a new comment") ;
		Comments comments = new Comments();
		// System.out.println("Assign details to comment"); 
		comments.setComments(detail);
		// System.out.println("Assign comment to support call") ; 
		comments.setSupportCall(sc);
		// System.out.println("Now add the comment ot the support call in a new hashSet");
		sc.setCallComments(new  LinkedHashSet<Comments>());
		// System.out.println("Add it now");
		sc.getCallComments().add(comments);
		// System.out.println("Done adding it...");
	}
	
	/**
	 * returns the URL for the current database 
	 * @param props
	 * @return
	 */
	public String getURL(Properties props){
		
		/* - Returns 127.0.0.1  on the live server so no good to use.
		String rtnVal = "";
		InetAddress ip;
		try {
			ip = InetAddress.getLocalHost();
			String curIp = ip.getHostAddress(); 
			String hostName = ""; 
			
			System.out.println("Current IP Address=" + curIp + " LIVE SERVER IP IS:" + props.getProperty("liveServer") );
			
			if(curIp.equals(props.getProperty("liveServer"))){
				hostName = curIp;
			}else{
				hostName =  props.getProperty("localHost");
			}

			rtnVal = props.getProperty("http") + "://" + hostName  + "/" + moduleName ;
		} catch (UnknownHostException e){
			e.printStackTrace();
		}
		*/
		return props.getProperty("http") + "://" + props.getProperty("liveServer") + "/" + moduleName ;
	}
	
	public Account getCreatedBy(SupportCall supportCall) {		
		return supportCall.getCreatedByUser() != null ? 
				supportCall.getCreatedByUser() : 
				udao.getAdminUserById(supportCall.getCreatedByAdminId()); 
	}
	
	public void setCreatedBy(Account createdBy, SupportCall supportCall){	
		if(createdBy instanceof User){
			supportCall.setCreatedByUser((User) createdBy); 
		}else if(createdBy instanceof AdminUser){
			supportCall.setCreatedByAdminId(createdBy.getId());   
		}
	}
	
	/**
	 * Send a response email to the author of the request acknowledging it
	 * @param sc
	 * @throws AddressException
	 * @throws MessagingException
	 * @throws IOException
	 */
	private void sendResponse(SupportCall sc) throws AddressException, MessagingException, IOException{
		setLanguage(sc.getCreatedByUser());
		String body = "<p>" +  getMessage("scd_email_response_body_part1") + ".<br />" + 
				getMessage("scd_email_response_body_part2") + ": " + sc.getCallRefs() + ".<br />" + 
				getMessage("scd_email_response_body_part3") + ".</p><br />" + 
				"<b>"+ getMessage("scd_email_response_body_part4") + ":</b><br />" + sc.getCallDetails() + "<br />";	
		sendMail(sc.getSenderEmail(), getMessage("scd_email_response_subject") + ": " + sc.getCallRefs(), body);	
	}
	
	/** 
	 * Check the current account for language settings, if found set the email language to their choice
	 * @param acc
	 */
	private void setLanguage(Account acc){
		language=""; 
		if (acc != null){
			language = acc.getLanguage();
		}
		language = !language.equals("") ? language : defaultLanguage ;	
	}
	
	/**
	 * Check to ensure the support call is not automated or the sender of the 
	 * support call is not from an ignored domain
	 * @param sc
	 * @return
	 */
	private boolean domainAndSubjectCheck(SupportCall sc){
		
		String[] subSplit = sc.getSubject().split("~");
		boolean rtnVal = true;	
		if(subSplit[0].equals(SYSTEM_SUPPORT)){
			rtnVal = false;
		}else{	
			String[] senderDomain = sc.getSenderEmail().split("@");
			if(ignoreDomains.contains(senderDomain[1].trim())){
				rtnVal = false;
			}
		}
		return rtnVal ; 
	}

	/**
	 * Returns HTML with details of the support call
	 * @param sc
	 * @return
	 */
	private String getCallDetails(SupportCall sc, boolean includeURL){
		
		String compName = UNASSIGNED; 
		String createdBy = sc.getSenderEmail(); 
		String assignedTo = UNASSIGNED ; 
		String rtnVal = "<div><b>" + getMessage("scd_email_body_callDetails_ref") + ": </b>" ;
		
		if(sc.getCompany()!=null){
			compName = sc.getCompany().getCompanyName(); 
		}
		if(getCreatedBy(sc) != null){
			 getCreatedBy(sc).getUserName(); 
		}
		if(sc.getAssignedToId()!=null){
			
			AdminUser at = udao.getAdminUserById(sc.getAssignedToId());
			if(at!=null){
				assignedTo = at.getUserName();
			}
		}
		
		if(includeURL){
			rtnVal += "<a href='" + url + "/supportCall.jsf?i=" + sc.getId() + "'>" +sc.getCallRefs()+"</a><br />"; 
		}else{ 
			rtnVal += sc.getCallRefs() + "<br />"; 
		}
				
		rtnVal += "<b>" + getMessage("scd_email_body_callDetails_title") + ":</b> " + sc.getSubject() + "<br />" + 
			"<b>" + getMessage("scd_email_body_callDetails_compName") + ":</b> " + compName + "<br />" +
			"<b>" + getMessage("scd_email_body_callDetails_createdBy") + ":</b> " + createdBy + "<br />" + 
			"<b>" + getMessage("scd_email_body_callDetails_assignedTo") + ":</b> " + assignedTo + "<br />" + 
			"<b>" + getMessage("scd_email_body_callDetails_callDetails") + ":</b><br />" + sc.getCallDetails() + "<br />" + 
			"</div><br />";
		
		return rtnVal ; 
	} 
	
	/**
	 * 
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws MessagingException
	 */
	private String getTextFromMessage(SupportCall sc, Message message) throws IOException, MessagingException {
		
	    String result = "";
	    if (message.isMimeType("text/plain")) {
	    	//System.out.println("*** This is a plain text message");
	        result = message.getContent().toString();
	    } else if (message.isMimeType("multipart/*")){
	    	//System.out.println("+++ This is a multipart message");
	        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
	        result = getTextFromMimeMultipart(sc, mimeMultipart);
	    }
	    return result;
	}

	private String getTextFromMimeMultipart(SupportCall sc, 
	        MimeMultipart mimeMultipart) throws IOException, MessagingException {

	    int count = mimeMultipart.getCount();
	    if (count == 0)
	        throw new MessagingException("Multipart with no body parts not supported.");
	    boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
	    if (multipartAlt)
	        // alternatives appear in an order of increasing 
	        // faithfulness to the original content. Customize as req'd.
	        return getTextFromBodyPart(sc, mimeMultipart.getBodyPart(count - 1));
	    String result = "";
	    for (int i = 0; i < count; i++) {
	        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
	        result += getTextFromBodyPart(sc, bodyPart);
	    }
	    return result;
	}

	private String getTextFromBodyPart(SupportCall sc, BodyPart bodyPart) throws IOException, MessagingException {
	
		String result = "";   
	    // Check for an attachment and save to disc if found
        if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
        	System.out.println("Inside email convertor. File in attachment=" + bodyPart.getInputStream().available() );
    		fcbean.uploadAttachment(sc, bodyPart.getInputStream().available(), bodyPart.getFileName(), bodyPart.getContentType(), bodyPart.getInputStream() ) ;
        }
        
	    if (bodyPart.isMimeType("text/plain")) {
	        result = (String) bodyPart.getContent();
	    } else if (bodyPart.isMimeType("text/html")) {
	        String html = (String) bodyPart.getContent();
	        // result = org.jsoup.Jsoup.clean(html, Whitelist.relaxed());	// parse(html).text();
	        // result = org.jsoup.Jsoup.parse(html.replaceAll("(?i)<br[^>]*>", "<pre>\n</pre>")).text();
	        // String prettyPrintedBodyFragment = org.jsoup.Jsoup.clean(html, "", Whitelist.none().addTags("div","br","p"), new org.jsoup.nodes.Document.OutputSettings().prettyPrint(true));
	        // result = org.jsoup.Jsoup.clean(prettyPrintedBodyFragment,  "", Whitelist.none() , new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));   
	        result = org.jsoup.Jsoup.clean(html, "", Whitelist.none(), new org.jsoup.nodes.Document.OutputSettings().prettyPrint(false));
	    } else if (bodyPart.getContent() instanceof MimeMultipart){
	    	//System.out.println("MimeMultipart");
	        result = getTextFromMimeMultipart(sc,(MimeMultipart)bodyPart.getContent());
	    }
	    return result;
	}
		
	/**
	 * Check to see if the current user is an admin user
	 * @param email
	 * @return
	 */
	private Integer lookupAdminUser(String email){
		// System.out.println("Looking up Admin user with email: " + email);
		Integer adminId = null; 
		// Loop through the admin users to see if the passed in email is an admin user, if so then assigned the support call to them by default
		for(AdminUser aUser : adminUsers){
			if(aUser.getEmail().equals(email)){
				adminId = aUser.getId();
				break; 
			}
		}
		return adminId;
	}
	
	/**
	 * Returns a User object based on the passed in email address
	 * @param from
	 * @return
	 */
	public Account lookupUser(String from){
		//	System.out.println("Looking up email: " + from);	
		return udao.getUserByEmail(from);			
		
		// Removed 20/10/16 - changed getUserByEmail to return single value instead of list. Changed this method to return Account instead of user
		// If only 1 account is found, return it ...
		//if(userList.size()==1){
		//	User user = (User) userList.get(0);
			// System.out.println("Found an account=" + user.getUserName());
		//	return user;
		//}else{
		//	return null;
		//}
	}
	
	/**
	 * Given a passed in User, use the user.id to locate a companyUser with the same id. 
	 * This will return one or more company id's. If more than one id is returned then
	 * return null and allow the assignee to select the company instead 
	 * @param user
	 * @return
	 */
	public CompanyUser lookupCompanyUser(Account user){	
		List<CompanyUser> compUserList = null; 
		CompanyUser compUser = null; 
		EntityGraph<CompanyUser> graph = emHelp.createEntityGraph(CompanyUser.class);
		graph.addAttributeNodes("company");
		graph.addAttributeNodes("user");
		TypedQuery<CompanyUser> tq = emHelp.createNamedQuery("findCompanyUser",CompanyUser.class);
		tq.setParameter("userId",user.getId());
		tq.setHint("javax.persistence.loadgraph", graph);
		compUserList = tq.getResultList();
		// If only 1 account is found, return it ... 
		if(compUserList != null){;
			if(compUserList.size()==1){
				compUser = compUserList.get(0);
			}
		}
		return compUser;		
	}
	
	public List<SupportCall> getCallsByCompanyId(int compId){
		TypedQuery<SupportCall> tq;		
		tq = emHelp.createNamedQuery("findCompanyCalls",SupportCall.class);
		tq.setParameter("companyId", compId);
		return tq.getResultList(); 
	}
	
	public List<SupportCall> getCallsByStatus(String status){
		TypedQuery<SupportCall> tq;		
		tq = emHelp.createNamedQuery("findCallsByStatus",SupportCall.class);
		tq.setParameter("status", status);
		return tq.getResultList(); 
	}
	
	public List<SupportCall> getCallsByAssignedByStatus(Integer adminId, String status){
		TypedQuery<SupportCall> tq;		
		tq = emHelp.createNamedQuery("findCallsByAssignedByStatus",SupportCall.class);
		tq.setParameter("adminId", adminId);
		tq.setParameter("status", status);
		return tq.getResultList();
	}
		
	/*
	public static void processMultipart(Multipart mp) throws MessagingException {
		for (int i = 0; i < mp.getCount(); i++) {
			System.out.println("Inside multipart...."); 
			processPart(mp.getBodyPart(i));
		}
	}

	public static void processPart(Part p) {
		try {
			String fileName = p.getFileName();
			String disposition = p.getDisposition();
			String contentType = p.getContentType();
			if (contentType.toLowerCase().startsWith("multipart/")) {
				System.out.println("#1 This is multipart...."); 
				processMultipart((Multipart) p.getContent());
			} else if (fileName == null && (Part.ATTACHMENT.equalsIgnoreCase(disposition)
					|| !contentType.equalsIgnoreCase("text/plain"))) {
				System.out.println("#2 This text/plain or attachment ..."); 
				// pick a random file name.
				fileName = File.createTempFile("attachment",".txt").getName();
			}
			
			if (fileName == null) { // likely inline
				System.out.println("#3 filename==null ..."); 
				p.writeTo(System.out);
			} else {
				System.out.println("#4 this has a file=" + fileName ); 
				File f = new File(fileName);
				// 	find a file that does not yet exist
				for (int i = 1; f.exists(); i++) {
					String newName = fileName + " " + i;
					f = new File(newName);
				}
				try (		
					OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
					InputStream in = new BufferedInputStream(p.getInputStream())) {
					// We can't just use p.writeTo() here because it doesn't
					// decode the attachment. Instead we copy the input stream
					// onto the output stream which does automatically decode
					// Base-64, quoted printable, and a variety of other formats.
					int b;
					while ((b = in.read()) != -1) out.write(b);
					out.flush();
				}
			}
			
		} catch (IOException| MessagingException ex){
			ex.printStackTrace();
		}
	}
	*/
	
	// JavaMail - 
	public void sendMail(String to, String subject, String body) throws AddressException, MessagingException, IOException{
		Properties properties = new Properties();
		//from http://stackoverflow.com/questions/15972175/reading-properties-file-in-jsf2-0-which-can-work-in-war-also
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		properties.load(cl.getResourceAsStream("config.properties"));
		String replyToAddress = properties.getProperty("replyTo");
		//Session session = Session.getInstance(properties);
		
		String username = properties.getProperty("userName");
		String password = properties.getProperty("password");
		
		// Connect to the IMAP server
		Session session = Session.getInstance(properties,
			new Authenticator() {
		  		protected PasswordAuthentication  getPasswordAuthentication() {
		  			return new PasswordAuthentication(username, password);
	        }
		});

		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(replyToAddress));
        message.setSubject(subject);
        message.setText("<div style='font-size:12px;font-family:Helvetica,Arial,sans-serif;'>" + body + "</div>" ,"utf-8","html");
    	message.addRecipient(Message.RecipientType.TO, new InternetAddress(to)); 
    	// Transport transport = session.getTransport("smtp");
        // transport.connect(host, from, pass);
        
        Transport.send(message);
	}		
}
