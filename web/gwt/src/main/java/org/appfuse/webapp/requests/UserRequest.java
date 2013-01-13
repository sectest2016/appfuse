package org.appfuse.webapp.requests;

import java.util.List;

import org.appfuse.webapp.proxies.UserProxy;
import org.appfuse.webapp.proxies.UsersSearchCriteriaProxy;
import org.appfuse.webapp.server.GwtServiceLocator;
import org.appfuse.webapp.server.locators.UserRequestService;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

@Service(value = UserRequestService.class, locator = GwtServiceLocator.class)
public interface UserRequest extends RequestContext {

	abstract Request<UserProxy> getCurrentUser();
	
    abstract Request<UserProxy> signUp();

    abstract Request<UserProxy> signUp(UserProxy user);

    abstract Request<UserProxy> editProfile();

    abstract Request<UserProxy> editProfile(UserProxy user);

    abstract Request<UserProxy> getUser(Long userId);

    abstract Request<UserProxy> saveUser(UserProxy user);

    abstract Request<Long> countUsers(UsersSearchCriteriaProxy searchCriteria);

    abstract Request<List<UserProxy>> searchUsers(UsersSearchCriteriaProxy searchCriteria, int firstResult, int maxResults);

    abstract Request<Void> removeUser(UserProxy user);
    
    abstract Request<Boolean> logout();

}
