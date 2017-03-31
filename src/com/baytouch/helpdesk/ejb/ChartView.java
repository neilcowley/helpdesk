package com.baytouch.helpdesk.ejb;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;


// import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.PieChartModel;

import com.baytouch.helpdesk.beans.GlobalsBean;
import com.baytouch.helpdesk.beans.SupportCallBean;
import com.baytouch.helpdesk.dao.CompanyDao;
import com.baytouch.helpdesk.dao.SupportCallDao;
import com.baytouch.helpdesk.dao.UserDao;
import com.baytouch.helpdesk.entities.AdminUser;
import com.baytouch.helpdesk.entities.Company;
import com.baytouch.helpdesk.entities.SupportCall;
 
import org.primefaces.model.chart.ChartSeries;
 
@Named
@RequestScoped
public class ChartView implements Serializable {
 
	private static final long serialVersionUID = 1L;
	private BarChartModel barModel;
    private PieChartModel pieModel;
    private LineChartModel lineModel;
    private int maxPieCalls = 0 ;
    private int maxLineCalls = 0 ; 
    private static String[] MONTHS = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"}; 
	private ResourceBundle bundle;
    
    @Inject
    UserDao udao;
    @Inject
    CompanyDao cdao;
    @Inject
    SupportCallDao scdao;
    @Inject
    SupportCallBean scbean;
    @Inject
    GlobalsBean gbean;
    
    @PostConstruct
    public void init(){	
        createBarModel();
        createPieModel();
        createLineCategoryModel();
    }
     
    public BarChartModel getBarModel() {
        return barModel;
    }
    
    public PieChartModel getPieModel(){
    	return pieModel; 
    }
        
    public LineChartModel getLineModel(){
    	return lineModel; 
    }
    
	private String getMessage(String key){
		// System.out.println("key to get=" + key);
		if(bundle == null){
		//	System.out.println("*** Getting the properties file");
			String locale = gbean.getLocale();
		//	System.out.println("*** Locale=" + locale  + "=");
			String langVal = !locale.equals("") && !locale.equals("en") ? "_" + locale : "";
		//	System.out.println("*** Properties will be: " + langVal);
			bundle = ResourceBundle.getBundle("com.baytouch.helpdesk.language.select" + langVal);
		}
		return bundle.getString(key);
	}
    
    private void createLineCategoryModel(){
    	lineModel = initCategoryModel();
        lineModel.setTitle( getMessage("rp_lineChartTitle")); // "Completed Calls By Assigned By Month"
        lineModel.setLegendPosition("ne");
        lineModel.setShowPointLabels(true);
        lineModel.getAxes().put(AxisType.X, new CategoryAxis( getMessage("rp_lineChartYAxisLabel"))); // "Months"
        Axis yAxis = lineModel.getAxis(AxisType.Y);
        yAxis.setLabel( getMessage("rp_lineChartXAxisLabel")); // "No Calls"
        yAxis.setMin(0);
        yAxis.setMax(maxLineCalls + 2);
    }
    
    private void createPieModel(){
    	pieModel = initPieModel(); 
    	pieModel.setTitle(getMessage("rp_pieChartTitle")); // "All Calls By Status"
    	pieModel.setLegendPosition("w");
    }
        
    private void createBarModel(){
        barModel = initBarModel();
        barModel.setTitle(getMessage("rp_barChartTitle")); // Support Calls Created by Company by Month
        barModel.setLegendPosition("ne");
        Axis yAxis = barModel.getAxis(AxisType.Y);
        yAxis.setLabel(getMessage("rp_barChartYAxisLabel")); // No. Calls
        yAxis.setMin(0);
        yAxis.setMax(maxPieCalls + 2);
        Axis xAxis = barModel.getAxis(AxisType.X);
        xAxis.setLabel(getMessage("rp_barChartXAxisLabel")); // No. Calls
    }

    private LineChartModel initCategoryModel(){
    	
    	LineChartModel model = new LineChartModel();
    	Map<String, Map<String, Integer>> assignedYrData = getCompletedCallsByAssigned();
    	
        for (Entry<String, Map<String, Integer>> entry : assignedYrData.entrySet()){
        	
		    String assigned = entry.getKey();
	        LineChartSeries tmpSeries = new LineChartSeries();
	        tmpSeries.setLabel(assigned);
		    Map<String, Integer> compList = entry.getValue();
		    for (Entry<String, Integer> data : compList.entrySet()){
		    //	System.out.println(assigned + ": " + data.getKey() + " - " + data.getValue() );
		    	tmpSeries.set(data.getKey(), data.getValue());
		    }
		    model.addSeries(tmpSeries);
        }
		return model;
    }
        
    private PieChartModel initPieModel(){
    	
    	PieChartModel model = new PieChartModel();
    	List<SupportCall> statusCalls = new ArrayList<SupportCall>();
    	List<String> statusList = scbean.getStatusList();
    	
    	for(String status : statusList){
    		statusCalls = scdao.getCallsByStatus(status); 
    		if(statusCalls.size()>0){
    			model.set(status, statusCalls.size());
    		}		
    	}		
		return model; 
    }
    
    private BarChartModel initBarModel(){
    		
        BarChartModel model = new BarChartModel();
        Map<String, Map<String, Integer>> compYrData = getCompanyCalls();
        
        for (Entry<String, Map<String, Integer>> entry : compYrData.entrySet()){
		    String company = entry.getKey();
		    Map<String, Integer> scList = entry.getValue();
		    ChartSeries tmpSeries = new ChartSeries();
		    tmpSeries.setLabel(company);
		    for (Entry<String, Integer> data : scList.entrySet()){
		//    	System.out.println(company + ": " + data.getKey() + " - " + data.getValue() );
		    	tmpSeries.set(data.getKey() , data.getValue());
		    }
		    model.addSeries(tmpSeries);
        }
        return model;
    }
    
    private Map<String, Map<String, Integer>> getCompanyCalls(){
    	
    //	System.out.println("#1 - Inside getCompanyCalls");
    	Map<String, Map<String, Integer>> compYrData = new HashMap<String, Map<String, Integer> >();
        List<Company> companies = new ArrayList<Company>();
        List<SupportCall> compCalls = new ArrayList<SupportCall>();
        
    //   System.out.println("#2 - about to getAllCompanies");
        companies = cdao.getAllCompanies();
        
        for(Company comp : companies){
        	String compName = comp.getCompanyName();   
    //     	System.out.println("#3 - Looking for all calls for company: " + compName);
        	compCalls = scdao.getCallsByCompanyId(comp.getId()); 
        	
        	if(compCalls.size()>0){
    //    		System.out.println("#4 - Number of Calls for company: " + compName + " is: " + compCalls.size() );
        		Map<String, Integer> byMonths = new LinkedHashMap<String, Integer>();
		        for(SupportCall supportCall : compCalls){    	
		        	for(String month : MONTHS){
		        		@SuppressWarnings("deprecation")
						String callMonth = MONTHS[supportCall.getDateCreated().getMonth()]; 
	//	        		System.out.println(month + " - LOOP - The Support call Month =" + callMonth );
		        		int val = month.equals(callMonth) ? 1 : 0 ; 
		        		int count = byMonths.containsKey(month) ? byMonths.get(month) : 0;
		        		int tCount = count + val;
		        		maxPieCalls = tCount > maxPieCalls ? tCount : maxPieCalls ; 
		        		byMonths.put(month, tCount);
		        	}
		        }
		        // Add it to the main HashMap
	//	        System.out.println("#5 - Putting all the data back inside the main HashMap container.");
		        compYrData.put(compName, byMonths);
        	}
        }
        
        return compYrData; 
    }
    
    private  Map<String, Map<String, Integer>> getCompletedCallsByAssigned(){
    	
    	Map<String, Map<String, Integer>> assignedYrData = new HashMap<String, Map<String, Integer> >();
    	List<AdminUser> adminList = udao.getAllAdminUsers();

    	for(AdminUser admin : adminList ){ 
    		List<SupportCall> assignedCalls = scdao.getCallsByAssignedByStatus(admin.getId(), SupportCallBean.STATUS_COMPLETE ) ; 
    	//	System.out.println("Looking for calls for : " + admin.getId() +  " Found=" + assignedCalls.size() ) ; 
    		if(assignedCalls.size()>0){
    	//		System.out.println("Found: " + assignedCalls.size() + " for Admin user: " + admin.getUserName() );
    			Map<String, Integer> byMonths = new LinkedHashMap<String, Integer>();		
 		        for(SupportCall supportCall : assignedCalls){          	
 		        	for(String month : MONTHS){
 		        		@SuppressWarnings("deprecation")
 						String callMonth = MONTHS[supportCall.getDateCompleted().getMonth()]; 
 		        	//	System.out.println(month + " - LOOP - The Support call Month =" + callMonth );
 		        		int val = month.equals(callMonth) ? 1 : 0 ; 
 		        		int count = byMonths.containsKey(month) ? byMonths.get(month) : 0;
 		        		int tCount = count + val;
 		        		maxLineCalls = tCount > maxLineCalls ? tCount : maxLineCalls ; 
 		        		byMonths.put(month, tCount);
 		        	}
 		       }
 		       assignedYrData.put(admin.getUserName() , byMonths);
    		}
    	}
    	return assignedYrData; 
    }     
}