package org.appfuse.webapp.server.locators;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.appfuse.Constants;
import org.appfuse.model.User;
import org.appfuse.service.RoleManager;
import org.appfuse.service.UserExistsException;
import org.appfuse.service.UserManager;
import org.appfuse.webapp.util.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;

@Component
public class UserRequestService extends AbstractBaseLocator<User, Long> {

	
    @Autowired
    private UserManager userManager;
    @Autowired
    private RoleManager roleManager;
    
    //-----------------------
    // Locator Methods
    //-----------------------
    
    public User create(Class<? extends User> clazz) {
        return new User();
    }

    public User find(Class<? extends User> clazz, Long id) {
        return userManager.get(id);
    }

    public Class<User> getDomainType() {
        return User.class;
    }

    public Long getId(User user) {
        return user.getId();
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    public Object getVersion(User user) {
        return user.getVersion();
    }
    
    //-----------------------
    // Application Methods
    //-----------------------
    
    public String getCurrentUserName() {
    	if(SecurityContextHolder.getContext() != null 
    			&& SecurityContextHolder.getContext().getAuthentication() != null
    			&& !(SecurityContextHolder.getContext().getAuthentication() 
    					instanceof AnonymousAuthenticationToken)) 
    	{
    		return SecurityContextHolder.getContext().getAuthentication().getName();
    	}
    	return null;
    }
    /**
     * 
     * @return
     */
    public User getCurrentUser() {
    	String username = getCurrentUserName();
    	if(username != null) {
    		return userManager.getUserByUsername(username);
    	}
    	return null;
    }
    
    /**
     * 
     * @return
     */
    public User signUp() {
    	return new User();
    }

    /**
     * 
     * @param user
     * @return
     * @throws Exception
     */
    public User signUp(User user) throws Exception {
        user.setEnabled(true);
        // Set the default user role on this new user
        user.getRoles().clear();
        user.addRole(roleManager.getRole(Constants.USER_ROLE));

        try {
            userManager.saveUser(user);
        } catch (UserExistsException e) {
        	log.debug(String.format("Trying to duplicate user username=%s, email=%d", user.getUsername(), user.getEmail()), e);
//            errors.rejectValue("username", "errors.existing.user",
//                    new Object[]{user.getUsername(), user.getEmail()}, "duplicate user");
        	throw e;
        }


        // log user in automatically
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), user.getConfirmPassword(), user.getAuthorities());
        auth.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Send user an e-mail
        if (log.isDebugEnabled()) {
            log.debug("Sending user '" + user.getUsername() + "' an account information e-mail");
        }

        // Send an account information e-mail
        Locale locale = LocaleContextHolder.getLocale();
        message.setSubject(getText("signup.email.subject", locale));

        try {
            sendUserMessage(user, "accountCreated.vm", getText("signup.email.message", locale), RequestUtil.getAppURL(getServletRequest()));
        } catch (MailException me) {
            //saveError(request, me.getMostSpecificCause().getMessage());
        	throw me;
        }
        
        return user;
    }

    
    /**
     * 
     * @return
     */
    @PreAuthorize("isAuthenticated()")
    public User editProfile() {
    	String username = getCurrentUsername();
    	User user = userManager.getUserByUsername(username);
    	return user;
    }
    
    /**
     * 
     * @param user
     * @return
     * @throws Exception
     */
    @PreAuthorize("isFullyAuthenticated()")
    public User editProfile(User user) throws Exception {
    	String username = getCurrentUsername();
    	if(!username.equals(user.getUsername())) {
    		throw new AccessDeniedException("Trying to edit another users profile");
    	}
    	return userManager.saveUser(user);
    }
    
    /**
     * 
     * @param userId
     * @return
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public User getUser(Long userId) {
    	return userManager.get(userId);
    }
    
    /**
     * 
     * @param user
     * @return
     * @throws Exception
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public User saveUser(User user) throws Exception {
    	return userManager.saveUser(user);
    }
    
    /**
     * 
     * @param searchCriteria
     * @return
     */
    public long countUsers(UsersSearchCriteria searchCriteria) {
    	String searchTerm = searchCriteria != null? searchCriteria.getSearchTerm() : null;
   		return userManager.search(searchTerm).size();
    }

    /**
     * 
     * @param searchCriteria
     * @param firstResult
     * @param maxResults
     * @return
     */
    public List<User> searchUsers(UsersSearchCriteria searchCriteria, int firstResult, int maxResults){
    	String searchTerm = searchCriteria != null? searchCriteria.getSearchTerm() : null;
    	List<User> users = userManager.search(searchTerm);
    	int fromIndex = Math.min(firstResult, users.size() -1);
    	int toIndex = Math.min(fromIndex + maxResults, users.size() -1);
    	return users.subList(fromIndex, toIndex);
    }

    /**
     * 
     * @param user
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void removeUser(User user) {
    	userManager.removeUser(user.getId().toString());
    }
    
    /**
     * 
     */
    public boolean logout() {
    	HttpServletRequest request = getServletRequest();
    	HttpServletResponse response = getServletResponse();
    	if (request.getSession(false) != null) {
    	    request.getSession(false).invalidate();
    	}
    	Cookie terminate = new Cookie(TokenBasedRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY, null);
    	String contextPath = request.getContextPath();
    	terminate.setPath(contextPath != null && contextPath.length() > 0 ? contextPath : "/");
    	terminate.setMaxAge(0);
    	response.addCookie(terminate);
    	return true;
    }
}
