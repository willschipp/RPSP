package com.emc.rpsp.users.service.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emc.rpsp.accounts.domain.Account;
import com.emc.rpsp.accounts.repository.AccountRepository;
import com.emc.rpsp.infra.common.auth.domain.AbstractCurrentUser;
import com.emc.rpsp.packages.domain.PackageDefinition;
import com.emc.rpsp.users.domain.User;
import com.emc.rpsp.users.repository.UserRepository;
import com.emc.rpsp.users.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@PersistenceContext(unitName = "rpsp")
	@Qualifier("entityManagerFactory")
	private EntityManager entityManager;

	@Value("${rpsp.admin.login}")
	private String adminLogin;

	@Value("${rpsp.admin.password}")
	private String adminPaswword;

	@PostConstruct
	public void init() {
		User existingAdmin = userRepository.findOneByLogin(adminLogin);
		if (existingAdmin == null) {
			User newAdmin = new User();
			newAdmin.setLogin(adminLogin);
			newAdmin.setEncodedPassword(adminPaswword);
			newAdmin.setPermission("ADMIN");
			newAdmin.setFirstName("admin");
			newAdmin.setLastName("admin");
			updateAuditFields(newAdmin);
			userRepository.save(newAdmin);
		}
	}

	@Override
	public List<User> findUsers() {
		List<User> users = userRepository.findAll();
		setAdditionalValues(users);
		return users;
	}

	@Override
	public AbstractCurrentUser findCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		AbstractCurrentUser currentUser = (AbstractCurrentUser) auth
				.getPrincipal();
		return currentUser;
	}

	@Override
	public User findUser(Long id) {
		User user = userRepository.findOne(id);
		user.setAdditionalValues();
		return user;
	}

	@Override
	@Transactional("transactionManager")
	public User createUser(User user) {
		updateAuditFields(user);
		user.setEncodedPassword(user.getPassword());
		user.setPermission("USER");

		entityManager.persist(user);
		entityManager.flush();
		Account account = accountRepository.findOne(user.getTenantId());
		user.setAccount(account);
		User createdUser = entityManager.merge(user);
		entityManager.flush();
		return createdUser;
	}

	/*
	 * @Override public User createUser(User user, Long accountId) {
	 * updateAuditFields(user); user.setEncodedPassword(user.getPassword());
	 * user.setPermission("USER"); User createdUser = userRepository.save(user);
	 * return createdUser; }
	 */

	@Override
	public User updateUser(User user) {
		User existingUser = userRepository.findOne(user.getId());
		if (existingUser == null) {
			return null;
		}
		if (user.getLogin() != null) {
			existingUser.setLogin(user.getLogin());
		}
		if (user.getPassword() != null) {
			existingUser.setEncodedPassword(user.getPassword());
		}
		if (user.getFirstName() != null) {
			existingUser.setFirstName(user.getFirstName());
		}
		if (user.getLastName() != null) {
			existingUser.setLastName(user.getLastName());
		}
		if (user.getEmail() != null) {
			existingUser.setEmail(user.getEmail());
		}
		User updatedUser = userRepository.save(existingUser);
		return updatedUser;
	}

	@Override
	public void deleteUser(Long id) {
		userRepository.delete(id);
	}

	@Override
	public User findUserByLogin(String login) {
		User user = userRepository.findOneByLogin(login);
		if(user != null){
			user.setAdditionalValues();
		}
		return user;
	}

	public String getAdminLogin() {
		return adminLogin;
	}

	public void setAdminLogin(String adminLogin) {
		this.adminLogin = adminLogin;
	}

	public String getAdminPaswword() {
		return adminPaswword;
	}

	public void setAdminPaswword(String adminPaswword) {
		this.adminPaswword = adminPaswword;
	}

	void updateAuditFields(User user) {
		user.setCreatedBy("Anonimous");
		user.setCreatedDate(new DateTime());
	}
	
	private void setAdditionalValues(List<User> users){
		if(users != null){
			for(User currPackageDefinition : users){
				currPackageDefinition.setAdditionalValues();
			}
		}
	}

}
