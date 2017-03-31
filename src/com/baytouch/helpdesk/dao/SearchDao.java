package com.baytouch.helpdesk.dao;

import java.io.Serializable;
import java.util.List;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.PersistenceContext;
import javax.persistence.EntityManager;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

import com.baytouch.helpdesk.entities.SupportCall;

@ViewScoped
@Named
public class SearchDao implements Serializable {

	private static final long serialVersionUID = 1L;
	@PersistenceContext(unitName="helpdesk")
	private EntityManager em;
	// private List<SupportCall> allSupportCalls;
	private String searchString;
	private boolean hasIndexLoaded = false; 
	
	public String getSearchString() {
		return searchString;
	}
	
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
	
	public void refreshIndex(){
		hasIndexLoaded=false;
		loadIndex(); 
	}
	
	public String loadIndex(){
		if(!hasIndexLoaded){
			System.out.println("*** SearchDao.loadIndex()");
			FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
			try {
				System.out.println("*** About to start and wait the index rebuild");
				fullTextEntityManager.createIndexer().startAndWait();
				System.out.println("*** Done");
			} catch (InterruptedException e){
				e.printStackTrace();
			}
			hasIndexLoaded=true; 
		}
		return ""; 
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked"})
	public List<SupportCall> fullTextSearch(){
		
		// System.out.println("inside full text index...");
		if(searchString != null && !searchString.isEmpty()){
			
			FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
			// create native Lucene query using the query DSL
			// alternatively you can write the Lucene query using the Lucene query parser
			// or the Lucene programmatic API. The Hibernate Search DSL is recommended though
			QueryBuilder qb = fullTextEntityManager.getSearchFactory()
			    .buildQueryBuilder()
			    .forEntity(SupportCall.class).get();
			
			org.apache.lucene.search.Query query = qb
				.keyword()
				.onFields("subject","callRefs","callType","callDetails","status","senderEmail","senderName",
					  "company.companyName","callComments.comments","attachinfo.fileName","attachinfo.tmpFileName")
				.matching(searchString)
				.createQuery();
	
			// wrap Lucene query in a javax.persistence.Query
			javax.persistence.Query persistenceQuery =
			    fullTextEntityManager.createFullTextQuery(query, SupportCall.class);
	
			// execute search
			List result = persistenceQuery.getResultList();	
			// System.out.println("Number of results found=" + result.size() );
		
			return result;
		}
		return null;
	}
	
}
