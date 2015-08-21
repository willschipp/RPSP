package com.emc.rpsp.infra.common.auth.domain;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import com.emc.rpsp.accounts.domain.Account;
import com.emc.rpsp.accounts.domain.AccountConfig;
import com.emc.rpsp.rpsystems.SystemSettings;
import com.emc.rpsp.users.domain.User;
import com.emc.rpsp.vms.domain.VmOwnership;

public abstract class AbstractCurrentUser extends org.springframework.security.core.userdetails.User {


    public AbstractCurrentUser(String username, String password,
			                        	Collection<? extends GrantedAuthority> authorities) {
		super(username, password, authorities);
	}
    
	public abstract User getUser();
    public abstract Account getAccount();
/*    public abstract List<SystemSettings> getSystemSettings();
    public abstract List<VmOwnership> getVms();
    public abstract List<AccountConfig> getAccountConfig();*/
    

}
