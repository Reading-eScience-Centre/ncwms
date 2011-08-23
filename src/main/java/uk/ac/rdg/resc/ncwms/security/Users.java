/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.security;

import java.io.Serializable;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

/**
 * Contains details of the users that are allowed to access the ncWMS application.
 * This bean is instantiated by Acegi, through the applicationContext.xml file.
 * The admin account is activated (and the password is set) when the Config object
 * is created in the DispatcherServlet (through WMS-servlet.xml).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class Users implements UserDetailsService, Serializable
{
    private AdminUser adminUser = new AdminUser();
    
    /**
     * Required by the UserDetailsService interface
     */
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException, DataAccessException
    {
        if (username.equals("admin"))
        {
            return this.adminUser;
        }
        throw new UsernameNotFoundException(username);
    }
    
    /**
     * Sets the admin password.  This is only called from
     * {@link uk.ac.rdg.resc.ncwms.config.Config#setApplicationContext}
     * and should not be called separately.
     */
    public void setAdminPassword(String password)
    {
        this.adminUser.password = password;
    }
    
    /**
     * Class to describe the admin user: has the username "admin"
     */
    private class AdminUser implements UserDetails
    {
        private String password = null;
        
        /**
         * Account is enabled once the admin password is set
         */
        public boolean isEnabled()
        {
            return this.password != null;
        }
        
        public boolean isCredentialsNonExpired()
        {
            return true;
        }
        
        public boolean isAccountNonLocked()
        {
            return true;
        }
        
        public boolean isAccountNonExpired()
        {
            return true;
        }
        
        public String getUsername()
        {
            return "admin";
        }
        
        public String getPassword()
        {
            return this.password;
        }
        
        /**
         * @return a single GrantedAuthority called "ROLE_ADMIN"
         */
        public GrantedAuthority[] getAuthorities()
        {
            return new GrantedAuthority[]
            {
                new GrantedAuthority()
                {
                    public String getAuthority()
                    {
                        // This string must match up with the roles in the
                        // filterInvocationInterceptor in applicationContext.xml
                        return "ROLE_ADMIN";
                    }
                }
            };
        }
        
    }
    
}
