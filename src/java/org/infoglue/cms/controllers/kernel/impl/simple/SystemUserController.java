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

package org.infoglue.cms.controllers.kernel.impl.simple;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.OQLQuery;
import org.exolab.castor.jdo.PersistenceException;
import org.exolab.castor.jdo.QueryResults;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.entities.management.Group;
import org.infoglue.cms.entities.management.Role;
import org.infoglue.cms.entities.management.SystemUser;
import org.infoglue.cms.entities.management.SystemUserVO;
import org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl;
import org.infoglue.cms.entities.management.impl.simple.SystemUserGroupImpl;
import org.infoglue.cms.entities.management.impl.simple.SystemUserImpl;
import org.infoglue.cms.entities.management.impl.simple.SystemUserRoleImpl;
import org.infoglue.cms.exception.Bug;
import org.infoglue.cms.exception.ConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.ConstraintExceptionBuffer;
import org.infoglue.cms.util.PasswordGenerator;
import org.infoglue.cms.util.mail.MailServiceFactory;

/**
 * SystemUserController.java
 * Created on 2002-aug-28 
 * @author Stefan Sik, ss@frovi.com 
 * 
 * This class is a helper class for the use case handle SystemUsers
 * 
 */
public class SystemUserController extends BaseController
{
    private final static Logger logger = Logger.getLogger(SystemUserController.class.getName());

	/**
	 * Factory method
	 */

	public static SystemUserController getController()
	{
		return new SystemUserController();
	}
	
	/*
    public static SystemUser getSystemUserWithId(Integer systemUserId, Database db) throws SystemException, Bug
    {
		return (SystemUser) getObjectWithId(SystemUserImpl.class, systemUserId, db);
    }
    
    public SystemUserVO getSystemUserVOWithId(Integer systemUserId) throws SystemException, Bug
    {
		return (SystemUserVO) getVOWithId(SystemUserImpl.class, systemUserId);
    }
	*/

	public SystemUserVO getSystemUserVOWithName(String name)  throws SystemException, Bug
	{
		SystemUserVO systemUserVO = null;
		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

			systemUserVO = getReadOnlySystemUserVOWithName(name, db);

			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			logger.info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}
		
		return systemUserVO;		
	}	
	
	
	/**
	 * 	Get the SystemUser with the userName
	 */
	 
	public SystemUserVO getReadOnlySystemUserVOWithName(String userName, Database db)  throws SystemException, Bug
	{
		SystemUserVO systemUserVO = null;
		
        try
        {										
        	OQLQuery oql = db.getOQLQuery("SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl u WHERE u.userName = $1");
        	oql.bind(userName);
        	
        	QueryResults results = oql.execute(Database.ReadOnly);
			
			if (results.hasMore()) 
            {
				SystemUser systemUser = (SystemUser)results.next();
				systemUserVO = systemUser.getValueObject();
            }
			
			results.close();
			oql.close();
        }
        catch(Exception e)
        {
            throw new SystemException("An error occurred when we tried to fetch " + userName + " Reason:" + e.getMessage(), e);    
        }    

		return systemUserVO;		
	}

	
	/**
	 * 	Get the SystemUser with the userName
	 */
	 
	public SystemUser getReadOnlySystemUserWithName(String userName, Database db)  throws SystemException, Bug
	{
		SystemUser systemUser = null;
        OQLQuery	oql;
        try
        {										
        	oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u WHERE u.userName = $1");
        	oql.bind(userName);
        	
        	QueryResults results = oql.execute(Database.ReadOnly);
			
			if (results.hasMore()) 
            {
            	systemUser = (SystemUser)results.next();
            	logger.info("found one:" + systemUser.getFirstName());
            }
			results.close();
			oql.close();
        }
        catch(Exception e)
        {
            throw new SystemException("An error occurred when we tried to fetch " + userName + " Reason:" + e.getMessage(), e);    
        }    

		return systemUser;		
	}

	/**
	 * 	Get if the SystemUser with the userName exists
	 */
	 
	public boolean systemUserExists(String userName, Database db) throws SystemException, Bug
	{
		boolean systemUserExists = false;
		
        try
        {										
        	OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl u WHERE u.userName = $1");
        	oql.bind(userName);
        	
        	QueryResults results = oql.execute(Database.ReadOnly);
			
			if (results.hasMore()) 
            {
				systemUserExists = true;
            }
			
			results.close();
			oql.close();
        }
        catch(Exception e)
        {
        	throw new SystemException("An error occurred when we tried to fetch " + userName + " Reason:" + e.getMessage(), e);  
        }    
        
        return systemUserExists;		
	}


	/**
	 * 	Get the SystemUser with the userName
	 */
	 
	public SystemUser getSystemUserWithName(String userName, Database db)  throws SystemException, Bug
	{
		SystemUser systemUser = null;
        OQLQuery oql;
        try
        {					
        	oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u WHERE u.userName = $1");
        	oql.bind(userName);
        	
        	QueryResults results = oql.execute();
    		this.logger.info("Fetching entity in read/write mode" + userName);

			if (results.hasMore()) 
            {
            	systemUser = (SystemUser)results.next();
            	logger.info("found one:" + systemUser.getFirstName());
            }
			
			results.close();
			oql.close();
        }
        catch(Exception e)
        {
            throw new SystemException("An error occurred when we tried to fetch " + userName + " Reason:" + e.getMessage(), e);    
        }    

		return systemUser;		
	}



	public SystemUserVO getSystemUserVO(String userName, String password)  throws SystemException, Bug
	{
		SystemUserVO systemUserVO = null;

		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);

			systemUserVO = getSystemUserVO(db, userName, password);

			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			logger.info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}
		
		return systemUserVO;		
	}	


	public SystemUserVO getSystemUserVO(Database db, String userName, String password)  throws SystemException, Exception
	{
		SystemUserVO systemUserVO = null;

		OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl u WHERE u.userName = $1 AND u.password = $2");
		oql.bind(userName);
		oql.bind(password);
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		if (results.hasMore()) 
		{
			SystemUser systemUser = (SystemUser)results.next();
			systemUserVO = systemUser.getValueObject();
		}

		results.close();
		oql.close();

		return systemUserVO;		
	}	
    
	public SystemUser getSystemUser(Database db, String userName, String password)  throws SystemException, Exception
	{
		SystemUser systemUser = null;
		
		OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u WHERE u.userName = $1 AND u.password = $2");
		oql.bind(userName);
		oql.bind(password);
    	
		QueryResults results = oql.execute();
		this.logger.info("Fetching entity in read/write mode" + userName);

		if (results.hasMore()) 
		{
			systemUser = (SystemUser)results.next();
			logger.info("found one:" + systemUser.getFirstName());
		}

		results.close();
		oql.close();

		return systemUser;		
	}	
	/*
    public List getSystemUserVOList() throws SystemException, Bug
    {
        return getAllVOObjects(SmallSystemUserImpl.class, "userName");
    }

    public List getSystemUserVOList(Database db) throws SystemException, Bug
    {
        return getAllVOObjects(SmallSystemUserImpl.class, "userName", db);
    }

    public List getSystemUserList(Database db) throws SystemException, Bug
    {
        return getAllObjects(SystemUserImpl.class, "userName", db);
    }
    */
	
	public List getSystemUserList(Database db) throws SystemException, Bug, Exception
	{
		List<SystemUser> filteredVOList = new ArrayList<SystemUser>();
		
		OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u ORDER BY u.userName");
    	//oql.bind(1000);
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			filteredVOList.add(extranetUser);
		}
		
		results.close();
		oql.close();

		return filteredVOList;
	}

	public List<SystemUserVO> getSystemUserVOList(Database db) throws SystemException, Bug, Exception
	{
		List<SystemUserVO> userList = new ArrayList<SystemUserVO>();
		
		OQLQuery oql = db.getOQLQuery("SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl u ORDER BY u.userName ASC");
		
		QueryResults results = oql.execute(Database.ReadOnly);

		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			userList.add(extranetUser.getValueObject());
		}
		
		results.close();
		oql.close();

		return userList;
	}
	public List<SystemUserVO> getSystemUserVOListWithPassword(String password, Database db) throws SystemException, Bug, Exception
	{
		List<SystemUserVO> filteredVOList = new ArrayList<SystemUserVO>();
		
		OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl u WHERE u.password = $1 ORDER BY u.userName");
    	oql.bind(password);
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			filteredVOList.add(extranetUser.getValueObject());
		}
		
		results.close();
		oql.close();

		return filteredVOList;
	}
	
	public List<SystemUserVO> getFilteredSystemUserVOList(String searchString) throws SystemException, Bug
	{
		List<SystemUserVO> filteredList = new ArrayList<SystemUserVO>();
		
		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);
								
			filteredList = getFilteredSystemUserVOList(searchString, db);
			
			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			logger.info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException("An error occurred so we should not complete the transaction:" + e, e);
		}
		
		return filteredList;
	}

	public List getFilteredSystemUserList(String searchString, Database db) throws SystemException, Bug, Exception
	{
		List filteredList = new ArrayList();
		
		OQLQuery oql = null;
		String oqlBase = "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u WHERE firstName LIKE $1 OR lastName LIKE $2 OR userName LIKE $3 OR email LIKE $4 ORDER BY u.userName ASC";
		oql = db.getOQLQuery(oqlBase);
		oql.bind("%" + searchString + "%");
		oql.bind("%" + searchString + "%");
		oql.bind("%" + searchString + "%");
		oql.bind("%" + searchString + "%");
		
		QueryResults results = oql.execute(Database.ReadOnly);

		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			filteredList.add(extranetUser);
		}
		
		results.close();
		oql.close();

		return filteredList;
	}

	public List<SystemUserVO> getFilteredSystemUserVOList(String searchString, Database db) throws SystemException, Bug, Exception
	{
		List<SystemUserVO> filteredList = new ArrayList<SystemUserVO>();
		
		OQLQuery oql = null;
		String oqlBase = "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SmallSystemUserImpl u WHERE firstName LIKE $1 OR lastName LIKE $2 OR userName LIKE $3 OR email LIKE $4 ORDER BY u.userName ASC";
		oql = db.getOQLQuery(oqlBase);
		oql.bind("%" + searchString + "%");
		oql.bind("%" + searchString + "%");
		oql.bind("%" + searchString + "%");
		oql.bind("%" + searchString + "%");
		
		QueryResults results = oql.execute(Database.ReadOnly);

		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			filteredList.add(extranetUser.getValueObject());
		}
		
		results.close();
		oql.close();

		return filteredList;
	}

	public List getFilteredSystemUserListOld(String searchString, Database db) throws SystemException, Bug, Exception
	{
		List filteredList = new ArrayList();
		
		OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u ORDER BY u.userName");
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			boolean include = false;
			if(searchString == null || searchString.equals(""))
			{
				include = true;
			}
			else
			{
				if(extranetUser.getFirstName().toLowerCase().indexOf(searchString.toLowerCase()) > -1)
					include = true;
				else if(extranetUser.getLastName().toLowerCase().indexOf(searchString.toLowerCase()) > -1)
					include = true;
				else if(extranetUser.getUserName().toLowerCase().indexOf(searchString.toLowerCase()) > -1)
					include = true;
				else if(extranetUser.getEmail().toLowerCase().indexOf(searchString.toLowerCase()) > -1)
					include = true;
			}
			
			if(include)
				filteredList.add(extranetUser);
		}
		
		results.close();
		oql.close();

		return filteredList;
	}

	public List getFilteredSystemUserVOList(String firstName, String lastName, String userName, String email, String[] roleNames) throws SystemException, Bug
	{
		List filteredList = new ArrayList();
		
		Database db = CastorDatabaseService.getDatabase();

		try 
		{
			beginTransaction(db);
								
			filteredList = getFilteredSystemUserList(firstName, lastName, userName, email, roleNames, db);
			
			commitTransaction(db);
		} 
		catch (Exception e) 
		{
			logger.info("An error occurred so we should not complete the transaction:" + e);
			rollbackTransaction(db);
			throw new SystemException("An error occurred so we should not complete the transaction:" + e, e);
		}
		
		return toVOList(filteredList);
	}

	public List getFilteredSystemUserList(String firstName, String lastName, String userName, String email, String[] roleNames, Database db) throws SystemException, Bug, Exception
	{
		List filteredList = new ArrayList();
		
		OQLQuery oql = db.getOQLQuery( "SELECT u FROM org.infoglue.cms.entities.management.impl.simple.SystemUserImpl u ORDER BY u.userName");
    	
		QueryResults results = oql.execute(Database.ReadOnly);
		
		while (results.hasMore()) 
		{
			SystemUser extranetUser = (SystemUser)results.next();
			boolean include = true;
			
			if(firstName != null && !firstName.equals("") && extranetUser.getFirstName().toLowerCase().indexOf(firstName.toLowerCase()) == -1)
				include = false;
			
			if(lastName != null && !lastName.equals("") && extranetUser.getLastName().toLowerCase().indexOf(lastName.toLowerCase()) == -1)
				include = false;
			
			if(userName != null && !userName.equals("") && extranetUser.getUserName().toLowerCase().indexOf(userName.toLowerCase()) == -1)
				include = false;
			
			if(email != null && !email.equals("") && extranetUser.getEmail().toLowerCase().indexOf(email.toLowerCase()) == -1)
				include = false;
			
			boolean hasRoles = true;
			if(roleNames != null && roleNames.length > 0)
			{	
				for(int i=0; i < roleNames.length; i++)
				{
					String roleName = roleNames[i];
					if(roleName != null && !roleName.equals(""))
					{	
						Collection roles = extranetUser.getRoles();
						Iterator rolesIterator = roles.iterator();
						boolean hasRole = false;
						while(rolesIterator.hasNext())
						{
							Role role = (Role)rolesIterator.next();
							if(role.getRoleName().equalsIgnoreCase(roleName))
							{
								hasRole = true;
								break;
							}
						}
						
						if(!hasRole)
						{
							hasRoles = false;
							break;
						}
					}
				}					
			}
			
			if(include && hasRoles)
				filteredList.add(extranetUser);
		}
		
		results.close();
		oql.close();

		return filteredList;
	}

	/*
	 * CREATE
	 * 
	 */
    public SystemUserVO create(SystemUserVO systemUserVO) throws ConstraintException, SystemException
    {
		if(CmsPropertyHandler.getUsePasswordEncryption())
		{
	    	String password = systemUserVO.getPassword();
			try
			{
				byte[] encryptedPassRaw = DigestUtils.sha(password);
				String encryptedPass = new String(new Base64().encode(encryptedPassRaw), "ASCII");
				password = encryptedPass;
				systemUserVO.setPassword(password);
			}
			catch (Exception e) 
			{
				logger.error("Error generating password:" + e.getMessage());
			}
		}
    	
    	SystemUser systemUser = new SystemUserImpl();
        systemUser.setValueObject(systemUserVO);
        systemUser = (SystemUser) createEntity(systemUser);
        return systemUser.getValueObject();
    }     

	/*
	 * CREATE
	 * 
	 */
    public SystemUser create(SystemUserVO systemUserVO, Database db) throws ConstraintException, SystemException, Exception
    {
		if(CmsPropertyHandler.getUsePasswordEncryption())
		{
	    	String password = systemUserVO.getPassword();
			
			try
			{
				byte[] encryptedPassRaw = DigestUtils.sha(password);
				String encryptedPass = new String(new Base64().encode(encryptedPassRaw), "ASCII");
				password = encryptedPass;
				systemUserVO.setPassword(password);
			}
			catch (Exception e) 
			{
				logger.error("Error generating password:" + e.getMessage());
			}
		}

    	SystemUser systemUser = new SystemUserImpl();
        systemUser.setValueObject(systemUserVO);
        systemUser = (SystemUser) createEntity(systemUser, db);
        return systemUser;
    }     

	/*
	 * DELETE
	 * 
	 */
	 
    public void delete(String userName) throws ConstraintException, SystemException
    {
		Database db = CastorDatabaseService.getDatabase();

		beginTransaction(db);

		try
		{
		    delete(userName, db);
            
			commitTransaction(db);
		}
		catch(ConstraintException ce)
		{
			logger.warn("An error occurred so we should not completes the transaction:" + ce, ce);
			rollbackTransaction(db);
			throw ce;
		}
		catch(Exception e)
		{
			logger.error("An error occurred so we should not completes the transaction:" + e, e);
			rollbackTransaction(db);
			throw new SystemException(e.getMessage());
		}

    }        

    /*
	 * DELETE
	 * 
	 */
	 
    public void delete(String userName, Database db) throws ConstraintException, SystemException, Exception
    {
    	deleteRoles(userName, db);
    	deleteGroups(userName, db);
    	deleteEntity(SmallSystemUserImpl.class, userName, db);
    }        

    
	/**
	 * 	Delete all roles for a user
	 * @throws Exception 
	 */
	 
	public void deleteRoles(String userName) throws SystemException, Bug, Exception
	{
        Database db = CastorDatabaseService.getDatabase();

        beginTransaction(db);

        try
        {
        	deleteRoles(userName, db);
        	
            commitTransaction(db);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
 	}
	
	public List<String> deleteRolesAndReturnExisting(String userName, String[] rolesToKeepArray, Database db) throws SystemException, Bug, Exception
	{
		List<String> keptRoles = new ArrayList<String>();
		List<String> rolesToKeep = Arrays.asList(rolesToKeepArray);
			
    	OQLQuery oql = db.getOQLQuery( "SELECT sur FROM org.infoglue.cms.entities.management.impl.simple.SystemUserRoleImpl sur WHERE sur.userName = $1");
    	oql.bind(userName);
    	
    	QueryResults results = oql.execute();
		while (results.hasMore()) 
        {
			SystemUserRoleImpl sur = (SystemUserRoleImpl)results.nextElement();
			if(!rolesToKeep.contains(sur.getRoleName()))
				db.remove(sur);
			else
				keptRoles.add(sur.getRoleName());
        }
		
		results.close();
		oql.close();
		
		return keptRoles;
 	}

	public void deleteRoles(String userName, Database db) throws SystemException, Bug, Exception
	{
    	OQLQuery oql = db.getOQLQuery( "SELECT sur FROM org.infoglue.cms.entities.management.impl.simple.SystemUserRoleImpl sur WHERE sur.userName = $1");
    	oql.bind(userName);
    	
    	QueryResults results = oql.execute();
		while (results.hasMore()) 
        {
			SystemUserRoleImpl sur = (SystemUserRoleImpl)results.nextElement();
			db.remove(sur);
        }
		
		results.close();
		oql.close();
 	}

	/**
	 * 	Delete all roles for a user
	 * @throws Exception 
	 */
	 
	public void deleteGroups(String userName) throws SystemException, Bug, Exception
	{
        Database db = CastorDatabaseService.getDatabase();

        beginTransaction(db);

        try
        {
        	deleteGroups(userName, db);
        
    		commitTransaction(db);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
 	}
   
	
	public List<String> deleteGroupsAndReturnExisting(String userName, String[] groupsToKeepArray, Database db) throws SystemException, Bug, Exception
	{
		List<String> keptGroups = new ArrayList<String>();
		List<String> groupsToKeep = Arrays.asList(groupsToKeepArray);
			
    	OQLQuery oql = db.getOQLQuery( "SELECT sur FROM org.infoglue.cms.entities.management.impl.simple.SystemUserGroupImpl sur WHERE sur.userName = $1");
    	oql.bind(userName);
    	
    	QueryResults results = oql.execute();
		while (results.hasMore()) 
        {
			SystemUserGroupImpl sur = (SystemUserGroupImpl)results.nextElement();
			if(!groupsToKeep.contains(sur.getGroupName()))
				db.remove(sur);
			else
				keptGroups.add(sur.getGroupName());
        }
		
		results.close();
		oql.close();
		
		return keptGroups;
 	}
	/**
	 * 	Delete all roles for a user
	 * @throws Exception 
	 */
	 
	public void deleteGroups(String userName, Database db) throws SystemException, Bug, Exception
	{
    	OQLQuery oql = db.getOQLQuery( "SELECT sur FROM org.infoglue.cms.entities.management.impl.simple.SystemUserGroupImpl sur WHERE sur.userName = $1");
    	oql.bind(userName);
    	
    	QueryResults results = oql.execute();
		while (results.hasMore()) 
        {
			SystemUserGroupImpl sur = (SystemUserGroupImpl)results.nextElement();
			db.remove(sur);
        }
		
		results.close();
		oql.close();
    }
	
	
    public SystemUserVO update(SystemUserVO systemUserVO) throws ConstraintException, SystemException
    {
    	return (SystemUserVO) updateEntity(SystemUserImpl.class, (BaseEntityVO) systemUserVO);
    }        


    public SystemUserVO update(SystemUserVO systemUserVO, String[] roleNames, String[] groupNames) throws ConstraintException, SystemException, Exception
    {
        Database db = CastorDatabaseService.getDatabase();

        SystemUser systemUser = null;

        beginTransaction(db);

        try
        {
       		systemUser = update(systemUserVO, roleNames, groupNames, db);
            
            commitTransaction(db);
        }
        catch(ConstraintException ce)
        {
        	ce.printStackTrace();
            logger.warn("An error occurred so we should not completes the transaction:" + ce, ce);
            rollbackTransaction(db);
            throw ce;
        }
        catch(Exception e)
        {
        	e.printStackTrace();
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
                
        return systemUser.getValueObject();
    }        

    public SystemUser update(SystemUserVO systemUserVO, String[] roleNames, String[] groupNames, Database db) throws Bug, Exception
    {
    	if(roleNames != null)
    	{
    		List<String> roleNamesList = new ArrayList<String>();
    		roleNamesList.addAll(Arrays.asList(roleNames));
    		logger.info("roleNamesList:" + roleNamesList.size());
    		List<String> keptRoleNamesList = deleteRolesAndReturnExisting(systemUserVO.getUserName(), roleNames, db);
    		logger.info("keptRoleNamesList:" + keptRoleNamesList.size());
    		roleNamesList.removeAll(keptRoleNamesList);
    		roleNames = roleNamesList.toArray(new String[roleNamesList.size()]);
    		logger.info("roleNames:" + roleNames.length);
    	}
		if(groupNames != null)
		{
    		List<String> groupNamesList = new ArrayList<String>();
    		groupNamesList.addAll(Arrays.asList(groupNames));
    		logger.info("groupNamesList:" + groupNamesList.size());
    		List<String> keptGroupNamesList = deleteGroupsAndReturnExisting(systemUserVO.getUserName(), groupNames, db);
    		logger.info("keptGroupNamesList:" + keptGroupNamesList.size());
    		groupNamesList.removeAll(keptGroupNamesList);
    		groupNames = groupNamesList.toArray(new String[groupNamesList.size()]);
    		logger.info("groupNames:" + groupNames.length);
		}
		
		logger.info("roleNames:" + roleNames.length);
		logger.info("groupNames:" + groupNames.length);
		
        SystemUser systemUser = getSystemUserWithName(systemUserVO.getUserName(), db);
        
    	systemUserVO.setUserName(systemUser.getUserName());
		systemUserVO.setPassword(systemUser.getPassword());
		systemUser.setValueObject(systemUserVO);
		
		if(roleNames != null)
		{
            List<String> roleNamesList = Arrays.asList(roleNames);
            Set<String> roleNamesSet = new HashSet<String>(roleNamesList);

            for (String roleName : roleNamesSet)
            {
				try 
				{
					RoleController.getController().addUser(roleName, systemUser.getUserName(), db);
				} 
				catch (PersistenceException e) 
				{
					e.printStackTrace();
				}
            }
		}

    	if(groupNames != null)
		{
            List<String> groupNamesList = Arrays.asList(groupNames);
            Set<String> groupNamesSet = new HashSet<String>(groupNamesList);

			for (String groupName : groupNamesSet)
            {
				try 
				{
					GroupController.getController().addUser(groupName, systemUser.getUserName(), db);
				} 
				catch (PersistenceException e) 
				{
					e.printStackTrace();
				}
            }
		}
		
        return systemUser;
    }     


    public SystemUserVO update(SystemUserVO systemUserVO, String oldPassword, String[] roleNames, String[] groupNames) throws ConstraintException, SystemException
    {
        Database db = CastorDatabaseService.getDatabase();

        SystemUser systemUser = null;

        beginTransaction(db);

        try
        {
            systemUser = update(systemUserVO, oldPassword, roleNames, groupNames, db);
            
            commitTransaction(db);
        }
        catch(ConstraintException ce)
        {
            logger.warn("An error occurred so we should not completes the transaction:" + ce, ce);
            rollbackTransaction(db);
            throw ce;
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }

        return systemUser.getValueObject();
    }        

    public SystemUser update(SystemUserVO systemUserVO, String oldPassword, String[] roleNames, String[] groupNames, Database db) throws ConstraintException, SystemException, Exception
    {
    	if(roleNames != null)
    	{
    		List<String> roleNamesList = new ArrayList<String>();
    		roleNamesList.addAll(Arrays.asList(roleNames));
    		logger.info("roleNamesList:" + roleNamesList.size());
    		List<String> keptRoleNamesList = deleteRolesAndReturnExisting(systemUserVO.getUserName(), roleNames, db);
    		logger.info("keptRoleNamesList:" + keptRoleNamesList.size());
    		roleNamesList.removeAll(keptRoleNamesList);
    		roleNames = roleNamesList.toArray(new String[roleNamesList.size()]);
    		logger.info("roleNames:" + roleNames.length);
    	}
		if(groupNames != null)
		{
    		List<String> groupNamesList = new ArrayList<String>();
    		groupNamesList.addAll(Arrays.asList(groupNames));
    		logger.info("groupNamesList:" + groupNamesList.size());
    		List<String> keptGroupNamesList = deleteGroupsAndReturnExisting(systemUserVO.getUserName(), groupNames, db);
    		logger.info("keptGroupNamesList:" + keptGroupNamesList.size());
    		groupNamesList.removeAll(keptGroupNamesList);
    		groupNames = groupNamesList.toArray(new String[groupNamesList.size()]);
    		logger.info("groupNames:" + groupNames.length);
		}
		
		logger.info("roleNames:" + roleNames.length);
		logger.info("groupNames:" + groupNames.length);
		
    	logger.info("systemUserVO:" + systemUserVO.getUserName());
    	logger.info("oldPassword:" + oldPassword);
    	logger.info("newPassword:" + systemUserVO.getPassword());
    	logger.info("roleNames:" + roleNames);
    	logger.info("groupNames:" + groupNames);
    	if(CmsPropertyHandler.getUsePasswordEncryption())
		{
    		String password = systemUserVO.getPassword();
			try
			{
				byte[] encryptedPassRaw = DigestUtils.sha(password);
				String encryptedPass = new String(new Base64().encode(encryptedPassRaw), "ASCII");
				password = encryptedPass;
				systemUserVO.setPassword(password);
			
				byte[] encryptedOldPasswordRaw = DigestUtils.sha(oldPassword);
				String encryptedOldPassword = new String(new Base64().encode(encryptedOldPasswordRaw), "ASCII");
				oldPassword = encryptedOldPassword;
			}
			catch (Exception e) 
			{
				logger.error("Error generating password:" + e.getMessage());
			}
		}
    	
    	SystemUser systemUser = getSystemUser(db, systemUserVO.getUserName(), oldPassword);
        if(systemUser == null)
        	throw new SystemException("Wrong user or password.");
        
        systemUserVO.setUserName(systemUser.getUserName());
        
		if(roleNames != null)
		{
			for (int i=0; i < roleNames.length; i++)
            {
				RoleController.getController().addUser(roleNames[i], systemUser.getUserName(), db);
            }			
		}
		
		if(groupNames != null)
		{
			for (int i=0; i < groupNames.length; i++)
            {
				logger.info("Adding group:" + groupNames[i]);
				GroupController.getController().addUser(groupNames[i], systemUser.getUserName(), db);
            }
		}
		
		//systemUserVO.setPassword(systemUser.getPassword());
		systemUser.setValueObject(systemUserVO);

        return systemUser;
    }     

    public void updatePassword(String userName) throws ConstraintException, SystemException
    {
        Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
            updatePassword(userName, db);
            
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
    }        

    public void updatePassword(String userName, Database db) throws ConstraintException, SystemException
    {
        SystemUser systemUser = getSystemUserWithName(userName, db);
        
        String newPassword = PasswordGenerator.generate();
        
        String password = newPassword;
		if(CmsPropertyHandler.getUsePasswordEncryption())
		{
			try
			{
				byte[] encryptedPassRaw = DigestUtils.sha(password);
				String encryptedPass = new String(new Base64().encode(encryptedPassRaw), "ASCII");
				password = encryptedPass;
			}
			catch (Exception e) 
			{
				logger.error("Error generating password:" + e.getMessage());
			}
		}

        systemUser.setPassword(password);
		
		StringBuffer sb = new StringBuffer();
		sb.append("<div><h2>Password changed</h2></div>");
		sb.append("<div>CMS notification: You or an administrator have requested a new password for your account (" + userName + "). <br/>");
		sb.append("<br/>");
		sb.append("The new password is '" + newPassword + "'.<br/>");
		sb.append("<br/>");
		sb.append("Please notify the administrator if this does not work. <br/>");
		sb.append("<br/>");
		sb.append("-----------------------------------------------------------------------<br/>");
		sb.append("This email was automatically generated and the sender is the CMS-system. <br/>");
		sb.append("Do not reply to this email. </div>");

		String systemEmailSender = CmsPropertyHandler.getSystemEmailSender();
		if(systemEmailSender == null || systemEmailSender.equalsIgnoreCase(""))
			systemEmailSender = "InfoGlueCMS@" + CmsPropertyHandler.getMailSmtpHost();

		try
		{
			MailServiceFactory.getService().send(systemEmailSender, systemUser.getEmail(), null, "InfoGlue Information - Password changed!!", sb.toString());
		}
		catch(Exception e)
		{
			logger.error("The notification was not sent to [" + systemEmailSender + ", " + systemUser.getEmail() + "]. Reason:" + e.getMessage(), e);
		}
    }        

    public void updateAnonymousPassword(String userName) throws ConstraintException, SystemException
    {
        Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
            updateAnonymousPassword(userName, db);
            
            commitTransaction(db);
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
    }        

    public void updateAnonymousPassword(String userName, Database db) throws ConstraintException, SystemException
    {
        SystemUser systemUser = getSystemUserWithName(userName, db);
        String newPassword = "anonymous";
        
        String password = newPassword;
		if(CmsPropertyHandler.getUsePasswordEncryption())
		{
			try
			{
				byte[] encryptedPassRaw = DigestUtils.sha(password);
				String encryptedPass = new String(new Base64().encode(encryptedPassRaw), "ASCII");
				password = encryptedPass;
			}
			catch (Exception e) 
			{
				logger.error("Error generating password:" + e.getMessage());
			}
		}

        systemUser.setPassword(password);		
    }        

    public void updatePassword(String userName, String oldPassword, String newPassword) throws ConstraintException, SystemException
    {
        Database db = CastorDatabaseService.getDatabase();
        ConstraintExceptionBuffer ceb = new ConstraintExceptionBuffer();

        beginTransaction(db);

        try
        {
            //If any of the validations or setMethods reported an error, we throw them up now before create.
            ceb.throwIfNotEmpty();

            updatePassword(userName, oldPassword, newPassword, db);
            
            commitTransaction(db);
        }
        catch(ConstraintException ce)
        {
            rollbackTransaction(db);
            throw ce;
        }
        catch(Exception e)
        {
            logger.error("An error occurred so we should not completes the transaction:" + e, e);
            rollbackTransaction(db);
            throw new SystemException(e.getMessage());
        }
    }    
    
    public void updatePassword(String userName, String oldPassword, String newPassword, Database db) throws ConstraintException, SystemException, Exception
    {
        if(newPassword == null)
            throw new ConstraintException("SystemUser.newPassword", "301");

		if(CmsPropertyHandler.getUsePasswordEncryption())
		{
			try
			{
				byte[] encryptedPassRaw = DigestUtils.sha(newPassword);
				String encryptedPass = new String(new Base64().encode(encryptedPassRaw), "ASCII");
				newPassword = encryptedPass;

				byte[] encryptedOldPasswordRaw = DigestUtils.sha(oldPassword);
				String encryptedOldPass = new String(new Base64().encode(encryptedOldPasswordRaw), "ASCII");
				oldPassword = encryptedOldPass;
			}
			catch (Exception e) 
			{
				logger.error("Error generating password:" + e.getMessage());
			}
		}

        SystemUser systemUser = getSystemUser(db, userName, oldPassword);
        if(systemUser == null)
            throw new ConstraintException("SystemUser.oldPassword", "310");
            
        systemUser.setPassword(newPassword);		
    }        

  
	/**
	 * This is a method that gives the user back an newly initialized ValueObject for this entity that the controller
	 * is handling.
	 */

	public BaseEntityVO getNewVO()
	{
		return new SystemUserVO();
	}

}
 
