package com.baytouch.helpdesk.beans;

import com.baytouch.helpdesk.dao.SupportCallDao;
import com.baytouch.helpdesk.dao.UserDao;
import com.baytouch.helpdesk.entities.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.*;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
// import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.*;
import javax.mail.internet.*;
import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

@Named
@ViewScoped
public class SupportCallBean implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String STATUS_COMPLETE = "complete";
	private static final String STATUS_IN_PROGRESS = "in_progress";
	private static final String STATUS_ON_HOLD = "on_hold";
	private static final String STATUS_NEW = "new";
	private static final String STATUS_CANCELLED = "cancelled";
	private static final String STATUS_UPDATE = "update";
	private static final String STATUS_REASSIGN = "re_assign";
	private static final String STATUS_ASSIGN = "assign";
//	public static long UPLOAD_NOLOCALFILE = 101;

	@PersistenceContext(unitName = "helpdesk")
	private EntityManager em;
	@Inject
	private NavigatorBean nb;
	@Inject
	private UsersBean usersBean;
	@Inject
	private SupportCallDao scdao;
	@Inject
	private UserDao udao;
	@Inject
	private GlobalsBean gbean;
	@Inject
	FileControllerBean fcbean;

	private Account curUser;
	private AdminUser assignedTo;
	private SupportCall supportCall;
	private List<Company> companies;
	private List<CompanyUser> companyUsers;
	private boolean hasAssignedUser;
	private String i;
	private Boolean editMode = true;
	private boolean isNew = false;
	private String callUpdateType;
	private String updateComments;
	private Set<Account> emailSendList;
	private double timeTaken;
	private String subject = "";
	private String body = "";
	private UploadedFile file;
	private List<String> statusList = null;
	private List<String> statusListOpen = null;
	private static ResourceBundle bundle;
	private String curLocale = "";
	private String url = ""; 

	@PostConstruct
	public void initBean() {
		curUser = usersBean.getLoggedOnUser();
		setURL();
		HttpServletRequest hsr = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
				.getRequest();
		i = hsr.getParameter("i");
		statusList = new ArrayList<String>();
		statusListOpen = new ArrayList<String>();
		statusList.add(STATUS_NEW);
		statusListOpen.add(STATUS_NEW);
		statusList.add(STATUS_IN_PROGRESS);
		statusListOpen.add(STATUS_IN_PROGRESS);
		statusList.add(STATUS_ON_HOLD);
		statusListOpen.add(STATUS_ON_HOLD);
		statusList.add(STATUS_COMPLETE);
		statusList.add(STATUS_CANCELLED);
	}

	// GETTERS AND SETTERS ================================================
	@Transactional
	public SupportCall getSupportCall() {
		if (supportCall == null) {
			if (i == null || i.isEmpty()) {
				isNew = true;
				callUpdateType = STATUS_NEW;
				supportCall = new SupportCall();
				setCreatedBy(curUser);
				supportCall.setSenderName(curUser.getUserName());
				supportCall.setStatus(callUpdateType);
				// initialise All multi value entities
				supportCall.setCallComments(new LinkedHashSet<Comments>());
				supportCall.setAttachments(new LinkedHashSet<AttachInfo>());
				updateHistory("CREATED", "");
			} else {
				editMode = false;
				// Multiple entities preloaded
				//EntityGraph<SupportCall> graph = em.createEntityGraph(SupportCall.class);
				//graph.addAttributeNodes("callComments");
				//graph.addAttributeNodes("attachments");
				//Map<String, Object> hints = new HashMap<String, Object>();
				//hints.put("javax.persistence.loadgraph", graph);
				
				EntityGraph<?> graph = em.getEntityGraph("graph.supportCall.children");
				Map<String, Object> hints = new HashMap<String, Object>();
				hints.put("javax.persistence.fetchgraph", graph);
				supportCall = em.find(SupportCall.class, Integer.parseInt(i), hints);
			}
		}
		
		if (supportCall.getAssignedToId() == null) {
			hasAssignedUser = false;
		} else {
			hasAssignedUser = true;
		}
		
		return supportCall;
	}

	public void setSupportCall(SupportCall supportCall) {
		this.supportCall = supportCall;
	}

	public boolean isNew() {
		return isNew;
	}

	public Boolean getEditMode() {
		return editMode;
	}

	public void setEditMode(Boolean editMode) {
		this.editMode = editMode;
	}

	public List<Company> getCompanies() {
		if (companies == null) {
			findCompanies();
		}
		return companies;
	}

	public void setCompanies(List<Company> companies) {
		this.companies = companies;
	}

	public List<CompanyUser> getCompanyUsers() {
		if (companyUsers == null && supportCall.getCompany() != null) {
			getAssociatedCompanyUsers();
		}
		return companyUsers;
	}

	public void setCompanyUsers(List<CompanyUser> companyUsers) {
		this.companyUsers = companyUsers;
	}

	public String getCallUpdateType() {
		return callUpdateType;
	}

	public void setCallUpdateType(String callUpdateType) {
		this.callUpdateType = callUpdateType;
	}

	public String getUpdateComments() {
		return updateComments;
	}

	public void setUpdateComments(String updateComments) {
		this.updateComments = updateComments;
	}

	public double getTimeTaken() {
		return timeTaken;
	}

	public void setTimeTaken(double unitsUsed) {
		this.timeTaken = unitsUsed;
	}

	public UploadedFile getFile() {
		return file;
	}

	public void setFile(UploadedFile file) {
		this.file = file;
	}

	public AdminUser getAssignedTo() {
		if (assignedTo == null) {
			if (supportCall.getAssignedToId() != null) {
				assignedTo = udao.getAdminUserById(supportCall.getAssignedToId());
			}
		}
		return assignedTo;
	}

	public void setAssignedTo(AdminUser adminUser) {
		assignedTo = adminUser;
		supportCall.setAssignedToId(adminUser.getId());
	}

	/**
	 * Required to display the name of the assigned user in read mode
	 * 
	 * @return
	 */
	public String getAssignedToName() {
		String rtnVal = "";
		Account acc = getAssignedTo();
		if (acc != null) {
			rtnVal = acc.getUserName();
		}
		return rtnVal;
	}
	
	/**
	 * Returns the name of the person who created the support call, based on the
	 * call being created manually or automatically through the EJB
	 * 
	 * @return
	 */
	public String getSenderName() {
		String rtnVal = "";
		if (getCreatedBy() != null) {
			rtnVal = getCreatedBy().getUserName();
		} else {
			rtnVal = supportCall.getSenderName();
		}
		return rtnVal;
	}

	public Account getCreatedBy() {
		return scdao.getCreatedBy(supportCall);
	}

	public void setCreatedBy(Account createdBy) {
		scdao.setCreatedBy(createdBy, supportCall);
	}

	public List<String> getStatusList() {
		return statusList;
	}
	
	public List<String> getStatusListOpen() {
		return statusListOpen;
	}

	public boolean getEditRTField() {

		boolean rtnVal = false;

		if (!editMode) {
			rtnVal = false; // No editing at all in read mode
		} else if (curUser.getRole().equals(UsersBean.ADMIN)) {
			rtnVal = true; // Admin can always edit.
		} else if (supportCall.getStatus().equals(STATUS_COMPLETE)) {
			rtnVal = false; // creators cannot edit if the support call is
		} else if (curUser.getUserName().equals(getCreatedBy().getUserName())) {
			rtnVal = true;
		}

		return rtnVal;
	}

	/**
	 * Looks up a passed in parameter language key and returns the translation
	 * @param key
	 * @return
	 */
	private String getMessage(String key) {

		String locale = gbean.getLocale();
		String msg = "" ; 
		
		// System.out.println("Locale=" + locale + "= curLocale=" + curLocale );
		try{ 
			if (bundle == null || !curLocale.equals(locale)) {
				// System.out.println("Locale needs to be obtained again - Getting
				// the properties file");
				curLocale = locale;
				String langVal = !locale.equals("") && !locale.equals("en") ? "_" + locale : "";
				// System.out.println("Properties will be: " + langVal );
				bundle = ResourceBundle.getBundle("com.baytouch.helpdesk.language.select" + langVal);
			}
			msg = bundle.getString(key);
		}catch(Exception e){
			System.out.println("*** ERROR SupportCallBean.getMessage() for key: " + key + " ****");
			e.printStackTrace();
		}
		return msg ; 
	}

	/**
	 * Returns the downloadable file Streamed Content. If it is an image,
	 * the file is retrieved from the file system on the server first...
	 * 
	 * @param attachment
	 * @return
	 * @throws IOException
	 */
	public StreamedContent getDownloadFile(AttachInfo attachInfo) throws IOException {
		InputStream is = null;
		if (attachInfo.getTmpFileName() != null) {
			File f=FileControllerBean.getAttachmentLocal(attachInfo);
			if (f!=null){
				is = new FileInputStream(f);
			}
		}else{
			// System.out.println("This file is stored in the database...");
			AttachData attachData = attachInfo.getAttachData(); 
			is = new ByteArrayInputStream(attachData.getFileData());
		}
		
		return is!=null? new DefaultStreamedContent(is, attachInfo.getContentType(), attachInfo.getFileName()) : null;
	}

	/**
	 * Converts the attachment Set to a List for use with repeat controls.
	 * Attachments are stored in a set in order to initialise the set lazily
	 * along with the comments set when the document loads
	 * 
	 * @return
	 */
	public ArrayList<AttachInfo> getAttachmentList() {
		if (supportCall.getAttachments() == null) {
			return null;
		} else {
			return new ArrayList<AttachInfo>(supportCall.getAttachments());
		}
	}
	
	// END - GETTERS AND SETTERS
	// ================================================

	/**
	 * Check to see if current user has access to edit the document
	 * 
	 * @return
	 */
	public boolean isEditable() {
		String thisUser = usersBean.getLoggedOnUser().getUserName();
		String manager = usersBean.getManagerUserName();
		return !editMode && (usersBean.isAdminUser() || thisUser.equalsIgnoreCase(manager)
				|| thisUser.equalsIgnoreCase(getCreatedBy().getUserName())
				|| thisUser.equalsIgnoreCase(supportCall.getCompanyUser().getUser().getUserName()));
	}

	/**
	 * Sets the editMode from the Company 'Edit' button
	 * 
	 * @return
	 */
	public String edit() {
		setEditMode(true);
		return null;
	}

	/**
	 * Saves the current document if it isn't new; if true is passed in the
	 * document will close If the document IsNew then the document is saved in
	 * UpdateCall as an id is required before the email can be sent.
	 * 
	 * @param isSaveAndExit
	 * @return
	 */
	@Transactional
	public String SaveSupportCall(boolean isSaveAndExit) {

		String rtnVal = null;

		if (supportCall.getStatus().equals(STATUS_NEW) && supportCall.getAssignedToId() != null) {
			supportCall.setStatus(STATUS_IN_PROGRESS);
		}

		if (file != null && file.getSize() > 0) {
			try {
				fcbean.uploadAttachment(supportCall, file.getSize(), file.getFileName(), file.getContentType(), file.getInputstream() ) ;
			} catch (IOException e) {
				e.printStackTrace();
			}
			updateHistory(getMessage("scb_history_addedFile") + ": " + file.getFileName(), "");
			file = null;
		}

		if (isSaveAndExit) {
			if (!isNew) {
				em.merge(supportCall);
			}
			rtnVal = nb.getPageFrom() + "?faces-redirect=true";
		} else {
			if (!isNew) {
				supportCall = em.merge(supportCall);
			}
		}
		isNew = false;
		return rtnVal;
	}
	
	/** 
	 * handleFileUpload - called when the 'Upload' button is pressed for each file to upload.
	 * the onComplete event of the p:fileUpload control uses JavaScript to check if all files have 
	 * been uploaded. If they have it triggers a p:remoteCommand that calls a save on the document. 
	 * This is required in order to save the attachInfo objects to the back end.
	 * @param event
	 */
	@Transactional
    public void handleFileUpload(FileUploadEvent event) {
	
    	// setFile(event.getFile()); 
    	UploadedFile file = event.getFile();

    	if (file != null && file.getSize() > 0) {
			try {
				// System.out.println("UPLOADING=" + file.getInputstream() );
				fcbean.uploadAttachment(supportCall, file.getSize(), file.getFileName(), file.getContentType(), file.getInputstream() ) ;
			} catch (IOException e) {
				e.printStackTrace();
			}
			// System.out.println("Writing History....") ; 
			updateHistory(getMessage("scb_history_addedFile") + ": " + file.getFileName(), "");
			// file = null;
		}
    	
        // FacesMessage message = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        // FacesContext.getCurrentInstance().addMessage(null, message);
    }
		
	/** 
	 * Removes the selected attachment from the supportCall
	 * 
	 * @param attachment
	 * @return
	 */
	@Transactional
	public String removeAttachment(AttachInfo attachInfo, boolean isDeleteCall ) throws IOException {
		String fileName = attachInfo.getFileName();
		
		if (attachInfo.getTmpFileName()!= null) {
			// File is stored on the local file system, so remove the file first...
			File f=FileControllerBean.getAttachmentLocal(attachInfo);
			if(f!=null){
				f.delete();
			}
		}else{		
			AttachData attachData = attachInfo.getAttachData(); 
			em.remove(em.contains(attachData) ? attachData : em.merge(attachData));
		}
		
		supportCall.getAttachments().remove(attachInfo);
		em.remove(em.contains(attachInfo) ? attachInfo : em.merge(attachInfo));
		
		if(!isDeleteCall){
			updateHistory(getMessage("scb_history_removedFile") + ": " + fileName, "");
			supportCall = em.merge(supportCall);
		}
		return null;
	}

	/**
	 * Deletes the current support call permanently and removes any associated attachments if required
	 * @return
	 */
	@Transactional
	public String deleteCall() {
		
		try {
			// Start my looping around any attachments and removing those....
			ArrayList<AttachInfo> attList = getAttachmentList();
			// System.out.println("Deleting call: attachments=" + attList.size());
			for(AttachInfo ai : attList){
				removeAttachment(ai, true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		// Now that the attachments have been removed, delete the call.
		em.remove(em.contains(supportCall) ? supportCall : em.merge(supportCall));
		return nb.getPageFrom() + "?faces-redirect=true";
	}

	/**
	 * Finds the associated company relating to the assigned company contact
	 * based on the passed in userId
	 * 
	 * @param userId
	 */
	@Transactional
	private void findCompanies() {
		// http://stackoverflow.com/questions/4180115/jpa-query-to-get-results-based-on-value-of-foreign-key-defined-in-the-entity-cl
		// TypedQuery<CompanyUser> tq = em.createQuery("SELECT u from
		// CompanyUser u WHERE u.user.id=:userId", CompanyUser.class);
		// TypedQuery<Company> tq = em.createQuery("SELECT cu.company from
		// CompanyUser cu WHERE cu.user.id=:userId", Company.class);
		TypedQuery<Company> tq = em.createNamedQuery("findCompaniesByCompanyUser", Company.class);
		tq.setParameter("userId", curUser.getId());
		if (tq.getResultList().size() == 0 && curUser.getRole().equals(UsersBean.ADMIN)) {
			// No companies found so return all companies to select from
			tq = em.createNamedQuery("findAllCompanies", Company.class);
		}
		companies = tq.getResultList();
	}

	/**
	 * Given a passed in company, updates the list of company users for
	 * selection
	 * 
	 * @param comp
	 */
	public String getAssociatedCompanyUsers() {
		if (supportCall.getCompany() != null) {
			TypedQuery<CompanyUser> tq = em.createNamedQuery("findCompanyUserByCompanyId", CompanyUser.class);
			companyUsers = tq.setParameter("companyId", supportCall.getCompany().getId()).getResultList();
		} else {
			companyUsers = null;
		}
		return null;
	}

	/**
	 * Adds a comment to the history list of values
	 * 
	 * @param msg
	 */
	private void updateHistory(String str1, String str2) {
		String curUser = usersBean.getLoggedOnUser().getUserName();
		DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		String detail = curUser + ": " + str1 + " on " + dateFormat.format(cal.getTime()) + " at "
				+ timeFormat.format(cal.getTime());
		if (str2 != null && !str2.equals("")) {
			detail += "<br />================ " + getMessage("scb_history_comments") + " ================<br />" + str2;
		}
		Comments comments = new Comments();
		comments.setComments(detail);
		comments.setSupportCall(supportCall);
		supportCall.getCallComments().add(comments);
	}

	// DIALOG BOX METHODS ===================================

	/**
	 * This is called when a newly received support call is initial assigned to a user
	 */
	@Transactional
	public void AssignCall() {
		// TODO - hasAssignedUser should always be false at this stage.
		//String assignType = hasAssignedUser ? getMessage("scb_history_reassigned") : getMessage("scb_history_assigned");
		// Get person assigned to support call
		//AdminUser assignedTo = udao.getAdminUserById(supportCall.getAssignedToId());
		//String detail = assignType + " " + getMessage("scb_history_thisSupportCallTo") + " " + assignedTo.getUserName();
		//updateHistory(detail, "");
		setCallUpdateType(STATUS_ASSIGN); 
		hasAssignedUser = true;
	}

	@Transactional
	public void reAssignCall() {
		setCallUpdateType(STATUS_REASSIGN);
		UpdateCall(false);
	}

	/**
	 * Reactivates a support call that is 'On Hold'
	 */
	@Transactional
	public void ReactivateCall() {
		setCallUpdateType(STATUS_IN_PROGRESS);
		UpdateCall(false);
	}

	/**
	 * Called from the Dialog box which doesn't pass in a saveAndClose value due
	 * to the mutiple options available to the dialog. This can be resolved in
	 * the UpdateCall method based on the current CallUpdateType
	 * 
	 * @return
	 */
	@Transactional
	public String UpdateCall() {
		return UpdateCall(false);
	}

	/**
	 * Updates the support call with comments
	 * 
	 * @throws IOException
	 * @throws MessagingException
	 * @throws AddressException
	 */
	@Transactional
	public String UpdateCall(boolean saveAndClose) {
	
		if (callUpdateType == null) {
			callUpdateType = "";
		}

		String historyStatus = "";
		boolean updateHistory = true;
		boolean updateStatus = true;
		String curUser = usersBean.getLoggedOnUser().getUserName() ; 

		switch (callUpdateType) {

		case STATUS_NEW:
			updateHistory = false;
			Account cb = getCreatedBy() ; 
			String createdBy = cb != null ?  cb.getUserName() : supportCall.getSenderName() + " (" + supportCall.getSenderEmail() + ")"; 
			
			body = emailCallDetails() + "<br /><p>" + getMessage("scb_email_new_body_part1") + ": "
				+ createdBy + " " + getMessage("scb_email_new_body_part2") + ".<br /></p>"
				+ "<p><b>" + getMessage("scb_email_new_body_part3") + ":</b></p>" + "<p>"
				+ supportCall.getCallDetails() + "</p>";
			emailSendList = new LinkedHashSet<Account>();
			// If the person who created the support call is not the same person
			// who is assigned it then send an email
			if (hasAssignedUser) {
				if (!assignedTo.getUserName().equalsIgnoreCase(getCreatedBy().getUserName())) {
					emailSendList.add(assignedTo);
				}
				callUpdateType = STATUS_IN_PROGRESS; // Change Status to 'In Progress'
			}
			break;
		case STATUS_COMPLETE:
			// System.out.println("This call is completed");
			historyStatus = getMessage("scb_history_completed");
			saveAndClose = true;
			supportCall.setTimeTaken(timeTaken);
			supportCall.setDateCompleted(new Date());
			
			body = emailCallDetails() + "<br /><p>" + getMessage("scb_email_complete_body_part1") + ": "
				+ curUser + "<br />" + "<p><b>" + getMessage("scb_email_complete_body_part2")
				+ ":</b></p>" + "<p>" + getUpdateComments() + "</p>";
			// "<p>" + supportCall.getCallDetails() + "</p>";
			break;
		case STATUS_CANCELLED:
			// System.out.println("This call is cancelled");
			historyStatus = getMessage("scb_history_cancelled");
			saveAndClose = true;
			// String personAssinged = assignedTo!= null ? assignedTo.getUserName() : getMessage("scb_email_body_callDetails_unassigned") ; 
			
			body = emailCallDetails() + "<br /><p>" + getMessage("scb_email_cancelled_body_part1") + ": "
				+ curUser + "<br />" + "<p><b>" + getMessage("scb_email_cancelled_body_part2")
				+ ":</b></p>" + "<p>" + getUpdateComments() + "</p>";
			// "<p>" + supportCall.getCallDetails() + "</p>";
			break;
		case STATUS_ON_HOLD:
			historyStatus = getMessage("scb_history_onHold");
			saveAndClose = false;
			body = emailCallDetails() + "<p>" + getMessage("scb_email_onHold_body_part1") + ": "
				+ curUser + "<br />" + "<p><b>" + getMessage("scb_email_onHold_body_part2")
				+ ":</b></p>" + "<p>" + getUpdateComments() + "</p>";
			break;
		case STATUS_IN_PROGRESS:
			historyStatus = getMessage("scb_history_reactivated");
			saveAndClose = false;
			break;
		case STATUS_UPDATE:
			historyStatus = getMessage("scb_history_callUpdate");
			updateStatus = false;
			saveAndClose = true;
			body = emailCallDetails() + "<p>" + getMessage("scb_email_update_body_part1") + ": "
				+ curUser + "<br />" + "<p><b>" + 
				getMessage("scb_email_update_body_part2") + ":</b></p>" + "<p>" + 
				getUpdateComments() + "</p>";
			break;
		case STATUS_REASSIGN:
			historyStatus = getMessage("scb_history_reassigned") + " " + getMessage("scb_history_thisSupportCallTo")
				+ " : " + assignedTo.getUserName();
			saveAndClose = false;
			updateStatus = false;
			emailSendList = new LinkedHashSet<Account>();
			emailSendList.add(assignedTo);
			body = emailCallDetails() + "<p>" + getMessage("scb_email_reassign_body_part1") + " ("
				+ assignedTo.getUserName() + ")." + " " + getMessage("scb_email_reassign_body_part2")
				+ ".<br /><br />";
			break;
		case STATUS_ASSIGN:
			// TODO - This should only be called when anonymous support calls are received and assigned for the first time.
			historyStatus = getMessage("scb_history_assigned") + " " + getMessage("scb_history_thisSupportCallTo") + ": " 
				+ assignedTo.getUserName();
			emailSendList = new LinkedHashSet<Account>();
			emailSendList.add(assignedTo);
			body = emailCallDetails() + "<p>" + getMessage("scb_email_assign_body_part1") + " ("
				+ assignedTo.getUserName() + ")." + " " + getMessage("scb_email_assign_body_part2")
				+ ".<br /><br />";
			updateStatus = false;
			break;
		default:
			updateStatus = false;
			updateHistory = false;
		}

		if (updateStatus) {
			supportCall.setStatus(callUpdateType);
		}
		if (updateHistory) {
			updateHistory(historyStatus, getUpdateComments());
		}

		String tmpCallUpdateType = !callUpdateType.equals("") ? getMessage(callUpdateType) : "" ; 
		setCallUpdateType("");
		setUpdateComments("");

		// This is new so need to call a save first before
		if (isNew) {
			supportCall = em.merge(supportCall);
		}

		// if an email is required send it ...
		if (emailSendList != null && emailSendList.size() > 0) {
			subject = getMessage("scb_email_subject_part1") + " " + supportCall.getCompany().getCompanyName() + " - "
					+ tmpCallUpdateType + ". " + getMessage("scb_email_subject_part2") + ": " + supportCall.getCallRefs();
			body += getMessage("scb_email_body_start") + ": <a href='" + getRequestURL() + "'>"
					+ supportCall.getCallRefs() + "</a>";
			try {
				// Loop round the list of people to send to, if any names fail
				// the other emails are still sent...
				for (Account user : emailSendList) {
					if (!user.getEmail().equalsIgnoreCase(usersBean.getLoggedOnUser().getEmail())) {
						scdao.sendMail(user.getEmail(), subject, body);
					}
				}
				emailSendList.clear();
			} catch (AddressException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return SaveSupportCall(saveAndClose);
	}

	/**
	 * Output the email body header
	 * 
	 * @return
	 */
	private String emailCallDetails() {
			
		Account cb = getCreatedBy() ; 
		Company comp =  supportCall.getCompany(); 
		String createdBy = cb != null ?  cb.getUserName() : supportCall.getSenderName() + " (" + supportCall.getSenderEmail() + ")"; 
		String compName = comp != null ? comp.getCompanyName() : "No company assigned";
		
		return "<b>" + getMessage("scb_email_body_callDetails_ref") + ":</b> " + supportCall.getCallRefs() + "<br />"
			+ "<b>" + getMessage("scb_email_body_callDetails_title") + ":</b> " + supportCall.getSubject()
			+ "<br />" + "<b>" + getMessage("scb_email_body_callDetails_compName") + ":</b> "
			+ compName + "<br />" + "<b>"
			+ getMessage("scb_email_body_callDetails_createdBy") + ":</b> " + createdBy
			+ "<br /><br />";
	}
	
	private void setURL(){	
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Properties props = new Properties(); 
			props.load(cl.getResourceAsStream("config.properties"));
			this.url = scdao.getURL(props);	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getRequestURL(){
		
		/*
		Object request = FacesContext.getCurrentInstance().getExternalContext().getRequest();
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			StringBuffer requestURL = httpRequest.getRequestURL();
			return requestURL.toString() + "?i=" + supportCall.getId();
		}
		return "";
		*/
    	
		return url + "/supportCall.jsf?i=" + supportCall.getId(); 
	}

	/**
	 * Adds all the people involved with the support call to a HashSet to return
	 * a unique list loops over hashSet to convert to a list and return value.
	 * 
	 * @return
	 */
	public List<Account> AllSupportCallUsers() {

		List<Account> users = new ArrayList<Account>();

		if (supportCall.getAssignedToId() != null) {
			checkAccountExits(users, udao.getAdminUserById(supportCall.getAssignedToId()));
		}
		if (getCreatedBy() != null) {
			checkAccountExits(users, getCreatedBy());
		}
		if (supportCall.getCompanyUser() != null) {
			checkAccountExits(users, supportCall.getCompanyUser().getUser());
		}
		// Remove the current user
		if (users.contains(curUser))
			users.remove(curUser);

		return users;
	}

	private void checkAccountExits(List<Account> users, Account tmpUser) {
		if (!users.contains(tmpUser)) {
			users.add(tmpUser);
		}
	}

	public void toggleSendList(Account user) {
		if (isUserInList(user)) {
			emailSendList.remove(user);
		} else {
			emailSendList.add(user);
		}
	}

	public boolean isUserInList(Account user) {
		boolean rtnVal = false;
		if (emailSendList == null) {
			emailSendList = new LinkedHashSet<Account>();
		}
		if (emailSendList.contains(user)) {
			rtnVal = true;
		}
		return rtnVal;
	}

	/**
	 * Converter required to convert drop down selection lists and repeat
	 * controls from object to text related values
	 * 
	 * @return
	 */
	@Produces
	@Named
	public Converter getCompanyUserConverter() {
		return new Converter() {
			public Object getAsObject(FacesContext context, UIComponent component, String value) {
				// System.out.println("getAsObject=" + value);
				return em.find(CompanyUser.class, Integer.parseInt(value));
			}

			public String getAsString(FacesContext context, UIComponent component, Object value) {
				// System.out.println("getAsString=" + value);
				if (value == null) {
					return "";
				} else {
					return ((CompanyUser) value).getId().toString();
				}
			}
		};
	}
	
	/*
	private void checkAttachmentUpload2() {

		System.out.println("The file size=" + file.getSize() );

		if (file != null && file.getSize() > 0) {
			if (file.getSize() <= MAX_FILE_SIZE) {

				// A file have been attached so upload it
				try {
					Attachment attachment = new Attachment();
					attachment.setSupportCall(supportCall);
					attachment.setFileName(file.getFileName());
					attachment.setContentType(file.getContentType());

					String[] contentType = file.getContentType().split("/");

					if (contentType[0].equals("image")){
						Path folderUpload = getUploadLocation();
						int pos = file.getFileName().lastIndexOf(".");
						// String fileName = file.getFileName().substring(0, pos);
						// System.out.println("FileUpload fileName=" + fileName);
						String extension = file.getFileName().substring(pos, file.getFileName().length());
						//System.out.println("FileUpload Extension=" + extension);
						Path filePath = Files.createTempFile(folderUpload, file.getFileName() + "-", extension);
						//System.out.println("Created a temp file to store data in: " + filePath);
						Files.copy(file.getInputstream(), filePath, StandardCopyOption.REPLACE_EXISTING);
						//System.out.println("Uploaded file successfully saved in " + file);
						attachment.setTmpFileName(filePath.getFileName().toString());
					} else {
						// File may required indexing so store in database
						attachment.setFileData(file.getContents());
					}

					supportCall.getAttachments().add(attachment);
					updateHistory(getMessage("scb_history_addedFile") + ": " + file.getFileName(), "");

				} catch (Exception ex) {
					System.out.println("*** ERROR: " + ex.getMessage() + " ***");
					ex.printStackTrace();
				}

			} else {
				System.out
						.println("*** ERROR - MAX_FILE_SIZE limit (" + MAX_FILE_SIZE + ") exceeded: " + file.getSize());
			}
		}
		file = null;
	}
	*/
	
	/**  
	 * Checks for any attachment uploads
	private void checkAttachmentUpload() {

		if (file != null && file.getSize() > 0) {
			// System.out.println("#1 checkAttachmentUpload - The file size=" + file.getSize() );
			if (file.getSize() <= MAX_FILE_SIZE){
				// System.out.println("#2 checkAttachmentUpload - Less than max size=" + (MAX_FILE_SIZE-file.getSize()) ) ; 
				// A file have been attached so upload it
				try {
					AttachInfo attachinfo = new AttachInfo();
					attachinfo.setSupportCall(supportCall);
					attachinfo.setFileName(file.getFileName());
					attachinfo.setContentType(file.getContentType());
					String[] contentType = file.getContentType().split("/");
					
					if (contentType[0].equals("image")){
						// System.out.println("#4 checkAttachmentUpload - Its an image so store it locally") ;
						Path folderUpload = getUploadLocation();
						int pos = file.getFileName().lastIndexOf(".");
						// String fileName = file.getFileName().substring(0, pos);
						// System.out.println("FileUpload fileName=" + fileName);
						String extension = file.getFileName().substring(pos, file.getFileName().length());
						//System.out.println("FileUpload Extension=" + extension);
						Path filePath = Files.createTempFile(folderUpload, file.getFileName() + "-", extension);
						//System.out.println("Created a temp file to store data in: " + filePath);
						Files.copy(file.getInputstream(), filePath, StandardCopyOption.REPLACE_EXISTING);
						//System.out.println("Uploaded file successfully saved in " + file);
						attachinfo.setTmpFileName(filePath.getFileName().toString());
					} else {
						// System.out.println("#5 checkAttachmentUpload - Its a file so create a data store");
						// File may required indexing so store in database in a separate table 
						// to prevent delay when loading the support call
						AttachData attachdata = new AttachData(); 
						attachdata.setAttachInfo(attachinfo);
						attachdata.setFileData(file.getContents());	
						attachinfo.setAttachdata(attachdata);
						// System.out.println("#6 checkAttachmentUpload - Saved it") ; 
					}
					
					supportCall.getAttachments().add(attachinfo);
					updateHistory(getMessage("scb_history_addedFile") + ": " + file.getFileName(), "");

				} catch (Exception ex) {
					System.out.println("*** ERROR: " + ex.getMessage() + " ***");
					ex.printStackTrace();
				}

			} else {
				System.out
						.println("*** ERROR - MAX_FILE_SIZE limit (" + MAX_FILE_SIZE + ") exceeded: " + file.getSize());
			}
		}
		file = null;
	}

	
	private Path getUploadLocation() {
		// ResourceBundle props = ResourceBundle.getBundle("config.properties");
		Properties props = new Properties();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Path path = null;
		try {
			props.load(cl.getResourceAsStream("config.properties"));
			path = Paths.get(props.getProperty("uploadLocation"));

			if (Files.notExists(path)) {
				//System.out.println("Path doesn't exist so create it...");
				Files.createDirectory(path);
			}

		} catch (IOException e) {
			System.out.println("*** ERROR getUploadLocation() " + e.getMessage() + " ***");
			e.printStackTrace();
		}
		return path;
	}
	
	
	private File getAttachmentLocal(AttachInfo attachInfo) throws IOException {
		
		Path folderUpload = getUploadLocation();
		File dir = new File(folderUpload.toString());
		File f = new File(dir, attachInfo.getTmpFileName());

		if (!f.exists()) {
			return null;
		} else {
			return f;
		}
	}
	 */
}
