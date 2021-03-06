/* ===============================================================================
 *
 * Part of the InfoGlue Content Management Platform (www.infoglue.org)
 *
 * ===============================================================================
 *
 *  Copyright (C)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2, as published by the
 * Free Software Foundation. See the file LICENSE.html for more information.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
 * Place, Suite 330 / Boston, MA 02111-1307 / USA.
 *
 * ===============================================================================
 */

package org.infoglue.cms.applications.managementtool.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.RoleControllerProxy;
import org.infoglue.cms.controllers.kernel.impl.simple.UserControllerProxy;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.sorters.ReflectionComparator;
import org.infoglue.deliver.util.Timer;


/**
 * @author Magnus G�venal
 * @author Mattias Bogeblad
 *
 *	Action class for usecase ViewListSystemUserUCC 
 * 
 */

public class ViewListSystemUserAction extends InfoGlueAbstractAction 
{
	private static final long serialVersionUID = 1L;

    private final static Logger logger = Logger.getLogger(ViewListSystemUserAction.class.getName());

	private List infogluePrincipals;
	
	private String mode 				= null;
	private String filterUserName 		= null;
	private String filterFirstName 		= null; 
	private String filterLastName 		= null; 
	private String filterEmail 			= null; 
	private String[] filterRoleNames	= null;
	private int slotId					= 0;
	private int numberOfSlots			= 0;
	
	private String filterChar = null;
	
	//DataTable parameters for dynamic filtering
	private String sEcho = null;
	private int iTotalRecords = 0;
	private int iTotalDisplayRecords = 0;
	private String sSearch = null;

	protected String doExecute() throws Exception 
	{
		/*
		this.filterRoleNames = this.getRequest().getParameterValues("filterRoleName");
		if(filterFirstName == null && filterLastName == null && filterUserName == null && filterEmail == null && (filterRoleNames == null || filterRoleNames.length == 0 || (filterRoleNames.length == 1 && filterRoleNames[0].equals(""))))
		{
			this.infogluePrincipals = UserControllerProxy.getController().getAllUsers();
			this.numberOfSlots = this.infogluePrincipals.size() / 100;
			int startIndex = 0 + (slotId * 100);
			int endIndex = 0 + (slotId * 100) + 100;
			if(endIndex > this.infogluePrincipals.size())
				endIndex = this.infogluePrincipals.size();
			
			this.infogluePrincipals = this.infogluePrincipals.subList(startIndex, endIndex);
		}
		else
		{
			this.infogluePrincipals = UserControllerProxy.getController().getFilteredUsers(this.filterFirstName, this.filterLastName, this.filterUserName, this.filterEmail, filterRoleNames);
		}
		if(this.infogluePrincipals.size() > 20)
			this.infogluePrincipals = this.infogluePrincipals.subList(0, 20);
		*/

		this.infogluePrincipals = new ArrayList();

		
	    return "success";
	}

	public String doV3() throws Exception 
	{
		/*
		this.filterRoleNames = this.getRequest().getParameterValues("filterRoleName");
		if(filterFirstName == null && filterLastName == null && filterUserName == null && filterEmail == null && (filterRoleNames == null || filterRoleNames.length == 0 || (filterRoleNames.length == 1 && filterRoleNames[0].equals(""))))
		{
			this.infogluePrincipals = UserControllerProxy.getController().getAllUsers();
			this.numberOfSlots = this.infogluePrincipals.size() / 100;
			int startIndex = 0 + (slotId * 100);
			int endIndex = 0 + (slotId * 100) + 100;
			if(endIndex > this.infogluePrincipals.size())
				endIndex = this.infogluePrincipals.size();
			
			this.infogluePrincipals = this.infogluePrincipals.subList(startIndex, endIndex);
		}
		else
		{
			this.infogluePrincipals = UserControllerProxy.getController().getFilteredUsers(this.filterFirstName, this.filterLastName, this.filterUserName, this.filterEmail, filterRoleNames);
		}
		*/

	    return "successV3";
	}

	/**
	 * 
	 */

	public String doPopupProcessAndFilter() throws Exception 
	{
		doProcessAndFilter();
		
		return "successPopupFiltered"; 
	}
	
	public String doProcessAndFilter() throws Exception 
	{
		Timer t = new Timer();
    	if(!logger.isInfoEnabled())
    		t.setActive(false);

		logger.info("sSearch:" + sSearch);
		
		String sortColNumber = getRequest().getParameter("iSortCol_0");
		String sortDirection = getRequest().getParameter("sSortDir_0");
		if(sortDirection == null || sortDirection.equals(""))
			sortDirection = "asc";
		
		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);
								
			if(sSearch == null || sSearch.equals(""))
				this.infogluePrincipals = new ArrayList(); //UserControllerProxy.getController(db).getAllUsers();
			else
				this.infogluePrincipals = UserControllerProxy.getController(db).getFilteredUsers(this.sSearch);
			
			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			logger.info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException("An error occurred so we should not complete the transaction:" + e, e);
		}

		String sortProperty = "name";
		if(sortColNumber != null && sortColNumber.equals("1"))
			sortProperty = "firstName";
		else if(sortColNumber != null && sortColNumber.equals("2"))
			sortProperty = "lastName";
		
		t.printElapsedTime("Before with:" + sortProperty);
		Collections.sort(this.infogluePrincipals, new ReflectionComparator(sortProperty));
		t.printElapsedTime("Sorting took " + sortDirection);
		if(sortDirection.equals("desc"))
		{
			Collections.reverse(this.infogluePrincipals);
			t.printElapsedTime("Reverse took...");
		}
			
		this.iTotalRecords = this.infogluePrincipals.size();
		this.iTotalDisplayRecords = this.infogluePrincipals.size();
		t.printElapsedTime("Getting all users took ");
		
		String iDisplayStartString = getRequest().getParameter("iDisplayStart");
		String iDisplayLengthString = getRequest().getParameter("iDisplayLength");
		int start = new Integer(iDisplayStartString);
		int end = start + new Integer(iDisplayLengthString);
		
		logger.info("Getting principals:" + start + " to " + end + " from original list:" + this.infogluePrincipals.size());
		if(this.infogluePrincipals.size() > end)
			this.infogluePrincipals = this.infogluePrincipals.subList(start, end);
		else
			this.infogluePrincipals = this.infogluePrincipals.subList(start, this.infogluePrincipals.size());
			
		t.printElapsedTime("Getting subset of users took ");
		
	    return "successFiltered";
	}

	public String doUserListForPopup() throws Exception 
	{
		this.infogluePrincipals = new ArrayList(); //UserControllerProxy.getController().getAllUsers();
		//Collections.sort(this.infogluePrincipals, new ReflectionComparator("firstName"));
		
	    return "successPopup";
	}
	
	public String doUserListForPopupV3() throws Exception 
	{
		/*
		this.infogluePrincipals = UserControllerProxy.getController().getAllUsers();
		Collections.sort(this.infogluePrincipals, new ReflectionComparator("firstName"));
		*/
	    return "successPopupV3";
	}

	public List getUsersFirstNameChars()
	{
		List usersFirstNameChars = new ArrayList();
		Iterator principalIterator = this.infogluePrincipals.iterator();
		while(principalIterator.hasNext())
		{
			InfoGluePrincipal infogluePrincipal = (InfoGluePrincipal)principalIterator.next();
			if(!usersFirstNameChars.contains(infogluePrincipal.getName().charAt(0)))
				usersFirstNameChars.add(infogluePrincipal.getName().charAt(0));
			//else
			//	System.out.println("Exists:" + infogluePrincipal.getName().charAt(0));
		}
		
		Collections.sort(usersFirstNameChars);
		
		return usersFirstNameChars;
	}
	
	public List getFilteredInfogluePrincipals()
	{
		List subList = new ArrayList();
		
		char filterChar = ((InfoGluePrincipal)this.infogluePrincipals.get(0)).getFirstName().charAt(0);
		if(this.filterChar != null && this.filterChar.length() == 1)
			filterChar = this.filterChar.charAt(0);
			
		Iterator infogluePrincipalsIterator = this.infogluePrincipals.iterator();
		boolean foundSection = false;
		while(infogluePrincipalsIterator.hasNext())
		{
			InfoGluePrincipal infogluePrincipal = (InfoGluePrincipal)infogluePrincipalsIterator.next();
			if(infogluePrincipal.getName().charAt(0) == filterChar)
			{
				subList.add(infogluePrincipal);
				foundSection = true;
			}
			else if(foundSection)
				break;
		}
		
		return subList;
	}
	public String doUserListSearch() throws Exception
	{
		String searchString 					= this.getRequest().getParameter("searchString");		
		List<InfoGluePrincipal> searchResult 	= UserControllerProxy.getController().getFilteredUsers(searchString, null, null, null, null);
		ServletOutputStream myOut 				= getResponse().getOutputStream();
		
		myOut.print("<select name=\"searchResult\" size=\"10\" class=\"userSelectBox\" multiple=\"true\">");
		
		for (InfoGluePrincipal igp : searchResult)
		{
			myOut.print("<option value=\"" + igp.getName() + "\">" + igp.getFirstName() + " " + igp.getLastName() + "</option>");
		}
		
		myOut.print("</select>");
		
		return "none";
	}
	
	public List getRoles() throws Exception
	{
		List roles = RoleControllerProxy.getController().getAllRoles();
		
		return roles; 
	}	
	
	public List getInfogluePrincipals()
	{
		return this.infogluePrincipals;		
	}
	
	public String getFilterEmail()
	{
		return filterEmail;
	}

	public void setFilterEmail(String email)
	{
		if(email != null && !email.equals(""))
			this.filterEmail = email;
	}

	public String getFilterFirstName()
	{
		return filterFirstName;
	}

	public void setFilterFirstName(String firstName)
	{
		if(firstName != null && !firstName.equals(""))
			this.filterFirstName = firstName;
	}

	public String getFilterLastName()
	{
		return filterLastName;
	}

	public void setFilterLastName(String lastName)
	{
		if(lastName != null && !lastName.equals(""))
			this.filterLastName = lastName;
	}

	public String getFilterUserName()
	{
		return filterUserName;
	}

	public void setFilterUserName(String userName)
	{
		if(userName != null && !userName.equals(""))
			this.filterUserName = userName;
	}

	public String getFilterChar()
	{
		return filterChar;
	}
	
	public void setFilterChar(String filterChar)
	{
		if(filterChar != null && !filterChar.equals(""))
			this.filterChar = filterChar;
	}

	public String getMode()
	{
		return mode;
	}

	public void setMode(String mode)
	{
		this.mode = mode;
	}

	public String[] getFilterRoleNames()
	{
		return filterRoleNames;
	}

	public int getSlotId() 
	{
		return slotId;
	}

	public void setSlotId(int slotId) 
	{
		this.slotId = slotId;
	}

	public int getNumberOfSlots() 
	{
		return numberOfSlots;
	}

	public void setNumberOfSlots(int numberOfSlots) 
	{
		this.numberOfSlots = numberOfSlots;
	}
	
	public int getTotalRecords() 
	{
		return iTotalRecords;
	}

	public int getTotalDisplayRecords() 
	{
		return iTotalDisplayRecords;
	}

	public String getsEcho() 
	{
		return sEcho;
	}

	public void setsEcho(String sEcho) 
	{
		this.sEcho = sEcho;
	}
	
	public String getsSearch() 
	{
		return sSearch;
	}

	public void setsSearch(String sSearch) 
	{
		this.sSearch = sSearch;
	}
	

}
